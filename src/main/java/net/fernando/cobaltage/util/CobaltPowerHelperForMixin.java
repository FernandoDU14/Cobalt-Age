package net.fernando.cobaltage.util;

import net.fernando.cobaltage.block.CobaltWireBlock;
import net.fernando.cobaltage.block.wire.CobaltWireNetwork;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class CobaltPowerHelperForMixin {

    /**
     * Verifica se la posizione riceve energia Cobalt (Diretta o Forte attraverso i blocchi).
     */
    public static boolean isPoweredByCobalt(World world, BlockPos pos) {
        // 1. Controllo rapido: Strong Power diretta (Torce Cobalt, ecc.)
        if (CobaltWireNetwork.getStrongPowerFromNeighbors(world, pos) > 0) return true;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // 2. Alimentazione Diretta da Cavo Cobalt
            if (neighborState.getBlock() instanceof CobaltWireBlock) {
                if (neighborState.get(CobaltWireBlock.POWER) > 0) {
                    if (isWirePointingTo(neighborState, dir.getOpposite())) return true;
                }
            }

            // 3. Alimentazione Forte tramite blocco solido
            if (neighborState.isSolidBlock(world, neighborPos)) {
                // Cavo sopra il blocco solido
                BlockState stateAbove = world.getBlockState(neighborPos.up());
                if (stateAbove.getBlock() instanceof CobaltWireBlock && stateAbove.get(CobaltWireBlock.POWER) > 0) return true;

                // Cavo laterale che punta al blocco solido
                for (Direction sideDir : Direction.Type.HORIZONTAL) {
                    BlockPos sidePos = neighborPos.offset(sideDir);
                    BlockState sideState = world.getBlockState(sidePos);
                    if (sideState.getBlock() instanceof CobaltWireBlock && sideState.get(CobaltWireBlock.POWER) > 0) {
                        if (isWirePointingTo(sideState, sideDir.getOpposite())) return true;
                    }
                }

                // Altre fonti forti (es. Torcia Cobalt) che alimentano il blocco solido
                if (CobaltWireNetwork.getStrongPowerFromNeighbors(world, neighborPos) > 0) return true;
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