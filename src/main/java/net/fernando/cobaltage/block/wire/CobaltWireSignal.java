package net.fernando.cobaltage.block.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface CobaltWireSignal {
    int getCobaltSignalIfLinked(BlockState state, Level world, BlockPos pos, Direction direction);
    default int getDirectCobaltSignalIfLinked(BlockState state, Level world, BlockPos pos, Direction direction) {
        return 0;
    }
}