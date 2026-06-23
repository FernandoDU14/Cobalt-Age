package net.fernando.cobaltage.block;

import net.fernando.cobaltage.util.interfaces.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jspecify.annotations.NonNull;

import static net.fernando.cobaltage.util.signal.SignalType.COBALT;

public class CobaltRailBlock extends PoweredRailBlock {

    public CobaltRailBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(SHAPE, RailShape.NORTH_SOUTH)
                        .setValue(POWERED, false)
                        .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NonNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected void updateState(BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull Block block) {
        boolean bl = blockState.getValue(POWERED);
        boolean bl2 = ((SignalGetterByType) level).cobaltage$hasNeighbourSignalByType(COBALT, blockPos) || this.findPoweredRailSignal(level, blockPos, blockState, true, 0) || this.findPoweredRailSignal(level, blockPos, blockState, false, 0);
        if (bl2 != bl) {
            level.setBlock(blockPos, blockState.setValue(POWERED, bl2), 3);
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
            return (new RailState(level, blockPos, blockState)).place(((SignalGetterByType) level).cobaltage$hasNeighbourSignalByType(COBALT, blockPos), bl, railShape).getState();
        }
    }

    @Override
    protected boolean isSameRailWithPower(Level level, @NonNull BlockPos blockPos, boolean bl, int i, @NonNull RailShape railShape) {
        BlockState blockState = level.getBlockState(blockPos);
        if (!blockState.is(this)) {
            return false;
        } else {
            RailShape railShape2 = blockState.getValue(SHAPE);
            if (railShape != RailShape.EAST_WEST || railShape2 != RailShape.NORTH_SOUTH && railShape2 != RailShape.ASCENDING_NORTH && railShape2 != RailShape.ASCENDING_SOUTH) {
                if (railShape != RailShape.NORTH_SOUTH || railShape2 != RailShape.EAST_WEST && railShape2 != RailShape.ASCENDING_EAST && railShape2 != RailShape.ASCENDING_WEST) {
                    if (blockState.getValue(POWERED)) {
                        return ((SignalGetterByType) level).cobaltage$hasNeighbourSignalByType(COBALT, blockPos) || this.findPoweredRailSignal(level, blockPos, blockState, bl, i + 1);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

}