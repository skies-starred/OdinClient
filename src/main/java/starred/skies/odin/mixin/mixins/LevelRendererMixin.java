package starred.skies.odin.mixin.mixins;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import starred.skies.odin.features.impl.cheats.SecretHitboxes;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    //? if >= 1.21.10 {
    @Redirect(method = "extractBlockOutline(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/state/LevelRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
     //? } else {
    /*@Redirect(method = "renderHitOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    *///? }
    VoxelShape extractBlockOutline(BlockState instance, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        var outline = SecretHitboxes.INSTANCE.getShape(instance);

        if (outline != null) return outline;
        else return instance.getShape(blockGetter, blockPos, collisionContext);
    }
}

