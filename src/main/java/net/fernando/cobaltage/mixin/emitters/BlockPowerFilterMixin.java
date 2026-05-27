package net.fernando.cobaltage.mixin.emitters;

import net.fernando.cobaltage.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ComparatorBlock.class)
public class BlockPowerFilterMixin {

    @Inject(method = "getInputSignal", at = @At("HEAD"), cancellable = true)
    private void blockCobaltInput(Level world, BlockPos pos, BlockState state, CallbackInfoReturnable<Integer> cir) {

        if (state.is(ModBlocks.COBALT_DUST)) {
            cir.setReturnValue(0);
        }
    }
}