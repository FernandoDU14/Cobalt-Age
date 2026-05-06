package net.fernando.cobaltage.mixin.consumers;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.fernando.cobaltage.util.CobaltPowerHelper.isPoweredOrQuasiPoweredByCobalt;

@Mixin(PistonBlock.class)
public class PistonBlockMixin {

    @Inject(method = "shouldExtend", at = @At("RETURN"), cancellable = true)
    private void cobalt$checkCobaltPower(RedstoneView world, BlockPos pos, Direction pistonFace, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!(world instanceof World actualWorld)) return;

        cir.setReturnValue(isPoweredOrQuasiPoweredByCobalt(actualWorld, pos, pistonFace));
    }
}