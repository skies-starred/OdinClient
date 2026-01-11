package starred.skies.odin.mixin.mixins.od.terminals;

import com.odtheking.odin.features.impl.floor7.terminalhandler.*;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import starred.skies.odin.events.TerminalUpdateEvent;

import java.util.List;

@Mixin(value = StartsWithHandler.class, remap = false)
abstract class StartsWithHandlerMixin {
    @Inject(method = "handleSlotUpdate", at = @At("RETURN"), remap = false)
    private void afterHandleSlotUpdate(ClientboundContainerSetSlotPacket packet, List<ItemStack> items, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            new TerminalUpdateEvent((TerminalHandler) (Object) this).postAndCatch();
        }
    }
}

