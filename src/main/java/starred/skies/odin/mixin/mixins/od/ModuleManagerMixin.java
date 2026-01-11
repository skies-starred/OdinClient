package starred.skies.odin.mixin.mixins.od;

import com.odtheking.odin.config.ModuleConfig;
import com.odtheking.odin.features.Module;
import com.odtheking.odin.features.ModuleManager;
import com.odtheking.odin.features.impl.dungeon.BreakerDisplay;
import com.odtheking.odin.features.impl.dungeon.KeyHighlight;
import com.odtheking.odin.features.impl.dungeon.LividSolver;
import com.odtheking.odin.features.impl.dungeon.SpiritBear;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;

@Mixin(value = ModuleManager.class, remap = false)
public class ModuleManagerMixin {
    @ModifyVariable(method = "registerModules", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Module[] filterModules(Module[] modules, ModuleConfig config) {
        return Arrays.stream(modules)
                .filter(module -> !(module instanceof LividSolver ||
                        module instanceof BreakerDisplay ||
                        module instanceof KeyHighlight ||
                        module instanceof SpiritBear))
                .toArray(Module[]::new);
    }
}