package net.fernando.cobaltage.mixin.world.level.block.redstone;

import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.block.signal.cobalt.CobaltSignalSource;
import net.fernando.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import static net.fernando.cobaltage.block.signal.SignalType.REDSTONE;

@Mixin(RedStoneWireBlock.class)
public class RedStoneWireBlockMixin {

    @Inject(at = @At("HEAD"), method = "shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z", cancellable = true)
    private static void shouldConnectTo(BlockState state, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof CobaltSignalSource && !state.is(ModBlocks.CONVERTER)) {
            cir.setReturnValue(false);
        }

        if(state.is(ModBlocks.CONVERTER)){
            cir.setReturnValue(direction == state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
    }

    @Redirect(
            method = "getBlockSignal",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBestNeighborSignal(Lnet/minecraft/core/BlockPos;)I")
    )
    private int getBestNeighborSignalByType(Level level, BlockPos blockPos) {
        return ((SignalGetterByType) level).cobaltage$getBestNeighborSignalByType(REDSTONE, blockPos);
    }

}

