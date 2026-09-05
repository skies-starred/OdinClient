/**
 * Taken from OdinFabric
 * odtheking's BSD-3 Clause License applies to this file.
 *
 * BSD 3-Clause License
 *
 * Copyright (c) 2023-2025, odtheking
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package foo.starred.odinclient.features.impl.dungeons

import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.MapSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.renderPos
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.player.Player
import foo.starred.odinclient.events.EntityMetadataEvent
import foo.starred.odinclient.utils.Category
import foo.starred.odinclient.utils.drawTracer

object Highlight : Module(
    name = "Highlight (C)",
    description = "Allows you to highlight selected entities.",
    category = Category.CHEATS
) {
    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Disable to enable ESP")
    private val highlightStar by BooleanSetting("Highlight Starred Mobs", true, desc = "Highlights starred dungeon mobs.")
    private val starredTracer by BooleanSetting("Starred mobs tracers", desc = "Draws a tracer to the starred mobs.")
    val color by ColorSetting("Highlight color", Colors.WHITE, true, desc = "The color of the highlight.")
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")
    private val hideNonNames by BooleanSetting("Hide non-starred names", true, desc = "Hides names of entities that are not starred.")
    private val teammateClassGlow by BooleanSetting("Teammate Class Glow", true, desc = "Highlights dungeon teammates based on their class color.")
    private val highlightWither by BooleanSetting("Highlight Withers", true, desc = "Highlights Necron, Goldor, Storm and Maxor.")
    private val witherColor by ColorSetting("Wither ESP Color", Color(255, 0, 0, 1f), true, desc = "The color of the wither highlight.").withDependency { highlightWither }
    private val witherTracer by BooleanSetting("Wither Tracer", true, desc = "Draws a tracer to the wither boss in P3 section 4.").withDependency { highlightWither }
    private val highlightBats by BooleanSetting("Highlight Bats", true, desc = "Highlights bats in dungeons.")
    private val batColor by ColorSetting("Bat color", Color(0, 255, 255, 1f), true, desc = "The color of the bat highlight.").withDependency { highlightBats }
    private val customTracer by BooleanSetting("Custom tracer", desc = "Draws a tracer to the mobs added manually")

    private val dungeonMobSpawns = hashSetOf("Lurker", "Dreadlord", "Souleater", "Zombie", "Skeleton", "Skeletor", "Sniper", "Super Archer", "Spider", "Fels", "Withermancer", "Lost Adventurer", "Angry Archaeologist", "Frozen Adventurer", "Shadow Assassin")
    private val starredRegex = Regex("^.*✯ .*\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?[kM]?❤$")

    val highlightMap by MapSetting("highlightMap", mutableMapOf<String, Color>())

    private val starredIds = hashSetOf<Int>()
    private val customIds = hashMapOf<Int, Color>()
    private val witherIds = hashSetOf<Int>()
    private val spiritSceptreIds = hashSetOf<Int>()
    private val checkedIds = hashSetOf<Int>()

    init {
        OdinMod.logger.debug("Loaded ${highlightMap.entries.size}")

        on<EntityMetadataEvent> {
            if (!entity.isAlive) return@on
            val d = DungeonUtils.inDungeons
            val b = DungeonUtils.inBoss
            val p = DungeonUtils.getF7Phase()

            when {
                d && p != M7Phases.P5 && highlightWither && entity is WitherBoss && !entity.isInvisible && entity.invulnerableTicks != 800 -> {
                    witherIds.add(entity.id)
                }

                d && !b && highlightBats && entity is Bat && !entity.isPassenger && !entity.isInvisible -> {
                    val player = mc.player ?: return@on
                    if (player.distanceTo(entity) < 1.0) {
                        spiritSceptreIds.add(entity.id)
                        return@on
                    }
                }

                d && !b && highlightStar && entity is Player && entity != mc.player && entity.gameProfile.name.contains("Shadow Assassin") -> {
                    starredIds.add(entity.id)
                }

                !b && (highlightStar || highlightMap.isNotEmpty()) && entity is ArmorStand -> {
                    val rawName = entity.customName?.string?.noControlCodes ?: return@on
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
            if (customIds.isEmpty() && starredIds.isEmpty() && witherIds.isEmpty() && !highlightBats) return@on

            val world = mc.level ?: return@on
            val bool0 = starredTracer
            val bool1 = witherTracer && DungeonUtils.getF7Phase() == M7Phases.P3
            val bool2 = customTracer

            starredIds.forEach { id ->
                val entity = world.getEntity(id) ?: return@forEach
                if (!entity.isAlive) return@forEach
                drawStyledBox(entity.renderBoundingBox, color, renderStyle, depthCheck)
                if (bool0) drawTracer(entity.renderPos, color, depth = depthCheck)
            }

            witherIds.forEach { id ->
                val entity = world.getEntity(id) as? WitherBoss ?: return@forEach
                if (!entity.isAlive) return@forEach
                drawStyledBox(entity.renderBoundingBox, witherColor, renderStyle, depthCheck)
                if (entity.invulnerableTicks == 0) return@forEach
                if (bool1) drawTracer(entity.renderPos, witherColor, depth = depthCheck)
            }

            if (highlightBats && DungeonUtils.inDungeons && !DungeonUtils.inBoss) {
                world.entitiesForRendering()
                    .filterIsInstance<Bat>()
                    .filter { !it.isPassenger && !it.isInvisible && it.isAlive && it.id !in spiritSceptreIds }
                    .forEach { drawStyledBox(it.renderBoundingBox, batColor, renderStyle, depthCheck) }
            }

            customIds.forEach { (id, color) ->
                val entity = world.getEntity(id) ?: return@forEach
                if (!entity.isAlive) return@forEach
                drawStyledBox(entity.renderBoundingBox, color, renderStyle, depthCheck)
                if (bool2) drawTracer(entity.renderPos, color, depth = depthCheck)
            }
        }

        on<LevelEvent.Load> {
            starredIds.clear()
            customIds.clear()
            witherIds.clear()
            spiritSceptreIds.clear()
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
        return DungeonUtils.dungeonTeammates.find { it.name == entity.name.string }?.clazz?.color?.rgba
    }
}
