package net.fernando.cobaltage.mixin.emitters;

import net.fernando.cobaltage.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedStoneWireBlock.class)
public class RedstoneIsolationMixin {

    @Inject(
            method = {
                    "getSignal",
                    "getDirectSignal"
            },
            at = @At("HEAD"),
            cancellable = true
    )
    private void blockCobalt(BlockState state, BlockGetter world, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {

        if (state.is(ModBlocks.COBALT_DUST)) {
            cir.setReturnValue(0);
        }
    }


    // 1. This will allow the Redstone Dust to Avoid these Energy-Like blocks
    @Inject(at = @At("HEAD"), method = "shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z", cancellable = true)
    private static void redstone$canConnectTo(BlockState state, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(ModBlocks.COBALT_DUST) ||
                state.is(ModBlocks.COBALT_RELAY) ||
                state.is(ModBlocks.COBALT_TORCH) ||
                state.is(ModBlocks.COBALT_REPEATER) ||
                state.is(ModBlocks.COBALT_COMPARATOR) ||
                state.is(ModBlocks.COBALT_WALL_TORCH)) {
            cir.setReturnValue(false);
        }

        if(state.is(ModBlocks.CONVERTER)){
            cir.setReturnValue(direction == state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
    }
}

