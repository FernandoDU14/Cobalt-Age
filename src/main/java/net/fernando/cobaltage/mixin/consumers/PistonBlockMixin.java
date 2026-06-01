package net.fernando.cobaltage.mixin.consumers;
import net.fernando.cobaltage.util.signal.SignalTypeLevelExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import static net.fernando.cobaltage.util.signal.SignalType.COBALT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

@Mixin(PistonBaseBlock.class)
public class PistonBlockMixin {

    @Inject(method = "getNeighborSignal", at = @At("HEAD"), cancellable = true)
    private void cobaltage$hasBothSignals(SignalGetter signalGetter, BlockPos pos, Direction pistonFace, CallbackInfoReturnable<Boolean> cir) {
        if (!(signalGetter instanceof Level actualWorld)) return;

        SignalTypeLevelExtensions level = (SignalTypeLevelExtensions) actualWorld;

        // Neighbours
        for(Direction side : Direction.values()) {
            if (side != pistonFace &&
                level.hasSignalByType(COBALT, pos.relative(side), side)) {
                cir.setReturnValue(true);
                return;
            }
        }
        // Under me
        if (level.hasSignalByType(COBALT, pos, Direction.DOWN)) {
            cir.setReturnValue(true);
            return;
        } else { // QC
            BlockPos posAbove = pos.above();
            for (Direction sideAbove : Direction.values()) {
                if (sideAbove != Direction.DOWN && level.hasSignalByType(COBALT, posAbove.relative(sideAbove), sideAbove)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }
}