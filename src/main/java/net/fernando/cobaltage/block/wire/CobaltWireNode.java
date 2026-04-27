package net.fernando.cobaltage.block.wire;

import net.fernando.cobaltage.block.CobaltWireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class CobaltWireNode extends CobaltNode {

    public int currentPower;
    public int virtualPower;
    public int externalPower;

    // ⚡ FIX LAG: Tracciamento preciso per evitare Flood-Fill infiniti
    public int oldPower;
    public boolean externalCalculated;
    public boolean discoveredAsDependent;
    public boolean addedToBucket;

    // Cache delle connessioni per azzerare le letture nel World
    public final List<CobaltWireNode> connectedWires = new ArrayList<>();

    public CobaltWireNode(BlockPos pos, BlockState state) {
        super(pos, state);
        this.currentPower = state.get(CobaltWireBlock.POWER);
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