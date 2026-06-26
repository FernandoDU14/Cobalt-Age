package net.fernando.cobaltage.block.signal.engine.legacy;

import net.fernando.cobaltage.block.abstracts.WireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

import net.fernando.cobaltage.block.signal.SignalType;

public abstract class WireEvaluator {
    protected final WireBlock wireBlock;

    protected WireEvaluator(WireBlock wireBlock) {
        this.wireBlock = wireBlock;
    }

    public abstract void updatePowerStrengthByType(SignalType signalType, Level level, BlockPos blockPos, BlockState blockState, @Nullable Orientation orientation, boolean bl);

    protected int getBlockSignalByType(SignalType signalType, Level level, BlockPos blockPos) {
        return this.wireBlock.getBlockSignalByType(signalType, level, blockPos);
    }

    protected int getWireSignal(BlockPos blockPos, BlockState blockState) {
        return blockState.is(this.wireBlock) ? blockState.getValue(WireBlock.POWER) : 0;
    }

    protected int getIncomingWireSignal(Level level, BlockPos blockPos) {
        int i = 0;

        for(Direction direction : Plane.HORIZONTAL) {
            BlockPos neighborPos = blockPos.relative(direction);
            BlockState blockState = level.getBlockState(neighborPos);
            i = Math.max(i, this.getWireSignal(neighborPos, blockState));
            BlockPos abovePos = blockPos.above();
            if ((blockState.isRedstoneConductor(level, neighborPos) || level.getBlockState(neighborPos).getBlock() instanceof PoweredBlock)
                    && (!(level.getBlockState(abovePos).isRedstoneConductor(level, abovePos) || level.getBlockState(abovePos).getBlock() instanceof PoweredBlock))) {
                BlockPos blockPos4 = neighborPos.above();
                i = Math.max(i, this.getWireSignal(blockPos4, level.getBlockState(blockPos4)));
            } else if (!(blockState.isRedstoneConductor(level, neighborPos) || level.getBlockState(neighborPos).getBlock() instanceof PoweredBlock)) {
                BlockPos blockPos4 = neighborPos.below();
                i = Math.max(i, this.getWireSignal(blockPos4, level.getBlockState(blockPos4)));
            }
        }

        return Math.max(0, i - 1);
    }
}
