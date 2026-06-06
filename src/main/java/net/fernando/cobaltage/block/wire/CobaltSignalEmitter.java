package net.fernando.cobaltage.block.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Marker interface per la rete Cobalt.
 * Qualsiasi blocco che implementa questa interfaccia
 * può essere considerato una sorgente di energia Cobalt.
 * Non contiene logica: serve solo per separare la rete Cobalt
 * dalla redstone vanilla.
 */
public interface CobaltSignalEmitter {
    // The logic is the same of getSignal and getDirectSignal
    int getCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction);
    default int getDirectCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return 0;
    }
}