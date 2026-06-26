package net.fernando.cobaltage.block.signal.engine.legacy;

import com.google.common.collect.Sets;
import java.util.Set;

import net.fernando.cobaltage.block.CobaltWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class DefaultCobaltWireEvaluator extends CobaltWireEvaluator {
    public DefaultCobaltWireEvaluator(CobaltWireBlock cobaltWireBlock) {
        super(cobaltWireBlock);
    }

    public void updatePowerStrength(@NonNull Level level, @NonNull BlockPos blockPos, BlockState blockState, @Nullable Orientation orientation, boolean bl) {
        int i = this.calculateTargetStrength(level, blockPos);
        if (blockState.getValue(CobaltWireBlock.POWER) != i) {
            if (level.getBlockState(blockPos) == blockState) {
                level.setBlock(blockPos, blockState.setValue(CobaltWireBlock.POWER, i), 2);
            }

            Set<BlockPos> set = Sets.newHashSet();
            set.add(blockPos);

            for(Direction direction : Direction.values()) {
                set.add(blockPos.relative(direction));
            }

            for(BlockPos blockPos2 : set) {
                level.updateNeighborsAt(blockPos2, this.wireBlock);
            }
        }
    }

    private int calculateTargetStrength(Level level, BlockPos blockPos) {
        int i = this.getBlockSignal(level, blockPos);
        return i == 15 ? i : Math.max(i, this.getIncomingCobaltWireSignal(level, blockPos));
    }
}