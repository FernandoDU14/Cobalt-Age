package net.cobaltmc.cobaltage.block.cobalt;

import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jspecify.annotations.NonNull;

public class CobaltRailBlock extends PoweredRailBlock {

    public CobaltRailBlock(Properties settings) {
        super(settings);
        BlockState defaultState = this.stateDefinition.any()
                .setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false);

        this.registerDefaultState(defaultState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NonNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected void updateState(BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull Block block) {
        boolean currentlyPowered = blockState.getValue(POWERED);

        // Check if directly powered or powered via propagation down the track in either direction
        boolean shouldBePowered = ((SignalGetterByType) level).cobaltage$hasNeighborSignalByType(SignalType.COBALT, blockPos)
                || this.findPoweredRailSignal(level, blockPos, blockState, true, 0)
                || this.findPoweredRailSignal(level, blockPos, blockState, false, 0);

        if (shouldBePowered != currentlyPowered) {
            level.setBlock(blockPos, blockState.setValue(POWERED, shouldBePowered), 3);
            level.updateNeighborsAt(blockPos.below(), this);
            if (blockState.getValue(SHAPE).isSlope()) {
                level.updateNeighborsAt(blockPos.above(), this);
            }
        }
    }

    @Override
    protected @NonNull BlockState updateDir(Level level, @NonNull BlockPos blockPos, @NonNull BlockState blockState, boolean bl) {
        if (level.isClientSide()) {
            return blockState;
        } else {
            RailShape railShape = blockState.getValue(this.getShapeProperty());
            return (new RailState(level, blockPos, blockState)).place(((SignalGetterByType) level).cobaltage$hasNeighborSignalByType(SignalType.COBALT, blockPos), bl, railShape).getState();
        }
    }

    @Override
    protected boolean isSameRailWithPower(Level level, BlockPos pos, boolean searchDirection, int distance, RailShape shape) {
        BlockState blockState = level.getBlockState(pos);
        if (!blockState.is(this)) {
            return false;
        }

        RailShape neighborShape = blockState.getValue(SHAPE);

        // Prevent power propagation through misaligned perpendicular rails
        if (shape == RailShape.EAST_WEST && (neighborShape == RailShape.NORTH_SOUTH || neighborShape == RailShape.ASCENDING_NORTH || neighborShape == RailShape.ASCENDING_SOUTH)) {
            return false;
        }
        if (shape == RailShape.NORTH_SOUTH && (neighborShape == RailShape.EAST_WEST || neighborShape == RailShape.ASCENDING_EAST || neighborShape == RailShape.ASCENDING_WEST)) {
            return false;
        }

        // 1. Direct signal source check at this position
        if (((SignalGetterByType) level).cobaltage$hasNeighborSignalByType(SignalType.COBALT, pos)) {
            return true;
        }

        // 2. Propagate power up to 8 rails away
        if (distance < 8) {
            return this.findPoweredRailSignal(level, pos, blockState, searchDirection, distance + 1);
        }

        return false;
    }
}