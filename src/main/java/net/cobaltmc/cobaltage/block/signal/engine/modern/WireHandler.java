package net.cobaltmc.cobaltage.block.signal.engine.modern;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Iterator;
import java.util.Queue;
import net.cobaltmc.cobaltage.block.cobalt.CobaltRelayBlock;
import net.cobaltmc.cobaltage.block.cobalt.CobaltWireBlock;
import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.block.signal.engine.legacy.InstantNeighborUpdater;
import net.cobaltmc.cobaltage.block.signal.engine.legacy.NeighborUpdater;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.BlockStateBaseSignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.LevelStorageSource;

public class WireHandler {
    static final int[] FLOW_IN_TO_FLOW_OUT = new int[64];
    static final int[] SHAPE_UPDATE_ORDER;
    private static final int POWER_MIN = 0;
    private static final int POWER_MAX = 15;
    private static final int POWER_STEP = 1;
    private final ServerLevel level;
    private final Config config;
    private final Long2ObjectMap<Node> nodes;
    private final Queue<WireNode> search;
    private final Queue<Node> updates;
    private final NeighborUpdater neighborUpdater;
    private Node[] nodeCache;
    private int nodeCount;
    private boolean updating;

    private static int resolve3DFlow(int mask) {
        if (mask == 0) {
            return -1;
        } else {
            int bits = Integer.bitCount(mask);
            if (bits == 1) {
                return Integer.numberOfTrailingZeros(mask);
            } else {
                int canceled = mask;
                if ((mask & 5) == 5) {
                    canceled = mask & -6;
                }

                if ((canceled & 10) == 10) {
                    canceled &= -11;
                }

                if ((canceled & 48) == 48) {
                    canceled &= -49;
                }

                if (Integer.bitCount(canceled) == 1) {
                    return Integer.numberOfTrailingZeros(canceled);
                } else {
                    if (bits == 2 && (mask & 48) == 0) {
                        if (mask == 3) {
                            return 1;
                        }

                        if (mask == 6) {
                            return 2;
                        }

                        if (mask == 9) {
                            return 0;
                        }

                        if (mask == 12) {
                            return 3;
                        }
                    }

                    return -1;
                }
            }
        }
    }

    public WireHandler(ServerLevel level, LevelStorageSource.LevelStorageAccess storage) {
        this.level = level;
        this.config = Config.forLevel(level, storage);
        this.config.load();
        this.nodes = new Long2ObjectOpenHashMap();
        this.search = new SimpleQueue();
        this.updates = new PriorityQueue();
        this.neighborUpdater = new InstantNeighborUpdater(this.level);
        this.nodeCache = new Node[16];
        this.fillNodeCache(0, 16);
    }

    public Config getConfig() {
        return this.config;
    }

    private Node getOrAddNode(BlockPos pos) {
        return this.getOrAddNode(pos, (BlockState)null);
    }

    private Node getOrAddNode(BlockPos pos, BlockState state) {
        return (Node)this.nodes.compute(pos.asLong(), (key, node) -> {
            if (node == null) {
                return this.getNextNode(pos, state != null ? state : this.level.getBlockState(pos));
            } else {
                return node.invalid ? this.revalidateNode(node) : node;
            }
        });
    }

    private Node removeNode(BlockPos pos) {
        return (Node)this.nodes.remove(pos.asLong());
    }

    private Node getNextNode(BlockPos pos, BlockState state) {
        return (Node)(state.getBlock() instanceof CobaltWireBlock ? new WireNode(this.level, pos, state) : this.getNextNode().set(pos, state, true));
    }

    private Node getNextNode() {
        if (this.nodeCount == this.nodeCache.length) {
            this.increaseNodeCache();
        }

        return this.nodeCache[this.nodeCount++];
    }

    private void increaseNodeCache() {
        Node[] oldCache = this.nodeCache;
        this.nodeCache = new Node[oldCache.length << 1];

        for(int index = 0; index < oldCache.length; ++index) {
            this.nodeCache[index] = oldCache[index];
        }

        this.fillNodeCache(oldCache.length, this.nodeCache.length);
    }

    private void fillNodeCache(int start, int end) {
        for(int index = start; index < end; ++index) {
            this.nodeCache[index] = new Node(this.level);
        }

    }

    private Node revalidateNode(Node node) {
        if (!node.invalid) {
            return node;
        } else {
            BlockPos pos = node.pos;
            BlockState state = this.level.getBlockState(pos);
            boolean wasWire = node.isWire();
            boolean isWire = state.getBlock() instanceof CobaltWireBlock;
            if (wasWire != isWire) {
                return this.getNextNode(pos, state);
            } else {
                node.invalid = false;
                if (isWire) {
                    WireNode wire = node.asWire();
                    wire.root = false;
                    wire.discovered = false;
                    wire.searched = false;
                } else {
                    node.set(pos, state, false);
                }

                return node;
            }
        }
    }

    private Node getNeighbor(Node node, int iDir) {
        Node neighbor = node.neighbors[iDir];
        if (neighbor == null || neighbor.invalid) {
            Direction dir = WireHandler.Directions.ALL[iDir];
            BlockPos pos = node.pos.relative(dir);
            Node oldNeighbor = neighbor;
            neighbor = this.getOrAddNode(pos);
            if (neighbor != oldNeighbor) {
                int iOpp = WireHandler.Directions.iOpposite(iDir);
                node.neighbors[iDir] = neighbor;
                neighbor.neighbors[iOpp] = node;
            }
        }

        return neighbor;
    }

    public boolean onWireUpdated(BlockPos pos, BlockState state, Orientation orientation) {
        Node node = this.getOrAddNode(pos, state);
        if (!node.isWire()) {
            return false;
        } else {
            WireNode wire = node.asWire();
            this.invalidate();
            this.revalidateNode(wire);
            this.findRoots(wire, orientation);
            this.tryUpdate();
            return true;
        }
    }

    public void onWireAdded(BlockPos pos, BlockState state) {
        Node node = this.getOrAddNode(pos, state);
        if (node.isWire()) {
            WireNode wire = node.asWire();
            wire.added = true;
            this.invalidate();
            this.revalidateNode(wire);
            this.findRoot(wire);
            this.tryUpdate();
        }
    }

    public void onWireRemoved(BlockPos pos, BlockState state) {
        Node node = this.removeNode(pos);
        WireNode wire;
        if (node != null && node.isWire()) {
            wire = node.asWire();
        } else {
            wire = new WireNode(this.level, pos, state);
        }

        wire.invalid = true;
        wire.removed = true;
        if (!this.updating || !wire.shouldBreak) {
            this.invalidate();
            this.revalidateNode(wire);
            this.findRoot(wire);
            this.tryUpdate();
        }
    }

    private void invalidate() {
        Node node;
        if (this.updating && !this.nodes.isEmpty()) {
            for(Iterator<Long2ObjectMap.Entry<Node>> it = Long2ObjectMaps.fastIterator(this.nodes); it.hasNext(); node.invalid = true) {
                Long2ObjectMap.Entry<Node> entry = (Long2ObjectMap.Entry)it.next();
                node = (Node)entry.getValue();
            }
        }

    }

    private void findRoots(WireNode wire, Orientation orientation) {
        int iDirBias = -1;
        if (orientation != null) {
            Direction dir = orientation.getFront().getAxis().isHorizontal() ? orientation.getFront() : orientation.getUp();
            iDirBias = WireHandler.Directions.index(dir);
        }

        this.findRoot(wire, iDirBias);
        if (wire.searched) {
            if (orientation == null) {
                for(int iDir : this.config.getUpdateOrder().directNeighbors(wire.iFlowDir)) {
                    this.findRootsAround(wire, iDir);
                }
            } else {
                this.findRootsAround(wire, WireHandler.Directions.index(orientation.getFront().getOpposite()));
            }

        }
    }

    private void findRootsAround(WireNode wire, int iDir) {
        Node node = this.getNeighbor(wire, iDir);
        if (node.isConductor() || node.isSignalSource()) {
            for(int iSide : this.config.getUpdateOrder().cardinalNeighbors(wire.iFlowDir)) {
                Node neighbor = this.getNeighbor(node, iSide);
                if (neighbor.isWire()) {
                    this.findRoot(neighbor.asWire(), iSide);
                }
            }
        }

    }

    private void findRoot(WireNode wire) {
        this.findRoot(wire, -1);
    }

    private void findRoot(WireNode wire, int iDiscoveryDir) {
        if (!wire.discovered) {
            this.discover(wire);
            this.findExternalPower(wire);
            this.findPower(wire, false);
            if (this.needsUpdate(wire)) {
                this.searchRoot(wire, iDiscoveryDir);
            }

        }
    }

    private void discover(WireNode wire) {
        if (!wire.discovered) {
            wire.discovered = true;
            wire.searched = false;
            if (!wire.removed && !wire.shouldBreak && !wire.state.canSurvive(this.level, wire.pos)) {
                wire.shouldBreak = true;
            }

            wire.virtualPower = wire.currentPower;
            wire.externalPower = -1;
            wire.connections.set(this::getNeighbor, this);
        }
    }

    private void findPower(WireNode wire, boolean ignoreSearched) {
        wire.virtualPower = wire.externalPower;
        wire.flowIn = 0;
        if (!wire.removed && !wire.shouldBreak) {
            if (wire.externalPower < 14) {
                this.findWirePower(wire, ignoreSearched);
            }

        }
    }

    private void findWirePower(WireNode wire, boolean ignoreSearched) {
        wire.connections.forEach((connection) -> {
            if (connection.accept) {
                WireNode neighbor = connection.wire;
                if (!ignoreSearched || !neighbor.searched) {
                    int power = Math.max(0, neighbor.virtualPower - 1);
                    int iOpp = WireHandler.Directions.iOpposite(connection.iDir);
                    wire.offerPower(power, iOpp);
                }

            }
        });
    }

    private void findExternalPower(WireNode wire) {
        if (!wire.removed && !wire.shouldBreak && wire.externalPower < 0) {
            wire.externalPower = this.getExternalPower(wire);
            if (wire.externalPower > wire.virtualPower) {
                wire.virtualPower = wire.externalPower;
            }

        }
    }

    private int getExternalPower(WireNode wire) {
        int power = 0;

        for(int iDir = 0; iDir < WireHandler.Directions.ALL.length; ++iDir) {
            Node neighbor = this.getNeighbor(wire, iDir);
            if (!neighbor.isWire()) {
                if (neighbor.isConductor()) {
                    power = Math.max(power, this.getDirectSignalToByType(SignalType.COBALT, wire, neighbor));
                }

                if (neighbor.isSignalSource()) {
                    power = Math.max(power, ((BlockStateBaseSignalGetterByType)neighbor.state).cobaltage$getSignalByType(SignalType.COBALT, this.level, neighbor.pos, WireHandler.Directions.ALL[iDir]));
                }

                if (power >= 15) {
                    return 15;
                }
            }
        }

        return power;
    }

    private int getDirectSignalToByType(SignalType signalType, WireNode wire, Node node) {
        int power = 0;

        for(int iDir = 0; iDir < WireHandler.Directions.ALL.length; ++iDir) {
            Node neighbor = this.getNeighbor(node, iDir);
            if (neighbor.isSignalSource()) {
                power = Math.max(power, ((BlockStateBaseSignalGetterByType)neighbor.state).cobaltage$getDirectSignalByType(signalType, this.level, neighbor.pos, WireHandler.Directions.ALL[iDir]));
                if (power >= 15) {
                    return 15;
                }
            }
        }

        return power;
    }

    private boolean needsUpdate(WireNode wire) {
        return wire.removed || wire.shouldBreak || wire.virtualPower != wire.currentPower;
    }

    private void searchRoot(WireNode wire, int iBackupFlowDir) {
        if (wire.connections.iFlowDir >= 0) {
            iBackupFlowDir = wire.connections.iFlowDir;
        } else if (iBackupFlowDir < 0) {
            iBackupFlowDir = 0;
        }

        this.search(wire, true, iBackupFlowDir);
    }

    private void search(WireNode wire, boolean root, int iBackupFlowDir) {
        this.search.offer(wire);
        wire.root = root;
        wire.searched = true;
        wire.iFlowDir = iBackupFlowDir;
    }

    private void tryUpdate() {
        if (!this.search.isEmpty()) {
            this.update();
        }

        if (!this.updating) {
            this.nodes.clear();
            this.nodeCount = 0;
        }

    }

    private void update() {
        this.searchNetwork();
        this.depowerNetwork();

        try {
            this.powerNetwork();
        } catch (Throwable t) {
            this.updating = false;
            throw t;
        }
    }

    private void searchNetwork() {
        for(WireNode wire : this.search) {
            wire.connections.forEach((connection) -> {
                if (connection.offer) {
                    WireNode neighbor = connection.wire;
                    if (!neighbor.searched) {
                        this.discover(neighbor);
                        this.findPower(neighbor, false);
                        if (neighbor.virtualPower < neighbor.currentPower) {
                            this.findExternalPower(neighbor);
                        }

                        if (this.needsUpdate(neighbor)) {
                            this.search(neighbor, false, connection.iDir);
                        }

                    }
                }
            }, this.config.getUpdateOrder(), wire.iFlowDir);
        }

    }

    private void depowerNetwork() {
        while(!this.search.isEmpty()) {
            WireNode wire = (WireNode)this.search.poll();
            this.findPower(wire, true);
            if (!wire.root && !wire.removed && !wire.shouldBreak && wire.virtualPower <= 0) {
                --wire.virtualPower;
            } else {
                this.queueWire(wire);
            }
        }

    }

    private void powerNetwork() {
        if (!this.updating) {
            this.updating = true;

            while(!this.updates.isEmpty()) {
                Node node = (Node)this.updates.poll();
                if (node.isWire()) {
                    WireNode wire = node.asWire();
                    if (this.needsUpdate(wire)) {
                        this.findPowerFlow(wire);
                        this.transmitPower(wire);
                        if (wire.setPower()) {
                            this.queueNeighbors(wire);
                            this.updateNeighborShapes(wire);
                        }
                    }
                } else {
                    WireNode neighborWire = node.neighborWire;
                    if (neighborWire != null) {
                        BlockPos neighborPos = neighborWire.pos;
                        Block neighborBlock = neighborWire.state.getBlock();
                        this.updateBlock(node, neighborPos, neighborBlock);
                    }
                }
            }

            this.updating = false;
        }
    }

    private void findPowerFlow(WireNode wire) {
        int flow = FLOW_IN_TO_FLOW_OUT[wire.flowIn];
        if (flow >= 0) {
            wire.iFlowDir = flow;
        } else if (wire.connections.iFlowDir >= 0) {
            wire.iFlowDir = wire.connections.iFlowDir;
        }

    }

    private void transmitPower(WireNode wire) {
        wire.connections.forEach((connection) -> {
            if (connection.offer) {
                WireNode neighbor = connection.wire;
                int power = Math.max(0, wire.virtualPower - 1);
                int iDir = connection.iDir;
                if (neighbor.offerPower(power, iDir)) {
                    this.queueWire(neighbor);
                }

            }
        }, this.config.getUpdateOrder(), wire.iFlowDir);
    }

    private void updateNeighborShapes(WireNode wire) {
        BlockPos wirePos = wire.pos;
        BlockState wireState = wire.state;

        for(int iDir : SHAPE_UPDATE_ORDER) {
            Node neighbor = this.getNeighbor(wire, iDir);
            if (!neighbor.isWire() && !neighbor.state.isAir()) {
                int iOpp = WireHandler.Directions.iOpposite(iDir);
                Direction opp = WireHandler.Directions.ALL[iOpp];
                this.updateShape(neighbor, opp, wirePos, wireState);
            }
        }

    }

    private void updateShape(Node node, Direction dir, BlockPos neighborPos, BlockState neighborState) {
        this.neighborUpdater.shapeUpdate(dir, neighborState, node.pos, neighborPos, 2, 512);
    }

    private void queueNeighbors(WireNode wire) {
        this.config.getUpdateOrder().forEachNeighbor(this::getNeighbor, wire, wire.iFlowDir, (neighbor) -> this.queueNeighbor(neighbor, wire));
    }

    private void queueNeighbor(Node node, WireNode neighborWire) {
        if (!node.isWire() && !node.state.isAir()) {
            node.neighborWire = neighborWire;
            this.updates.offer(node);
        }

    }

    private void queueWire(WireNode wire) {
        if (this.needsUpdate(wire)) {
            this.updates.offer(wire);
        } else {
            this.findPowerFlow(wire);
            this.transmitPower(wire);
        }

    }

    private void updateBlock(Node node, BlockPos neighborPos, Block neighborBlock) {
        this.neighborUpdater.neighborChanged(node.pos, neighborBlock, (Orientation)null);
    }

    public boolean shouldCobaltWireNodesLink(WireNode nodeA, WireNode nodeB) {
        BlockState stateA = nodeA.state;
        BlockState stateB = nodeB.state;
        boolean aIsRelay = stateA.getBlock() instanceof CobaltRelayBlock;
        boolean bIsRelay = stateB.getBlock() instanceof CobaltRelayBlock;
        boolean aIsDust = !aIsRelay;
        boolean bIsDust = !bIsRelay;
        boolean diffXZ = nodeA.pos.getX() != nodeB.pos.getX() || nodeA.pos.getZ() != nodeB.pos.getZ();
        boolean diffY = nodeA.pos.getY() != nodeB.pos.getY();
        if (aIsRelay && bIsRelay && diffXZ) {
            return false;
        } else {
            if (aIsRelay && bIsDust || aIsDust && bIsRelay) {
                if (diffY && diffXZ) {
                    return false;
                }

                if (!diffXZ && diffY) {
                    if (aIsRelay && nodeA.pos.getY() > nodeB.pos.getY()) {
                        return false;
                    }

                    if (bIsRelay && nodeB.pos.getY() > nodeA.pos.getY()) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    static {
        for(int i = 0; i < 64; ++i) {
            FLOW_IN_TO_FLOW_OUT[i] = resolve3DFlow(i);
        }

        SHAPE_UPDATE_ORDER = new int[]{0, 2, 1, 3, 4, 5};
    }

    public static class Directions {
        public static final Direction[] ALL;
        public static final int WEST = 0;
        public static final int NORTH = 1;
        public static final int EAST = 2;
        public static final int SOUTH = 3;
        public static final int DOWN = 4;
        public static final int UP = 5;

        public static int iOpposite(int iDir) {
            return iDir ^ 2 >>> (iDir >>> 2);
        }

        public static int index(Direction dir) {
            for(int i = 0; i < ALL.length; ++i) {
                if (dir == ALL[i]) {
                    return i;
                }
            }

            return -1;
        }

        static {
            ALL = new Direction[]{Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.DOWN, Direction.UP};
        }
    }

    @FunctionalInterface
    public interface NodeProvider {
        Node getNeighbor(Node var1, int var2);
    }
}
