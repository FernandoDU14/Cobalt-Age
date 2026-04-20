package net.fernando.cobaltrails.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class CobaltWireShape {

    public static void updateConnections(World world, BlockPos pos, BlockState state) {

        boolean north = canConnect(world, pos.north());
        boolean south = canConnect(world, pos.south());
        boolean east  = canConnect(world, pos.east());
        boolean west  = canConnect(world, pos.west());

        BlockState newState = state
                .with(CobaltWireBlock.NORTH, north)
                .with(CobaltWireBlock.SOUTH, south)
                .with(CobaltWireBlock.EAST, east)
                .with(CobaltWireBlock.WEST, west);

        world.setBlockState(pos, newState, Block.NOTIFY_ALL);
    }

    private static boolean canConnect(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        return state.getBlock() instanceof CobaltWireBlock
                || state.isSideSolidFullSquare(world, pos, Direction.UP);
    }
}