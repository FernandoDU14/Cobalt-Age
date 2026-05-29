package net.fernando.cobaltage.block.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CobaltNode {
    public BlockPos pos;
    public BlockState state;

    // Maschera di bit per compattare i booleani (Occupa solo 1 byte anziché multipli)
    protected byte flags;

    public CobaltNode(BlockPos pos, BlockState state) {
        this.pos = pos;
        this.state = state;
        this.flags = 0;
    }

    public boolean isWire() {
        return false;
    }

    public CobaltWireNode asWire() {
        throw new UnsupportedOperationException("Not a WireNode!");
    }
}