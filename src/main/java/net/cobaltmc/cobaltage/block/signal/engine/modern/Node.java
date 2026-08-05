package net.cobaltmc.cobaltage.block.signal.engine.modern;

import net.cobaltmc.cobaltage.block.abstracts.WireBlock;
import net.cobaltmc.cobaltage.block.signal.engine.modern.WireHandler.Directions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class Node {
    private static final int CONDUCTOR = 1;
    private static final int SOURCE = 2;
    final ServerLevel level;
    final Node[] neighbors;
    BlockPos pos;
    BlockState state;
    boolean invalid;
    private int flags;
    Node prev_node;
    Node next_node;
    int priority;
    WireNode neighborWire;

    Node(ServerLevel level) {
        this.level = level;
        this.neighbors = new Node[Directions.ALL.length];
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Node)) {
            return false;
        } else {
            Node node = (Node)obj;
            return this.level == node.level && this.pos.equals(node.pos);
        }
    }

    public int hashCode() {
        return this.pos.hashCode();
    }

    Node set(BlockPos pos, BlockState state, boolean clearNeighbors) {
        if (state.getBlock() instanceof WireBlock) {
            throw new IllegalStateException("Cannot update a regular Node to a CobaltWireNode!");
        } else {
            if (clearNeighbors) {
                Arrays.fill(this.neighbors, (Object)null);
            }

            this.pos = pos.immutable();
            this.state = state;
            this.invalid = false;
            this.flags = 0;
            if (this.state.isRedstoneConductor(this.level, this.pos) || this.state.is(Blocks.REDSTONE_WIRE)) {
                this.flags |= 1;
            }

            if (this.state.isSignalSource()) {
                this.flags |= 2;
            }

            return this;
        }
    }

    int priority() {
        return this.neighborWire.priority;
    }

    public boolean isWire() {
        return false;
    }

    public boolean isConductor() {
        return (this.flags & 1) != 0;
    }

    public boolean isSignalSource() {
        return (this.flags & 2) != 0;
    }

    public WireNode asWire() {
        throw new UnsupportedOperationException("Not a WireNode!");
    }
}