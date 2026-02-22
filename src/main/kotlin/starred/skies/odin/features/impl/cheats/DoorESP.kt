package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.dungeon.DungeonMap
import com.odtheking.odin.features.impl.dungeon.map.Door
import com.odtheking.odin.features.impl.dungeon.map.DungMap
import com.odtheking.odin.features.impl.dungeon.map.MapScanner
import com.odtheking.odin.features.impl.dungeon.map.SpecialColumn
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.ScanUtils
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.phys.AABB
import starred.skies.odin.utils.Skit

object DoorESP : Module(
    name = "Doors ESP",
    description = "Highlights doors in dungeons",
    category = Skit.CHEATS
) {
    private val highlightAll by BooleanSetting("Highlight all doors", desc = "Highlights all doors, including normal ones.")
    private val checkRoom by BooleanSetting("Check current room", desc = "If enabled, only shows the normal doors connected to the current room.").withDependency { highlightAll }
    private val highlightAllWither by BooleanSetting("Highlight all special doors", desc = "If enabled, highlights all wither and blood doors.")
    private val hideOpened by BooleanSetting("Hide opened doors", true, desc = "Hides opened doors if enabled.")
    private val normalColor by ColorSetting("Normal door color", Colors.WHITE, desc = "Color for the normal door type.")
    private val witherColor by ColorSetting("Wither door color", Colors.BLACK, desc = "Color for the wither door type.")
    private val bloodColor by ColorSetting("Blood door color", Colors.MINECRAFT_RED, desc = "Color for the blood door type.")
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")

    init {
        on<RenderEvent.Extract> {
            if (!DungeonUtils.inDungeons) return@on
            if (DungeonUtils.inBoss) return@on

            val currentRoomName = ScanUtils.currentRoom?.data?.name

            for (d in MapScanner.doors) {
                if (!d.show(currentRoomName)) continue
                if (hideOpened && !d.locked && d.type != Door.Type.NORMAL) continue

                val color = when (d.type) {
                    Door.Type.NORMAL -> normalColor
                    Door.Type.WITHER -> witherColor
                    Door.Type.BLOOD -> bloodColor
                }

                val aabb =
                    if (((d.pos.x + 185) shr 4) % 2 == 1) AABB(d.pos.x - 1.0, 69.0, d.pos.z - 1.0, d.pos.x + 2.0, 73.0, d.pos.z + 2.0)
                    else AABB(d.pos.x - 1.0, 69.0, d.pos.z - 1.0, d.pos.x + 2.0, 73.0, d.pos.z + 2.0)

                drawStyledBox(aabb, color, renderStyle, false)
            }
        }

        on<WorldEvent.Load> {
            if (DungeonMap.enabled) return@on
            SpecialColumn.unload()
            MapScanner.unload()
            DungMap.unload()
        }

        on<TickEvent.End> {
            if (!DungeonMap.enabled) MapScanner.scan(world)
        }

        onReceive<ClientboundMapItemDataPacket> {
            if (!DungeonMap.enabled) mc.execute { DungMap.rescanMapItem(this) }
        }
    }

    private fun Door.show(currentRoomName: String?): Boolean {
        return when (type) {
            Door.Type.NORMAL -> highlightAll && (!checkRoom || rooms.any { it.owner.data.name == currentRoomName })
            else -> highlightAll || highlightAllWither
        }
    }
}