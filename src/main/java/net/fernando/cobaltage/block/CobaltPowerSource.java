package net.fernando.cobaltage.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Marker interface per la rete Cobalt.
 * Qualsiasi blocco che implementa questa interfaccia
 * può essere considerato una sorgente di energia Cobalt.
 * Non contiene logica: serve solo per separare la rete Cobalt
 * dalla redstone vanilla.
 */
public interface CobaltPowerSource {
    int getCobaltPower(BlockState state, World world, BlockPos pos);

    // NUOVO: Calcola se il blocco sta iniettando "Energia Forte" in una specifica direzione
    default int getStrongCobaltPower(BlockState state, World world, BlockPos pos, Direction direction) {
        return 0; // Di default nessun blocco spara energia forte attraverso i blocchi
    }

    default CobaltSignalType getSignalType() {
        return CobaltSignalType.COBALT;
    }

    enum CobaltSignalType {
        COBALT,
        REDSTONE,
        NONE
    }

}