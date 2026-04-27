package net.fernando.cobaltage.block.wire;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;

public class CobaltLevelHelper {

    /**
     * Versione ultra-ottimizzata di setBlockState.
     * Salta ricalcolo della luce, heightmaps e aggiornamenti dei vicini.
     */
    public static boolean setWireState(ServerWorld world, BlockPos pos, BlockState state) {
        int y = pos.getY();
        if (world.isOutOfHeightLimit(y)) return false;

        int x = pos.getX();
        int z = pos.getZ();
        int sectionIndex = world.getSectionIndex(y);

        // Prendi il chunk e la sezione
        Chunk chunk = world.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
        if (chunk == null) return false;

        ChunkSection section = chunk.getSectionArray()[sectionIndex];
        if (section == null) return false;

        // Scrittura diretta nella PalettedContainer della sezione
        // Questo è il punto dove usiamo il codice che hai postato!
        BlockState prevState = section.setBlockState(x & 15, y & 15, z & 15, state);

        if (state == prevState) return false;

        // Notifica i client (altrimenti il cavo non cambia colore visivamente)
        world.getChunkManager().markForUpdate(pos);

        // Segna il chunk come "da salvare" su disco
        chunk.markNeedsSaving();

        return true;
    }
}