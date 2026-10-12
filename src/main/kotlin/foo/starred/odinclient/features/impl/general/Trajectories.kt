/**
 * Taken from OdinClient 1.8.9
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

package foo.starred.odinclient.features.impl.general

//~ if >= 26.2 'Setting' -> 'RenderableSetting'
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.*
import com.odtheking.odin.utils.Color.Companion.multiplyAlpha
import com.odtheking.odin.utils.render.drawFilledBox
import com.odtheking.odin.utils.render.drawLine
import com.odtheking.odin.utils.render.drawWireFrameBox
import foo.starred.odinclient.api.category.OdinClientCategory
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object Trajectories : Module(
    name = "Trajectories",
    description = "Shows the trajectories of arrows, snowballs, etc.",
    category = OdinClientCategory.CHEATS
) {
    private val bows by BooleanSetting("Bows", true, desc = "Render trajectories of bow arrows.")
    private val pearls by BooleanSetting("Pearls", true, desc = "Render trajectories of ender pearls.")
    private val plane by BooleanSetting("Show Plane", false, desc = "Shows a flat square rotated relative to the predicted block that will be hit.")
    private val boxes by BooleanSetting("Show Boxes", true, desc = "Shows boxes displaying where arrows or pearls will hit.")
    private val entities by BooleanSetting("Show Entities", true, desc = "Show boxes highlighting entities which the arrows will hit.")
    private val lines by BooleanSetting("Show Lines", true, desc = "Shows the trajectory as a line.")
    //~ if >= 26.2 '1, 120' -> '1..120'
    private val range by NumberSetting("Solver Range", 30, 1, 120, 1, desc = "How many ticks are simulated.")
    //~ if >= 26.2 '0.1f, 5.0' -> '0.1..5.0'
    private val width by NumberSetting("Line Width", 1f, 0.1f, 5.0, 0.1f, desc = "The width of the line.")
    //~ if >= 26.2 '0.1f, 5.0' -> '0.1..5.0'
    private val planeSize by NumberSetting("Plane Size", 2f, 0.1f, 5.0, 0.1f, desc = "The size of the plane.").withDependency { plane }
    //~ if >= 26.2 '0.5f, 3.0f' -> '0.5..3.0'
    private val boxSize by NumberSetting("Box Size", 0.5f, 0.5f, 3.0f, 0.1f, desc = "The size of the box.").withDependency { boxes }
    private val color by ColorSetting("Color", Colors.MINECRAFT_DARK_AQUA, true, desc = "The color of the trajectory.")
    private val depth by BooleanSetting("Depth Check", true, desc = "Whether or not to depth check the trajectory.")

    private val VEC_NULL = emptyList<Vec3>() to null

    private var charge = 0f
    private var lastCharge = 0f
    private val boxRenderQueue = mutableListOf<AABB>()
    private val entityRenderQueue = mutableListOf<Entity>()
    private var pearlImpactPos: AABB? = null

    init {
        on<TickEvent.End> {
            val player = mc.player ?: return@on
            lastCharge = charge
            val useCount = player.useItemRemainingTicks
            charge = min((72000 - useCount) / 20f, 1.0f) * 2f
            if ((lastCharge - charge) > 1f) lastCharge = charge
        }

        //~ if >= 26.2 'RenderEvent.Extract' -> 'RenderExtractEvent'
        on<RenderEvent.Extract> {
            entityRenderQueue.clear()
            boxRenderQueue.clear()
            pearlImpactPos = null

            val player = mc.player ?: return@on
            val heldItem = player.mainHandItem
            val term = heldItem.itemId == "TERMINATOR"

            if (bows && heldItem.item is BowItem) {
                val l =
                    if (term) listOf(calculateTrajectory(0f, isPearl = false), calculateTrajectory(-5f, isPearl = false), calculateTrajectory(5f, isPearl = false))
                    else listOf(calculateTrajectory(0f, isPearl = false, useCharge = true))

                for ((a, b) in l) {
                    if (lines) drawLine(a, color, depth, width)
                    if (boxes) drawCollisionBoxes(isPearl = false)
                    if (b != null) if (plane) drawPlaneCollision(b)
                }

                return@on
            }

            if (pearls && heldItem.item is EnderpearlItem) {
                if (heldItem.displayName.string.contains("Spirit")) return@on

                val (a, b) = calculateTrajectory(0f, isPearl = true)

                if (lines) drawLine(a, color, depth, width)
                if (boxes) drawCollisionBoxes(isPearl = true)
                if (b != null) if (plane) drawPlaneCollision(b)
            }
        }
    }

    private fun calculateTrajectory(yawOffset: Float, isPearl: Boolean, useCharge: Boolean = false): Pair<List<Vec3>, BlockHitResult?> {
        val player = mc.player ?: return VEC_NULL
        val level = mc.level ?: return VEC_NULL

        val yaw = Math.toRadians(player.yRot.toDouble())
        val x = -cos(yaw) * 0.16
        val z = -sin(yaw) * 0.16
        var pos = player.renderPos.add(Vec3(x, player.eyeHeight - 0.1, z))
        var prevPos = pos

        val speed = if (isPearl) 1.5 else pull(useCharge) * 3.0
        var motion = getLook(player.yRot + yawOffset, player.xRot).normalize().scale(speed)

        var hitResult = false
        val lines = mutableListOf<Vec3>()
        var rayTraceHit: BlockHitResult? = null

        repeat(range) {
            if (hitResult) return@repeat
            lines.add(pos)

            if (!isPearl) {
                val scanBox = AABB(prevPos, pos.add(motion)).inflate(1.0)
                val hit = level.getEntities(player, scanBox)
                    .filter { it !is AbstractArrow && it !is ArmorStand }
                    .mapNotNull { entity ->
                        entity.boundingBox.inflate(entity.pickRadius.toDouble())
                            .clip(prevPos, pos.add(motion))
                            .map { entity to it }
                            .orElse(null)
                    }
                    .minByOrNull { (_, hitPos) -> prevPos.distanceToSqr(hitPos) }

                if (hit != null) {
                    val (entity, hitPos) = hit
                    lines.add(hitPos)
                    entityRenderQueue.add(entity)
                    hitResult = true
                    return@repeat
                }
            }

            val blockHit = level.clip(ClipContext(pos, pos.add(motion), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
            if (blockHit.type == HitResult.Type.BLOCK) {
                rayTraceHit = blockHit
                lines.add(blockHit.location)

                if (boxes) {
                    val box = AABB(blockHit.location.x - 0.15 * boxSize, blockHit.location.y - 0.15 * boxSize, blockHit.location.z - 0.15 * boxSize, blockHit.location.x + 0.15 * boxSize, blockHit.location.y + 0.15 * boxSize, blockHit.location.z + 0.15 * boxSize)
                    if (isPearl) pearlImpactPos = box else boxRenderQueue.add(box)
                }

                hitResult = true
            }

            if (isPearl) {
                motion = Vec3(motion.x * 0.99, (motion.y - 0.03) * 0.99, motion.z * 0.99)
                pos = pos.add(motion)
                return@repeat
            }

            pos = pos.add(motion)
            motion = Vec3(motion.x * 0.99, motion.y * 0.99 - 0.05, motion.z * 0.99)
            prevPos = pos
        }

        return lines to rayTraceHit
    }

    //~ if >= 26.2 'RenderEvent.Extract' -> 'RenderExtractEvent'
    private fun RenderEvent.Extract.drawPlaneCollision(hit: BlockHitResult) {
        val (vec1, vec2) = when (hit.direction) {
            Direction.DOWN, Direction.UP -> hit.location.addVec(-0.15 * planeSize, -0.02, -0.15 * planeSize) to hit.location.addVec(0.15 * planeSize, 0.02, 0.15 * planeSize)
            Direction.NORTH, Direction.SOUTH -> hit.location.addVec(-0.15 * planeSize, -0.15 * planeSize, -0.02) to hit.location.addVec(0.15 * planeSize, 0.15 * planeSize, 0.02)
            Direction.WEST, Direction.EAST -> hit.location.addVec(-0.02, -0.15 * planeSize, -0.15 * planeSize) to hit.location.addVec(0.02, 0.15 * planeSize, 0.15 * planeSize)
        }

        drawFilledBox(AABB(vec1.x, vec1.y, vec1.z, vec2.x, vec2.y, vec2.z), color.multiplyAlpha(0.5f), depth)
    }

    //~ if >= 26.2 'RenderEvent.Extract' -> 'RenderExtractEvent'
    private fun RenderEvent.Extract.drawCollisionBoxes(isPearl: Boolean) {
        if (isPearl) {
            pearlImpactPos?.let { aabb ->
                drawWireFrameBox(aabb, color, width, depth)
                drawFilledBox(aabb, color.multiplyAlpha(0.3f), depth)
            }
            return
        }

        for (box in boxRenderQueue) {
            drawWireFrameBox(box, color, width, depth)
            drawFilledBox(box, color.multiplyAlpha(0.3f), depth)
        }

        if (!entities) return
        for (entity in entityRenderQueue) {
            val aabb = entity.renderBoundingBox
            drawWireFrameBox(aabb, color, width, depth)
            drawFilledBox(aabb, color.multiplyAlpha(0.3f), depth)
        }
    }

    private fun getLook(yaw: Float, pitch: Float): Vec3 {
        val f2 = -cos(-pitch * 0.017453292) * 1.0
        return Vec3(
            sin(-yaw * 0.017453292 - Math.PI) * f2,
            sin(-pitch * 0.017453292) * 1.0,
            cos(-yaw * 0.017453292 - Math.PI) * f2
        )
    }

    private fun pull(b: Boolean): Float {
        if (!b) return 1f

        val t = lastCharge + (charge - lastCharge) * mc.deltaTracker.getGameTimeDeltaPartialTick(true)
        val f = (t / 2f).coerceIn(0f, 1f)
        return (f * f + f * 2f) / 3f
    }
}
