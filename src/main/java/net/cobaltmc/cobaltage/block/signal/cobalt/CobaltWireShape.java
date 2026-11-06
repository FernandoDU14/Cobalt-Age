package net.cobaltmc.cobaltage.block.signal.cobalt;

import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.block.cobalt.CobaltConverterBlock;
import net.cobaltmc.cobaltage.block.cobalt.CobaltRepeaterBlock;
import net.cobaltmc.cobaltage.block.cobalt.CobaltWireBlock;
import net.cobaltmc.cobaltage.util.signal.SignalUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.jetbrains.annotations.Nullable;

public class CobaltWireShape {
    public static BlockState getUpdatedState(BlockGetter world, BlockPos pos, BlockState state) {
        RedstoneSide north = getRenderConnection(world, pos, Direction.NORTH);
        RedstoneSide south = getRenderConnection(world, pos, Direction.SOUTH);
        RedstoneSide east = getRenderConnection(world, pos, Direction.EAST);
        RedstoneSide west = getRenderConnection(world, pos, Direction.WEST);
        boolean isRetracted = state.hasProperty(CobaltWireBlock.RETRACTED) && state.getValue(CobaltWireBlock.RETRACTED);
        boolean hasNorth = north.isConnected();
        boolean hasSouth = south.isConnected();
        boolean hasEast = east.isConnected();
        boolean hasWest = west.isConnected();
        if (!hasNorth && !hasSouth && !hasEast && !hasWest) {
            return isDot(state) ? state : (((
                    state.setValue(CobaltWireBlock.NORTH, RedstoneSide.SIDE))
                    .setValue(CobaltWireBlock.SOUTH, RedstoneSide.SIDE))
                    .setValue(CobaltWireBlock.EAST, RedstoneSide.SIDE))
                    .setValue(CobaltWireBlock.WEST, RedstoneSide.SIDE);
        } else if (!isRetracted) {
            if (!hasNorth && !hasSouth) {
                if (!hasEast) {
                    east = RedstoneSide.SIDE;
                }

                if (!hasWest) {
                    west = RedstoneSide.SIDE;
                }
            } else if (!hasEast && !hasWest) {
                if (!hasNorth) {
                    north = RedstoneSide.SIDE;
                }

                if (!hasSouth) {
                    south = RedstoneSide.SIDE;
                }
            }

            return (((state.setValue(CobaltWireBlock.NORTH, north))
                    .setValue(CobaltWireBlock.SOUTH, south))
                    .setValue(CobaltWireBlock.EAST, east))
                    .setValue(CobaltWireBlock.WEST, west);
        } else {
            return ((((state.setValue(CobaltWireBlock.RETRACTED,
                    world.getBlockState(pos.below()).is(ModBlocks.COBALT_RELAY) ||
                            CobaltWireBlock.isFreeLine((Level)world, pos))).setValue(CobaltWireBlock.NORTH, north))
                    .setValue(CobaltWireBlock.SOUTH, south)).setValue(CobaltWireBlock.EAST, east))
                    .setValue(CobaltWireBlock.WEST, west);
        }
    }

    private static boolean isDot(BlockState state) {
        return state.getValue(CobaltWireBlock.NORTH) == RedstoneSide.NONE && state.getValue(CobaltWireBlock.SOUTH) == RedstoneSide.NONE && state.getValue(CobaltWireBlock.EAST) == RedstoneSide.NONE && state.getValue(CobaltWireBlock.WEST) == RedstoneSide.NONE;
    }

    public static RedstoneSide getRenderConnection(BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockState sourceState = world.getBlockState(pos);
        mutable.setWithOffset(pos, direction);
        BlockState neighborState = world.getBlockState(mutable);
        mutable.setWithOffset(pos, Direction.UP);
        BlockState stateAboveNeighbor = world.getBlockState(mutable);
        boolean canAscend = !stateAboveNeighbor.isRedstoneConductor(world, mutable);
        if (canAscend) {
            mutable.setWithOffset(pos, direction).move(Direction.UP);
            if (world.getBlockState(mutable).is(ModBlocks.COBALT_DUST)) {
                mutable.setWithOffset(pos, direction);
                boolean canSurviveOnNeighbor = neighborState.getBlock() instanceof TrapDoorBlock || neighborState.isFaceSturdy(world, mutable, Direction.UP) || neighborState.is(Blocks.HOPPER);
                if (canSurviveOnNeighbor) {
                    if (neighborState.isFaceSturdy(world, mutable, direction.getOpposite()) && !neighborState.is(ModBlocks.COBALT_RELAY)) {
                        return RedstoneSide.UP;
                    }

                    return RedstoneSide.SIDE;
                }
            }
        }

        mutable.setWithOffset(pos, direction);
        if (canSourceConnectToTarget(sourceState, neighborState, direction)) {
            return RedstoneSide.SIDE;
        } else {
            if (!neighborState.isRedstoneConductor(world, mutable)) {
                mutable.setWithOffset(pos, Direction.DOWN);
                if (!world.getBlockState(mutable).is(ModBlocks.COBALT_RELAY)) {
                    mutable.setWithOffset(pos, direction).move(Direction.DOWN);
                    if (world.getBlockState(mutable).is(ModBlocks.COBALT_DUST)) {
                        return RedstoneSide.SIDE;
                    }
                }
            }

            return RedstoneSide.NONE;
        }
    }

    public static boolean canSourceConnectToTarget(BlockState sourceState, BlockState targetState, @Nullable Direction dir) {
        if (sourceState.is(ModBlocks.COBALT_RELAY)) {
            if (targetState.is(ModBlocks.COBALT_RELAY)) {
                return false;
            }

            if (targetState.is(ModBlocks.COBALT_WALL_TORCH)) {
                if (dir == null) {
                    return true;
                }

                Direction attachedFace = (targetState.getValue(BlockStateProperties.HORIZONTAL_FACING)).getOpposite();
                return dir == attachedFace;
            }
        }

        if (targetState.getBlock() instanceof CobaltWireBlock) {
            return true;
        } else if (targetState.getBlock() instanceof CobaltRepeaterBlock) {
            Direction facing = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return dir == facing || dir == facing.getOpposite();
        } else if (targetState.getBlock() instanceof CobaltConverterBlock) {
            Direction facing = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return dir == facing.getOpposite();
        } else {
            assert dir != null;

            if (dir.getAxis().isHorizontal() && SignalUtils.shouldCobaltWireLinkToDirectionalRedstoneSource(targetState, dir)) {
                return true;
            } else {
                return targetState.getBlock() instanceof CobaltSignalSource || SignalUtils.shouldRedstoneSourceEmitCobalt(targetState);
            }
        }
    }
}
