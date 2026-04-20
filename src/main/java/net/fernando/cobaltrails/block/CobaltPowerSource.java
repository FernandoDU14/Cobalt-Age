package net.fernando.cobaltrails.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Marker interface per la rete Cobalt.
 *
 * Qualsiasi blocco che implementa questa interfaccia
 * può essere considerato una sorgente di energia Cobalt.
 *
 * Non contiene logica: serve solo per separare la rete Cobalt
 * dalla redstone vanilla.
 */
public interface CobaltPowerSource {
    int getCobaltPower(BlockState state, World world, BlockPos pos);

    default CobaltSignalType getSignalType() {
        return CobaltSignalType.COBALT;
    }

    public enum CobaltSignalType {
        COBALT,
        REDSTONE,
        NONE
    }

}