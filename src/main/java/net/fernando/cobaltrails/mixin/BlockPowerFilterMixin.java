package net.fernando.cobaltrails.mixin;

import net.fernando.cobaltrails.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ComparatorBlock.class)
public class BlockPowerFilterMixin {

    @Inject(method = "getPower", at = @At("HEAD"), cancellable = true)
    private void blockCobaltInput(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<Integer> cir) {

        if (state.isOf(ModBlocks.COBALT_DUST)) {
            cir.setReturnValue(0);
        }
    }
}