package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import starred.skies.odin.utils.Skit

object CloseChest : Module(
    name = "Close Chest",
    description = "Allows you to instantly close chests with any key or automatically.",
    category = Skit.CHEATS
) {
    private val mode by SelectorSetting("Mode", "Auto", arrayListOf("Auto", "Any Key"), desc = "The mode to use.")

    init {
        onReceive<ClientboundOpenScreenPacket> {
            if (mode != 0) return@onReceive
            if (!DungeonUtils.inDungeons) return@onReceive
            if (!title.string.noControlCodes.equalsOneOf("Chest", "Large Chest")) return@onReceive

            mc.connection?.send(ServerboundContainerClosePacket(containerId))
            it.cancel()
        }

        //~ if >=1.21.11 'GuiEvent' -> 'ScreenEvent'
        on<GuiEvent.KeyPress> {
            if (!DungeonUtils.inDungeons) return@on
            if (mc.options.keyInventory.matches(input)) return@on

            handleInput(screen)
        }

        //~ if >=1.21.11 'GuiEvent' -> 'ScreenEvent'
        on<GuiEvent.MouseClick> {
            if (!DungeonUtils.inDungeons) return@on

            handleInput(screen)
        }
    }

    private fun handleInput(screen: Screen?) {
        if (mode != 1) return
        val screen = screen as? ContainerScreen? ?: return
        if (screen.title.string.noControlCodes.equalsOneOf("Chest", "Large Chest")) mc.player?.closeContainer()
    }
}