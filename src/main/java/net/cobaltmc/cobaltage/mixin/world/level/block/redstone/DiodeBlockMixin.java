package net.cobaltmc.cobaltage.mixin.world.level.block.redstone;

import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.BlockStateBaseSignalGetterByType;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
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
        return ((BlockStateBaseSignalGetterByType) state).cobaltage$getSignalByType(SignalType.REDSTONE, blockGetter, blockPos, direction);
    }

    @Redirect(method = "getInputSignal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I"))
    private int getSignalByType(Level level, BlockPos blockPos, Direction direction) {
        return ((SignalGetterByType) level).cobaltage$getSignalByType(SignalType.REDSTONE, blockPos, direction);
    }

}