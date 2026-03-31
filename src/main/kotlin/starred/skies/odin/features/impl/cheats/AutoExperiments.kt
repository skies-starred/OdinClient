package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.hasGlint
import com.odtheking.odin.utils.noControlCodes
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.Items
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.guiClick
import java.util.concurrent.ConcurrentHashMap

object AutoExperiments : Module (
    name = "Auto Experiments",
    description = "Automatically click on the Chronomatron and Ultrasequencer experiments.",
    category = Skit.CHEATS
) {
    private val clickDelay by NumberSetting("Click Delay", 200, 100, 1000, 10, unit = "ms", desc = "Time in ms between automatic test clicks.")
    private val delayVariety by NumberSetting("Delay variety", 50, 0, 1000, 10, unit = "ms", desc = "Variance in delays")
    private val autoClose by BooleanSetting("Auto Close", true, desc = "Automatically close the GUI after completing the experiment.")
    private val serumCount by NumberSetting("Serum Count", 0, 0, 3, 1, desc = "Consumed Metaphysical Serum count.")
    private val getMaxXp by BooleanSetting("Get Max XP", false, desc = "Solve Chronomatron to 15 and Ultrasequencer to 20 for max XP.")

    private var handler: ExperimentHandler? = null
    private var lastClick: Long = 0

    init {
        //~ if >=1.21.11 'GuiEvent' -> 'ScreenEvent'
        on<GuiEvent.Open> {
            val title = screen.title?.string ?: return@on

            handler = when {
                title.startsWith("Chronomatron (") -> ChronomatronHandler()
                title.startsWith("Ultrasequencer (") -> UltrasequencerHandler()
                else -> null
            }
        }

        on<GuiEvent.SlotUpdate> {
            handler?.onSlotUpdate(this)
        }

        on<TickEvent.End> {
            val handler = handler ?: return@on
            val screen = mc.screen as? AbstractContainerScreen<*> ?: return@on

            val now = System.currentTimeMillis()
            if (now - lastClick < delay()) return@on

            handler.nextClick()?.let { slotId ->
                guiClick(screen.menu.containerId, slotId, clickType = ClickType.CLONE)
                lastClick = now
            }

            if (!handler.shouldClose(autoClose)) return@on

            mc.player?.closeContainer()
            AutoExperiments.handler = null
        }
    }

    private class ChronomatronHandler : ExperimentHandler() {
        private val order = mutableListOf<Int>()
        private var lastAddedSlot = -1
        private var close = false

        override fun onSlotUpdate(event: GuiEvent.SlotUpdate) {
            val slots = event.menu.slots
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

        override fun onSlotUpdate(event: GuiEvent.SlotUpdate) {
            val slots = event.menu.slots
            val center = slots[49].item ?: return

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

        abstract fun onSlotUpdate(event: GuiEvent.SlotUpdate)

        abstract fun nextClick(): Int?

        abstract fun shouldClose(autoClose: Boolean): Boolean
    }

    private fun delay(): Long =
        (clickDelay + (0..delayVariety).random()).toLong()
}