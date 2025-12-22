package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.InputEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.skyblock.SkyblockPlayer
import org.lwjgl.glfw.GLFW

object Gloomlock : Module(
    name = "Gloomlock Helper",
    description = "Stops you from clicking the Gloomlock more than you should."
) {
    private val blockLeftClick by BooleanSetting("Block Left Click", false, desc = "")
    private val blockRightClick by BooleanSetting("Block Right Click", false, desc = "")

    init {
        on<InputEvent> {
            if (mc.player?.mainHandItem?.itemId != "GLOOMLOCK_GRIMOIRE") return@on

            when (key.value) {
                GLFW.GLFW_MOUSE_BUTTON_LEFT if blockLeftClick -> {
                    if (SkyblockPlayer.overflowMana == 600 || SkyblockPlayer.currentHealth < (SkyblockPlayer.maxHealth * 0.3)) this.cancel()
                }

                GLFW.GLFW_MOUSE_BUTTON_RIGHT if blockRightClick -> {
                    if (SkyblockPlayer.currentHealth > (SkyblockPlayer.maxHealth * 0.8)) this.cancel()
                }
            }
        }
    }
}