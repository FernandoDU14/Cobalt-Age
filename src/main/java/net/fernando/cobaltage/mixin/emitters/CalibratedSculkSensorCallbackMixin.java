package net.fernando.cobaltage.mixin.emitters;

import net.minecraft.block.BlockState;
import net.minecraft.block.CalibratedSculkSensorBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.fernando.cobaltage.block.CobaltWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.block.entity.CalibratedSculkSensorBlockEntity$Callback")
public class CalibratedSculkSensorCallbackMixin {

    @Inject(
            method = "getCalibrationFrequency(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)I",
            at = @At("RETURN"),
            cancellable = true,
            remap = true
    )
    private void cobalt_injectCobaltFrequency(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        // 1. Retrieve the direction (amethyst side)
        Direction direction = state.get(CalibratedSculkSensorBlock.FACING).getOpposite();
        BlockPos inputPos = pos.offset(direction);
        BlockState inputState = world.getBlockState(inputPos);

        // 2. If cobalt wire, read power
        if (inputState.getBlock() instanceof CobaltWireBlock) {
            int cobaltPower = inputState.get(CobaltWireBlock.POWER);

            // 3. If cobalt has energy, override the calibration value
            if (cobaltPower > 0) {
                cir.setReturnValue(cobaltPower);
            }
        }
    }
}