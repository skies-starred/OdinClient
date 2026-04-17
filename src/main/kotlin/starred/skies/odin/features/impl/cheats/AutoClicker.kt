package starred.skies.odin.features.impl.cheats

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.serialization.Codec
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.itemId
import net.minecraft.client.KeyMapping
import net.minecraft.world.phys.BlockHitResult
import org.lwjgl.glfw.GLFW
import starred.skies.odin.helpers.Scribble
import starred.skies.odin.mixin.accessors.KeyMappingAccessor
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.leftClick
import starred.skies.odin.utils.nullableID
import starred.skies.odin.utils.nullableUUID
import starred.skies.odin.utils.rightClick
import xyz.aerii.library.api.bound

object AutoClicker : Module(
    name = "Auto Clicker",
    description = "Auto clicker with options for left-click, right-click, or both.",
    category = Skit.CHEATS
) {
    private val whiteListOnly by BooleanSetting("Whitelist only", desc = "Only click when holding a whitelisted item, whitelist using \"/autoclicker add [left|right]\" while holding the item.")
    private val allowBreaking by BooleanSetting("Allow breaking blocks", desc = "Allows you to break blocks when auto clicking.")
    private val blockBreaker by BooleanSetting("Block dungeon breaker", true, desc = "Prevents auto clicker from working with Dungeon Breaker.")
    private val terminatorOnly by BooleanSetting("Terminator Only", true, desc = "Only click when the terminator and right click are held.")
    private val cps by NumberSetting("Clicks Per Second", 5.0f, 3.0, 15.0, .5, desc = "The amount of clicks per second to perform.").withDependency { terminatorOnly }

    private val enableLeftClick by BooleanSetting("Enable Left Click", true, desc = "Enable auto-clicking for left-click.").withDependency { !terminatorOnly }
    private val enableRightClick by BooleanSetting("Enable Right Click", true, desc = "Enable auto-clicking for right-click.").withDependency { !terminatorOnly }
    private val leftCps by NumberSetting("Left Clicks Per Second", 5.0f, 3.0, 15.0, .5, desc = "The amount of left clicks per second to perform.").withDependency { !terminatorOnly }
    private val rightCps by NumberSetting("Right Clicks Per Second", 5.0f, 3.0, 15.0, .5, desc = "The amount of right clicks per second to perform.").withDependency { !terminatorOnly }
    private val leftClickKeybind = KeybindSetting("Left Click", GLFW.GLFW_KEY_UNKNOWN, desc = "The keybind to hold for the auto clicker to click left click.").withDependency { !terminatorOnly }
    private val rightClickKeybind = KeybindSetting("Right Click", GLFW.GLFW_KEY_UNKNOWN, desc = "The keybind to hold for the auto clicker to click right click.").withDependency { !terminatorOnly }

    private val scribble = Scribble("features/autoClicker")
    val leftWhitelist = scribble.mutableSet("leftWhitelist", Codec.STRING)
    val rightWhitelist = scribble.mutableSet("rightWhitelist", Codec.STRING)

    private var nlc = .0
    private var nrc = .0

    init {
        registerSetting(leftClickKeybind)
        registerSetting(rightClickKeybind)

        on<TickEvent.Start> {
            if (mc.screen != null) return@on
            if (mc.player == null) return@on
            if (mc.player!!.isUsingItem) return@on
            if (mc.gameMode?.isDestroying ?: false) return@on
            val now = System.currentTimeMillis()

            if (terminatorOnly) {
                if (mc.player?.mainHandItem?.itemId != "TERMINATOR" || !mc.options.keyUse.isDown) return@on
                if (now < nrc) return@on

                nrc = now + ((1000 / cps) + ((Math.random() - .5) * 60.0))
                leftClick()
                return@on
            }

            val h1 = mc.player?.mainHandItem?.itemId ?: ""
            if (blockBreaker && h1 == "DUNGEONBREAKER") return@on

            val h2 = held()
            val a = !whiteListOnly || h2 in leftWhitelist.value
            val b = !whiteListOnly || h2 in rightWhitelist.value
            if (!a && !b) return@on

            val level = mc.level ?: return@on
            val hit = mc.hitResult as? BlockHitResult

            val lc = a && enableLeftClick && leftClickKeybind.value.isPressed()
            val rc = b && enableRightClick && rightClickKeybind.value.isPressed()

            if (hit != null && !level.getBlockState(hit.blockPos).isAir && lc && allowBreaking) {
                KeyMapping.set((mc.options.keyAttack as KeyMappingAccessor).boundKey, true)
                return@on
            }

            if (lc && now >= nlc) {
                nlc = now + ((1000 / leftCps) + ((Math.random() - .5) * 60.0))
                leftClick()
            }

            if (rc && now >= nrc) {
                nrc = now + ((1000 / rightCps) + ((Math.random() - .5) * 60.0))
                rightClick()
            }
        }
    }

    fun held(): String? {
        val held = mc.player?.mainHandItem
        return held?.nullableUUID ?: held?.nullableID ?: held?.hoverName?.string
    }

    private fun InputConstants.Key.isPressed(): Boolean {
        if (!value.bound) return false
        val window = mc.window
        return if (value > 7) InputConstants.isKeyDown(window, value)
        else GLFW.glfwGetMouseButton(window.handle(), value) == GLFW.GLFW_PRESS
    }
}
