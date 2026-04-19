package net.fernando.cobaltrails.mixin;

import net.fernando.cobaltrails.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedstoneWireBlock.class)
public class RedstoneWireConnectionMixin {

    // 1. Permette alla polvere di connettersi orizzontalmente
    @Inject(at = @At("HEAD"), method = "connectsTo(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)Z", cancellable = true)
    private static void cobalt$canConnectTo(BlockState state, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (state.isOf(ModBlocks.COBALT_DUST) ||
                state.isOf(ModBlocks.COBALT_REPEATER) ||
                state.isOf(ModBlocks.COBALT_TORCH) ||
                state.isOf(ModBlocks.COBALT_WALL_TORCH) ||
                state.isOf(ModBlocks.COBALT_COMPARATOR)) {
            cir.setReturnValue(true);
        }
    }

    // 2. Permette alla polvere di "salire" verticalmente (Risolve il crash al break)
    @Inject(at = @At("HEAD"), method = "canRunOnTop", cancellable = true)
    private void cobalt$canRunOnTop(BlockView world, BlockPos pos, BlockState floor, CallbackInfoReturnable<Boolean> cir) {
        if (floor.isOf(ModBlocks.COBALT_DUST)) {
            cir.setReturnValue(true);
        }
    }
}