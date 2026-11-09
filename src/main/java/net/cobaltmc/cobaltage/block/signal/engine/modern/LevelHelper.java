package net.cobaltmc.cobaltage.block.signal.engine.modern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

class LevelHelper {

	/**
	 * An optimized version of {@link net.minecraft.world.level.Level#setBlock
	 * Level.setBlock}. Since this method is only used to update cobalt wire block
	 * states, lighting checks, height map updates, and block entity updates are
	 * omitted.
	 */
    static boolean setWireState(ServerLevel level, BlockPos pos, BlockState state, boolean updateNeighborShapes) {
        int y = pos.getY();
        if (y >= level.getMinY() && y <= level.getMaxY()) {
            int x = pos.getX();
            int z = pos.getZ();
            int index = level.getSectionIndex(y);

            ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
            LevelChunkSection section = chunk.getSections()[index];
            if (section == null) {
                return false;
            } else {
                BlockState prevState = section.setBlockState(x & 15, y & 15, z & 15, state);
                if (state == prevState) {
                    return false;}
                else {
                    level.getChunkSource().blockChanged(pos);
                    chunk.markUnsaved();
                    if (updateNeighborShapes) {
                        prevState.updateIndirectNeighbourShapes(level, pos, 2);
                        state.updateNeighbourShapes(level, pos, 2);
                        state.updateIndirectNeighbourShapes(level, pos, 2);
                    }

                    return true;
                }
            }
        } else {
            return false;
        }
    }
}