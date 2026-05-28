package net.fernando.cobaltage.block.wire;

import net.fernando.cobaltage.block.CobaltWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

public class CobaltWireNode extends CobaltNode {

    public int currentPower;
    public int virtualPower;
    public int externalPower;

    // ⚡ efficiency: precise tracking to avoid infinite Flood-Fill
    public int oldPower;
    public boolean externalCalculated;
    public boolean discoveredAsDependent;
    public boolean addedToBucket;

    // Cache of connections to reset reads in the World
    public final List<CobaltWireNode> connectedWires = new ArrayList<>();

    public CobaltWireNode(BlockPos pos, BlockState state) {
        super(pos, state);
        this.currentPower = state.getValue(CobaltWireBlock.POWER);
        this.virtualPower = this.currentPower;
        this.externalPower = 0;
        this.oldPower = 0;
        this.externalCalculated = false;
        this.discoveredAsDependent = false;
        this.addedToBucket = false;
    }

    @Override
    public boolean isWire() {
        return true;
    }

    @Override
    public CobaltWireNode asWire() {
        return this;
    }
}