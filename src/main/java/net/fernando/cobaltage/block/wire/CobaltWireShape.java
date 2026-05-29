package net.fernando.cobaltage.block.wire;

import net.fernando.cobaltage.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.jetbrains.annotations.Nullable;
import static net.fernando.cobaltage.block.wire.CobaltWireNetwork.compatibleCobaltPowerSource;

public class CobaltWireShape {

    public static BlockState getUpdatedState(BlockGetter world, BlockPos pos, BlockState state) {
        // In new Mojang Mappings, RedstoneSide substitutes WireConnection
        RedstoneSide north = getRenderConnection(world, pos, Direction.NORTH);
        RedstoneSide south = getRenderConnection(world, pos, Direction.SOUTH);
        RedstoneSide east = getRenderConnection(world, pos, Direction.EAST);
        RedstoneSide west = getRenderConnection(world, pos, Direction.WEST);

        boolean hasNorth = north.isConnected();
        boolean hasSouth = south.isConnected();
        boolean hasEast = east.isConnected();
        boolean hasWest = west.isConnected();

        // 🛑 Default state return logic
        if (!hasNorth && !hasSouth && !hasEast && !hasWest) {
            // Keeping dot state
            if (isNotConnected(state)) {
                return state;
            }
            // If it's not a dot and has no neighbors (e.g., if I just broke the last neighboring wire),
            // go back to cross-shape state (default placement state).
            // this will force a block update and of its neighbors
            return state
                    .setValue(CobaltWireBlock.NORTH, RedstoneSide.SIDE)
                    .setValue(CobaltWireBlock.SOUTH, RedstoneSide.SIDE)
                    .setValue(CobaltWireBlock.EAST, RedstoneSide.SIDE)
                    .setValue(CobaltWireBlock.WEST, RedstoneSide.SIDE);
        }

        // When connections are present, recalculate shape normally
        boolean isIsolated = state.hasProperty(CobaltWireBlock.ISOLATED) && state.getValue(CobaltWireBlock.ISOLATED);
        if (!isIsolated) {
            if (!hasNorth && !hasSouth) {
                if (!hasEast) east = RedstoneSide.SIDE;
                if (!hasWest) west = RedstoneSide.SIDE;
            } else if (!hasEast && !hasWest) {
                if (!hasNorth) north = RedstoneSide.SIDE;
                if (!hasSouth) south = RedstoneSide.SIDE;
            }
        }

        return state
                .setValue(CobaltWireBlock.NORTH, north)
                .setValue(CobaltWireBlock.SOUTH, south)
                .setValue(CobaltWireBlock.EAST, east)
                .setValue(CobaltWireBlock.WEST, west);
    }

    private static boolean isNotConnected(BlockState state) {
        return state.getValue(CobaltWireBlock.NORTH) == RedstoneSide.NONE &&
                state.getValue(CobaltWireBlock.SOUTH) == RedstoneSide.NONE &&
                state.getValue(CobaltWireBlock.EAST) == RedstoneSide.NONE &&
                state.getValue(CobaltWireBlock.WEST) == RedstoneSide.NONE;
    }

    // Helper to evaluate the wire connection for a given direction
    public static RedstoneSide getRenderConnection(BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        BlockState sourceState = world.getBlockState(pos);

        mutable.setWithOffset(pos, direction);
        BlockState neighborState = world.getBlockState(mutable);

        mutable.setWithOffset(pos, Direction.UP);
        BlockState stateAboveNeighbor = world.getBlockState(mutable);

        boolean canAscend = !stateAboveNeighbor.isRedstoneConductor(world, mutable);

        // 1. Ascent - Run on Top - Climb Up
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
                    // If the lateral face of the neighbor block is sturdy (e.g. solid block or glass), you can climb up
                    if (neighborState.isFaceSturdy(world, mutable, direction.getOpposite()) &&
                    !neighborState.is(ModBlocks.COBALT_RELAY) // If the neighbor is a relay, it connects just horizontally
                    ) {
                        return RedstoneSide.UP;
                    }
                    // If it is not sturdy (es. Top-Slab o Scale), it connects diagonally
                    return RedstoneSide.SIDE;
                }
            }
        }

        // 2. (a) Horizontal connection (side)
        // Resetting mutable cursor to neighbor block for horizontal connection validation
        mutable.setWithOffset(pos, direction);
        if (canVisuallyHorizontallyConnectTo(sourceState, neighborState, direction)) {
            return RedstoneSide.SIDE;
        }

        // 3. (d) Horizontal connection (side) for descent
        // Does the neighbor block obstruct the passage (is it an opaque conductor)?
        if (!neighborState.isRedstoneConductor(world, mutable)) {
            mutable.setWithOffset(pos, Direction.DOWN);
            if (!world.getBlockState(mutable).is(ModBlocks.COBALT_RELAY)) {
                // If the block in which I survive is a relay, it does not connect to the other dust
                mutable.setWithOffset(pos, direction).move(Direction.DOWN);
                if (world.getBlockState(mutable).is(ModBlocks.COBALT_DUST)) {
                    return RedstoneSide.SIDE;
                }
            }
        }

        return RedstoneSide.NONE;
    }

    // Helper to evaluate if two Cobalt Wire Instances blocks can connect visually (Horizontal connection)
    // This overrides the default behavior of compatible cobalt power source as you can see down below
    private static boolean canVisuallyHorizontallyConnectTo(BlockState sourceState, BlockState targetState, @Nullable Direction dir) {

        if (sourceState.is(ModBlocks.COBALT_RELAY) && targetState.is(ModBlocks.COBALT_RELAY)) {
            return false;
        }

        if (targetState.getBlock() instanceof CobaltWireBlock) return true;

        // Special case (ModBlocks)
        if (targetState.getBlock() instanceof CobaltRepeaterBlock) {
            Direction facing = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (dir == null) return true;
            return dir == facing || dir == facing.getOpposite();
        }

        if(targetState.getBlock() instanceof CobaltConverterBlock){
            Direction facing = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (dir == null) return true;
            return dir == facing.getOpposite();
        }

        if(canRestrictedCobaltPowerSourceConnectTo(targetState, dir)){
            return true;
        }

        return targetState.getBlock() instanceof CobaltPowerSource ||
                compatibleCobaltPowerSource(targetState);
    }

    private static boolean canRestrictedCobaltPowerSourceConnectTo(BlockState state, Direction askingForOutputDirection) {
        // The Observer emits only in its opposite facing
        // The Calibrated Sculk Sensor does emit in its opposite facing, but it is always connected to it
        return( (state.is(Blocks.OBSERVER) && state.getValue(ObserverBlock.FACING) == askingForOutputDirection) ||
                (state.is(Blocks.CALIBRATED_SCULK_SENSOR))
        );
    }

}