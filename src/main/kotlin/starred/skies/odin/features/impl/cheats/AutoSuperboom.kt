package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import starred.skies.odin.events.InputEvent
import starred.skies.odin.mixin.accessors.InventoryAccessor
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.leftClick

object AutoSuperboom : Module(
    name = "Auto superboom",
    description = "Automatically swaps to superboom when you click a breakable wall!",
    category = Skit.CHEATS
) {
    private val minDelay by NumberSetting("Minimum delay", 1, 1, 5, unit = "ticks", desc = "The minimum delay for swapping.")
    private val maxDelay by NumberSetting("Maximum delay", 3, 1, 10, unit = "ticks", desc = "The maximum delay for swapping.")
    private val swapBack by BooleanSetting("Swap back", false, desc = "Whether to swap back to the original item.")
    private val minSB by NumberSetting("Min swap back delay", 1, 1, 5, unit = "ticks", desc = "The minimum delay for swapping back.").withDependency { swapBack }
    private val maxSB by NumberSetting("Max swap back delay", 2, 1, 10, unit = "ticks", desc = "The maximum delay for swapping back.").withDependency { swapBack }

    private val set = setOf("SUPERBOOM_TNT", "INFINITE_SUPERBOOM_TNT")
    private var tick = -1
    private var og = -1
    private var ts = -1
    private var int = 0

    init {
        on<InputEvent.Mouse.Press> {
            if (mc.screen != null) return@on
            if (!DungeonUtils.inDungeons) return@on
            if (DungeonUtils.inBoss) return@on
            val p = mc.player ?: return@on
            val h = mc.hitResult as? BlockHitResult ?: return@on

            val block = mc.level?.getBlockState(h.blockPos) ?: return@on
            if (block.block != Blocks.CRACKED_STONE_BRICKS) return@on

            val s = (p.inventory as InventoryAccessor).selectedSlot
            val t = fn()?.takeIf { it != s } ?: return@on

            tick = (minDelay..maxDelay.coerceAtLeast(minDelay)).random()
            og = s
            ts = t
            int = 0

            cancel()
        }

        on<TickEvent.Start> {
            val p = mc.player ?: return@on
            if (tick == -1) return@on
            if (tick-- > 0) return@on

            val acc = p.inventory as InventoryAccessor

            when (int) {
                0 -> {
                    val slot = ts.takeIf { it != -1 } ?: return@on reset()
                    acc.selectedSlot = slot
                    int = 1
                    tick = 1
                }

                1 -> {
                    leftClick()

                    if (!swapBack) return@on reset()
                    int = 2
                    tick = (minSB..maxSB.coerceAtLeast(minSB)).random()
                }

                2 -> {
                    acc.selectedSlot = og
                    reset()
                }
            }
        }
    }

    private fun fn(): Int? {
        val player = mc.player ?: return null
        for (i in 0..8) if (player.inventory.getItem(i)?.itemId in set) return i
        return null
    }

    private fun reset() {
        tick = -1
        og = -1
        int = 0
    }
}