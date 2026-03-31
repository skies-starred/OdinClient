package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.renderBoundingBox
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster/*? >= 1.21.11 {*//*.skeleton*//*? }*/.Skeleton
import net.minecraft.world.entity.monster/*? >= 1.21.11 {*//*.skeleton*//*? }*/.WitherSkeleton
import net.minecraft.world.entity.monster/*? >= 1.21.11 {*//*.zombie*//*? }*/.Zombie
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import starred.skies.odin.mixin.accessors.InventoryAccessor
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.RotationUtils
import starred.skies.odin.utils.leftClick
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object AutoDojo : Module(
    name = "Auto Dojo",
    description = "Automatically completes Hypixel SkyBlock dojo tests",
    category = Skit.CHEATS
) {
    private val enableControl by BooleanSetting("Enable Control", true, desc = "Automatically aim at skeleton in Test of Control")
    private val controlPredictionTicks by NumberSetting("Control Prediction Ticks", 5.0, 1.0, 20.0, 1.0, desc = "How many ticks ahead to predict skeleton movement")
    private val enableMastery by BooleanSetting("Enable Mastery", true, desc = "Automatically shoot blocks in Test of Mastery")
    private val masteryShootDelay by NumberSetting("Mastery Shoot Delay (ms)", 600.0, 0.0, 2000.0, 50.0, desc = "Time remaining on yellow block before shooting")
    private val enableDiscipline by BooleanSetting("Enable Discipline", true, desc = "Automatically switch swords in Test of Discipline")
    private val disciplineAutoAttack by BooleanSetting("Discipline Auto Attack", true, desc = "Automatically attack mobs in Test of Discipline")
    private val renderStyle by SelectorSetting("Render Style", "Filled", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")

    private var dojoType = DojoType.NONE
    private var targetSkeleton: Entity? = null
    private var lookCooldown = 0L
    private var lastSkeletonPos: Vec3? = null
    private var skeletonVel = Vec3.ZERO

    // Mastery state
    private val masteryBlocks = mutableListOf<MasteryBlock>()
    private var firingState = 0 // 0: Idle/Drawn, 1: Released (waiting to redraw)
    private var firingTimer = 0
    private var isDrawing = false

    private enum class DojoType {
        NONE, CONTROL, FORCE, MASTERY, DISCIPLINE
    }

    private data class MasteryBlock(
        val x: Int,
        val y: Int,
        val z: Int,
        val color: String,
        val expiryTime: Long
    )

    init {
        on<ChatPacketEvent> {
            val text = value.lowercase()

            if ("rank:" in text) {
                dojoType = DojoType.NONE
                targetSkeleton = null
                masteryBlocks.clear()
                return@on
            }

            if ("objective" !in text) return@on

            when {
                "control" in text -> {
                    dojoType = DojoType.CONTROL
                    lastSkeletonPos = null
                }

                "mastery" in text -> {
                    dojoType = DojoType.MASTERY
                    selectBow()
                }

                "discipline" in text -> {
                    dojoType = DojoType.DISCIPLINE
                }
            }
        }

        on<TickEvent.End> {
            if (dojoType == DojoType.NONE) return@on

            when (dojoType) {
                DojoType.CONTROL -> if (enableControl) handleControl()
                DojoType.MASTERY -> if (enableMastery) handleMastery()
                DojoType.DISCIPLINE -> if (enableDiscipline) handleDiscipline()
                else -> {}
            }
        }

        on<RenderEvent.Extract> {
            if (dojoType == DojoType.NONE) return@on

            // Render control target
            if (dojoType == DojoType.CONTROL && targetSkeleton != null) {
                val entity = targetSkeleton!!
                drawStyledBox(entity.renderBoundingBox, Colors.MINECRAFT_AQUA, renderStyle, false)
            }

            // Render mastery blocks
            if (dojoType == DojoType.MASTERY) {
                for (block in masteryBlocks) {
                    val aabb = AABB(block.x.toDouble(), block.y.toDouble(), block.z.toDouble(), block.x + 1.0, block.y + 1.0, block.z + 1.0)
                    drawStyledBox(aabb, Colors.MINECRAFT_RED, renderStyle, false)
                }
            }

            RotationUtils.update()
        }
    }

    override fun onDisable() {
        dojoType = DojoType.NONE
        targetSkeleton = null
        masteryBlocks.clear()
        RotationUtils.reset()
    }

    private fun handleControl() {
        val player = mc.player ?: return
        val level = mc.level ?: return

        var closestSkeleton: Entity? = null
        var minDist = 25.0
        var skeletonsFound = 0

        // Find nearest wither skeleton (excluding decoys with redstone helmet)
        for (entity in level.entitiesForRendering()) {
            if (entity is WitherSkeleton || (entity is Skeleton && entity.type == EntityType.WITHER_SKELETON)) {
                skeletonsFound++
                if (entity.getItemBySlot(EquipmentSlot.HEAD).item == Items.REDSTONE_BLOCK) continue

                val dist = player.position().distanceTo(entity.position())
                if (dist >= minDist) continue

                minDist = dist
                closestSkeleton = entity
            }
        }

        targetSkeleton = closestSkeleton ?: return

        val currentPos = closestSkeleton.position()
        if (lastSkeletonPos != null) skeletonVel = currentPos.subtract(lastSkeletonPos!!)
        lastSkeletonPos = currentPos

        val now = System.currentTimeMillis()
        if (now - lookCooldown <= 40) return
        lookCooldown = now

        // Predict position based on slider preference
        val predX = currentPos.x + (skeletonVel.x * controlPredictionTicks)
        val predY = currentPos.y + (skeletonVel.y * 2) + 2.5
        val predZ = currentPos.z + (skeletonVel.z * controlPredictionTicks)
        setRotation(predX, predY, predZ)
    }

    private fun handleMastery() {
        val player = mc.player ?: return
        val level = mc.level ?: return
        val now = System.currentTimeMillis()

        // Clean up expired blocks and verify blocks still exist
        masteryBlocks.removeAll { block ->
            if (block.expiryTime < now) return@removeAll true

            val state = level.getBlockState(BlockPos(block.x, block.y, block.z))
            state?.block != Blocks.YELLOW_WOOL
        }

        // Handle redrawing sequence (2 ticks delay)
        if (firingState == 1) {
            firingTimer++
            if (firingTimer < 2) return

            mc.options.keyUse.isDown = true
            isDrawing = true
            firingState = 0
            firingTimer = 0
            return
        }

        // Scan for yellow wool blocks
        scanForMasteryBlocks()
        val closest = masteryBlocks.firstOrNull() ?: return

        setRotation(closest.x + 0.5, closest.y + 1.1, closest.z + 0.5)

        val bowSlot = findItemSlot(Items.BOW) ?: return
        (player.inventory as InventoryAccessor).setSelectedSlot(bowSlot)

        if (!isDrawing) {
            mc.options.keyUse.isDown = true
            isDrawing = true
        }

        if (closest.color != "yellow") return

        val timeRemaining = closest.expiryTime - now
        if (timeRemaining >= masteryShootDelay) return
        if (!isDrawing) return

        mc.options.keyUse.isDown = false
        isDrawing = false
        firingState = 1
        firingTimer = 0
        masteryBlocks.removeAt(0)
    }

    private fun handleDiscipline() {
        val player = mc.player ?: return
        val level = mc.level ?: return

        var bestZombie: Zombie? = null
        var minDistance = 7.0 // Maximum range
        var zombiesFound = 0

        // Find closest zombie in FOV (fixes issues with line-ups)
        for (entity in level.entitiesForRendering()) {
            if (entity !is Zombie) continue
            zombiesFound++

            val dx = entity.x - player.x
            val dy = (entity.y + 1.2) - (player.y + player.eyeHeight)
            val dz = entity.z - player.z
            val dist = sqrt(dx * dx + dy * dy + dz * dz)

            if (dist > 6.0) continue
            val targetYaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
            val targetPitch = Math.toDegrees(atan2(-dy, sqrt(dx * dx + dz * dz))).toFloat()

            val yawDiff = abs(normalizeAngle(targetYaw - player.yRot))
            val pitchDiff = abs(targetPitch - player.xRot)

            if (yawDiff >= 20 || pitchDiff >= 35) continue
            if (dist >= minDistance) continue

            minDistance = dist
            bestZombie = entity
        }

        if (bestZombie == null) return
        // Check helmet to determine sword type
        val helmet = bestZombie.getItemBySlot(EquipmentSlot.HEAD).item
        val targetSword = when (helmet) {
            Items.LEATHER_HELMET -> Items.WOODEN_SWORD
            Items.IRON_HELMET -> Items.IRON_SWORD
            Items.GOLDEN_HELMET -> Items.GOLDEN_SWORD
            Items.DIAMOND_HELMET -> Items.DIAMOND_SWORD
            else -> return
        }

        val swordSlot = findItemSlot(targetSword) ?: return
        val inventory = player.inventory as InventoryAccessor
        if (inventory.selectedSlot != swordSlot) inventory.setSelectedSlot(swordSlot)
        if (disciplineAutoAttack) leftClick()
    }

    private fun scanForMasteryBlocks() {
        val player = mc.player ?: return
        val level = mc.level ?: return
        val now = System.currentTimeMillis()

        // Scan nearby blocks for yellow wool (within 25 block range)
        val playerPos = player.blockPosition()
        for (x in -25..25) {
            for (y in -10..10) {
                for (z in -25..25) {
                    val pos = playerPos.offset(x, y, z)
                    val dist = sqrt((x * x + z * z).toDouble())
                    if (dist > 25) continue
                    if (level.getBlockState(pos)?.block != Blocks.YELLOW_WOOL) continue

                    val isDuplicate = masteryBlocks.any { it.x == pos.x && it.z == pos.z && it.color == "yellow" }
                    if (!isDuplicate) masteryBlocks.add(MasteryBlock(pos.x, pos.y, pos.z, "yellow", now + 3500))
                }
            }
        }
    }

    private fun setRotation(x: Double, y: Double, z: Double) {
        val rot = RotationUtils.getRotation(x, y, z)
        RotationUtils.smartSmoothLook(rot.yaw, rot.pitch, 350)
    }

    private fun findItemSlot(it: Item): Int? {
        val player = mc.player ?: return null
        for (i in 0..8) if (player.inventory.getItem(i)?.item  == it) return i
        return null
    }

    private fun selectBow() {
        val player = mc.player ?: return
        val bowSlot = findItemSlot(Items.BOW) ?: return
        (player.inventory as InventoryAccessor).setSelectedSlot(bowSlot)
    }

    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360
        if (normalized > 180) normalized -= 360
        if (normalized < -180) normalized += 360
        return normalized
    }
}
