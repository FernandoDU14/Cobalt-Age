package net.fernando.cobaltage.mixin.signal.redstone;

import net.fernando.cobaltage.util.signal.SignalType;
import net.fernando.cobaltage.util.signal.SignalTypeBlockStateExtensions;
import net.fernando.cobaltage.util.signal.SignalTypeLevelExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(DiodeBlock.class)
public class DiodeBlockMixin {

    @Redirect(method = "getDirectSignal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSignal(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I"))
    private int getSignalByType(BlockState state, BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
        return ((SignalTypeBlockStateExtensions) state).getSignalByType(SignalType.REDSTONE, blockGetter, blockPos, direction);
    }

    @Redirect(method = "getInputSignal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I"))
    private int getSignalByType(Level level, BlockPos blockPos, Direction direction) {
        return ((SignalTypeLevelExtensions) level).getSignalByType(SignalType.REDSTONE, blockPos, direction);
    }

}