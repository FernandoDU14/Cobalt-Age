package net.fernando.cobaltage.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

public interface SignalTypeBlockStateExtensions {
    int getSignalByType(SignalType type, BlockGetter level, BlockPos pos, Direction dir);
    int getDirectSignalByType(SignalType type, BlockGetter level, BlockPos pos, Direction dir);
}