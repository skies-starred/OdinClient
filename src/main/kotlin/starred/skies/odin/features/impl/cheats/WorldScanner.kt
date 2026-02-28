package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.drawText
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import starred.skies.odin.utils.Skit
import com.odtheking.odin.OdinMod.mc
import java.util.concurrent.ConcurrentHashMap

object WorldScanner : Module(
    name = "World Scanner",
    description = "Scans and highlights structures in Crystal Hollows",
    category = Skit.CHEATS
) {
    private val scanCrystals by BooleanSetting("Scan Crystals", true, desc = "Scans for crystal waypoints")
    private val scanMobSpots by BooleanSetting("Scan Mob Spots", true, desc = "Scans for mob spawn locations")
    private val scanFairyGrottos by BooleanSetting("Scan Fairy Grottos", true, desc = "Scans for fairy grottos")
    private val scanDragonNest by BooleanSetting("Scan Dragon Nest", true, desc = "Scans for golden dragon nest")
    private val scanWormFishing by BooleanSetting("Scan Worm Fishing", false, desc = "Scans for worm fishing spots")
    private val lavaEsp by BooleanSetting("Lava ESP", false, desc = "Highlights lava blocks")
    private val waterEsp by BooleanSetting("Water ESP", false, desc = "Highlights water blocks")
    private val espRange by NumberSetting("ESP Range", 32, 8, 128, 1, unit = "m", desc = "Range for lava and water ESP")
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")
    private val renderText by BooleanSetting("Render Text", true, desc = "Renders 3D text labels at waypoints")
    private val sendCoordsInChat by BooleanSetting("Send Coords in Chat", true, desc = "Sends coordinates to chat when found")

    private val waypoints = ConcurrentHashMap<String, WaypointData>()
    data class WaypointData(val pos: BlockPos, val category: Int, val color: Color)

    private val scannedChunks = HashSet<Long>()
    private val lavaBlocks = ConcurrentHashMap.newKeySet<Long>()
    private val waterBlocks = ConcurrentHashMap.newKeySet<Long>()

    private enum class Quarter {
        NUCLEUS, JUNGLE, PRECURSOR, GOBLIN, MITHRIL, MAGMA, ANY;
        fun test(x: Int, y: Int, z: Int): Boolean {
            return when (this) {
                NUCLEUS -> x in 449..576 && z in 449..576
                JUNGLE -> x <= 576 && z <= 576
                PRECURSOR -> x > 448 && z > 448
                GOBLIN -> x <= 576 && z > 448
                MITHRIL -> x > 448 && z <= 576
                MAGMA -> y < 80
                ANY -> true
            }
        }
    }

    private enum class Structure(
        val displayName: String,
        val blocks: List<Block?>,
        val color: Color,
        val quarter: Quarter,
        val offset: BlockPos = BlockPos.ZERO,
        val category: Int // 0: Crystal, 1: Mob, 2: Grotto, 3: Dragon, 4: Worm
    ) {
        KING("King", listOf(Blocks.RED_WOOL, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_STAIRS), Colors.MINECRAFT_GOLD, Quarter.GOBLIN, BlockPos(1, -1, 2), 0),
        QUEEN("Queen", listOf(Blocks.STONE, Blocks.ACACIA_WOOD, Blocks.ACACIA_WOOD, Blocks.ACACIA_WOOD, Blocks.ACACIA_WOOD, Blocks.CAULDRON), Colors.MINECRAFT_GOLD, Quarter.ANY, BlockPos(0, 5, 0), 0),
        DIVAN("Divan", listOf(Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_STAIRS, Blocks.STONE_BRICK_STAIRS, Blocks.CHISELED_STONE_BRICKS), Colors.MINECRAFT_GREEN, Quarter.MITHRIL, BlockPos(0, 5, 0), 0),
        CITY("City", listOf(Blocks.STONE_BRICKS, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE_STAIRS, Blocks.POLISHED_ANDESITE, Blocks.POLISHED_ANDESITE, Blocks.DARK_OAK_STAIRS), Colors.MINECRAFT_AQUA, Quarter.PRECURSOR, BlockPos(24, 0, -17), 0),
        TEMPLE("Temple", listOf(Blocks.BEDROCK, Blocks.BEDROCK, Blocks.BEDROCK, Blocks.BEDROCK, Blocks.STONE, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, Blocks.OAK_LEAVES, Blocks.OAK_LEAVES, Blocks.LIME_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.GREEN_TERRACOTTA), Colors.MINECRAFT_DARK_PURPLE, Quarter.ANY, BlockPos(-45, 47, -18), 0),
        BAL("Bal", listOf(Blocks.LAVA, Blocks.BARRIER, Blocks.BARRIER, Blocks.BARRIER, Blocks.BARRIER, Blocks.BARRIER, Blocks.BARRIER, Blocks.BARRIER, Blocks.BARRIER, Blocks.BARRIER, Blocks.BARRIER), Colors.MINECRAFT_GOLD, Quarter.MAGMA, BlockPos(0, 1, 0), 0),
        CORLEONE_DOCK("Corleone Dock", listOf(
            Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, // 0-3
            null, null, null, null, null, null, null, null, null, null, // 4-13
            null, null, null, null, null, null, null, null, null, null, // 14-23
            Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.FIRE, Blocks.STONE_BRICKS // 24-27
        ), Colors.MINECRAFT_GREEN, Quarter.MITHRIL, BlockPos(23, 11, 17), 1),
        CORLEONE_HOLE("Corleone Hole", listOf(Blocks.SMOOTH_STONE_SLAB, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS, Blocks.POLISHED_GRANITE), Colors.MINECRAFT_GREEN, Quarter.MITHRIL, BlockPos(-18, -1, 29), 1),
        KEY_GUARDIAN_SPIRAL("Key Guardian Spiral", listOf(Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_PLANKS, Blocks.GLOWSTONE), Colors.MINECRAFT_DARK_PURPLE, Quarter.JUNGLE, BlockPos.ZERO, 1),
        KEY_GUARDIAN_TOWER("Key Guardian Tower", listOf(Blocks.STONE, Blocks.POLISHED_GRANITE, Blocks.JUNGLE_SLAB), Colors.MINECRAFT_DARK_PURPLE, Quarter.JUNGLE, BlockPos.ZERO, 1),
        XALX("Xalx", listOf(Blocks.STONE, Blocks.COAL_BLOCK, Blocks.FIRE, Blocks.NETHER_QUARTZ_ORE, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR), Colors.MINECRAFT_GREEN, Quarter.GOBLIN, BlockPos(-2, 1, -2), 1),
        PETE("Pete", listOf(Blocks.NETHERRACK, Blocks.FIRE, Blocks.IRON_BARS, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR), Colors.MINECRAFT_GOLD, Quarter.GOBLIN, BlockPos.ZERO, 1),
        ODAWA("Odawa", listOf(Blocks.JUNGLE_LOG, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.JUNGLE_LOG), Colors.MINECRAFT_GREEN, Quarter.JUNGLE, BlockPos.ZERO, 1),
        GOLDEN_DRAGON("Golden Dragon", listOf(Blocks.STONE, Blocks.RED_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.PLAYER_HEAD, Blocks.RED_WOOL), Colors.WHITE, Quarter.ANY, BlockPos(0, -3, 5), 3)
    }

    init {
        ClientChunkEvents.CHUNK_LOAD.register { _, chunk ->
            if (!enabled || !LocationUtils.isCurrentArea(Island.CrystalHollows)) return@register
            val chunkKey = getChunkKey(chunk)
            if (!scannedChunks.contains(chunkKey)) {
                handleChunkLoad(chunk)
                scannedChunks.add(chunkKey)
            }
        }

        on<WorldEvent.Load> {
            clearWaypoints()
        }

        on<RenderEvent.Extract> {
            if (!LocationUtils.isCurrentArea(Island.CrystalHollows)) return@on

            for ((name, data) in waypoints) {
                val shouldRender = when (data.category) {
                    0 -> scanCrystals
                    1 -> scanMobSpots
                    2 -> scanFairyGrottos
                    3 -> scanDragonNest
                    4 -> scanWormFishing
                    else -> false
                }
                if (!shouldRender) continue

                renderWaypoint(name, data.pos, data.color)
            }

            if (lavaEsp || waterEsp) {
                val playerPos = mc.player?.position() ?: return@on
                val rangeSq = espRange * espRange
                val localMutablePos = BlockPos.MutableBlockPos()

                if (lavaEsp) {
                    lavaBlocks.forEach { packed ->
                        localMutablePos.set(packed)
                        if (localMutablePos.distToCenterSqr(playerPos) <= rangeSq) {
                            renderWaypoint("Lava", localMutablePos, Colors.MINECRAFT_GOLD, false)
                        }
                    }
                }
                if (waterEsp) {
                    waterBlocks.forEach { packed ->
                        localMutablePos.set(packed)
                        if (localMutablePos.distToCenterSqr(playerPos) <= rangeSq) {
                            renderWaypoint("Water", localMutablePos, Colors.MINECRAFT_AQUA, false)
                        }
                    }
                }
            }
        }
    }

    private fun RenderEvent.Extract.renderWaypoint(name: String, pos: BlockPos, color: Color, showText: Boolean = true) {
        val centerPos = Vec3.atCenterOf(pos)
        val aabb = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos))
        drawStyledBox(aabb, color, renderStyle, false)
        
        if (renderText && showText) {
            val distance = mc.player?.distanceToSqr(centerPos) ?: 1.0
            val distMeters = kotlin.math.sqrt(distance)
            val scale = (distMeters / 10.0).coerceAtLeast(1.0).toFloat()
            drawText(name, centerPos.add(0.0, 1.5, 0.0), scale, false)
            drawText("§7${distMeters.toInt()}m", centerPos.add(0.0, 1.5 - (0.2 * scale), 0.0), scale * 0.8f, false)
        }
    }

    private fun clearWaypoints() {
        waypoints.clear()
        scannedChunks.clear()
        lavaBlocks.clear()
        waterBlocks.clear()
    }


    private fun handleChunkLoad(chunk: LevelChunk) {
        val mutablePos = BlockPos.MutableBlockPos()
        for (x in 0..15) {
            for (z in 0..15) {
                val worldX = chunk.pos.x * 16 + x
                val worldZ = chunk.pos.z * 16 + z
                
                for (y in 0..170) {
                    val state = getBlockState(chunk, x, y, z)
                    if (state.isAir) continue
                    val block = state.block

                    // High efficiency branch based on entry-point block
                    when (block) {
                        Blocks.RED_WOOL -> {
                            if (scanCrystals && !waypoints.containsKey("King") && Quarter.GOBLIN.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.KING.blocks)) addWaypoint(Structure.KING, mutablePos.set(worldX, y, worldZ))
                            }
                        }
                        Blocks.STONE -> {
                            if (scanCrystals && !waypoints.containsKey("Queen") && Quarter.ANY.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.QUEEN.blocks)) addWaypoint(Structure.QUEEN, mutablePos.set(worldX, y, worldZ))
                            }
                            if (scanMobSpots && !waypoints.containsKey("Key Guardian Tower") && Quarter.JUNGLE.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.KEY_GUARDIAN_TOWER.blocks)) addWaypoint(Structure.KEY_GUARDIAN_TOWER, mutablePos.set(worldX, y, worldZ))
                            }
                            if (scanMobSpots && !waypoints.containsKey("Xalx") && Quarter.GOBLIN.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.XALX.blocks)) addWaypoint(Structure.XALX, mutablePos.set(worldX, y, worldZ))
                            }
                            if (scanDragonNest && !waypoints.containsKey("Golden Dragon") && Quarter.ANY.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.GOLDEN_DRAGON.blocks)) addWaypoint(Structure.GOLDEN_DRAGON, mutablePos.set(worldX, y, worldZ))
                            }
                        }
                        Blocks.QUARTZ_PILLAR -> {
                            if (scanCrystals && !waypoints.containsKey("Divan") && Quarter.MITHRIL.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.DIVAN.blocks)) addWaypoint(Structure.DIVAN, mutablePos.set(worldX, y, worldZ))
                            }
                        }
                        Blocks.STONE_BRICKS -> {
                            if (scanCrystals && !waypoints.containsKey("City") && Quarter.PRECURSOR.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.CITY.blocks)) addWaypoint(Structure.CITY, mutablePos.set(worldX, y, worldZ))
                            }
                            if (scanMobSpots && !waypoints.containsKey("Corleone Dock") && Quarter.MITHRIL.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.CORLEONE_DOCK.blocks)) addWaypoint(Structure.CORLEONE_DOCK, mutablePos.set(worldX, y, worldZ))
                            }
                        }
                        Blocks.BEDROCK -> {
                            if (scanCrystals && !waypoints.containsKey("Temple") && Quarter.ANY.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.TEMPLE.blocks)) addWaypoint(Structure.TEMPLE, mutablePos.set(worldX, y, worldZ))
                            }
                        }
                        Blocks.LAVA -> {
                            if (lavaEsp && getBlockState(chunk, x, y + 1, z).isAir) {
                                lavaBlocks.add(mutablePos.set(worldX, y, worldZ).asLong())
                            }
                            if (scanCrystals && !waypoints.containsKey("Bal") && Quarter.MAGMA.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.BAL.blocks)) addWaypoint(Structure.BAL, mutablePos.set(worldX, y, worldZ))
                            }
                            if (scanWormFishing && y > 63 && !waypoints.containsKey("Worm Fishing") && ((worldX >= 564 && worldZ >= 513) || (worldX >= 513 && worldZ >= 564))) {
                                if (getBlockState(chunk, x, y + 1, z).isAir) {
                                    addWaypointDirect("Worm Fishing", mutablePos.set(worldX, y, worldZ), Colors.MINECRAFT_GOLD, 4)
                                }
                            }
                        }
                        Blocks.WATER -> {
                            if (waterEsp && getBlockState(chunk, x, y + 1, z).isAir) {
                                waterBlocks.add(mutablePos.set(worldX, y, worldZ).asLong())
                            }
                        }
                        Blocks.SMOOTH_STONE_SLAB -> {
                            if (scanMobSpots && !waypoints.containsKey("Corleone Hole") && Quarter.MITHRIL.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.CORLEONE_HOLE.blocks)) addWaypoint(Structure.CORLEONE_HOLE, mutablePos.set(worldX, y, worldZ))
                            }
                        }
                        Blocks.JUNGLE_STAIRS -> {
                            if (scanMobSpots && !waypoints.containsKey("Key Guardian Spiral") && Quarter.JUNGLE.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.KEY_GUARDIAN_SPIRAL.blocks)) addWaypoint(Structure.KEY_GUARDIAN_SPIRAL, mutablePos.set(worldX, y, worldZ))
                            }
                        }
                        Blocks.NETHERRACK -> {
                            if (scanMobSpots && !waypoints.containsKey("Pete") && Quarter.GOBLIN.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.PETE.blocks)) addWaypoint(Structure.PETE, mutablePos.set(worldX, y, worldZ))
                            }
                        }
                        Blocks.JUNGLE_LOG -> {
                            if (scanMobSpots && !waypoints.containsKey("Odawa") && Quarter.JUNGLE.test(worldX, y, worldZ)) {
                                if (checkSequence(chunk, x, y, z, Structure.ODAWA.blocks)) addWaypoint(Structure.ODAWA, mutablePos.set(worldX, y, worldZ))
                            }
                        }
                        Blocks.MAGENTA_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS_PANE -> {
                            if (scanFairyGrottos && !waypoints.containsKey("Fairy Grotto") && !Quarter.NUCLEUS.test(worldX, y, worldZ)) {
                                addWaypointDirect("Fairy Grotto", mutablePos.set(worldX, y, worldZ), Colors.MINECRAFT_LIGHT_PURPLE, 2)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addWaypoint(structure: Structure, pos: BlockPos) {
        val targetPos = pos.offset(structure.offset)
        if (!waypoints.containsKey(structure.displayName)) {
            waypoints[structure.displayName] = WaypointData(targetPos.immutable(), structure.category, structure.color)
            if (sendCoordsInChat) modMessage("${structure.displayName} found at ${targetPos.x}, ${targetPos.y}, ${targetPos.z}")
        }
    }

    private fun addWaypointDirect(name: String, pos: BlockPos, color: Color, category: Int) {
        if (!waypoints.containsKey(name)) {
            waypoints[name] = WaypointData(pos.immutable(), category, color)
            if (sendCoordsInChat) modMessage("$name found at ${pos.x}, ${pos.y}, ${pos.z}")
        }
    }

    private fun checkSequence(chunk: LevelChunk, x: Int, y: Int, z: Int, sequence: List<Block?>): Boolean {
        val blockPos = BlockPos.MutableBlockPos()
        for (i in sequence.indices) {
            val expected = sequence[i] ?: continue
            val py = y + i
            if (py > 255) return false
            blockPos.set(x, py, z)
            if (chunk.getBlockState(blockPos).block != expected) return false
        }
        return true
    }

    private fun getBlockState(chunk: LevelChunk, x: Int, y: Int, z: Int): BlockState {
        val sectionIndex = y shr 4
        val sections = chunk.sections
        if (sectionIndex < 0 || sectionIndex >= sections.size) return Blocks.AIR.defaultBlockState()
        val section = sections[sectionIndex]
        if (section == null || section.hasOnlyAir()) return Blocks.AIR.defaultBlockState()
        return section.getBlockState(x, y and 15, z)
    }

    private fun getChunkKey(chunk: LevelChunk): Long {
        return (chunk.pos.x.toLong() shl 32) or (chunk.pos.z.toLong() and 0xFFFFFFFFL)
    }
}
