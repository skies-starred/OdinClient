package foo.starred.odinclient.features.impl.dungeons

//~ if >= 26.2 'Setting' -> 'RenderableSetting'
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import foo.starred.odinclient.api.category.OdinClientCategory
import net.minecraft.core.Direction
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

object SecretHitboxes : Module(
    name = "Secret Hitboxes",
    description = "Extends the hitboxes of secret blocks to a full block.",
    category = OdinClientCategory.CHEATS
) {
    val lever by BooleanSetting("Lever", false, desc = "Extends the lever hitbox.")
    val oldLeverStyle by BooleanSetting("1.8 lever hitbox", true, desc = "Use the 1.8 lever hitbox.").withDependency { lever }
    val button by BooleanSetting("Button", false, desc = "Extends the button hitbox.")
    val flatButtonStyle by BooleanSetting("Flat button hitbox", false, desc = "Use the flat button hitbox.").withDependency { button }
    val skull by BooleanSetting("Skulls", false, desc = "Extends the skulls hitbox.")
    val chests by BooleanSetting("Chests", false, desc = "Extends the chest hitbox.")
    val onlyTrappedChests by BooleanSetting("Only trapped chests", false, desc = "Only extends the trapped chests hitbox.").withDependency { chests }

    private val LEVER_FLOOR_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0)
    private val LEVER_NORTH_SHAPE = Block.box(5.0, 3.0, 10.0, 11.0, 13.0, 16.0)
    private val LEVER_SOUTH_SHAPE = Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 6.0)
    private val LEVER_EAST_SHAPE = Block.box(0.0, 3.0, 5.0, 6.0, 13.0, 11.0)
    private val LEVER_WEST_SHAPE = Block.box(10.0, 3.0, 5.0, 16.0, 13.0, 11.0)

    private fun getOldLeverShape(face: AttachFace, direction: Direction): VoxelShape {
        return if (face == AttachFace.FLOOR || face == AttachFace.CEILING) {
            LEVER_FLOOR_SHAPE
        } else when (direction) {
            Direction.EAST -> LEVER_EAST_SHAPE
            Direction.WEST -> LEVER_WEST_SHAPE
            Direction.SOUTH -> LEVER_SOUTH_SHAPE
            Direction.NORTH -> LEVER_NORTH_SHAPE
            else -> LEVER_FLOOR_SHAPE
        }
    }

    private fun getHackButtonShape(face: AttachFace, direction: Direction, powered: Boolean): VoxelShape {
        val f2 = (if (powered) 1 else 2) / 16.0
        return when (face) {
            AttachFace.CEILING -> Shapes.box(0.0, 1.0 - f2, 0.0, 1.0, 1.0, 1.0)
            AttachFace.FLOOR -> Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0 + f2, 1.0)
            else -> when (direction) {
                Direction.EAST -> Shapes.box(0.0, 0.0, 0.0, f2, 1.0, 1.0)
                Direction.WEST -> Shapes.box(1.0 - f2, 0.0, 0.0, 1.0, 1.0, 1.0)
                Direction.SOUTH -> Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, f2)
                Direction.NORTH -> Shapes.box(0.0, 0.0, 1.0 - f2, 1.0, 1.0, 1.0)
                Direction.UP -> Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0 + f2, 1.0)
                Direction.DOWN -> Shapes.box(0.0, 1.0 - f2, 0.0, 1.0, 1.0, 1.0)
            }
        }
    }

    fun BlockState.getShape(): VoxelShape? {
        if (!enabled || !DungeonUtils.inDungeons) return null

        return when (block) {
            is SkullBlock if skull -> {
                Shapes.block()
            }

            is ChestBlock if chests -> {
                if (!onlyTrappedChests || block is TrappedChestBlock) Shapes.block()
                else null
            }

            is LeverBlock if lever -> {
                if (oldLeverStyle) {
                    getOldLeverShape(
                        getValue(FaceAttachedHorizontalDirectionalBlock.FACE),
                        getValue(FaceAttachedHorizontalDirectionalBlock.FACING)
                    )
                } else {
                    Shapes.block()
                }
            }

            is ButtonBlock if button -> {
                if (flatButtonStyle) {
                    getHackButtonShape(
                        getValue(FaceAttachedHorizontalDirectionalBlock.FACE),
                        getValue(FaceAttachedHorizontalDirectionalBlock.FACING),
                        getValue(ButtonBlock.POWERED)
                    )
                } else {
                    Shapes.block()
                }
            }

            else -> null
        }
    }
}
