package starred.skies.odin.mixin.mixins.od;

import com.odtheking.odin.clickgui.Panel;
import com.odtheking.odin.features.Category;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import starred.skies.odin.utils.Skit;

import static starred.skies.odin.commands.StreamCommandKt.streamMode;

@Mixin(value = Panel.class, remap = false)
public class PanelMixin {
    @Final
    @Shadow
    private Category category;

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void onDraw(float mouseX, float mouseY, CallbackInfo ci) {
        if (category == Skit.CHEATS && streamMode) ci.cancel();
    }

    @Inject(method = "handleScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(int amount, CallbackInfoReturnable<Boolean> cir) {
        if (category == Skit.CHEATS && streamMode) cir.setReturnValue(false);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onClick(float mouseX, float mouseY, MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        if (category == Skit.CHEATS && streamMode) cir.setReturnValue(false);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onRelease(MouseButtonEvent click, CallbackInfo ci) {
        if (category == Skit.CHEATS && streamMode) ci.cancel();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPress(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (category == Skit.CHEATS && streamMode) cir.setReturnValue(false);
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void onKeyType(CharacterEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (category == Skit.CHEATS && streamMode) cir.setReturnValue(false);
    }
}