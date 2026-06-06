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
    public int oldPower;

    // Bit masks
    private static final byte FLAG_EXTERNAL_CALCULATED = 2;   // Bit 1 (0010)
    private static final byte FLAG_DISCOVERED_DEPENDENT = 4;  // Bit 2 (0100)
    private static final byte FLAG_ADDED_TO_BUCKET = 8;       // Bit 3 (1000)

    public final List<CobaltWireNode> connectedWires = new ArrayList<>(4);

    public CobaltWireNode(BlockPos pos, BlockState state) {
        super(pos, state);
        this.currentPower = state.getValue(CobaltWireBlock.POWER);
        this.virtualPower = this.currentPower;
        this.externalPower = 0;
        this.oldPower = 0;
    }

    public boolean isExternalCalculated() {
        return (this.flags & FLAG_EXTERNAL_CALCULATED) != 0;
    }

    public void setExternalCalculated(boolean v) {
        if (v) {
            this.flags |= FLAG_EXTERNAL_CALCULATED;
        } else {
            this.flags &= ~FLAG_EXTERNAL_CALCULATED;
        }
    }

    public boolean isDiscoveredAsDependent() {
        return (this.flags & FLAG_DISCOVERED_DEPENDENT) != 0;
    }

    public void setDiscoveredAsDependent(boolean v) {
        if (v) {
            this.flags |= FLAG_DISCOVERED_DEPENDENT;
        } else {
            this.flags &= ~FLAG_DISCOVERED_DEPENDENT;
        }
    }

    public boolean isAddedToBucket() {
        return (this.flags & FLAG_ADDED_TO_BUCKET) != 0;
    }

    public void setAddedToBucket(boolean v) {
        if (v) {
            this.flags |= FLAG_ADDED_TO_BUCKET;
        } else {
            this.flags &= ~FLAG_ADDED_TO_BUCKET;
        }
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