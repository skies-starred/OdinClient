package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.loreString
import com.odtheking.odin.utils.modMessage
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import org.lwjgl.glfw.GLFW
import starred.skies.odin.helpers.croesus.ChestInfo
import starred.skies.odin.helpers.croesus.ChestParseResult
import starred.skies.odin.helpers.croesus.CroesusParser
import starred.skies.odin.helpers.croesus.PriceClient
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.guiClick

/**
 * Auto Croesus — highlights unclaimed runs on the top-level Croesus screen,
 * shows a per-chest cost / value / profit overlay in any run sub-screen, and
 * (with the module enabled) auto-claims the best chest of every unclaimed run
 * via the [CLAIM_KEY] keybind.
 *
 * Driver flow:
 *  - In a run sub-screen, press [CLAIM_KEY] -> claims the highest-profit chest.
 *  - In the top-level Croesus list, press [CLAIM_KEY] -> walks every unclaimed
 *    run on the current page (open run -> claim best -> back out -> repeat),
 *    advancing to the next page when the current one is exhausted.
 *
 * Deliberately omitted vs. the upstream COP implementation:
 *  - Kismet reroll layer
 *  - In-run chain claim (multi-chest per single run)
 *  - Loot log + persisted loot summary
 *  - Always-buy / worthless item lists
 *  - User-configurable settings (everything below is a hardcoded constant).
 *
 * Original implementation in COP (GPL-3.0); contributed here under BSD-3-Clause
 * by the sole author.
 */
object AutoCroesus : Module(
    name = "Auto Croesus",
    description = "Highlights unclaimed Croesus runs, shows per-chest profit, and (with the module on) auto-claims the best chest of every unclaimed run via the R key.",
    category = Skit.CHEATS,
) {
    // -- Hardcoded constants ---------------------------------------------------

    /** ARGB. Purple, ~70% alpha — outline drawn around runs with unopened chests. */
    private const val UNCLAIMED_COLOUR_ARGB: Int = 0xB3800080.toInt()
    /** ARGB. Yellow, ~85% alpha — outline drawn around the best-profit chest icon. */
    private const val BEST_COLOUR_ARGB: Int = 0xD9FFFF00.toInt()
    private const val BORDER_WIDTH: Int = 2
    /** Re-parse the open chest GUI every N ticks. */
    private const val REFRESH_TICKS: Int = 5
    /** Refuse to auto-claim if the best chest's profit is below this (raw coins). */
    private const val MIN_PROFIT: Int = 0
    /** Abort an in-flight claim if the next screen doesn't appear within N ticks (20 = 1s). */
    private const val CLAIM_TIMEOUT_TICKS: Int = 60
    /** Ticks of padding between server actions in multi-run mode. */
    private const val MULTI_RUN_PACING_TICKS: Long = 6L
    /** R — keybind that triggers single-run or multi-run claim depending on
     *  which Croesus screen is currently open. */
    private const val CLAIM_KEY: Int = GLFW.GLFW_KEY_R
    /** Safety cap on "Next Page" clicks in a single multi-run cycle. */
    private const val MAX_PAGES_PER_CYCLE: Int = 5

    // -- Parser cache ----------------------------------------------------------

    @Volatile private var lastChests: List<ChestParseResult> = emptyList()
    private var cachedContainerId: Int = -1
    private var ticksSinceParse = 0

    // -- Driver state machine --------------------------------------------------

    private enum class ClaimState {
        IDLE,
        /** Sent chest-tier click; waiting for buy-confirm screen to open. */
        AWAIT_CONFIRM,
        /** Sent buy click; waiting for next screen (or full menu close, in
         *  multi-run mode where Hypixel sometimes closes everything). */
        AWAIT_AFTER_BUY,
        /** Multi-run: clicked an unclaimed run icon; waiting for the run
         *  sub-screen to open. */
        AWAIT_RUN_SCREEN,
        /** Multi-run: in a freshly-opened run sub-screen; waiting for parser
         *  data, then auto-claims the best chest. */
        AWAIT_RUN_CLAIM,
        /** Multi-run: post-back-out or post-NPC-reopen; waiting for Croesus
         *  list to populate before scanning for the next unclaimed run. */
        AWAIT_CROESUS_LIST,
    }
    @Volatile private var claimState = ClaimState.IDLE
    private var monotonicTick = 0L
    private var claimDeadlineTick = 0L
    private var pendingTier = ""
    private var multiRunChestsThisCycle = 0
    private var multiRunRunsThisCycle = 0
    private var noScreenSinceTick = 0L
    private var croesusReadyAtTick = 0L
    private var pagesVisitedThisCycle = 0
    /** True while a multi-run cycle is active (set when the user presses the
     *  key in the Croesus list; cleared by [resetCycle]). */
    private var multiRunActive = false

    init {
        //~ if >=1.21.11 'GuiEvent.Close' -> 'ScreenEvent.Close'
        on<ScreenEvent.Close> {
            reset()
            if (claimState == ClaimState.AWAIT_CONFIRM) {
                modMessage("§7AutoCroesus: GUI closed mid-claim — state reset.")
                resetCycle()
            }
        }

        //~ if >=1.21.11 'GuiEvent.Open' -> 'ScreenEvent.Open'
        on<ScreenEvent.Open> {
            reset()
            when (claimState) {
                ClaimState.AWAIT_CONFIRM      -> handleConfirmOpen(screen)
                ClaimState.AWAIT_AFTER_BUY    -> handleAfterBuyOpen(screen)
                ClaimState.AWAIT_RUN_SCREEN   -> handleRunScreenOpen(screen)
                ClaimState.AWAIT_CROESUS_LIST -> handleCroesusListOpen(screen)
                ClaimState.AWAIT_RUN_CLAIM,
                ClaimState.IDLE -> { /* nothing to do — TickEvent handles these */ }
            }
        }

        on<TickEvent.Start> {
            monotonicTick++
            if (claimState != ClaimState.IDLE && monotonicTick >= claimDeadlineTick) {
                modMessage("§cAutoCroesus: timeout (state=$claimState) — aborting.")
                resetCycle()
            }

            // Multi-run: after a buy, Hypixel fully closes the menu. Watch for
            // mc.screen staying null for [MULTI_RUN_PACING_TICKS] before
            // re-interacting with the Croesus NPC.
            if (claimState == ClaimState.AWAIT_AFTER_BUY && multiRunActive) {
                if (mc.screen == null) {
                    if (noScreenSinceTick == 0L) noScreenSinceTick = monotonicTick
                    else if (monotonicTick - noScreenSinceTick >= MULTI_RUN_PACING_TICKS) {
                        noScreenSinceTick = 0L
                        tryReopenCroesus()
                    }
                } else {
                    noScreenSinceTick = 0L
                }
            } else {
                noScreenSinceTick = 0L
            }

            val screen = mc.screen as? AbstractContainerScreen<*>
            if (screen == null) { reset(); return@on }

            // Kick the bulk refresh in the background.
            PriceClient.refreshIfStale()

            // Multi-run polling: wait for slot data to settle, then advance.
            if (claimState == ClaimState.AWAIT_CROESUS_LIST &&
                CroesusParser.inCroesusMenu(screen)) {
                val slot4 = screen.menu.slots.getOrNull(4)?.item?.isEmpty == false
                val slot49 = screen.menu.slots.getOrNull(49)?.item?.isEmpty == false
                val populated = slot4 && slot49
                if (populated) {
                    if (croesusReadyAtTick == 0L) croesusReadyAtTick = monotonicTick
                    if (monotonicTick - croesusReadyAtTick >= MULTI_RUN_PACING_TICKS) {
                        val unclaimed = CroesusParser.findUnclaimedRunSlots(screen.menu)
                        if (unclaimed.isNotEmpty()) {
                            clickUnclaimedRun(unclaimed.first())
                        } else if (tryAdvancePage(screen)) {
                            croesusReadyAtTick = 0L
                        } else {
                            completeMultiRun()
                        }
                        if (claimState != ClaimState.IDLE) croesusReadyAtTick = 0L
                    }
                } else {
                    croesusReadyAtTick = 0L
                }
            }

            if (CroesusParser.inRunMenu(screen)) {
                val cid = screen.menu.containerId
                if (cid != cachedContainerId) {
                    lastChests = emptyList()
                    cachedContainerId = cid
                    ticksSinceParse = REFRESH_TICKS
                }
                ticksSinceParse++
                if (lastChests.isEmpty() || ticksSinceParse >= REFRESH_TICKS) {
                    lastChests = CroesusParser.parseChests(screen.menu)
                    ticksSinceParse = 0
                }

                // Multi-run: in a freshly-opened run sub-screen, kick off the
                // claim as soon as parser data is ready.
                if (claimState == ClaimState.AWAIT_RUN_CLAIM && lastChests.isNotEmpty()) {
                    claimState = ClaimState.IDLE
                    tryStartClaim(screen, fromMultiRun = true)
                }
            } else if (lastChests.isNotEmpty()) {
                lastChests = emptyList()
            }
        }

        //~ if >=1.21.11 'GuiEvent.DrawSlot' -> 'GuiEvent.RenderSlot'
        on<GuiEvent.RenderSlot> {
            val screen = mc.screen as? AbstractContainerScreen<*> ?: return@on
            when {
                CroesusParser.inCroesusMenu(screen) -> {
                    val stack = slot.item.takeUnless { it.isEmpty } ?: return@on
                    val hasMarker = stack.loreString.any { CroesusParser.LORE_UNCLAIMED_MARKER in it }
                    if (hasMarker) drawSlotOutline(guiGraphics, slot.x, slot.y, UNCLAIMED_COLOUR_ARGB, BORDER_WIDTH)
                }
                CroesusParser.inRunMenu(screen) -> {
                    val bestSlot = lastChests
                        .filterIsInstance<ChestParseResult.Success>()
                        .maxByOrNull { it.chest.profit }
                        ?.chest?.slot ?: return@on
                    if (slot.index == bestSlot) {
                        drawSlotOutline(guiGraphics, slot.x, slot.y, BEST_COLOUR_ARGB, BORDER_WIDTH)
                    }
                }
            }
        }

        //~ if >=1.21.11 'GuiEvent.Draw' -> 'GuiEvent.Render'
        on<GuiEvent.Render> {
            val screen = mc.screen as? AbstractContainerScreen<*> ?: return@on
            if (!CroesusParser.inRunMenu(screen)) return@on
            if (lastChests.isEmpty()) return@on
            renderProfitOverlay(guiGraphics)
        }

        //~ if >=1.21.11 'GuiEvent.KeyPress' -> 'ScreenEvent.KeyPress'
        on<ScreenEvent.KeyPress> {
            if (input.key != CLAIM_KEY) return@on
            val containerScreen = screen as? AbstractContainerScreen<*> ?: return@on

            if (claimState != ClaimState.IDLE) {
                modMessage("§cAutoCroesus: already claiming (state=$claimState) — wait or close GUI.")
                cancel()
                return@on
            }
            when {
                CroesusParser.inRunMenu(containerScreen)     -> tryStartClaim(containerScreen, fromMultiRun = false)
                CroesusParser.inCroesusMenu(containerScreen) -> tryStartMultiRun()
                else -> modMessage("§cAutoCroesus: claim key works only in the Croesus list or a run sub-screen.")
            }
            cancel()
        }
    }

    // -- ScreenEvent.Open dispatch handlers ------------------------------------

    private fun handleConfirmOpen(screen: Screen) {
        if (!CroesusParser.inBuyConfirmMenu(screen)) {
            val title = (screen as? AbstractContainerScreen<*>)?.title?.string ?: "?"
            modMessage("§cAutoCroesus: aborted — expected buy-confirm, got \"$title\".")
            resetCycle()
            return
        }
        val container = screen as AbstractContainerScreen<*>
        if (!clickSlotIn(container, CroesusParser.BUY_CONFIRM_SLOT)) {
            modMessage("§cAutoCroesus: buy-confirm opened but click failed.")
            resetCycle()
            return
        }
        multiRunChestsThisCycle++
        modMessage("§a✓ AutoCroesus: bought §r${pendingTier}§a chest.")
        if (multiRunActive) {
            claimState = ClaimState.AWAIT_AFTER_BUY
            claimDeadlineTick = monotonicTick + (CLAIM_TIMEOUT_TICKS * 2).toLong()
        } else {
            claimState = ClaimState.IDLE
        }
    }

    private fun handleAfterBuyOpen(screen: Screen) {
        val containerScreen = screen as? AbstractContainerScreen<*> ?: return
        when {
            CroesusParser.inRunMenu(containerScreen) -> {
                // After buying we always leave the run (no in-run chain claim).
                clickGoBackToList()
            }
            CroesusParser.inCroesusMenu(containerScreen) -> {
                claimState = ClaimState.AWAIT_CROESUS_LIST
                claimDeadlineTick = monotonicTick + (CLAIM_TIMEOUT_TICKS * 2).toLong()
            }
            else -> {
                modMessage("§cAutoCroesus: aborted after buy — unexpected container \"${containerScreen.title.string}\".")
                resetCycle()
            }
        }
    }

    private fun handleRunScreenOpen(screen: Screen) {
        if (!CroesusParser.inRunMenu(screen)) {
            val title = (screen as? AbstractContainerScreen<*>)?.title?.string ?: "?"
            modMessage("§cAutoCroesus: aborted — expected run sub-screen, got \"$title\".")
            resetCycle()
            return
        }
        claimState = ClaimState.AWAIT_RUN_CLAIM
        claimDeadlineTick = monotonicTick + (CLAIM_TIMEOUT_TICKS * 2).toLong()
    }

    private fun handleCroesusListOpen(screen: Screen) {
        if (!CroesusParser.inCroesusMenu(screen)) {
            val title = (screen as? AbstractContainerScreen<*>)?.title?.string ?: "?"
            modMessage("§cAutoCroesus: aborted — expected Croesus list, got \"$title\".")
            resetCycle()
            return
        }
        claimDeadlineTick = monotonicTick + (CLAIM_TIMEOUT_TICKS * 2).toLong()
    }

    // -- Multi-run helpers -----------------------------------------------------

    private fun clickGoBackToList() {
        val screen = mc.screen as? AbstractContainerScreen<*> ?: run {
            modMessage("§cAutoCroesus: no screen to click Go Back on.")
            resetCycle(); return
        }
        if (!clickSlotIn(screen, CroesusParser.RUN_BACK_SLOT)) {
            modMessage("§cAutoCroesus: failed to click Go Back.")
            resetCycle()
            return
        }
        claimState = ClaimState.AWAIT_CROESUS_LIST
        claimDeadlineTick = monotonicTick + (CLAIM_TIMEOUT_TICKS * 2).toLong()
    }

    private fun resetCycle() {
        claimState = ClaimState.IDLE
        multiRunChestsThisCycle = 0
        multiRunRunsThisCycle = 0
        noScreenSinceTick = 0L
        croesusReadyAtTick = 0L
        pagesVisitedThisCycle = 0
        multiRunActive = false
    }

    private fun tryReopenCroesus() {
        val entity = findCroesusEntity()
        if (entity == null) {
            completeMultiRun()
            return
        }
        val player = mc.player ?: return
        mc.gameMode?.interact(player, entity, InteractionHand.MAIN_HAND)
        claimState = ClaimState.AWAIT_CROESUS_LIST
        claimDeadlineTick = monotonicTick + (CLAIM_TIMEOUT_TICKS * 3).toLong()
    }

    private fun findCroesusEntity(): Entity? {
        val player = mc.player ?: return null
        val world = mc.level ?: return null
        val playerPos = player.position()
        return world.entitiesForRendering()
            .asSequence()
            .filter { e ->
                if (e === player) return@filter false
                if (e.position().distanceTo(playerPos) > 6.0) return@filter false
                val custom = e.customName?.string ?: ""
                val hover = e.name.string
                "Croesus" in custom || "Croesus" in hover
            }
            .minByOrNull { it.position().distanceTo(playerPos) }
    }

    private fun tryStartMultiRun() {
        multiRunChestsThisCycle = 0
        multiRunRunsThisCycle = 0
        pagesVisitedThisCycle = 0
        multiRunActive = true
        modMessage("§aAutoCroesus: starting multi-run cycle…")
        claimState = ClaimState.AWAIT_CROESUS_LIST
        claimDeadlineTick = monotonicTick + (CLAIM_TIMEOUT_TICKS * 2).toLong()
    }

    private fun clickUnclaimedRun(slot: Int): Boolean {
        val screen = mc.screen as? AbstractContainerScreen<*> ?: return false
        if (!clickSlotIn(screen, slot)) {
            modMessage("§cAutoCroesus: failed to click run slot $slot.")
            resetCycle()
            return false
        }
        multiRunRunsThisCycle++
        claimState = ClaimState.AWAIT_RUN_SCREEN
        claimDeadlineTick = monotonicTick + (CLAIM_TIMEOUT_TICKS * 2).toLong()
        return true
    }

    private fun tryAdvancePage(screen: AbstractContainerScreen<*>): Boolean {
        if (pagesVisitedThisCycle >= MAX_PAGES_PER_CYCLE) return false
        val stack = screen.menu.slots.getOrNull(53)?.item ?: return false
        if (stack.isEmpty) return false
        val name = stack.hoverName.string
        if (!name.contains("Next Page", ignoreCase = true)) return false
        if (!clickSlotIn(screen, 53)) return false
        pagesVisitedThisCycle++
        modMessage("§7AutoCroesus: page exhausted, advancing to page §f${pagesVisitedThisCycle + 1}§7…")
        return true
    }

    private fun completeMultiRun() {
        modMessage(
            "§aMulti-run complete — bought §f$multiRunChestsThisCycle§a chest" +
                (if (multiRunChestsThisCycle == 1) "" else "s") + " across " +
                "§f$multiRunRunsThisCycle§a run" +
                (if (multiRunRunsThisCycle == 1) "" else "s") + "."
        )
        resetCycle()
    }

    /** Pick the best chest and click it. In multi-run mode, a sub-threshold
     *  best chest backs out of the run instead of complaining. */
    private fun tryStartClaim(screen: AbstractContainerScreen<*>, fromMultiRun: Boolean) {
        if (!CroesusParser.inRunMenu(screen)) {
            if (!fromMultiRun) modMessage("§cAutoCroesus: claim key only works inside a run sub-screen.")
            return
        }
        val candidates = lastChests
            .filterIsInstance<ChestParseResult.Success>()
            .map { it.chest }
        if (candidates.isEmpty()) {
            if (!fromMultiRun) modMessage("§cAutoCroesus: no chests parsed yet — wait a moment for the overlay.")
            return
        }
        val best: ChestInfo = candidates.maxByOrNull { it.profit }!!
        if (best.profit < MIN_PROFIT) {
            if (fromMultiRun) {
                modMessage(
                    "§7Run done — best (§r${best.tierColourCode}${best.tierName}§7) profit " +
                        "§f${PriceClient.formatPrice(best.profit)}§7 below threshold; backing out."
                )
                clickGoBackToList()
            } else {
                modMessage(
                    "§cAutoCroesus: best chest profit §f${PriceClient.formatPrice(best.profit)}§c " +
                        "below threshold §f${PriceClient.formatPrice(MIN_PROFIT.toDouble())}§c — refusing."
                )
            }
            return
        }
        if (!clickSlotIn(screen, best.slot)) {
            modMessage("§cAutoCroesus: click failed.")
            return
        }
        claimState = ClaimState.AWAIT_CONFIRM
        claimDeadlineTick = monotonicTick + CLAIM_TIMEOUT_TICKS.toLong()
        pendingTier = "${best.tierColourCode}${best.tierName}"
        modMessage(
            "§aAutoCroesus: claiming ★ §r${pendingTier}§a chest §7(profit ${PriceClient.formatPrice(best.profit)})."
        )
    }

    // -- Rendering -------------------------------------------------------------

    private fun reset() {
        lastChests = emptyList()
        cachedContainerId = -1
        ticksSinceParse = 0
    }

    private fun drawSlotOutline(ctx: GuiGraphics, x: Int, y: Int, colour: Int, bw: Int) {
        ctx.fill(x - bw, y - bw, x + 16 + bw, y, colour)
        ctx.fill(x - bw, y + 16, x + 16 + bw, y + 16 + bw, colour)
        ctx.fill(x - bw, y, x, y + 16, colour)
        ctx.fill(x + 16, y, x + 16 + bw, y + 16, colour)
    }

    private fun renderProfitOverlay(ctx: GuiGraphics) {
        val font = mc.font
        val x = 4
        var y = 4
        val bestSlot = lastChests
            .filterIsInstance<ChestParseResult.Success>()
            .maxByOrNull { it.chest.profit }
            ?.chest?.slot
        val lines = buildList<Component> {
            for (result in lastChests) {
                when (result) {
                    is ChestParseResult.Success -> {
                        val c = result.chest
                        val profitColour = if (c.profit >= 0) "§a+" else "§c"
                        val marker = if (c.slot == bestSlot) "§e§l★ " else "  "
                        add(Component.literal(
                            "$marker${c.tierColourCode}${c.tierName} Chest §7(${PriceClient.formatPrice(c.cost)}) " +
                                "$profitColour${PriceClient.formatPrice(c.profit)}"
                        ))
                        for (item in c.items.take(6)) {
                            val v = item.unitValue * item.qty
                            val vColour = if (v > 0) "§a" else "§7"
                            val name = item.displayName.removePrefix("§5§o")
                            add(Component.literal("    $name $vColour${PriceClient.formatPrice(v)}"))
                        }
                        if (c.items.size > 6) add(Component.literal("    §7… +${c.items.size - 6} more"))
                        add(Component.literal(""))
                    }
                    is ChestParseResult.Failure -> {
                        add(Component.literal("  §c${result.tierName} Chest §7(${result.reason})"))
                        add(Component.literal(""))
                    }
                }
            }
        }
        if (lines.isEmpty()) return
        val maxWidth = lines.maxOf { font.width(it) }
        val totalHeight = lines.size * (font.lineHeight + 1) + 6
        ctx.fill(x - 2, y - 2, x + maxWidth + 4, y + totalHeight, 0xC0000000.toInt())
        for (line in lines) {
            ctx.drawString(font, line, x, y, 0xFFFFFFFF.toInt(), false)
            y += font.lineHeight + 1
        }
    }

    private fun clickSlotIn(screen: AbstractContainerScreen<*>, slot: Int): Boolean {
        val containerId = screen.menu.containerId
        guiClick(containerId, slot)
        return true
    }
}
