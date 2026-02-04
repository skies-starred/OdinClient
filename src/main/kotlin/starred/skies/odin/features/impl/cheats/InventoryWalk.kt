package starred.skies.odin.features.impl.cheats

import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.events.core.onSend
import com.odtheking.odin.features.Module
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import starred.skies.odin.mixin.accessors.KeyMappingAccessor
import starred.skies.odin.utils.Skit

// Hypixel only checks for inventory walk on container clicks.
// Therefore, by only moving when not clicking, we can bypass the check!
object InventoryWalk : Module(
    name = "Inventory Walk (!!!)",
    description = "Use at your own risk! Only allows movement when not clicking.",
    category = Skit.CHEATS
) {
    private val ping by NumberSetting("Ping", 200, 1, 500, unit = "ms", desc = "The ping to use for checks.")

    private var clicked = false
    private var clickTime = 0L
    private var lastPing = System.currentTimeMillis()

    private val movementKeys: List<KeyMapping>
        get() = listOf(
            mc.options.keyUp,
            mc.options.keyLeft,
            mc.options.keyRight,
            mc.options.keyDown,
            mc.options.keyJump,
            mc.options.keySprint,
            mc.options.keyShift
        )

    init {
        on<TickEvent.Start> {
            val screen = mc.screen
            if (screen == null) {
                clicked = false
                return@on
            }

            if (screen.focused()) return@on
            val now = System.currentTimeMillis()

            val allowInput = (!clicked && now - lastPing < ping) || lastPing > clickTime + 350
            if (allowInput) applyMovementKeys() else setAllMovement(false)
        }

        onSend<ServerboundContainerClickPacket> {
            clicked = true
            clickTime = System.currentTimeMillis()
            setAllMovement(false)
        }

        onReceive<ClientboundOpenScreenPacket> {
            clicked = false
            mc.execute {
                val screen = mc.screen
                if (screen?.focused() == false) applyMovementKeys()
            }
        }

        onReceive<ClientboundPingPacket> {
            lastPing = System.currentTimeMillis()
        }
    }

    private fun applyMovementKeys() {
        movementKeys.forEach { key ->
            val actualKey = (key as KeyMappingAccessor).boundKey
            key.press(InputConstants.isKeyDown(mc.window, actualKey.value))
        }
    }

    private fun setAllMovement(state: Boolean) {
        movementKeys.forEach { key -> key.press(state) }
    }

    private fun KeyMapping.press(down: Boolean) {
        val actualKey = (this as KeyMappingAccessor).boundKey
        KeyMapping.set(actualKey, down)
    }

    private fun Screen.focused(): Boolean =
        this is ChatScreen || this is AbstractSignEditScreen || children()?.any { it is EditBox && it.isFocused } == true
}
