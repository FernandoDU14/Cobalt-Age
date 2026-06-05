package net.fernando.cobaltage.util.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
public interface SignalTypeLevelExtensions {
    int getSignalByType(SignalType type, BlockPos pos, Direction dir);
    int getDirectSignalByType(SignalType type, BlockPos pos, Direction dir);


    int getDirectSignalToByType(SignalType type, BlockPos pos);
    int getBestNeighborSignalByType(SignalType type, BlockPos pos);
    int getControlInputSignalByType(SignalType type, BlockPos blockPos, Direction direction, boolean bl);

    boolean hasSignalByType(SignalType type, BlockPos pos, Direction dir);
    boolean hasNeighbourSignalByType(SignalType type, BlockPos pos);
}