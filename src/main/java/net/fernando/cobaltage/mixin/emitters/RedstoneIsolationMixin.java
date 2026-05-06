package net.fernando.cobaltage.mixin.emitters;

import net.fernando.cobaltage.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedstoneWireBlock.class)
public class RedstoneIsolationMixin {

    @Inject(
            method = {
                    "getWeakRedstonePower",
                    "getStrongRedstonePower"
            },
            at = @At("HEAD"),
            cancellable = true
    )
    private void blockCobalt(BlockState state, BlockView world, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {

        if (state.isOf(ModBlocks.COBALT_DUST)) {
            cir.setReturnValue(0);
        }
    }


    // 1. This will allow the Redstone Dust to Avoid these Energy-Like blocks
    @Inject(at = @At("HEAD"), method = "connectsTo(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)Z", cancellable = true)
    private static void redstone$canConnectTo(BlockState state, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (state.isOf(ModBlocks.COBALT_DUST) ||
                state.isOf(ModBlocks.COBALT_TORCH) ||
                state.isOf(ModBlocks.COBALT_REPEATER) ||
                state.isOf(ModBlocks.COBALT_COMPARATOR) ||
                state.isOf(ModBlocks.COBALT_WALL_TORCH)) {
            cir.setReturnValue(false);
        }

        if(state.isOf(ModBlocks.CONVERTER)){
            cir.setReturnValue(direction == state.get(Properties.HORIZONTAL_FACING));
        }
    }
}

