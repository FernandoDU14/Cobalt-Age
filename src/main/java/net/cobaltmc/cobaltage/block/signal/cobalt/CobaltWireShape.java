package net.cobaltmc.cobaltage.block.signal.cobalt;

import net.cobaltmc.cobaltage.block.abstracts.CobaltDiodeBlock;
import net.cobaltmc.cobaltage.block.cobalt.CobaltDustBlock;
import net.cobaltmc.cobaltage.block.cobalt.CobaltWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.jetbrains.annotations.Nullable;

public class CobaltWireShape {

    public static BlockState getUpdatedState(BlockGetter world, BlockPos pos, BlockState state) {
        RedstoneSide north = getRenderConnection(world, pos, Direction.NORTH);
        RedstoneSide south = getRenderConnection(world, pos, Direction.SOUTH);
        RedstoneSide east = getRenderConnection(world, pos, Direction.EAST);
        RedstoneSide west = getRenderConnection(world, pos, Direction.WEST);

        boolean hasNorth = north.isConnected();
        boolean hasSouth = south.isConnected();
        boolean hasEast = east.isConnected();
        boolean hasWest = west.isConnected();

        int connectionCount = 0;
        if (hasNorth) connectionCount++;
        if (hasSouth) connectionCount++;
        if (hasEast) connectionCount++;
        if (hasWest) connectionCount++;

        if (connectionCount == 0) {
            return state
                    .setValue(CobaltWireBlock.NORTH, RedstoneSide.SIDE)
                    .setValue(CobaltWireBlock.SOUTH, RedstoneSide.SIDE)
                    .setValue(CobaltWireBlock.EAST, RedstoneSide.SIDE)
                    .setValue(CobaltWireBlock.WEST, RedstoneSide.SIDE);
        }

        boolean isRetracted = state.hasProperty(CobaltWireBlock.RETRACTED) && state.getValue(CobaltWireBlock.RETRACTED);
        if (connectionCount >= 2) {
            isRetracted = false;
        }

        if (!isRetracted && connectionCount == 1) {
            if (hasNorth) south = RedstoneSide.SIDE;
            else if (hasSouth) north = RedstoneSide.SIDE;
            else if (hasEast) west = RedstoneSide.SIDE;
            else east = RedstoneSide.SIDE;
        }

        return state
                .setValue(CobaltWireBlock.RETRACTED, isRetracted)
                .setValue(CobaltWireBlock.NORTH, north)
                .setValue(CobaltWireBlock.SOUTH, south)
                .setValue(CobaltWireBlock.EAST, east)
                .setValue(CobaltWireBlock.WEST, west);
    }

    public static RedstoneSide getRenderConnection(BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockState sourceState = world.getBlockState(pos);

        // 1. Horizontal connection
        mutable.setWithOffset(pos, direction);
        BlockState neighborState = world.getBlockState(mutable);
        if (canSourceConnectToTarget(world, pos, mutable, sourceState, neighborState, direction)) {
            return RedstoneSide.SIDE;
        }

        // 2. Step UP connection (Blocked if block directly above pos is a solid conductor)
        BlockPos abovePos = pos.above();
        if (!world.getBlockState(abovePos).isRedstoneConductor(world, abovePos)) {
            mutable.setWithOffset(pos, direction).move(Direction.UP);
            BlockState upperState = world.getBlockState(mutable);
            if (canSourceConnectToTarget(world, pos, mutable, sourceState, upperState, direction)) {
                return RedstoneSide.UP;
            }
        }

        // 3. Step DOWN connection (Blocked if side neighbor is a solid conductor)
        if (!neighborState.isRedstoneConductor(world, mutable)) {
            mutable.setWithOffset(pos, direction).move(Direction.DOWN);
            BlockState lowerState = world.getBlockState(mutable);
            if (canSourceConnectToTarget(world, pos, mutable, sourceState, lowerState, direction)) {
                return RedstoneSide.SIDE;
            }
        }

        return RedstoneSide.NONE;
    }

    public static boolean canSourceConnectToTarget(BlockState sourceState, BlockState targetState, @Nullable Direction dir) {
        return canSourceConnectToTarget(null, null, null, sourceState, targetState, dir);
    }

    public static boolean canSourceConnectToTarget(@Nullable BlockGetter world, @Nullable BlockPos sourcePos, @Nullable BlockPos targetPos, BlockState sourceState, BlockState targetState, @Nullable Direction dir) {
        if (targetState.getBlock() instanceof CobaltWireBlock) {
            return true;
        }
        if (targetState.getBlock() instanceof CobaltDustBlock) {
            return false;
        }
        return targetState.getBlock() instanceof CobaltDiodeBlock || targetState.getBlock() instanceof CobaltSignalSource;
    }
}