package net.cobaltmc.cobaltage.block.signal.engine.legacy;

import net.cobaltmc.cobaltage.block.abstracts.WireBlock;
import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public abstract class WireEvaluator {
    protected final WireBlock wireBlock;

    protected WireEvaluator(WireBlock wireBlock) {
        this.wireBlock = wireBlock;
    }

    public abstract void updatePowerStrengthByType(SignalType var1, Level var2, BlockPos var3, BlockState var4, @Nullable Orientation var5, boolean var6);

    protected int getBlockSignalByType(SignalType signalType, Level level, BlockPos blockPos) {
        return this.wireBlock.getBlockSignalByType(signalType, level, blockPos);
    }

    protected int getWireSignal(BlockPos blockPos, BlockState blockState) {
        return blockState.is(this.wireBlock) ? (Integer)blockState.getValue(WireBlock.POWER) : 0;
    }

    protected int getIncomingWireSignal(Level level, BlockPos blockPos) {
        int i = 0;

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = blockPos.relative(direction);
            BlockState blockState = level.getBlockState(neighborPos);
            i = Math.max(i, this.getWireSignal(neighborPos, blockState));
            BlockPos abovePos = blockPos.above();
            if ((blockState.isSignalSource() || blockState.getBlock() instanceof DiodeBlock) && !level.getBlockState(abovePos).isSignalSource() && !(level.getBlockState(abovePos).getBlock() instanceof DiodeBlock)) {
                BlockPos blockPos4 = neighborPos.above();
                i = Math.max(i, this.getWireSignal(blockPos4, level.getBlockState(blockPos4)));
            } else if (!blockState.isSignalSource() && !(blockState.getBlock() instanceof DiodeBlock)) {
                BlockPos blockPos4 = neighborPos.below();
                i = Math.max(i, this.getWireSignal(blockPos4, level.getBlockState(blockPos4)));
            }
        }

        for(Direction direction : Direction.Plane.VERTICAL) {
            BlockPos neighborPos = blockPos.relative(direction);
            BlockState blockState = level.getBlockState(neighborPos);
            i = Math.max(i, this.getWireSignal(neighborPos, blockState));
        }

        return Math.max(0, i - 1);
    }
}