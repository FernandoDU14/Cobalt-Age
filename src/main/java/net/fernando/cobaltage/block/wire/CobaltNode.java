package net.fernando.cobaltage.block.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

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