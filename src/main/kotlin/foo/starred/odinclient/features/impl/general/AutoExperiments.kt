package foo.starred.odinclient.features.impl.general

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.hasGlint
import com.odtheking.odin.utils.noControlCodes
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.gui.MinecraftGui
import foo.starred.odinclient.events.TickStartEvent
import foo.starred.odinclient.api.category.OdinClientCategory
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Items
import java.util.concurrent.ConcurrentHashMap

object AutoExperiments : Module(
    name = "Auto Experiments",
    description = "Automatically click on the Chronomatron and Ultrasequencer experiments.",
    category = OdinClientCategory.CHEATS
) {
    //~ if >= 26.2 '100, 1000' -> '100..1000'
    private val clickDelay by NumberSetting("Click Delay", 200, 100, 1000, 10, unit = "ms", desc = "Time in ms between automatic test clicks.")
    //~ if >= 26.2 '0, 1000' -> '0..1000'
    private val delayVariety by NumberSetting("Delay variety", 50, 0, 1000, 10, unit = "ms", desc = "Variance in delays")
    private val autoClose by BooleanSetting("Auto Close", true, desc = "Automatically close the GUI after completing the experiment.")
    //~ if >= 26.2 '0, 3' -> '0..3'
    private val serumCount by NumberSetting("Serum Count", 0, 0, 3, 1, desc = "Consumed Metaphysical Serum count.")
    private val getMaxXp by BooleanSetting("Get Max XP", false, desc = "Solve Chronomatron to 15 and Ultrasequencer to 20 for max XP.")

    private var handler: ExperimentHandler? = null
    private var lastClick: Long = 0

    init {
        on<ScreenEvent.Open> {
            val title = screen.title.string

            handler = when {
                title.startsWith("Chronomatron (") -> ChronomatronHandler()
                title.startsWith("Ultrasequencer (") -> UltrasequencerHandler()
                else -> null
            }
        }

        on<ScreenEvent.MouseClick> {
            if (handler == null) return@on
            if (MinecraftGui.screen !is AbstractContainerScreen<*>) return@on

            cancel()
        }

        on<ScreenEvent.MouseRelease> {
            if (handler == null) return@on
            if (MinecraftGui.screen !is AbstractContainerScreen<*>) return@on

            cancel()
        }

        onReceive<ClientboundContainerSetSlotPacket> {
            handler?.onSlotUpdate()
        }

        on<TickStartEvent> {
            val handler = handler ?: return@on
            val screen = MinecraftGui.container ?: return@on

            val now = System.currentTimeMillis()
            if (now - lastClick < delay()) return@on

            handler.nextClick()?.let { slotId ->
                MinecraftGui.click(screen.menu.containerId, slotId, clickType = ContainerInput.CLONE)
                lastClick = now
            }

            if (!handler.shouldClose(autoClose)) return@on

            client.player?.closeContainer()
            AutoExperiments.handler = null
        }
    }

    private class ChronomatronHandler : ExperimentHandler() {
        private val order = mutableListOf<Int>()
        private var lastAddedSlot = -1
        private var close = false

        override fun onSlotUpdate() {
            val slots = MinecraftGui.container?.menu?.slots ?: return
            val center = slots[49].item

            if (
                lastAddedSlot != -1 &&
                center.item == Items.GLOWSTONE &&
                !slots[lastAddedSlot].item.hasGlint()
            ) {
                close = order.size > if (getMaxXp) 15 else 11 - serumCount
                hasData = false
                return
            }

            if (hasData || center.item != Items.CLOCK) return

            val slot = slots.firstOrNull { it.index in 10..43 && it.item.hasGlint() } ?: return

            order.add(slot.index)
            lastAddedSlot = slot.index
            hasData = true
            clicks = 0
        }

        override fun nextClick(): Int? = if (hasData && clicks < order.size) order[clicks++] else null

        override fun shouldClose(autoClose: Boolean): Boolean {
            if (!autoClose || !close) return false
            if (clicks < order.size) return false

            close = false
            return true
        }
    }

    private class UltrasequencerHandler : ExperimentHandler() {
        private val order = ConcurrentHashMap<Int, Int>()

        override fun onSlotUpdate() {
            val slots = MinecraftGui.container?.menu?.slots ?: return
            val center = slots[49].item

            if (center.item == Items.CLOCK) {
                hasData = false
                return
            }

            if (hasData || center.item != Items.GLOWSTONE) return

            order.clear()

            for (slot in slots) {
                if (slot.index in 9..44 && slot.item.hoverName.string.noControlCodes.matches(Regex("\\d+"))) order[slot.item.count - 1] = slot.index
            }

            hasData = true
            clicks = 0
        }

        override fun nextClick(): Int? = if (!hasData) order[clicks++] else null

        override fun shouldClose(autoClose: Boolean): Boolean = autoClose && order.size > if (getMaxXp) 20 else 9 - serumCount
    }

    private abstract class ExperimentHandler {
        protected var clicks = 0
        protected var hasData = false

        abstract fun onSlotUpdate()

        abstract fun nextClick(): Int?

        abstract fun shouldClose(autoClose: Boolean): Boolean
    }

    private fun delay(): Long =
        (clickDelay + (0..delayVariety).random()).toLong()
}
