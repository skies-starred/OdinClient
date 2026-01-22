package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.player.Player
import starred.skies.odin.utils.Skit

object Highlight : Module(
    name = "Highlight (C)",
    description = "Allows you to highlight selected entities.",
    category = Skit.CHEATS
) {
    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Disable to enable ESP")
    private val highlightStar by BooleanSetting("Highlight Starred Mobs", true, desc = "Highlights starred dungeon mobs.")
    private val color by ColorSetting("Highlight color", Colors.WHITE, true, desc = "The color of the highlight.")
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")
    private val hideNonNames by BooleanSetting("Hide non-starred names", true, desc = "Hides names of entities that are not starred.")

    private val teammateClassGlow by BooleanSetting("Teammate Class Glow", true, desc = "Highlights dungeon teammates based on their class color.")

    private val dungeonMobSpawns = hashSetOf("Lurker", "Dreadlord", "Souleater", "Zombie", "Skeleton", "Skeletor", "Sniper", "Super Archer", "Spider", "Fels", "Withermancer", "Lost Adventurer", "Angry Archaeologist", "Frozen Adventurer")
    // https://regex101.com/r/QQf502/1
    private val starredRegex = Regex("^.*✯ .*\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?(?:[kM])?❤$")

    private val entities = mutableSetOf<Entity>()

    init {
        on<TickEvent.End> {
            if (!highlightStar || !DungeonUtils.inDungeons || DungeonUtils.inBoss) return@on

            val entitiesToRemove = mutableListOf<Entity>()
            for (e in (mc.level?.entitiesForRendering() ?: return@on)) {
                val entity = e ?: continue
                if (!entity.isAlive || entity !is ArmorStand) continue

                val entityName = entity.name?.string ?: continue
                if (!dungeonMobSpawns.any { it in entityName }) continue

                val isStarred = starredRegex.matches(entityName)

                if (hideNonNames && entity.isInvisible && !isStarred) {
                    entitiesToRemove.add(entity)
                    continue
                }

                if (!isStarred) continue

                mc.level
                    ?.getEntities(entity, entity.boundingBox.move(0.0, -1.0, 0.0)) { isValidEntity(it) }
                    ?.firstOrNull()
                    ?.let { entities.add(it) }
            }

            entitiesToRemove.forEach { it.remove(Entity.RemovalReason.DISCARDED) }
            entities.removeIf { entity -> !entity.isAlive }
        }

        on<RenderEvent./*? if >=1.21.10 {*/Extract/*?} else {*//*Last*//*?}*/> {
            if (!highlightStar || !DungeonUtils.inDungeons || DungeonUtils.inBoss) return@on

            entities.forEach { entity ->
                if (!entity.isAlive) return@forEach

                /*? if <1.21.10 {*//*context.*//*?}*/drawStyledBox(entity.renderBoundingBox, color, renderStyle, depthCheck)
            }
        }

        on</*? >= 1.21.10 {*/WorldEvent.Load/*? } else { *//*WorldLoadEvent*//*? } */> {
            entities.clear()
        }
    }

    private fun isValidEntity(entity: Entity): Boolean =
        when (entity) {
            is ArmorStand -> false
            is WitherBoss -> false
            is Player -> entity.uuid.version() == 2 && entity != mc.player
            else -> entity is EnderMan || !entity.isInvisible
        }

    @JvmStatic
    fun getTeammateColor(entity: Entity): Int? {
        if (!enabled || !teammateClassGlow || !DungeonUtils.inDungeons || entity !is Player) return null
        return DungeonUtils.dungeonTeammates.find { it.name == entity.name?.string }?.clazz?.color?.rgba
    }
}