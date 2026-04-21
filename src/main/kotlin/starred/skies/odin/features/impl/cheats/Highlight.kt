package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.MapSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.player.Player
import starred.skies.odin.events.EntityMetadataEvent
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.drawTracer

object Highlight : Module(
    name = "Highlight (C)",
    description = "Allows you to highlight selected entities.",
    category = Skit.CHEATS
) {
    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Disable to enable ESP")
    private val highlightStar by BooleanSetting("Highlight Starred Mobs", true, desc = "Highlights starred dungeon mobs.")
    val color by ColorSetting("Highlight color", Colors.WHITE, true, desc = "The color of the highlight.")
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")
    private val hideNonNames by BooleanSetting("Hide non-starred names", true, desc = "Hides names of entities that are not starred.")
    private val teammateClassGlow by BooleanSetting("Teammate Class Glow", true, desc = "Highlights dungeon teammates based on their class color.")
    private val highlightWither by BooleanSetting("Highlight Withers", true, desc = "Highlights Necron, Goldor, Storm and Maxor.")
    val witherColor by ColorSetting("Wither ESP Color", Color(255, 0, 0, 1f), true, desc = "The color of the wither highlight.")
    private val witherTracer by BooleanSetting("Wither Tracer", true, desc = "Draws a tracer to the wither boss in P3 section 4.")

    private val dungeonMobSpawns = hashSetOf("Lurker", "Dreadlord", "Souleater", "Zombie", "Skeleton", "Skeletor", "Sniper", "Super Archer", "Spider", "Fels", "Withermancer", "Lost Adventurer", "Angry Archaeologist", "Frozen Adventurer", "Shadow Assassin")
    private val starredRegex = Regex("^.*✯ .*\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?[kM]?❤$")

    val highlightMap by MapSetting("highlightMap", mutableMapOf<String, Color>())

    private val starredIds = hashSetOf<Int>()
    private val customIds = hashMapOf<Int, Color>()
    private val witherIds = hashSetOf<Int>()
    private val checkedIds = hashSetOf<Int>()

    init {
        OdinMod.logger.debug("Loaded ${highlightMap.entries.size}")

        on<EntityMetadataEvent> {
            if (!DungeonUtils.inDungeons) return@on
            val entity = entity

            when {
                highlightWither && entity is WitherBoss && entity.isAlive && !entity.isRemoved && entity.isPowered -> {
                    witherIds.add(entity.id)
                }
                highlightStar && !DungeonUtils.inBoss && entity is Player && entity != mc.player && entity.gameProfile.name.contains("Shadow Assassin") -> {
                    starredIds.add(entity.id)
                }
                (highlightStar || highlightMap.isNotEmpty()) && !DungeonUtils.inBoss && entity is ArmorStand && entity.isAlive && !entity.isRemoved -> {
                    val rawName = entity.displayName?.string?.noControlCodes?.takeIf { !it.equals("armor stand", true) } ?: return@on
                    val nameLower = rawName.lowercase()

                    if (highlightStar && dungeonMobSpawns.any(rawName::contains)) {
                        val starred = starredRegex.matches(rawName)
                        if (hideNonNames && entity.isInvisible && !starred) return@on
                        if (starred && checkedIds.add(entity.id)) {
                            entity.fn(true)?.let { starredIds.add(it.id) }
                        }
                    }

                    if (highlightMap.isNotEmpty()) {
                        val match = highlightMap.entries.firstOrNull { nameLower.contains(it.key) } ?: return@on
                        entity.fn(true)?.let { customIds[it.id] = match.value }
                    }
                }
            }
        }

        on<RenderEvent.Extract> {
            if (customIds.isEmpty() && starredIds.isEmpty() && witherIds.isEmpty()) return@on

            val world = mc.level ?: return@on

            starredIds.forEach { id ->
                val entity = world.getEntity(id) ?: return@forEach
                drawStyledBox(entity.renderBoundingBox, color, renderStyle, depthCheck)
            }

            witherIds.forEach { id ->
                val entity = world.getEntity(id) ?: return@forEach
                drawStyledBox(entity.renderBoundingBox, witherColor, renderStyle, depthCheck)

                if (witherTracer) {
                    val playerPos = mc.player?.position() ?: return@forEach
                    val minX = minOf(-3, 91).toDouble()
                    val maxX = maxOf(-3, 91).toDouble()
                    val minY = minOf(106, 158).toDouble()
                    val maxY = maxOf(106, 158).toDouble()
                    val minZ = minOf(30, 50).toDouble()
                    val maxZ = maxOf(30, 50).toDouble()

                    val inSection4 = playerPos.x in minX..maxX && playerPos.y in minY..maxY && playerPos.z in minZ..maxZ

                    if (inSection4) {
                        drawTracer(entity.position(), witherColor, depth = depthCheck)
                    }
                }
            }

            customIds.forEach { (id, color) ->
                val entity = world.getEntity(id) ?: return@forEach
                drawStyledBox(entity.renderBoundingBox, color, renderStyle, depthCheck)
            }
        }

        on<WorldEvent.Load> {
            starredIds.clear()
            customIds.clear()
            witherIds.clear()
            checkedIds.clear()
        }
    }

    private fun ArmorStand.fn(vis: Boolean = false): Entity? {
        val a = mc.level
            ?.getEntities(this, boundingBox.inflate(0.0, 1.0, 0.0)) { isValidEntity(it, vis) }
            ?.firstOrNull()

        if (a != null) return a

        return mc.level?.getEntity(id - 1)?.takeIf { isValidEntity(it, vis) }
    }

    private fun isValidEntity(entity: Entity, vis: Boolean = false): Boolean =
        when (entity) {
            is ArmorStand -> false
            is Player -> entity.uuid.version() == 2 && entity != mc.player
            is WitherBoss -> true
            else -> entity is EnderMan || (vis || !entity.isInvisible)
        }

    @JvmStatic
    fun getTeammateColor(entity: Entity): Int? {
        if (!enabled || !teammateClassGlow || !DungeonUtils.inDungeons || entity !is Player) return null
        return DungeonUtils.dungeonTeammates.find { it.name == entity.name?.string }?.clazz?.color?.rgba
    }
}
