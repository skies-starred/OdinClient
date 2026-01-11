package starred.skies.odin.mixin.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import starred.skies.odin.features.impl.cheats.SecretHitboxes;

@Mixin(ClipContext.class)
public class ClipContextMixin {
    @Redirect(method = "getBlockShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At(target = "Lnet/minecraft/world/level/ClipContext$Block;get(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", value = "INVOKE"))
    VoxelShape getBlockShape(ClipContext.Block instance, BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        var outline = SecretHitboxes.INSTANCE.getShape(blockState);
        if (outline != null) return outline;
        else return instance.get(blockState, blockGetter, blockPos, collisionContext);
    }
}
