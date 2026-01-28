package starred.skies.odin.features.impl.cheats

import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.OdinMod.mc
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
// Therefore by only moving when not clicking we can bypass the check!
object InventoryWalk : Module(
    name = "Inventory Walk",
    description = "Use at your own risk!",
    category = Skit.CHEATS
) {
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
            val screen = mc.screen ?: run {
                clicked = false
                return@on 
            }
            if (isTextInputFocused(screen)) return@on

            val now = System.currentTimeMillis()
            // these numbers have no reason behind them, its just what was stable for me 
            val allowInput = (!clicked && now - lastPing < 125) || lastPing > clickTime + 350

            if (allowInput) {
                applyMovementKeys()
            } else {
                setAllMovement(false)
            }
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
                if (!isTextInputFocused(screen)) applyMovementKeys()
            }
        }

        onReceive<ClientboundPingPacket> {
            lastPing = System.currentTimeMillis()
        }
    }

    private fun applyMovementKeys() {
        movementKeys.forEach { key ->
            val actualKey = (key as KeyMappingAccessor).boundKey
            setKeyState(key, InputConstants.isKeyDown(mc.window, actualKey.value))
        }
    }

    private fun setAllMovement(state: Boolean) {
        movementKeys.forEach { key -> setKeyState(key, state) }
    }

    private fun setKeyState(key: KeyMapping, down: Boolean) {
        val actualKey = (key as KeyMappingAccessor).boundKey
        KeyMapping.set(actualKey, down)
    }

    private fun isTextInputFocused(screen: Screen?): Boolean =
        screen is ChatScreen ||
            screen is AbstractSignEditScreen ||
            screen?.children()?.any { it is EditBox && it.isFocused } == true
}
