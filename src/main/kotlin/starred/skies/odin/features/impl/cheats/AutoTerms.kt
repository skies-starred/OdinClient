package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.floor7.TerminalSolver
import com.odtheking.odin.features.impl.floor7.terminalhandler.TerminalTypes
import starred.skies.odin.utils.Skit

object AutoTerms : Module(
    name = "Auto Terms (!!!!!)",
    description = "Automatically solves terminals - USE AT YOUR OWN RISK, NOT TESTED.",
    category = Skit.CHEATS
) {
    private val _unused by BooleanSetting("USE AT YOUR OWN RISK", true, desc = "AUTO TERMS HAS NOT BEEN TESTED!!!!!")
    private val autoDelay by NumberSetting("Delay", 170L, 100, 300, unit = "ms", desc = "Delay between clicks.")
    private val delayVariety by NumberSetting("Delay Variety", 70L, 0, 150, unit = "ms", desc = "Variety in the delay between clicks.")
    private val firstClickDelay by NumberSetting("First Click Delay", 350L, 300, 500, unit = "ms", desc = "Delay before first click.")
    private val breakThreshold by NumberSetting("Break Threshold", 500L, 350L, 1000L, 10L, unit = "ms", desc = "Time before breaking the click.")
    private val disableMelody by BooleanSetting("Disable Melody", false, desc = "Disables melody terminals.")
    private var lastClickTime = 0L
    private var firstClick = true

    init {
        on<GuiEvent.DrawBackground> {
            with (TerminalSolver.currentTerm ?: return@on) {
                if (firstClick && (System.currentTimeMillis() - lastClickTime < firstClickDelay))  return@on
                if (System.currentTimeMillis() - lastClickTime < autoDelay)  return@on
                if (System.currentTimeMillis() - lastClickTime > breakThreshold) isClicked = false
                if (solution.isEmpty() || (disableMelody && type == TerminalTypes.MELODY) || isClicked)  return@on

                val slotIndex = solution.firstOrNull() ?: return@on
                lastClickTime = System.currentTimeMillis() + (0..delayVariety).random()
                firstClick = false

                when (type) {
                    TerminalTypes.RUBIX -> click(slotIndex, if (solution.count { it == slotIndex } >= 3) 1 else 2, false)
                    TerminalTypes.MELODY -> click(solution.find { it % 9 == 7 } ?: return@on, 2, false)
                    else -> click(slotIndex, 2, false)
                }
            }
        }
    }
}