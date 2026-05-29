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

    // 1. CACHE DEGLI ARRAY: Evita la creazione di array e iteratori nei cicli for
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    // 2. THREAD-LOCAL MUTABLES: Allocazione ZERO durante i tick del gioco.
    // Usiamo un array di 3 posizioni riutilizzabili per ogni thread.
    private static final ThreadLocal<BlockPos.MutableBlockPos[]> MUTABLES = ThreadLocal.withInitial(() -> new BlockPos.MutableBlockPos[]{
            new BlockPos.MutableBlockPos(),
            new BlockPos.MutableBlockPos(),
            new BlockPos.MutableBlockPos()
    });

    public static boolean isPoweredByCobalt(Level world, BlockPos pos) {
        // 1. Rapid Check: Direct Strong Power From Neighbors (Cobalt Repeater, Cobalt Torch, Converter, ecc.)
        if (CobaltWireNetwork.getStrongPowerFromNeighbors(world, pos) > 0) return true;

        BlockPos.MutableBlockPos[] mutable = MUTABLES.get();
        BlockPos.MutableBlockPos neighborPos = mutable[0];
        BlockPos.MutableBlockPos sidePos = mutable[1];
        BlockPos.MutableBlockPos sideAbovePos = mutable[2];

        for (Direction dir : DIRECTIONS) {
            neighborPos.set(pos).move(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // 2. Direct Power by Cobalt Wire
            if (neighborState.getBlock() instanceof CobaltWireBlock &&
                neighborState.getValue(CobaltWireBlock.POWER) > 0 && isWirePointingTo(neighborState, dir.getOpposite())) {
                return true;
            }

            // 3. Weak Power from Strong Power Solid Block
            if (neighborState.isRedstoneConductor(world, neighborPos)) {
                // Cobalt Wire over the Solid Block
                sideAbovePos.set(neighborPos).move(Direction.UP);
                BlockState stateAbove = world.getBlockState(sideAbovePos);
                if (stateAbove.getBlock() instanceof CobaltWireBlock && stateAbove.getValue(CobaltWireBlock.POWER) > 0) return true;

                // Cobalt Wire pointing to the Solid Block
                for (Direction sideDir : Direction.Plane.HORIZONTAL) {
                    sidePos.set(neighborPos).move(sideDir);
                    BlockState sideState = world.getBlockState(sidePos);
                    if (sideState.getBlock() instanceof CobaltWireBlock &&
                        sideState.getValue(CobaltWireBlock.POWER) > 0 && isWirePointingTo(sideState, dir.getOpposite())) {
                        return true;
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

        BlockPos.MutableBlockPos[] mutable = MUTABLES.get();
        BlockPos.MutableBlockPos neighborPos = mutable[0];
        BlockPos.MutableBlockPos sidePos = mutable[1];
        BlockPos.MutableBlockPos neighbourAbovePos = mutable[2];

        for (Direction dir : DIRECTIONS) {
            if (dir == exceptDir) continue;

            neighborPos.set(pos).move(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // 2. Direct Power by Cobalt Wire
            if (neighborState.getBlock() instanceof CobaltWireBlock &&
                neighborState.getValue(CobaltWireBlock.POWER) > 0 && isWirePointingTo(neighborState, dir.getOpposite())) {
                return true;
            }

            BlockState neighborStateUp = null;

            // 3. Weak Power from Strong Power Solid Block
            if (neighborState.isRedstoneConductor(world, neighborPos)) {

                neighbourAbovePos.set(neighborPos).move(Direction.UP);
                neighborStateUp = world.getBlockState(neighbourAbovePos);

                // Cobalt Wire over the Solid Block
                if (neighborStateUp.getBlock() instanceof CobaltWireBlock && neighborStateUp.getValue(CobaltWireBlock.POWER) > 0) return true;

                // Undirect Strong Power From Neighbors (Cobalt Repeater, Cobalt Torch, Converter, ecc.)
                if (CobaltWireNetwork.getStrongPowerFromNeighbors(world, neighborPos) > 0) return true;

                // Cobalt Wire pointing to the Solid Block
                for (Direction sideDir : HORIZONTALS) {
                    sidePos.set(neighborPos).move(sideDir);
                    BlockState sideState = world.getBlockState(sidePos);
                    if (sideState.getBlock() instanceof CobaltWireBlock && sideState.getValue(CobaltWireBlock.POWER) > 0) {
                        if (isWirePointingTo(sideState, sideDir.getOpposite())) return true;
                    }
                }
            }

            // 4. QUASI-CONNECTIVITY: Legge dal mondo solo se non l'ha già fatto la sezione precedente
            if (neighborStateUp == null) {
                neighbourAbovePos.set(neighborPos).move(Direction.UP);
                neighborStateUp = world.getBlockState(neighbourAbovePos);
            }

            // 4. Quasi Connectivity Implementations
            if(dir == Direction.UP && neighborStateUp.getBlock() instanceof CobaltWireBlock){
                if (neighborStateUp.getValue(CobaltWireBlock.POWER) > 0) return true;
            }
            if(neighborStateUp.isRedstoneConductor(world, neighbourAbovePos)){
                BlockState stateAboveBlock = world.getBlockState(neighbourAbovePos.move(Direction.UP));
                if (stateAboveBlock.getBlock() instanceof CobaltWireBlock) {
                    if (stateAboveBlock.getValue(CobaltWireBlock.POWER) > 0) return true;
                }
            }
        }
        return false;
    }

    public static boolean isWirePointingTo(BlockState state, Direction dirToTarget) {
        return switch (dirToTarget) {
            case NORTH -> state.getValue(BlockStateProperties.NORTH_REDSTONE).isConnected();
            case SOUTH -> state.getValue(BlockStateProperties.SOUTH_REDSTONE).isConnected();
            case EAST -> state.getValue(BlockStateProperties.EAST_REDSTONE).isConnected();
            case WEST -> state.getValue(BlockStateProperties.WEST_REDSTONE).isConnected();
            default -> false;
        };
    }
}