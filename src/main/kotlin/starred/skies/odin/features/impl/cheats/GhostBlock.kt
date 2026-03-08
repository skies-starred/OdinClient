package starred.skies.odin.features.impl.cheats

import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onSend
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import org.lwjgl.glfw.GLFW
import starred.skies.odin.utils.Skit

object GhostBlock : Module(
    name = "Ghost Block",
    description = "Turns blocks you look at into ghost blocks.",
    category = Skit.CHEATS
) {
    private val UAYOR by BooleanSetting("Use at your own risk", desc = "This feature can get you banned if used improperly.")
    private val stonkGhostBlock by BooleanSetting("Stonk Ghost Block", true, desc = "Creates a ghost block when right-clicking with a pickaxe.")
    private val ghostBlockKey = KeybindSetting("Ghost Block Key", GLFW.GLFW_KEY_UNKNOWN, desc = "Hold this key to create ghost blocks.")

    private val blacklist = listOf(
        Blocks.ACACIA_DOOR, Blocks.BIRCH_DOOR, Blocks.DARK_OAK_DOOR, Blocks.JUNGLE_DOOR, Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.MANGROVE_DOOR, Blocks.CHERRY_DOOR, Blocks.BAMBOO_DOOR, Blocks.CRIMSON_DOOR, Blocks.WARPED_DOOR, Blocks.IRON_DOOR,
        Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL,
        Blocks.BEACON,
        Blocks.WHITE_BED, Blocks.ORANGE_BED, Blocks.MAGENTA_BED, Blocks.LIGHT_BLUE_BED, Blocks.YELLOW_BED, Blocks.LIME_BED, Blocks.PINK_BED, Blocks.GRAY_BED, Blocks.LIGHT_GRAY_BED, Blocks.CYAN_BED, Blocks.PURPLE_BED, Blocks.BLUE_BED, Blocks.BROWN_BED, Blocks.GREEN_BED, Blocks.RED_BED, Blocks.BLACK_BED,
        Blocks.BREWING_STAND,
        Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM,
        Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST,
        Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK,
        Blocks.CRAFTING_TABLE,
        Blocks.DAYLIGHT_DETECTOR,
        Blocks.DISPENSER,
        Blocks.DROPPER,
        Blocks.ENCHANTING_TABLE,
        Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
        Blocks.HOPPER,
        Blocks.LEVER,
        Blocks.NOTE_BLOCK,
        Blocks.REPEATER, Blocks.COMPARATOR,
        Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD, Blocks.SKELETON_SKULL, Blocks.SKELETON_WALL_SKULL, Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL, Blocks.ZOMBIE_HEAD, Blocks.ZOMBIE_WALL_HEAD, Blocks.CREEPER_HEAD, Blocks.CREEPER_WALL_HEAD, Blocks.DRAGON_HEAD, Blocks.DRAGON_WALL_HEAD, Blocks.PIGLIN_HEAD, Blocks.PIGLIN_WALL_HEAD,
        Blocks.STONE_BUTTON, Blocks.OAK_BUTTON, Blocks.BIRCH_BUTTON, Blocks.SPRUCE_BUTTON, Blocks.JUNGLE_BUTTON, Blocks.DARK_OAK_BUTTON, Blocks.ACACIA_BUTTON, Blocks.MANGROVE_BUTTON, Blocks.CHERRY_BUTTON, Blocks.BAMBOO_BUTTON, Blocks.CRIMSON_BUTTON, Blocks.WARPED_BUTTON, Blocks.POLISHED_BLACKSTONE_BUTTON,
        Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.BIRCH_TRAPDOOR, Blocks.JUNGLE_TRAPDOOR, Blocks.ACACIA_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.MANGROVE_TRAPDOOR, Blocks.CHERRY_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR, Blocks.CRIMSON_TRAPDOOR, Blocks.WARPED_TRAPDOOR, Blocks.IRON_TRAPDOOR,
        Blocks.OAK_SIGN, Blocks.SPRUCE_SIGN, Blocks.BIRCH_SIGN, Blocks.JUNGLE_SIGN, Blocks.ACACIA_SIGN, Blocks.DARK_OAK_SIGN, Blocks.MANGROVE_SIGN, Blocks.CHERRY_SIGN, Blocks.BAMBOO_SIGN, Blocks.CRIMSON_SIGN, Blocks.WARPED_SIGN,
        Blocks.OAK_WALL_SIGN, Blocks.SPRUCE_WALL_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.DARK_OAK_WALL_SIGN, Blocks.MANGROVE_WALL_SIGN, Blocks.CHERRY_WALL_SIGN, Blocks.BAMBOO_WALL_SIGN, Blocks.CRIMSON_WALL_SIGN, Blocks.WARPED_WALL_SIGN,
        Blocks.BARRIER, Blocks.BEDROCK, Blocks.END_PORTAL_FRAME, Blocks.END_PORTAL, Blocks.MOVING_PISTON
    )

    init {
        this.registerSetting(ghostBlockKey)

        on<TickEvent.Start> {
            if (ghostBlockKey.value.isPressed() && LocationUtils.isInSkyblock) {
                val hit = mc.hitResult
                if (hit is BlockHitResult && hit.type == HitResult.Type.BLOCK) {
                    toAir(hit.blockPos)
                }
            }
        }

        onSend<ServerboundUseItemOnPacket> {
            val item = mc.player?.getItemInHand(hand) ?: return@onSend
            if (stonkGhostBlock && LocationUtils.isInSkyblock && item.itemId.contains("PICKAXE", ignoreCase = true)) {
                if (toAir(hitResult.blockPos)) {
                    it.cancel()
                }
            }
        }
    }

    override fun onEnable() {
        super.onEnable()
        modMessage("Use \"GhostBlock\" at your own risk! This feature can get you banned if used improperly!")
    }

    private fun toAir(pos: BlockPos): Boolean {
        val world = mc.level ?: return false
        val state = world.getBlockState(pos)
        val block = state.block
        if (block != Blocks.AIR && !blacklist.contains(block)) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)
            return true
        }
        return false
    }

    private fun InputConstants.Key.isPressed(): Boolean {
        if (this.value == GLFW.GLFW_KEY_UNKNOWN) return false
        val window = mc.window
        return if (this.value > 7) InputConstants.isKeyDown(window, this.value)
        else GLFW.glfwGetMouseButton(window.handle(), this.value) == GLFW.GLFW_PRESS
    }
}
