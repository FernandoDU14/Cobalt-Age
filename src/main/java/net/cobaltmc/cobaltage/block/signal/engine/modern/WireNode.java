package net.cobaltmc.cobaltage.block.signal.engine.modern;

import net.cobaltmc.cobaltage.block.abstracts.WireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class WireNode extends Node {
    final WireConnectionManager connections;
    int currentPower;
    int virtualPower;
    int externalPower;
    int flowIn;
    int iFlowDir;
    boolean added;
    boolean removed;
    boolean shouldBreak;
    boolean root;
    boolean discovered;
    boolean searched;
    WireNode next_wire;

    WireNode(ServerLevel level, BlockPos pos, BlockState state) {
        super(level);
        this.pos = pos.immutable();
        this.state = state;
        this.connections = new WireConnectionManager(this);
        this.virtualPower = this.currentPower = (Integer)this.state.getValue(WireBlock.POWER);
        this.priority = this.priority();
    }

    Node set(BlockPos pos, BlockState state, boolean clearNeighbors) {
        throw new UnsupportedOperationException("Cannot update a WireNode!");
    }

    int priority() {
        return Mth.clamp(this.virtualPower, 0, 15);
    }

    public boolean isWire() {
        return true;
    }

    public WireNode asWire() {
        return this;
    }

    boolean offerPower(int power, int iDir) {
        if (!this.removed && !this.shouldBreak) {
            if (power == this.virtualPower) {
                this.flowIn |= 1 << iDir;
                return false;
            } else if (power > this.virtualPower) {
                this.virtualPower = power;
                this.flowIn = 1 << iDir;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    boolean setPower() {
        if (this.removed) {
            return true;
        } else {
            this.state = this.level.getBlockState(this.pos);
            if (!(this.state.getBlock() instanceof WireBlock)) {
                return false;
            } else if (this.shouldBreak) {
                Block.dropResources(this.state, this.level, this.pos);
                this.level.setBlock(this.pos, Blocks.AIR.defaultBlockState(), 2);
                return true;
            } else {
                this.currentPower = Mth.clamp(this.virtualPower, 0, 15);
                this.state = (BlockState)this.state.setValue(WireBlock.POWER, this.currentPower);
                return LevelHelper.setWireState(this.level, this.pos, this.state, this.added);
            }
        }
    }
}
