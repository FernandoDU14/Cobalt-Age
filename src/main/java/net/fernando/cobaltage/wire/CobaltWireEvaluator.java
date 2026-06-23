package net.fernando.cobaltage.wire;

import net.fernando.cobaltage.block.CobaltWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public abstract class CobaltWireEvaluator {
    protected final CobaltWireBlock wireBlock;

    protected CobaltWireEvaluator(CobaltWireBlock wireBlock) {
        this.wireBlock = wireBlock;
    }

    public abstract void updatePowerStrength(Level level, BlockPos blockPos, BlockState blockState, @Nullable Orientation orientation, boolean bl);

    protected int getBlockSignal(Level level, BlockPos blockPos) {
        return this.wireBlock.getBlockSignal(level, blockPos);
    }

    protected int getCobaltWireSignal(BlockPos blockPos, BlockState blockState) {
        return blockState.is(this.wireBlock) ? blockState.getValue(CobaltWireBlock.POWER) : 0;
    }

    protected int getIncomingCobaltWireSignal(Level level, BlockPos blockPos) {
        int i = 0;

        for(Direction direction : Plane.HORIZONTAL) {
            BlockPos neighborPos = blockPos.relative(direction);
            BlockState blockState = level.getBlockState(neighborPos);
            i = Math.max(i, this.getCobaltWireSignal(neighborPos, blockState));
            BlockPos abovePos = blockPos.above();
            if ((blockState.isRedstoneConductor(level, neighborPos) || level.getBlockState(neighborPos).getBlock() instanceof PoweredBlock)
                    && (!(level.getBlockState(abovePos).isRedstoneConductor(level, abovePos) || level.getBlockState(abovePos).getBlock() instanceof PoweredBlock))) {
                BlockPos blockPos4 = neighborPos.above();
                i = Math.max(i, this.getCobaltWireSignal(blockPos4, level.getBlockState(blockPos4)));
            } else if (!(blockState.isRedstoneConductor(level, neighborPos) || level.getBlockState(neighborPos).getBlock() instanceof PoweredBlock)) {
                BlockPos blockPos4 = neighborPos.below();
                i = Math.max(i, this.getCobaltWireSignal(blockPos4, level.getBlockState(blockPos4)));
            }
        }

        return Math.max(0, i - 1);
    }
}
