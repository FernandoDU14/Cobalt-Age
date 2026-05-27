package net.fernando.cobaltage.util;

import net.fernando.cobaltage.block.CobaltWireBlock;
import net.fernando.cobaltage.block.wire.CobaltWireNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class CobaltPowerHelper {

    /**
     * This is a Helper Class to get the Cobalt Power through Cobalt Power Sources specifically for Mixins
     * Either Standard Weak/Strong Power and Quasi-Connectivity Power.
     */
    public static boolean isPoweredByCobalt(Level world, BlockPos pos) {
        // 1. Rapid Check: Direct Strong Power From Neighbors (Cobalt Repeater, Cobalt Torch, Converter, ecc.)
        if (CobaltWireNetwork.getStrongPowerFromNeighbors(world, pos) > 0) return true;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // 2. Direct Power by Cobalt Wire
            if (neighborState.getBlock() instanceof CobaltWireBlock) {
                if (neighborState.getValue(CobaltWireBlock.POWER) > 0) {
                    if (isWirePointingTo(neighborState, dir.getOpposite())) return true;
                }
            }

            // 3. Weak Power from Strong Power Solid Block
            if (neighborState.isRedstoneConductor(world, neighborPos)) {
                // Cobalt Wire over the Solid Block
                BlockState stateAbove = world.getBlockState(neighborPos.above());
                if (stateAbove.getBlock() instanceof CobaltWireBlock && stateAbove.getValue(CobaltWireBlock.POWER) > 0) return true;

                // Cobalt Wire pointing to the Solid Block
                for (Direction sideDir : Direction.Plane.HORIZONTAL) {
                    BlockPos sidePos = neighborPos.relative(sideDir);
                    BlockState sideState = world.getBlockState(sidePos);
                    if (sideState.getBlock() instanceof CobaltWireBlock && sideState.getValue(CobaltWireBlock.POWER) > 0) {
                        if (isWirePointingTo(sideState, sideDir.getOpposite())) return true;
                    }
                }

                // Undirect Strong Power From Neighbors (Cobalt Repeater, Cobalt Torch, Converter, ecc.)
                if (CobaltWireNetwork.getStrongPowerFromNeighbors(world, neighborPos) > 0) return true;
            }
        }
        return false;
    }

    public static boolean isPoweredOrQuasiPoweredByCobalt(Level world, BlockPos pos, Direction exceptDir) {
        // 1. Rapid Check: Direct Strong Power From Neighbors (Cobalt Repeater, Cobalt Torch, Converter, ecc.)
        if (CobaltWireNetwork.getStrongPowerFromNeighbors(world, pos) > 0) return true;

        for (Direction dir : Direction.values()) {
            if (dir == exceptDir) continue;

            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            BlockPos neighborPosUp = pos.relative(dir).above();
            BlockState neighborStateUp = world.getBlockState(neighborPosUp);

            // 2. Direct Power by Cobalt Wire
            if (neighborState.getBlock() instanceof CobaltWireBlock) {
                if (neighborState.getValue(CobaltWireBlock.POWER) > 0) {
                    if (isWirePointingTo(neighborState, dir.getOpposite())) return true;
                }
            }

            // 3. Weak Power from Strong Power Solid Block
            if (neighborState.isRedstoneConductor(world, neighborPos)) {
                // Cobalt Wire over the Solid Block
                BlockState stateAbove = world.getBlockState(neighborPos.above());
                if (stateAbove.getBlock() instanceof CobaltWireBlock && stateAbove.getValue(CobaltWireBlock.POWER) > 0) return true;

                // Undirect Strong Power From Neighbors (Cobalt Repeater, Cobalt Torch, Converter, ecc.)
                if (CobaltWireNetwork.getStrongPowerFromNeighbors(world, neighborPos) > 0) return true;

                // Cobalt Wire pointing to the Solid Block
                for (Direction sideDir : Direction.Plane.HORIZONTAL) {
                    BlockPos sidePos = neighborPos.relative(sideDir);
                    BlockState sideState = world.getBlockState(sidePos);
                    if (sideState.getBlock() instanceof CobaltWireBlock && sideState.getValue(CobaltWireBlock.POWER) > 0) {
                        if (isWirePointingTo(sideState, sideDir.getOpposite())) return true;
                    }
                }
            }
            // 4. Quasi Connectivity Implementations
            if(dir == Direction.UP && neighborStateUp.getBlock() instanceof CobaltWireBlock){
                if (neighborStateUp.getValue(CobaltWireBlock.POWER) > 0) return true;
            }
            if(neighborStateUp.isRedstoneConductor(world, neighborPosUp)){
                BlockState stateAboveBlock = world.getBlockState(neighborPosUp.above());
                if (stateAboveBlock.getBlock() instanceof CobaltWireBlock) {
                    if (stateAboveBlock.getValue(CobaltWireBlock.POWER) > 0) return true;
                }
            }
        }
        return false;
    }

    private static boolean isWirePointingTo(BlockState state, Direction dirToTarget) {
        return switch (dirToTarget) {
            case NORTH -> state.getValue(BlockStateProperties.NORTH_REDSTONE).isConnected();
            case SOUTH -> state.getValue(BlockStateProperties.SOUTH_REDSTONE).isConnected();
            case EAST -> state.getValue(BlockStateProperties.EAST_REDSTONE).isConnected();
            case WEST -> state.getValue(BlockStateProperties.WEST_REDSTONE).isConnected();
            default -> false;
        };
    }
}