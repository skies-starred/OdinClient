package starred.skies.odin.mixin.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import starred.skies.odin.events.EntityMetadataEvent;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Shadow
    private ClientLevel level;

    @Inject(method = "handleSetEntityData", at = @At("TAIL"))
    private void onHandleSetEntityData(ClientboundSetEntityDataPacket packet, CallbackInfo ci, @Local Entity entity) {
        if (entity == null) return;
        if (new EntityMetadataEvent(entity, packet).postAndCatch() && this.level != null) {
            this.level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        }
    }
}