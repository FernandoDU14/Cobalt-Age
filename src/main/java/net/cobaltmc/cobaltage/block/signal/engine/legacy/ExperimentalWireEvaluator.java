package net.cobaltmc.cobaltage.block.signal.engine.legacy;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.cobaltmc.cobaltage.block.abstracts.WireBlock;
import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public class ExperimentalWireEvaluator extends WireEvaluator {
    private final Deque<BlockPos> wiresToTurnOff = new ArrayDeque<>();
    private final Deque<BlockPos> wiresToTurnOn = new ArrayDeque<>();
    private final Object2IntMap<BlockPos> updatedWires = new Object2IntLinkedOpenHashMap<>();

    public ExperimentalWireEvaluator(WireBlock wireBlock) {
        super(wireBlock);
    }

    public void updatePowerStrengthByType(SignalType signalType, Level level, BlockPos blockPos, BlockState blockState, @Nullable Orientation orientation, boolean bl) {
        Orientation orientation2 = getInitialOrientation(level, orientation);
        this.calculateCurrentChangesByType(signalType, level, blockPos, orientation2);
        ObjectIterator<Object2IntMap.Entry<BlockPos>> objectIterator = this.updatedWires.object2IntEntrySet().iterator();

        for(boolean bl2 = true; objectIterator.hasNext(); bl2 = false) {
            Object2IntMap.Entry<BlockPos> entry = objectIterator.next();
            BlockPos blockPos2 = (BlockPos)entry.getKey();
            int i = entry.getIntValue();
            int j = unpackPower(i);
            BlockState blockState2 = level.getBlockState(blockPos2);
            if (blockState2.is(this.wireBlock) && !((Integer)blockState2.getValue(WireBlock.POWER)).equals(j)) {
                int k = 2;
                if (!bl || !bl2) {
                    k |= 128;
                }

                level.setBlock(blockPos2, (BlockState)blockState2.setValue(WireBlock.POWER, j), k);
            } else {
                objectIterator.remove();
            }
        }

        this.causeNeighborUpdates(level);
    }

    private void causeNeighborUpdates(Level level) {
        this.updatedWires.forEach((blockPos, i) -> {
            Orientation orientation = unpackOrientation(i);
            BlockState blockState = level.getBlockState(blockPos);

            for(Direction direction : orientation.getDirections()) {
                if (isConnected(blockState, direction)) {
                    BlockPos blockPos2 = blockPos.relative(direction);
                    BlockState blockState2 = level.getBlockState(blockPos2);
                    Orientation orientation2 = orientation.withFrontPreserveUp(direction);
                    level.neighborChanged(blockState2, blockPos2, this.wireBlock, orientation2, false);
                    if (blockState2.isSignalSource() || blockState2.getBlock() instanceof RepeaterBlock) {
                        for(Direction direction2 : orientation2.getDirections()) {
                            if (direction2 != direction.getOpposite()) {
                                level.neighborChanged(blockPos2.relative(direction2), this.wireBlock, orientation2.withFrontPreserveUp(direction2));
                            }
                        }
                    }
                }
            }

        });
        if (level instanceof ServerLevel serverLevel) {
            if (serverLevel.isDebug()) {
                this.updatedWires.forEach((blockPos, i) -> {});
            }
        }

    }

    private static boolean isConnected(BlockState blockState, Direction direction) {
        EnumProperty<RedstoneSide> property = WireBlock.PROPERTY_BY_DIRECTION.get(direction);
        if (property == null) {
            return direction == Direction.DOWN;
        } else {
            RedstoneSide side = blockState.getValue(property);
            return side.isConnected();
        }
    }

    private static Orientation getInitialOrientation(Level level, @Nullable Orientation orientation) {
        Orientation orientation2;
        if (orientation != null) {
            orientation2 = orientation;
        } else {
            orientation2 = Orientation.random(level.getRandom());
        }

        return orientation2.withUp(Direction.UP).withSideBias(Orientation.SideBias.LEFT);
    }

    private void calculateCurrentChangesByType(SignalType signalType, Level level, BlockPos blockPos, Orientation orientation) {
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.is(this.wireBlock)) {
            this.setPower(blockPos, (Integer)blockState.getValue(WireBlock.POWER), orientation);
            this.wiresToTurnOff.add(blockPos);
        } else {
            this.propagateChangeToNeighbors(level, blockPos, 0, orientation, true);
        }

        BlockPos blockPos2;
        Orientation orientation2;
        int j;
        int m;
        int n;
        for(; !this.wiresToTurnOff.isEmpty(); this.propagateChangeToNeighbors(level, blockPos2, n, orientation2, j > m)) {
            blockPos2 = (BlockPos)this.wiresToTurnOff.removeFirst();
            int i = this.updatedWires.getInt(blockPos2);
            orientation2 = unpackOrientation(i);
            j = unpackPower(i);
            int k = this.getBlockSignalByType(signalType, level, blockPos2);
            int l = this.getIncomingWireSignal(level, blockPos2);
            m = Math.max(k, l);
            if (m < j) {
                if (k > 0 && !this.wiresToTurnOn.contains(blockPos2)) {
                    this.wiresToTurnOn.add(blockPos2);
                }

                n = 0;
            } else {
                n = m;
            }

            if (n != j) {
                this.setPower(blockPos2, n, orientation2);
            }
        }

        int lx;
        Orientation orientation3;
        for(; !this.wiresToTurnOn.isEmpty(); this.propagateChangeToNeighbors(level, blockPos2, lx, orientation3, false)) {
            blockPos2 = (BlockPos)this.wiresToTurnOn.removeFirst();
            int ix = this.updatedWires.getInt(blockPos2);
            int o = unpackPower(ix);
            j = this.getBlockSignalByType(signalType, level, blockPos2);
            int kx = this.getIncomingWireSignal(level, blockPos2);
            lx = Math.max(j, kx);
            orientation3 = unpackOrientation(ix);
            if (lx > o) {
                this.setPower(blockPos2, lx, orientation3);
            } else if (lx < o) {
                throw new IllegalStateException("Turning off wire while trying to turn it on. Should not happen.");
            }
        }
    }

    private static int packOrientationAndPower(Orientation orientation, int i) {
        return orientation.getIndex() << 4 | i;
    }

    private static Orientation unpackOrientation(int i) {
        return Orientation.fromIndex(i >> 4);
    }

    private static int unpackPower(int i) {
        return i & 15;
    }

    private void setPower(BlockPos blockPos, int i, Orientation orientation) {
        this.updatedWires.compute(blockPos, (blockPosx, integer) -> integer == null ? packOrientationAndPower(orientation, i) : packOrientationAndPower(unpackOrientation(integer), i));
    }

    private void propagateChangeToNeighbors(Level level, BlockPos blockPos, int i, Orientation orientation, boolean bl) {
        for(Direction direction : orientation.getHorizontalDirections()) {
            BlockPos blockPos2 = blockPos.relative(direction);
            this.enqueueNeighborWire(level, blockPos2, i, orientation.withFront(direction), bl);
        }

        for(Direction direction : orientation.getVerticalDirections()) {
            BlockPos blockPos2 = blockPos.relative(direction);
            boolean bl2 = level.getBlockState(blockPos2).isSignalSource() || level.getBlockState(blockPos2).getBlock() instanceof DiodeBlock;

            for(Direction direction2 : orientation.getHorizontalDirections()) {
                BlockPos blockPos3 = blockPos.relative(direction2);
                if (direction == Direction.UP && !bl2) {
                    BlockPos blockPos4 = blockPos2.relative(direction2);
                    this.enqueueNeighborWire(level, blockPos4, i, orientation.withFront(direction2), bl);
                } else if (direction == Direction.DOWN && !level.getBlockState(blockPos3).isSignalSource() && !(level.getBlockState(blockPos3).getBlock() instanceof DiodeBlock)) {
                    BlockPos blockPos4 = blockPos2.relative(direction2);
                    this.enqueueNeighborWire(level, blockPos4, i, orientation.withFront(direction2), bl);
                }
            }
        }
    }

    private void enqueueNeighborWire(Level level, BlockPos blockPos, int i, Orientation orientation, boolean bl) {
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.is(this.wireBlock)) {
            int j = this.getWireSignal(blockPos, blockState);
            if (j < i - 1 && !this.wiresToTurnOn.contains(blockPos)) {
                this.wiresToTurnOn.add(blockPos);
                this.setPower(blockPos, j, orientation);
            }

            if (bl && j > i && !this.wiresToTurnOff.contains(blockPos)) {
                this.wiresToTurnOff.add(blockPos);
                this.setPower(blockPos, j, orientation);
            }
        }

    }

    protected int getWireSignal(BlockPos blockPos, BlockState blockState) {
        int i = this.updatedWires.getOrDefault(blockPos, -1);
        return i != -1 ? unpackPower(i) : super.getWireSignal(blockPos, blockState);
    }
}