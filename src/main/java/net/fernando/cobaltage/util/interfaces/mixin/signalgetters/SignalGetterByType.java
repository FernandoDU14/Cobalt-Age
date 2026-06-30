package net.fernando.cobaltage.util.interfaces.mixin.signalgetters;

import net.fernando.cobaltage.block.signal.SignalType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
public interface SignalGetterByType {
    int cobaltage$getSignalByType(SignalType type, BlockPos pos, Direction dir);
    int cobaltage$getDirectSignalByType(SignalType type, BlockPos pos, Direction dir);


    int cobaltage$getDirectSignalToByType(SignalType type, BlockPos pos, Direction ignoreDirection);
    int cobaltage$getBestNeighborSignalByType(SignalType type, BlockPos pos);
    int cobaltage$getControlInputSignalByType(SignalType type, BlockPos blockPos, Direction direction, boolean bl);

    boolean cobaltage$hasSignalByType(SignalType type, BlockPos pos, Direction dir);
    boolean cobaltage$hasNeighborSignalByType(SignalType type, BlockPos pos);
}