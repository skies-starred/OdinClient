package foo.starred.odinclient.features.impl.general

import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.events.core.onSend
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.LocationUtils
import foo.starred.kontext.abstraction.gui.MinecraftGui
import foo.starred.odinclient.events.TickStartEvent
import foo.starred.odinclient.api.category.OdinClientCategory
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Items

object AutoHarp : Module(
    name = "Auto Harp",
    description = "Tries to do the Harp for you!",
    category = OdinClientCategory.CHEATS
) {
    private var bool: Boolean = false
    private var hash: Int = 0

    init {
        onReceive<ClientboundOpenScreenPacket> {
            if (!LocationUtils.isInSkyblock) return@onReceive
            if (!title.string.startsWith("Harp - ")) return@onReceive
            if (type != MenuType.GENERIC_9x6) return@onReceive

            bool = true
        }

        onReceive<ClientboundContainerClosePacket> {
            bool = false
        }

        onSend<ServerboundContainerClosePacket> {
            bool = false
        }

        on<TickStartEvent> {
            if (!bool) return@on

            val screen = MinecraftGui.container ?: return@on
            val slots = screen.menu.slots

            var hash0 = 0
            var slot = -1

            for (i in 37..43) {
                val b0 = slots.getOrNull(i)?.item?.item == Items.QUARTZ_BLOCK
                hash0 = (hash0 shl 1) or if (b0) 1 else 0
                if (slot == -1 && b0) slot = i
            }

            if (hash == hash0) return@on
            hash = hash0

            if (slot == -1) return@on
            MinecraftGui.click(screen.menu.containerId, slot, clickType = ContainerInput.CLONE)
        }
    }
}
