package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color.Companion.multiplyAlpha
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.addVec
import com.odtheking.odin.utils.render.drawFilledBox
import com.odtheking.odin.utils.render.drawLine
import com.odtheking.odin.utils.render.drawWireFrameBox
import com.odtheking.odin.utils.renderPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.projectile.AbstractArrow
import net.minecraft.world.level.ClipContext
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import starred.skies.odin.utils.Skit
import kotlin.math.*

//? if < 1.21.10 {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
*///? }

object Trajectories : Module(
    name = "Trajectories",
    description = "Shows the trajectories of arrows, snowballs, etc.",
    category = Skit.CHEATS
) {
    private val bows by BooleanSetting("Bows", true, desc = "Render trajectories of bow arrows.")
    private val pearls by BooleanSetting("Pearls", true, desc = "Render trajectories of ender pearls.")
    private val plane by BooleanSetting("Show Plane", false, desc = "Shows a flat square rotated relative to the predicted block that will be hit.")
    private val boxes by BooleanSetting("Show Boxes", true, desc = "Shows boxes displaying where arrows or pearls will hit.")
    private val lines by BooleanSetting("Show Lines", true, desc = "Shows the trajectory as a line.")
    private val range by NumberSetting("Solver Range", 30, 1, 120, 1, desc = "How many ticks are simulated.")
    private val width by NumberSetting("Line Width", 1f, 0.1f, 5.0, 0.1f, desc = "The width of the line.")
    private val planeSize by NumberSetting("Plane Size", 2f, 0.1f, 5.0, 0.1f, desc = "The size of the plane.").withDependency { plane }
    private val boxSize by NumberSetting("Box Size", 0.5f, 0.5f, 3.0f, 0.1f, desc = "The size of the box.").withDependency { boxes }
    private val color by ColorSetting("Color", Colors.MINECRAFT_DARK_AQUA, true, desc = "The color of the trajectory.")
    private val depth by BooleanSetting("Depth Check", true, desc = "Whether or not to depth check the trajectory.")

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

        on<RenderEvent./*? if >=1.21.10 {*/Extract/*?} else {*//*Last*//*?}*/> {
            entityRenderQueue.clear()
            boxRenderQueue.clear()
            pearlImpactPos = null

            val player = mc.player ?: return@on
            val heldItem = player.mainHandItem

            if (bows && heldItem.item is BowItem) {
                val trajectory = /*? if <1.21.10 {*//*context.*//*?}*/calculateTrajectory(0f, isPearl = false, useCharge = true)

                if (lines) /*? if <1.21.10 {*//*context.*//*?}*/drawLine(trajectory.first, color, depth, width)
                if (boxes) /*? if <1.21.10 {*//*context.*//*?}*/drawCollisionBoxes(isPearl = false)
                trajectory.second?.let { hit ->
                    if (plane) /*? if <1.21.10 {*//*context.*//*?}*/drawPlaneCollision(hit)
                }
            }

            if (pearls && heldItem.item is EnderpearlItem) {
                if (heldItem.displayName?.string?.contains("Spirit") == true) return@on

                val trajectory = /*? if <1.21.10 {*//*context.*//*?}*/calculateTrajectory(0f, isPearl = true)

                if (lines) /*? if <1.21.10 {*//*context.*//*?}*/drawLine(trajectory.first, color, depth, width)
                if (boxes) /*? if <1.21.10 {*//*context.*//*?}*/drawCollisionBoxes(isPearl = true)
                trajectory.second?.let { hit ->
                    if (plane) /*? if <1.21.10 {*//*context.*//*?}*/drawPlaneCollision(hit)
                }
            }
        }
    }

    private fun /*? >= 1.21.10 { */RenderEvent.Extract/*? } else {*//*WorldRenderContext*//*? }*/.calculateTrajectory(
        yawOffset: Float,
        isPearl: Boolean,
        useCharge: Boolean = false
    ): Pair<List<Vec3>, BlockHitResult?> {
        val player = mc.player ?: return emptyList<Vec3>() to null
        val level = mc.level ?: return emptyList<Vec3>() to null

        val yaw = Math.toRadians(player.yRot.toDouble())
        val x = -cos(yaw) * 0.16
        val y = player.eyeHeight - 0.1
        val z = -sin(yaw) * 0.16
        val offset = Vec3(x, y, z)
        var pos = player.renderPos.add(offset)

        val velocityMultiplier = if (isPearl) 1.5f else (if (!useCharge) 2f else lastCharge + (charge - lastCharge) * mc.deltaTracker.getGameTimeDeltaPartialTick(true)) * 1.5f
        var motion = getLook(player.yRot + yawOffset, player.xRot).normalize().scale(velocityMultiplier.toDouble())

        var hitResult = false
        val lines = mutableListOf<Vec3>()
        var rayTraceHit: BlockHitResult? = null

        repeat(range) {
            if (hitResult) return@repeat
            lines.add(pos)

            if (!isPearl) {
                val aabb = AABB(pos.x - 0.5, pos.y - 0.5, pos.z - 0.5, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5).inflate(0.01)
                val entityHit = level.getEntities(player, aabb).filter { it !is AbstractArrow && it !is ArmorStand }

                if (entityHit.isNotEmpty()) {
                    hitResult = true
                    entityRenderQueue.addAll(entityHit)
                    return@repeat
                }
            }

            val blockHit = level.clip(ClipContext(pos, pos.add(motion), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
            if (blockHit.type == HitResult.Type.BLOCK) {
                rayTraceHit = blockHit as BlockHitResult
                lines.add(blockHit.location)

                if (boxes) {
                    val box = AABB(
                        blockHit.location.x - 0.15 * boxSize, blockHit.location.y - 0.15 * boxSize, blockHit.location.z - 0.15 * boxSize,
                        blockHit.location.x + 0.15 * boxSize, blockHit.location.y + 0.15 * boxSize, blockHit.location.z + 0.15 * boxSize
                    )

                    if (isPearl) pearlImpactPos = box else boxRenderQueue.add(box)
                }

                hitResult = true
            }

            pos = pos.add(motion)
            motion = if (isPearl) Vec3(motion.x * 0.99, motion.y * 0.99 - 0.03, motion.z * 0.99) else Vec3(motion.x * 0.99, motion.y * 0.99 - 0.05, motion.z * 0.99)
        }

        return lines to rayTraceHit
    }

    private fun /*? >= 1.21.10 { */RenderEvent.Extract/*? } else {*//*WorldRenderContext*//*? }*/.drawPlaneCollision(hit: BlockHitResult) {
        val (vec1, vec2) = when (hit.direction) {
            Direction.DOWN, Direction.UP -> hit.location.addVec(-0.15 * planeSize, -0.02, -0.15 * planeSize) to hit.location.addVec(0.15 * planeSize, 0.02, 0.15 * planeSize)
            Direction.NORTH, Direction.SOUTH -> hit.location.addVec(-0.15 * planeSize, -0.15 * planeSize, -0.02) to hit.location.addVec(0.15 * planeSize, 0.15 * planeSize, 0.02)
            Direction.WEST, Direction.EAST -> hit.location.addVec(-0.02, -0.15 * planeSize, -0.15 * planeSize) to hit.location.addVec(0.02, 0.15 * planeSize, 0.15 * planeSize)
            else -> return
        }

        drawFilledBox(AABB(vec1.x, vec1.y, vec1.z, vec2.x, vec2.y, vec2.z), color.multiplyAlpha(0.5f), depth)
    }

    private fun /*? >= 1.21.10 { */RenderEvent.Extract/*? } else {*//*WorldRenderContext*//*? }*/.drawCollisionBoxes(isPearl: Boolean) {
        if (isPearl) {
            pearlImpactPos?.let { aabb ->
                drawWireFrameBox(aabb, color, width, depth)
                drawFilledBox(aabb, color.multiplyAlpha(0.3f), depth)
            }
        } else {
            for (box in boxRenderQueue) {
                drawWireFrameBox(box, color, width, depth)
                drawFilledBox(box, color.multiplyAlpha(0.3f), depth)
            }
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
}