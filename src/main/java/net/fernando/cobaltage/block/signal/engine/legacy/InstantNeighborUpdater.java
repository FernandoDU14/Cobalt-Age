//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package net.fernando.cobaltage.block.signal.engine.legacy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Block.UpdateFlags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class InstantNeighborUpdater implements NeighborUpdater {
    private final Level level;

    public InstantNeighborUpdater(Level level) {
        this.level = level;
    }

    public void shapeUpdate(@NonNull Direction direction, @NonNull BlockState blockState, @NonNull BlockPos blockPos, @NonNull BlockPos blockPos2, @UpdateFlags int i, int j) {
        NeighborUpdater.executeShapeUpdate(this.level, direction, blockPos, blockPos2, blockState, i, j - 1);
    }

    public void neighborChanged(@NonNull BlockPos blockPos, @NonNull Block block, @Nullable Orientation orientation) {
        BlockState blockState = this.level.getBlockState(blockPos);
        this.neighborChanged(blockState, blockPos, block, orientation, false);
    }

    public void neighborChanged(@NonNull BlockState blockState, @NonNull BlockPos blockPos, @NonNull Block block, @Nullable Orientation orientation, boolean bl) {
        NeighborUpdater.executeUpdate(this.level, blockState, blockPos, block, orientation, bl);
    }
}
