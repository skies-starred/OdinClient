package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.withAlpha
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.alert
import com.odtheking.odin.utils.render.drawWireFrameBox
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import starred.skies.odin.events.EntityMetadataEvent
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.drawTracer

object KeyHighlight : Module(
    name = "Key Highlight (C)",
    description = "Highlights wither and blood keys in dungeons.",
    category = Skit.CHEATS
) {
    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Disable to enable ESP")
    private val announceKeySpawn by BooleanSetting("Announce Key Spawn", true, desc = "Announces when a key is spawned.")
    private val tracer by BooleanSetting("Show Tracer", true, desc = "Draws a tracer to the wither key")
    private val tracerColor by ColorSetting("Tracer Color", Colors.MINECRAFT_AQUA, desc = "The color of the tracer.")
    private val witherColor by ColorSetting("Wither Color", Colors.BLACK.withAlpha(0.8f), true, desc = "The color of the box.")
    private val bloodColor by ColorSetting("Blood Color", Colors.MINECRAFT_RED.withAlpha(0.8f), true, desc = "The color of the box.")

    private var currentKey: KeyType? = null

    init {
        on<EntityMetadataEvent> {
            if (!DungeonUtils.inDungeons || DungeonUtils.inBoss) return@on
            if (currentKey?.entity == entity) return@on
            currentKey = KeyType.entries.find { it.displayName == entity.name?.string } ?: return@on
            currentKey?.entity = entity

            if (announceKeySpawn) alert("§${currentKey?.colorCode}${entity.name?.string}§7 spawned!")
        }

        on<RenderEvent./*? if >=1.21.10 {*/Extract/*?} else {*//*Last*//*?}*/> {
            if (!DungeonUtils.inDungeons || DungeonUtils.inBoss) return@on
            if (currentKey == null || currentKey?.entity == null) return@on
            currentKey?.let { keyType ->
                if (keyType.entity?.isAlive == false) {
                    currentKey = null
                    return@on
                }
                val position = keyType.entity?.position()?.add(-0.5, 1.0, -0.5) ?: return@on
                /*? if <1.21.10 {*//*context.*//*?}*/drawWireFrameBox(AABB.unitCubeFromLowerCorner(position), keyType.color(), 8f, depthCheck)
                if (tracer) /*? if <1.21.10 {*//*context.*//*?}*/drawTracer(position, tracerColor, depth = depthCheck)
            }
        }

        on</*? >= 1.21.10 {*/WorldEvent.Load/*? } else { *//*WorldLoadEvent*//*? } */> {
            currentKey = null
        }
    }

    private enum class KeyType(val displayName: String, val color: () -> Color, val colorCode: Char) {
        Wither("Wither Key", { witherColor }, '8'),
        Blood("Blood Key", { bloodColor }, 'c');

        var entity: Entity? = null
    }
}