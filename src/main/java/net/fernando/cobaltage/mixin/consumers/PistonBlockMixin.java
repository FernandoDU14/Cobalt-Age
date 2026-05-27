package net.fernando.cobaltage.mixin.consumers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.fernando.cobaltage.util.CobaltPowerHelper.isPoweredOrQuasiPoweredByCobalt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

@Mixin(PistonBaseBlock.class)
public class PistonBlockMixin {

    @Inject(method = "getNeighborSignal", at = @At("RETURN"), cancellable = true)
    private void cobalt$checkCobaltPower(SignalGetter world, BlockPos pos, Direction pistonFace, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!(world instanceof Level actualWorld)) return;

        cir.setReturnValue(isPoweredOrQuasiPoweredByCobalt(actualWorld, pos, pistonFace));
    }
}