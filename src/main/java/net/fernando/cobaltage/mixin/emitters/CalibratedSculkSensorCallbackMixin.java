package net.fernando.cobaltage.mixin.emitters;

import net.fernando.cobaltage.block.CobaltWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CalibratedSculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity$VibrationUser")
public class CalibratedSculkSensorCallbackMixin {

    @Inject(
            method = "getBackSignal(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)I",
            at = @At("RETURN"),
            cancellable = true,
            remap = true
    )
    private void cobalt_injectCobaltFrequency(Level world, BlockPos pos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        // 1. Retrieve the direction (amethyst side)
        Direction direction = state.getValue(CalibratedSculkSensorBlock.FACING).getOpposite();
        BlockPos inputPos = pos.relative(direction);
        BlockState inputState = world.getBlockState(inputPos);

        // 2. If cobalt wire, read power
        if (inputState.getBlock() instanceof CobaltWireBlock) {
            int cobaltPower = inputState.getValue(CobaltWireBlock.POWER);

            // 3. If cobalt has energy, override the calibration value
            if (cobaltPower > 0) {
                cir.setReturnValue(cobaltPower);
            }
        }
    }
}