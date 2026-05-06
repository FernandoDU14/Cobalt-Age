package net.fernando.cobaltage.util;

import net.fernando.cobaltage.block.CobaltWireBlock;
import net.fernando.cobaltage.block.wire.CobaltWireNetwork;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class CobaltPowerHelper {

    /**
     * This is a Helper Class to get the Cobalt Power through Cobalt Power Sources specifically for Mixins
     * Either Standard Weak/Strong Power and Quasi-Connectivity Power.
     */
    public static boolean isPoweredByCobalt(World world, BlockPos pos) {
        // 1. Rapid Check: Direct Strong Power From Neighbors (Cobalt Repeater, Cobalt Torch, Converter, ecc.)
        if (CobaltWireNetwork.getStrongPowerFromNeighbors(world, pos) > 0) return true;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // 2. Direct Power by Cobalt Wire
            if (neighborState.getBlock() instanceof CobaltWireBlock) {
                if (neighborState.get(CobaltWireBlock.POWER) > 0) {
                    if (isWirePointingTo(neighborState, dir.getOpposite())) return true;
                }
            }

            // 3. Weak Power from Strong Power Solid Block
            if (neighborState.isSolidBlock(world, neighborPos)) {
                // Cobalt Wire over the Solid Block
                BlockState stateAbove = world.getBlockState(neighborPos.up());
                if (stateAbove.getBlock() instanceof CobaltWireBlock && stateAbove.get(CobaltWireBlock.POWER) > 0) return true;

                // Cobalt Wire pointing to the Solid Block
                for (Direction sideDir : Direction.Type.HORIZONTAL) {
                    BlockPos sidePos = neighborPos.offset(sideDir);
                    BlockState sideState = world.getBlockState(sidePos);
                    if (sideState.getBlock() instanceof CobaltWireBlock && sideState.get(CobaltWireBlock.POWER) > 0) {
                        if (isWirePointingTo(sideState, sideDir.getOpposite())) return true;
                    }
                }

                // Undirect Strong Power From Neighbors (Cobalt Repeater, Cobalt Torch, Converter, ecc.)
                if (CobaltWireNetwork.getStrongPowerFromNeighbors(world, neighborPos) > 0) return true;
            }
        }
        return false;
    }

    public static boolean isPoweredOrQuasiPoweredByCobalt(World world, BlockPos pos, Direction exceptDir) {
        // 1. Rapid Check: Direct Strong Power From Neighbors (Cobalt Repeater, Cobalt Torch, Converter, ecc.)
        if (CobaltWireNetwork.getStrongPowerFromNeighbors(world, pos) > 0) return true;

        for (Direction dir : Direction.values()) {
            if (dir == exceptDir) continue;

            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            BlockPos neighborPosUp = pos.offset(dir).up();
            BlockState neighborStateUp = world.getBlockState(neighborPosUp);

            // 2. Direct Power by Cobalt Wire
            if (neighborState.getBlock() instanceof CobaltWireBlock) {
                if (neighborState.get(CobaltWireBlock.POWER) > 0) {
                    if (isWirePointingTo(neighborState, dir.getOpposite())) return true;
                }
            }

            // 3. Weak Power from Strong Power Solid Block
            if (neighborState.isSolidBlock(world, neighborPos)) {
                // Cobalt Wire over the Solid Block
                BlockState stateAbove = world.getBlockState(neighborPos.up());
                if (stateAbove.getBlock() instanceof CobaltWireBlock && stateAbove.get(CobaltWireBlock.POWER) > 0) return true;

                // Undirect Strong Power From Neighbors (Cobalt Repeater, Cobalt Torch, Converter, ecc.)
                if (CobaltWireNetwork.getStrongPowerFromNeighbors(world, neighborPos) > 0) return true;

                // Cobalt Wire pointing to the Solid Block
                for (Direction sideDir : Direction.Type.HORIZONTAL) {
                    BlockPos sidePos = neighborPos.offset(sideDir);
                    BlockState sideState = world.getBlockState(sidePos);
                    if (sideState.getBlock() instanceof CobaltWireBlock && sideState.get(CobaltWireBlock.POWER) > 0) {
                        if (isWirePointingTo(sideState, sideDir.getOpposite())) return true;
                    }
                }
            }
            // 4. Quasi Connectivity Implementations
            if(dir == Direction.UP && neighborStateUp.getBlock() instanceof CobaltWireBlock){
                if (neighborStateUp.get(CobaltWireBlock.POWER) > 0) return true;
            }
            if(neighborStateUp.isSolidBlock(world, neighborPosUp)){
                BlockState stateAboveBlock = world.getBlockState(neighborPosUp.up());
                if (stateAboveBlock.getBlock() instanceof CobaltWireBlock) {
                    if (stateAboveBlock.get(CobaltWireBlock.POWER) > 0) return true;
                }
            }
        }
        return false;
    }

    private static boolean isWirePointingTo(BlockState state, Direction dirToTarget) {
        return switch (dirToTarget) {
            case NORTH -> state.get(Properties.NORTH_WIRE_CONNECTION).isConnected();
            case SOUTH -> state.get(Properties.SOUTH_WIRE_CONNECTION).isConnected();
            case EAST -> state.get(Properties.EAST_WIRE_CONNECTION).isConnected();
            case WEST -> state.get(Properties.WEST_WIRE_CONNECTION).isConnected();
            default -> false;
        };
    }
}