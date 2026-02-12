package starred.skies.odin.mixin.accessors;

import com.odtheking.odin.features.impl.dungeon.puzzlesolvers.PuzzleSolvers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PuzzleSolvers.class)
public interface PuzzleSolversAccessor {
    @Invoker("getAutoDraft")
    boolean invokeDraft();
}