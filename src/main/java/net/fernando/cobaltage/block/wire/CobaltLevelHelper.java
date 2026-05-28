package net.fernando.cobaltage.block.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class CobaltLevelHelper {

    public static boolean setWireState(ServerLevel world, BlockPos pos, BlockState state) {
        int y = pos.getY();
        if (world.isOutsideBuildHeight(y)) return false;

        int x = pos.getX();
        int z = pos.getZ();
        int sectionIndex = world.getSectionIndex(y);

        // Taking the chunk and the section
        ChunkAccess chunk = world.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
        if (chunk == null) return false;

        LevelChunkSection section = chunk.getSections()[sectionIndex];
        // if (section == null) return false; <- Old But Gold

        // Direct writing to the PalettedContainer of the section
        BlockState prevState = section.setBlockState(x & 15, y & 15, z & 15, state);

        if (state == prevState) return false;

        // Notify clients (otherwise the wire will not change color visually)
        world.getChunkSource().blockChanged(pos);

        // Mark the chunk as "unsaved" to disk
        chunk.markUnsaved();

        return true;
    }
}