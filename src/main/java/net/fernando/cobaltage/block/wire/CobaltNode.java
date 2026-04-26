package net.fernando.cobaltage.block.wire;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class CobaltNode {
    public BlockPos pos;
    public BlockState state;

    // Campi per la gestione interna
    public boolean invalid;

    public CobaltNode(BlockPos pos, BlockState state) {
        this.pos = pos;
        this.state = state;
        this.invalid = false;
    }

    public boolean isWire() {
        return false;
    }

    public CobaltWireNode asWire() {
        throw new UnsupportedOperationException("Not a WireNode!");
    }
}