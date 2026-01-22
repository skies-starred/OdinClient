package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.addVec
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.toFixed
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.Blocks
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.drawTracer

object SpiritBear : Module(
    name = "Spirit Bear (C)",
    description = "Utilities for Spirit Bear in floor 4.",
    category = Skit.CHEATS
) {
    private val highlightSpirit by BooleanSetting("Highlight Bear", false, desc = "Highlights the spirit bear")
    private val color by ColorSetting("Highlight color", Colors.WHITE, true, desc = "The color of the highlight.").withDependency { highlightSpirit }
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.").withDependency { highlightSpirit }
    private val tracer by BooleanSetting("Show Tracer", true, desc = "Draws a tracer to the spirit bear").withDependency { highlightSpirit }
    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Disable to enable ESP").withDependency { highlightSpirit }

    private val hud by HUD(name, "Displays the current state of Spirit Bear in the HUD.", false) { example ->
        when {
            example -> "§e1.45s"
            !DungeonUtils.isFloor(4) || !DungeonUtils.inBoss -> null
            timer < 0 -> "§d$kills/$maxKills"
            timer > 0 -> "§e${(timer / 20f).toFixed()}s"
            else -> "§aAlive!"
        }?.let { text ->
            textDim("§6Spirit Bear: $text", 0, 0, Colors.WHITE)
        } ?: (0 to 0)
    }

    private inline val blockLocations get() = if (DungeonUtils.floor?.isMM == true) m4BlockLocations else f4BlockLocations
    private inline val maxKills get() = if (DungeonUtils.floor?.isMM == true) 30 else 25
    private val lastBlockLocation = BlockPos(7, 77, 34)
    private var timer = -1 // state: -1=NotSpawned, 0=Alive, 1+=Spawning
    private var kills = 0
    private var entity: Entity? = null

    init {
        on<BlockUpdateEvent> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss || !blockLocations.contains(pos)) return@on

            when (updated.block) {
                Blocks.SEA_LANTERN if old.block == Blocks.COAL_BLOCK -> {
                    if (kills < maxKills) kills++
                    if (pos == lastBlockLocation) timer = 68
                }
                Blocks.COAL_BLOCK if old.block == Blocks.SEA_LANTERN -> {
                    if (kills > 0) kills--
                    if (pos == lastBlockLocation) timer = -1
                }
            }
        }

        on<TickEvent.End> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss || !highlightSpirit) return@on

            val entities = mc.level?.entitiesForRendering() ?: return@on
            for (e in entities) {
                val entity = e ?: continue
                if (entity.isInvisible) continue
                val entityName = entity.name?.string ?: continue
                if (!entityName.startsWith("spirit bear", true)) continue

                this@SpiritBear.entity = entity
            }

            if (entity?.isAlive == false) entity = null
        }

        on<RenderEvent./*? if >=1.21.10 {*/Extract/*?} else {*//*Last*//*?}*/> {
            if (!DungeonUtils.isFloor(4) || !DungeonUtils.inBoss || !highlightSpirit) return@on
            entity?.let {
                /*? if <1.21.10 {*//*context.*//*?}*/drawStyledBox(it.renderBoundingBox, color, renderStyle, depthCheck)
                if (tracer) /*? if <1.21.10 {*//*context.*//*?}*/drawTracer(it.position().addVec(y = it.eyeHeight), color, depth = depthCheck)
            }
        }

        on<TickEvent.Server> {
            if (timer > 0) timer--
        }

        on</*? >= 1.21.10 {*/WorldEvent.Load/*? } else { *//*WorldLoadEvent*//*? } */> {
            kills = 0
            timer = -1
        }
    }

    private val f4BlockLocations = hashSetOf(
        BlockPos(-3, 77, 33), BlockPos(-9, 77, 31), BlockPos(-16, 77, 26), BlockPos(-20, 77, 20), BlockPos(-23, 77, 13),
        BlockPos(-24, 77, 6), BlockPos(-24, 77, 0), BlockPos(-22, 77, -7), BlockPos(-18, 77, -13), BlockPos(-12, 77, -19),
        BlockPos(-5, 77, -22), BlockPos(1, 77, -24), BlockPos(8, 77, -24), BlockPos(14, 77, -23), BlockPos(21, 77, -19),
        BlockPos(27, 77, -14), BlockPos(31, 77, -8), BlockPos(33, 77, -1), BlockPos(34, 77, 5), BlockPos(33, 77, 12),
        BlockPos(31, 77, 19), BlockPos(27, 77, 25), BlockPos(20, 77, 30), BlockPos(14, 77, 33), BlockPos(7, 77, 34)
    )
    private val m4BlockLocations = hashSetOf(
        BlockPos(-2, 77, 33), BlockPos(-7, 77, 32), BlockPos(-13, 77, 28), BlockPos(-17, 77, 24), BlockPos(-21, 77, 18),
        BlockPos(-23, 77, 13), BlockPos(-24, 77, 7), BlockPos(-24, 77, 2), BlockPos(-23, 77, -4), BlockPos(-21, 77, -9),
        BlockPos(-17, 77, -14), BlockPos(-12, 77, -19), BlockPos(-6, 77, -22), BlockPos(-1, 77, -23), BlockPos(5, 77, -24),
        BlockPos(10, 77, -24), BlockPos(16, 77, -22), BlockPos(21, 77, -19), BlockPos(27, 77, -15), BlockPos(30, 77, -10),
        BlockPos(32, 77, -5), BlockPos(34, 77, 1), BlockPos(34, 77, 7), BlockPos(33, 77, 12), BlockPos(31, 77, 18),
        BlockPos(28, 77, 23), BlockPos(23, 77, 28), BlockPos(18, 77, 31), BlockPos(12, 77, 33), BlockPos(7, 77, 34)
    )
}