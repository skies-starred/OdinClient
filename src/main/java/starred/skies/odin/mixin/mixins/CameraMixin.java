package starred.skies.odin.mixin.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import starred.skies.odin.features.impl.cheats.CameraHelper;

@Mixin(Camera.class)
public class CameraMixin {
    @WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double onSetup(LivingEntity instance, Holder<Attribute> attribute, Operation<Double> original) {
        return CameraHelper.INSTANCE.getEnabled() && CameraHelper.INSTANCE.getEnableDist() ? CameraHelper.INSTANCE.getCameraDist() : 0;
    }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float maxZoom, CallbackInfoReturnable<Float> cir) {
        if (CameraHelper.INSTANCE.getEnabled() && CameraHelper.INSTANCE.getCameraClip()) cir.setReturnValue(maxZoom);
    }
}