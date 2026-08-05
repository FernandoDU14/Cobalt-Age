package net.cobaltmc.cobaltage.block.signal.engine.legacy;

import com.google.common.collect.Sets;
import net.cobaltmc.cobaltage.block.abstracts.WireBlock;
import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class DefaultWireEvaluator extends WireEvaluator {
    public DefaultWireEvaluator(WireBlock WireBlock) {
        super(WireBlock);
    }

    public void updatePowerStrengthByType(SignalType signalType, @NonNull Level level, @NonNull BlockPos blockPos, BlockState blockState, @Nullable Orientation orientation, boolean bl) {
        int i = this.calculateTargetStrengthByType(signalType, level, blockPos);
        if ((Integer)blockState.getValue(WireBlock.POWER) != i) {
            if (level.getBlockState(blockPos) == blockState) {
                level.setBlock(blockPos, (BlockState)blockState.setValue(WireBlock.POWER, i), 2);
            }

            Set<BlockPos> set = Sets.newHashSet();
            set.add(blockPos);

            for(Direction direction : Direction.values()) {
                set.add(blockPos.relative(direction));
            }

            for(BlockPos blockPos2 : set) {
                level.neighborChanged(blockPos2, this.wireBlock, null);
            }
        }

    }

    private int calculateTargetStrengthByType(SignalType signalType, Level level, BlockPos blockPos) {
        int i = this.getBlockSignalByType(signalType, level, blockPos);
        return i == 15 ? i : Math.max(i, this.getIncomingWireSignal(level, blockPos));
    }
}