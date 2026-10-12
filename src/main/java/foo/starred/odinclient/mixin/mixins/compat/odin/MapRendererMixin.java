package foo.starred.odinclient.mixin.mixins.compat.odin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.odtheking.odin.features.impl.dungeon.map.MapRendererKt;
import com.odtheking.odin.features.impl.dungeon.map.tile.DungeonRoom;
import foo.starred.odinclient.features.impl.dungeons.CheaterMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MapRendererKt.class)
public class MapRendererMixin {
    @ModifyExpressionValue(method = "renderMap", at = @At(value = "INVOKE", target = "Lcom/odtheking/odin/features/impl/dungeon/map/tile/DungeonRoom;isViewable()Z"))
    private static boolean odinClient$renderMap(boolean original) {
        return CheaterMap.getShowRooms() || original;
    }

    @ModifyArg(method = "fillRoom", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), index = 4)
    private static int odinClient$fillRoom(int color, @Local(argsOnly = true) DungeonRoom room) {
        if (!CheaterMap.getShowRooms()) return color;
        if (!CheaterMap.getDarken()) return color;
        if (room.isViewable()) return color;

        return odinClient$darken(color, CheaterMap.INSTANCE.getDarkenFactor());
    }

    //~ if >= 26.2 'getWalkedInto' -> 'getShouldShowName'
    @ModifyExpressionValue(method = "renderRoomText", at = @At(value = "INVOKE", target = "Lcom/odtheking/odin/features/impl/dungeon/map/tile/DungeonRoom;getWalkedInto()Z"))
    private static boolean odinClient$renderRoomText(boolean original) {
        if (!CheaterMap.getShowRooms()) return original;
        if (!CheaterMap.getNames()) return original;
        return true;
    }

    @Unique
    private static int odinClient$darken(int rgba, double factor) {
        int a = (rgba >>> 24) & 0xFF;
        int r = (int)(((rgba >>> 16) & 0xFF) * factor);
        int g = (int)(((rgba >>> 8) & 0xFF) * factor);
        int b = (int)((rgba & 0xFF) * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
