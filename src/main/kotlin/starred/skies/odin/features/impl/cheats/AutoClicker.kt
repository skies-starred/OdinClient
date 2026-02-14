package starred.skies.odin.features.impl.cheats

import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.itemId
import org.lwjgl.glfw.GLFW
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.leftClick
import starred.skies.odin.utils.rightClick

object AutoClicker : Module(
    name = "Auto Clicker",
    description = "Auto clicker with options for left-click, right-click, or both.",
    category = Skit.CHEATS
) {
    private val terminatorOnly by BooleanSetting("Terminator Only", true, desc = "Only click when the terminator and right click are held.")
    private val cps by NumberSetting("Clicks Per Second", 5.0f, 3.0, 15.0, .5, desc = "The amount of clicks per second to perform.").withDependency { terminatorOnly }

    private val enableLeftClick by BooleanSetting("Enable Left Click", true, desc = "Enable auto-clicking for left-click.").withDependency { !terminatorOnly }
    private val enableRightClick by BooleanSetting("Enable Right Click", true, desc = "Enable auto-clicking for right-click.").withDependency { !terminatorOnly }
    private val leftCps by NumberSetting("Left Clicks Per Second", 5.0f, 3.0, 15.0, .5, desc = "The amount of left clicks per second to perform.").withDependency { !terminatorOnly }
    private val rightCps by NumberSetting("Right Clicks Per Second", 5.0f, 3.0, 15.0, .5, desc = "The amount of right clicks per second to perform.").withDependency { !terminatorOnly }
    private val leftClickKeybind = KeybindSetting("Left Click", GLFW.GLFW_KEY_UNKNOWN, desc = "The keybind to hold for the auto clicker to click left click.").withDependency { !terminatorOnly }
    private val rightClickKeybind = KeybindSetting("Right Click", GLFW.GLFW_KEY_UNKNOWN, desc = "The keybind to hold for the auto clicker to click right click.").withDependency { !terminatorOnly }

    private var nextLeftClick = .0
    private var nextRightClick = .0

    init {
        this.registerSetting(leftClickKeybind)
        this.registerSetting(rightClickKeybind)

        on<TickEvent.Start> {
            if (mc.screen != null) return@on
            if (mc.player == null) return@on
            if (mc.player!!.isUsingItem) return@on
            if (mc.gameMode?.isDestroying ?: false) return@on
            if (mc.player?.mainHandItem?.itemId == "DUNGEONBREAKER") return@on

            val nowMillis = System.currentTimeMillis()
            if (terminatorOnly) {
                if (mc.player?.mainHandItem?.itemId != "TERMINATOR" || !mc.options.keyUse.isDown) return@on
                if (nowMillis < nextRightClick) return@on
                nextRightClick = nowMillis + ((1000 / cps) + ((Math.random() - .5) * 60.0))
                leftClick()
            } else {
                if (enableLeftClick && leftClickKeybind.value.isPressed() && nowMillis >= nextLeftClick) {
                    nextLeftClick = nowMillis + ((1000 / leftCps) + ((Math.random() - .5) * 60.0))
                    leftClick()
                }

                if (enableRightClick && rightClickKeybind.value.isPressed() && nowMillis >= nextRightClick) {
                    nextRightClick = nowMillis + ((1000 / rightCps) + ((Math.random() - .5) * 60.0))
                    rightClick()
                }
            }
        }
    }

    private fun InputConstants.Key.isPressed(): Boolean {
        val value = this.value
        val window = mc.window/*? < 1.21.10 {*//*.window*//*? }*/
        return if (value > 7) InputConstants.isKeyDown(window, value)
        else GLFW.glfwGetMouseButton(window/*? >= 1.21.10 {*/.handle()/*? }*/, value) == GLFW.GLFW_PRESS
    }
}
