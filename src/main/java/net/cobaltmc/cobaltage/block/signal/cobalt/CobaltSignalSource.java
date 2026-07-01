package net.cobaltmc.cobaltage.block.signal.cobalt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface CobaltSignalSource {
    // The logic is the same of getSignal and getDirectSignal
    int getCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction);
    default int getDirectCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return 0;
    }
}