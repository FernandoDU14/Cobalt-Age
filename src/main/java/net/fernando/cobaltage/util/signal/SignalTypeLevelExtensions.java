package net.fernando.cobaltage.util.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface SignalTypeLevelExtensions {
    int getSignalByType(SignalType type, BlockPos pos, Direction dir);
    int getDirectSignalByType(SignalType type, BlockPos pos, Direction dir);
    boolean hasSignalByType(SignalType type, BlockPos pos, Direction dir);
    boolean hasNeighbourSignalByType(SignalType type, BlockPos pos);
}