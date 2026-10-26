package foo.starred.odinclient.features.impl.general

import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.events.core.onSend
import com.odtheking.odin.features.Module
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.gui.MinecraftGui
import foo.starred.kontext.abstraction.input.impl.KeyboardInputState
import foo.starred.odinclient.events.TickStartEvent
import foo.starred.odinclient.mixin.accessors.KeyMappingAccessor
import foo.starred.odinclient.api.category.OdinClientCategory
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket

// Hypixel only checks for inventory walk on container clicks.
// Therefore, by only moving when not clicking, we can bypass the check!
object InventoryWalk : Module(
    name = "Inventory Walk (!!!)",
    description = "Use at your own risk! Only allows movement when not clicking.",
    category = OdinClientCategory.CHEATS
) {
    //~ if >= 26.2 '1, 500' -> '1..500'
    private val ping by NumberSetting("Ping", 200, 1, 500, unit = "ms", desc = "The ping to use for checks.")

    private var clicked = false
    private var clickTime = 0L
    private var lastPing = System.currentTimeMillis()

    private val movementKeys: List<KeyMapping>
        get() = listOf(
            client.options.keyUp,
            client.options.keyLeft,
            client.options.keyRight,
            client.options.keyDown,
            client.options.keyJump,
            client.options.keySprint,
            client.options.keyShift
        )

    init {
        on<TickStartEvent> {
            val screen = MinecraftGui.screen
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
            client.execute {
                val screen = MinecraftGui.screen
                if (screen?.focused() == false) applyMovementKeys()
            }
        }

        onReceive<ClientboundPingPacket> {
            lastPing = System.currentTimeMillis()
        }
    }

    private fun applyMovementKeys() {
        movementKeys.forEach { key ->
            key.press(KeyboardInputState.pressed((key as KeyMappingAccessor).boundKey))
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
        this is ChatScreen || this is AbstractSignEditScreen || children().any { it is EditBox && it.isFocused }
}
