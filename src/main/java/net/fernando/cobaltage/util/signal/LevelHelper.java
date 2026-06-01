package net.fernando.cobaltage.util.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class LevelHelper {
    // Cache locale per thread dell'ultimo chunk visitato
    private static final ThreadLocal<ChunkCache> LOCAL_CACHE = ThreadLocal.withInitial(ChunkCache::new);

    private static class ChunkCache {
        int lastX = Integer.MIN_VALUE;
        int lastZ = Integer.MIN_VALUE;
        ChunkAccess lastChunk = null;
    }

    public static boolean setWireState(ServerLevel world, BlockPos pos, BlockState state) {
        int y = pos.getY();
        if (world.isOutsideBuildHeight(y)) return false;

        int x = pos.getX();
        int z = pos.getZ();
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        ChunkCache cache = LOCAL_CACHE.get();
        ChunkAccess chunk;

        // Se le coordinate del chunk corrispondono all'ultimo visitato, riutilizzalo istantaneamente!
        if (cache.lastChunk != null && cache.lastX == chunkX && cache.lastZ == chunkZ) {
            chunk = cache.lastChunk;
        } else {
            chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            if (chunk == null) return false;
            // Aggiorna la cache
            cache.lastChunk = chunk;
            cache.lastX = chunkX;
            cache.lastZ = chunkZ;
        }

        int sectionIndex = world.getSectionIndex(y);
        LevelChunkSection section = chunk.getSections()[sectionIndex];

        BlockState prevState = section.setBlockState(x & 15, y & 15, z & 15, state);
        if (state == prevState) return false;

        world.getChunkSource().blockChanged(pos);
        chunk.markUnsaved();
        return true;
    }
}