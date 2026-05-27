package net.fernando.cobaltage.block.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class CobaltLevelHelper {

    /**
     * Versione ultra-ottimizzata di setBlockState.
     * Salta ricalcolo della luce, heightmaps e aggiornamenti dei vicini.
     */
    public static boolean setWireState(ServerLevel world, BlockPos pos, BlockState state) {
        int y = pos.getY();
        if (world.isOutsideBuildHeight(y)) return false;

        int x = pos.getX();
        int z = pos.getZ();
        int sectionIndex = world.getSectionIndex(y);

        // Prendi il chunk e la sezione
        ChunkAccess chunk = world.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
        if (chunk == null) return false;

        LevelChunkSection section = chunk.getSections()[sectionIndex];
        // if (section == null) return false;

        // Scrittura diretta nella PalettedContainer della sezione
        // Questo è il punto dove usiamo il codice che hai postato!
        BlockState prevState = section.setBlockState(x & 15, y & 15, z & 15, state);

        if (state == prevState) return false;

        // Notifica i client (altrimenti il cavo non cambia colore visivamente)
        world.getChunkSource().blockChanged(pos);

        // Segna il chunk come "da salvare" su disco
        chunk.markUnsaved();

        return true;
    }
}