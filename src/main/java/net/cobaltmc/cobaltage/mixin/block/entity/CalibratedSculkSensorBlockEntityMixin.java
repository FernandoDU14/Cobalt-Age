package net.cobaltmc.cobaltage.mixin.block.entity;

import net.cobaltmc.cobaltage.block.cobalt.CobaltWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CalibratedSculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        targets = {"net/minecraft/world/level/block/entity/CalibratedSculkSensorBlockEntity$VibrationUser"}
)
public class CalibratedSculkSensorBlockEntityMixin {
    @Inject(
            method = {"getBackSignal"},
            at = {@At("RETURN")},
            cancellable = true,
            remap = true
    )
    private void cobalt_injectCobaltFrequency(Level world, BlockPos pos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        Direction direction = (state.getValue(CalibratedSculkSensorBlock.FACING)).getOpposite();
        BlockPos inputPos = pos.relative(direction);
        BlockState inputState = world.getBlockState(inputPos);
        if (inputState.getBlock() instanceof CobaltWireBlock) {
            int cobaltPower = (Integer)inputState.getValue(CobaltWireBlock.POWER);
            if (cobaltPower > 0) {
                cir.setReturnValue(cobaltPower);
            }
        }

    }
}