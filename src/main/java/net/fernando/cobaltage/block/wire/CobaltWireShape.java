package net.fernando.cobaltage.block.wire;

import net.fernando.cobaltage.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.jetbrains.annotations.Nullable;
import static net.fernando.cobaltage.util.signal.SignalHelper.compatibleCobaltPowerSource;

public class CobaltWireShape {

    // Helper to compute the new wire state shapes
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
            if (isNotConnected(state)) {
                return state;
            }
            return state
                    .setValue(CobaltWireBlock.NORTH, RedstoneSide.SIDE)
                    .setValue(CobaltWireBlock.SOUTH, RedstoneSide.SIDE)
                    .setValue(CobaltWireBlock.EAST, RedstoneSide.SIDE)
                    .setValue(CobaltWireBlock.WEST, RedstoneSide.SIDE);
        }

        if (!isRetracted) {
            if (!hasNorth && !hasSouth) {
                if (!hasEast) east = RedstoneSide.SIDE;
                if (!hasWest) west = RedstoneSide.SIDE;
            } else if (!hasEast && !hasWest) {
                if (!hasNorth) north = RedstoneSide.SIDE;
                if (!hasSouth) south = RedstoneSide.SIDE;
            }
        }else{
            return state
                    .setValue(CobaltWireBlock.RETRACTED,
                            world.getBlockState(pos.below()).is(ModBlocks.COBALT_RELAY)
                            || CobaltWireBlock.hasOneFreeConnectionInALineShape( (Level) world, pos))
                    .setValue(CobaltWireBlock.NORTH, north)
                    .setValue(CobaltWireBlock.SOUTH, south)
                    .setValue(CobaltWireBlock.EAST, east)
                    .setValue(CobaltWireBlock.WEST, west);
        }

        return state
                .setValue(CobaltWireBlock.NORTH, north)
                .setValue(CobaltWireBlock.SOUTH, south)
                .setValue(CobaltWireBlock.EAST, east)
                .setValue(CobaltWireBlock.WEST, west);
    }

    // is . State?
    private static boolean isNotConnected(BlockState state) {
        return state.getValue(CobaltWireBlock.NORTH) == RedstoneSide.NONE &&
                state.getValue(CobaltWireBlock.SOUTH) == RedstoneSide.NONE &&
                state.getValue(CobaltWireBlock.EAST) == RedstoneSide.NONE &&
                state.getValue(CobaltWireBlock.WEST) == RedstoneSide.NONE;
    }

    // Helper to compute the wire connection for a given direction
    public static RedstoneSide getRenderConnection(BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockState sourceState = world.getBlockState(pos);
        mutable.setWithOffset(pos, direction);
        BlockState neighborState = world.getBlockState(mutable);
        mutable.setWithOffset(pos, Direction.UP);
        BlockState stateAboveNeighbor = world.getBlockState(mutable);

        boolean canAscend = !stateAboveNeighbor.isRedstoneConductor(world, mutable);

        // 1. Computing ascent checks
        if (canAscend) {
            // Moving mutable cursor
            mutable.setWithOffset(pos, direction).move(Direction.UP);
            if (world.getBlockState(mutable).is(ModBlocks.COBALT_DUST)) {
                // Removing mutable cursor
                mutable.setWithOffset(pos, direction);
                boolean canSurviveOnNeighbor = neighborState.getBlock() instanceof TrapDoorBlock ||
                        neighborState.isFaceSturdy(world, mutable, Direction.UP) ||
                        neighborState.is(Blocks.HOPPER);
                if (canSurviveOnNeighbor) {
                    // If the lateral face of the neighbour block is sturdy (e.g. solid block or glass), you can climb up
                    if (neighborState.isFaceSturdy(world, mutable, direction.getOpposite()) &&
                    !neighborState.is(ModBlocks.COBALT_RELAY) // If the neighbour is a relay, it connects just horizontally
                    ) {
                        return RedstoneSide.UP;
                    }
                    // If it is not sturdy (ex. top-slab or diagonal stairs), it just connects horizontally
                    return RedstoneSide.SIDE;
                }
            }
        }

        // 2. Computing horizontal checks
        mutable.setWithOffset(pos, direction);
        if (canSourceConnectToTarget(sourceState, neighborState, direction)) {
            return RedstoneSide.SIDE;
        }

        // 3. Computing descend checks
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

    // Helper to evaluate if two Cobalt Wire Instances blocks can connect (just visually a horizontal connection)
    public static boolean canSourceConnectToTarget(BlockState sourceState, BlockState targetState, @Nullable Direction dir) {

        if (sourceState.is(ModBlocks.COBALT_RELAY)) {
            if(targetState.is(ModBlocks.COBALT_RELAY)){
                return false;
            }
            if(targetState.is(ModBlocks.COBALT_WALL_TORCH)){
                if (dir == null) return true;
                Direction attachedFace = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
                return dir == attachedFace;
            }
            // Future implementations for Cobalt Relay...
        }

        if (targetState.getBlock() instanceof CobaltWireBlock) return true;

        // Special cases (ModBlocks)
        if (targetState.getBlock() instanceof CobaltRepeaterBlock) {
            Direction facing = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return dir == facing || dir == facing.getOpposite();
        }

        if(targetState.getBlock() instanceof CobaltConverterBlock){
            Direction facing = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return dir == facing.getOpposite();
        }

        // Special cases (Vanilla blocks)
        assert dir != null;
        if(dir.getAxis().isHorizontal()){
            if(canRestrictedCobaltPowerSourceConnectTo(targetState, dir)){
                return true;
            }
        }

        return targetState.getBlock() instanceof CobaltSignalEmitter ||
                compatibleCobaltPowerSource(targetState);
    }

    private static boolean canRestrictedCobaltPowerSourceConnectTo(BlockState state, Direction askingForLinkDirection) {
        // The Observer emits only in its opposite facing
        // The Calibrated Sculk Sensor does emit in its opposite facing, but it is always connected to it
        return( (state.is(Blocks.OBSERVER) && state.getValue(ObserverBlock.FACING) == askingForLinkDirection) ||
                (state.is(Blocks.CALIBRATED_SCULK_SENSOR))
        );
    }

}