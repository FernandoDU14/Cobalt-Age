package net.fernando.cobaltage.block.wire;
import net.fernando.cobaltage.block.*;

import net.fernando.cobaltage.util.signal.LevelHelper;
import net.fernando.cobaltage.util.signal.SignalHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

public class CobaltWireNetwork {

    private final Long2ObjectOpenHashMap<CobaltNode> nodes = new Long2ObjectOpenHashMap<>();
    // BUCKET QUEUE: Insert O(1) for every node to update, ordered by virtual power (0-15)
    private final Queue<CobaltWireNode>[] priorityBuckets = new Queue[16];
    private boolean isUpdating = false;

    // ⚡ Instance reusable fields (no more allocation for every tick)
    private final BlockPos.MutableBlockPos reusableMutable = new BlockPos.MutableBlockPos();
    private static final ThreadLocal<BlockPos.MutableBlockPos> STATIC_MUTABLE = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private final List<CobaltWireNode> changedWiresCache = new ArrayList<>();



    public CobaltWireNetwork() {
        for (int i = 0; i < 16; i++) {
            priorityBuckets[i] = new ArrayDeque<>();
        }
    }

    public void updateNetwork(Level world, BlockPos startPos) {
        if (world.isClientSide() || isUpdating) return;
        isUpdating = true;

        try {
            CobaltWireNode startNode = getOrAddWireNode(world, startPos, null);
            if (startNode == null) return;

            int oldPower = startNode.currentPower;
            int newExt = calculateExternalPower(world, startNode.pos);

            // ⚡ fast quit when no energy now and before
            if (oldPower == 0 && newExt == 0) return;

            startNode.externalPower = newExt;
            startNode.setExternalCalculated(true);

            Queue<CobaltWireNode> depletionQueue = new ArrayDeque<>();

            if (newExt > oldPower) {
                // Increment: Skip depletion, go directly to propagation
                startNode.virtualPower = newExt;
                addToBucket(startNode, newExt);
            } else if (newExt < oldPower) {
                // Decrement: (Observer if it turns off/broken cable): Cutting only the branch was depending on us
                startNode.oldPower = oldPower;
                startNode.virtualPower = newExt;
                startNode.setDiscoveredAsDependent(true);
                depletionQueue.add(startNode);
            } else {
                // Same energy (shape could be changed in the adjacent site). Putting in bucket to update neighbors.
                startNode.virtualPower = newExt;
                addToBucket(startNode, newExt);
            }

            // --- ⚡ PHASE 1: DEPLETION (selected energy drop) ---
            while (!depletionQueue.isEmpty()) {
                CobaltWireNode node = depletionQueue.poll();

                if (node.virtualPower > 0) {
                    addToBucket(node, node.virtualPower);
                }

                findAndCacheConnectedWires(world, node);

                // Clean cache variables for this specific node
                boolean isSolidBelow = false;
                boolean hasCheckedSolidBelow = false;

                for (CobaltWireNode neighbor : node.connectedWires) {
                    if (neighbor.isDiscoveredAsDependent()) continue;

                    // Can we send energy to it? (Did it depend on us?)
                    boolean weCanSendToNeighbor = true;
                    boolean differentXZ = node.pos.getX() != neighbor.pos.getX() || node.pos.getZ() != neighbor.pos.getZ();
                    if (neighbor.pos.getY() < node.pos.getY() && differentXZ) {
                        if (!hasCheckedSolidBelow) {
                            isSolidBelow = world.getBlockState(node.pos.below()).isRedstoneConductor(world, node.pos.below());
                            hasCheckedSolidBelow = true;
                        }
                        weCanSendToNeighbor = isSolidBelow;
                    }

                    // Can it send energy to us? (Did we depend on it? - Backfill support)
                    boolean neighborCanSendToUs = true;
                    if (neighbor.pos.getY() > node.pos.getY() && differentXZ){
                        neighborCanSendToUs = world.getBlockState(neighbor.pos.below()).isRedstoneConductor(world, neighbor.pos.below());
                    }

                    if (neighbor.currentPower > 0 && neighbor.currentPower <= node.oldPower - 1) {
                        // If the glass blocked us from passing energy to him, it couldn't depend on us
                        if (!weCanSendToNeighbor) continue;

                        neighbor.setDiscoveredAsDependent(true);
                        neighbor.oldPower = neighbor.currentPower;

                        if (!neighbor.isExternalCalculated()) {
                            neighbor.externalPower = calculateExternalPower(world, neighbor.pos);
                            neighbor.setExternalCalculated(true);
                        }
                        neighbor.virtualPower = neighbor.externalPower;
                        depletionQueue.add(neighbor);

                    } else if (neighbor.currentPower > node.oldPower - 1) {
                        // Backfill: The neighbor has high energy
                        // But it can help us only if its block allows it to send energy down.
                        if (!neighborCanSendToUs) continue;

                        neighbor.virtualPower = neighbor.currentPower;
                        addToBucket(neighbor, neighbor.virtualPower);
                    }
                }
            }

            // --- ⚡ PHASE 2: RAM PROPAGATION ---
            for (int p = 15; p > 0; p--) {
                Queue<CobaltWireNode> currentBucket = priorityBuckets[p];

                while (!currentBucket.isEmpty()) {
                    CobaltWireNode node = currentBucket.poll();
                    node.setAddedToBucket(false);

                    if (node.virtualPower != p) continue;

                    int powerToTransmit = node.virtualPower - 1;
                    if (powerToTransmit <= 0) continue;

                    if (node.connectedWires.isEmpty()) {
                        findAndCacheConnectedWires(world, node);
                    }

                    // Isolated cache for preventing leaks on diagonal blocks
                    boolean isSolidBelow = false;
                    boolean hasCheckedSolidBelow = false;

                    for (CobaltWireNode neighbor : node.connectedWires) {

                        // Glass Diode
                        boolean differentXZ = node.pos.getX() != neighbor.pos.getX() || node.pos.getZ() != neighbor.pos.getZ();
                        if (neighbor.pos.getY() < node.pos.getY() && differentXZ) {
                            if (!hasCheckedSolidBelow) {
                                isSolidBelow = world.getBlockState(node.pos.below()).isRedstoneConductor(world, node.pos.below());
                                hasCheckedSolidBelow = true;
                            }
                            if (!isSolidBelow) continue;
                        }

                        if (powerToTransmit > neighbor.virtualPower) {
                            neighbor.virtualPower = powerToTransmit;
                            addToBucket(neighbor, powerToTransmit);
                        }
                    }
                }
            }

            // --- ⚡ PHASE 3: WRITING AND UPDATE ---
            applyPowerChanges(world);

        } finally {
            // Free Cache
            nodes.clear();
            for (Queue<CobaltWireNode> q : priorityBuckets) q.clear();
            isUpdating = false;
        }
    }

    // Helper O(1) to manage unique insertion in buckets
    private void addToBucket(CobaltWireNode node, int power) {
        if (!node.isAddedToBucket()) {
            node.setAddedToBucket(true);
            priorityBuckets[power].add(node);
        }
    }

    // Helper to apply power changes
    private void applyPowerChanges(Level world) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        changedWiresCache.clear();

        for (CobaltNode node : nodes.values()) {
            if (node.isWire()) {
                CobaltWireNode wireNode = node.asWire();
                if (wireNode.virtualPower != wireNode.currentPower) {
                    BlockState newState = wireNode.state.setValue(CobaltWireBlock.POWER, wireNode.virtualPower);
                    if (LevelHelper.setWireState(serverLevel, wireNode.pos, newState)) {
                        wireNode.state = newState;
                        wireNode.currentPower = wireNode.virtualPower;
                        changedWiresCache.add(wireNode);
                    }
                }
            }
        }

        for (CobaltWireNode wire : changedWiresCache) {
            updateNeighbors(world, wire);
        }
    }

    // Helper to checkAndLink connected wires and cache them.
    private void findAndCacheConnectedWires(Level world, CobaltWireNode node) {
        if (!node.connectedWires.isEmpty()) return; // Se già riempito, salta! Costo ZERO.

        BlockPos pos = node.pos;

        // ---- Vertical Wire Connections for Relay Blocks ----
        reusableMutable.setWithOffset(pos, Direction.UP);
        checkAndLink(world, node, reusableMutable.immutable(), null);
        reusableMutable.setWithOffset(pos, Direction.DOWN);
        checkAndLink(world, node, reusableMutable.immutable(), null);

        // ---- Horizontal Wire Connections for both Relay Blocks and Cobalt Wire Blocks ----
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            reusableMutable.setWithOffset(pos, dir);
            BlockState neighborState = world.getBlockState(reusableMutable);

            // Horizontal link
            checkAndLink(world, node, reusableMutable.immutable(), neighborState);

            // 2. Block descent
            if (!neighborState.isRedstoneConductor(world, reusableMutable)) { // The Diagonal Block That Could Interrupt The Wire Web
                // 🛑 Important: We do not check 'canGoDown' here, as the wire web could still be
                // influenced by a (diagonal) above cobalt wire.
                // Memorizing the neighbor regardless to receive potential Backfill
                reusableMutable.setWithOffset(pos, dir).move(Direction.DOWN);
                checkAndLink(world, node, reusableMutable.immutable(), null);
            }

            // 3. Block ascent
            reusableMutable.setWithOffset(pos, Direction.UP);
            if (!world.getBlockState(reusableMutable).isRedstoneConductor(world, reusableMutable)) { // The Diagonal Block That Could Interrupt The Wire Web
                reusableMutable.setWithOffset(pos, dir).move(Direction.UP);
                checkAndLink(world, node, reusableMutable.immutable(), null);
            }
        }
    }

    // Helper to check and link between wires and relays, implementing here the logic for their mixed behavior
    private void checkAndLink(Level world, CobaltWireNode source, BlockPos targetPos, @Nullable BlockState knownState) {
        CobaltWireNode target = getOrAddWireNode(world, targetPos, knownState);
        if (target == null) return;

        boolean sourceIsRelay = source.state.is(ModBlocks.COBALT_RELAY);
        boolean targetIsRelay = target.state.is(ModBlocks.COBALT_RELAY);
        boolean sourceIsDust = source.state.is(ModBlocks.COBALT_DUST);
        boolean targetIsDust = target.state.is(ModBlocks.COBALT_DUST);

        boolean diffXZ = source.pos.getX() != target.pos.getX() || source.pos.getZ() != target.pos.getZ();
        boolean diffY = source.pos.getY() != target.pos.getY();

        // 1. Relay <-> Relay - Allowed only pure vertical link
        if (sourceIsRelay && targetIsRelay) {
            if (diffXZ) return;
        }

        // 2. Relay <-> Dust: Pure vertical relay pillars
        if ((sourceIsRelay && targetIsDust) || (sourceIsDust && targetIsRelay)) {
            // Diagonal not allowed
            if (diffY && diffXZ) return;

            // Mixed vertical is not allowed
            // You can't take cobalt power from a powered cobalt wire below if i'm a relay
            // However you can power a cobalt wire above relay block
            if (!diffXZ && diffY) {
                // If the Relay is above the dust, we block both direction links
                // So then the Relay is not powering downwards and cannot receive power from that direction as well
                if (sourceIsRelay && source.pos.getY() > target.pos.getY()) return;
                if (targetIsRelay && target.pos.getY() > source.pos.getY()) return;
            }
        }

        source.connectedWires.add(target);
    }


    // Helper to compute energy changes from external cobalt energy source or compatible energy cobalt source
    private int calculateExternalPower(Level world, BlockPos pos) {
        int maxPower = 0;

        for (Direction dir : Direction.values()) {
            reusableMutable.setWithOffset(pos, dir);
            BlockState neighborState = world.getBlockState(reusableMutable);

            // 1. Ignore other cobalt wires / cobalt relays, as their energies are managed in the Phase 2 (RAM Propagation)
            if (neighborState.getBlock() instanceof CobaltWireBlock) {
                continue;
            }

            // 2. Direct Cobalt Power Source detected (Repeater, Comparator, Torch, Converter, ecc.)
            if (neighborState.getBlock() instanceof CobaltSignalSource source) {
                maxPower = Math.max(maxPower, source.getCobaltSignal(neighborState, world, reusableMutable, dir));
            }
            // 3. When we meet a solid block we check if it is Strong Powered by a Cobalt Source
            else if (neighborState.isRedstoneConductor(world, reusableMutable) || neighborState.getBlock() instanceof PoweredBlock) {
                maxPower = Math.max(maxPower, getStrongPowerFromNeighbors(world, reusableMutable));
            }

            // 4. Compatible Cobalt Power Source (Lectern, Lever, Button...)
            if (SignalHelper.compatibleCobaltPowerSource(neighborState)) {
                maxPower = Math.max(maxPower, neighborState.getSignal(world, reusableMutable, dir));
            }

            // 5. Restricted Cobalt Power Source (Observer, Calibrated Sculk Sensor...)
            if (SignalHelper.restrictedCobaltPowerSource(neighborState)) {
                maxPower = Math.max(maxPower, neighborState.getSignal(world, reusableMutable, dir));
            }
        }
        return maxPower;
    }

    // Helper to evaluate the Strong Cobalt Power or a Compatible Strong Redstone Power of a Block.
    public static int getStrongPowerFromNeighbors(Level world, BlockPos blockPos) {
        int maxStrong = 0;
        BlockPos.MutableBlockPos mutable = STATIC_MUTABLE.get();

        for (Direction dir : Direction.values()) {

            mutable.setWithOffset(blockPos, dir);
            BlockState sourceState = world.getBlockState(mutable);

            if (sourceState.getBlock() instanceof CobaltSignalSource source) {
                // We ask for Strong Cobalt Power
                maxStrong = Math.max(maxStrong, source.getDirectCobaltSignal(sourceState, world, mutable, dir));
            }

            if (SignalHelper.compatibleCobaltPowerSource(sourceState)) {
                // We ask for Strong Redstone Power (getDirectSignal substitutes getStrongRedstonePower)
                maxStrong = Math.max(maxStrong, sourceState.getDirectSignal(world, mutable, dir));
            }

            if (SignalHelper.restrictedCobaltPowerSource(sourceState)) {
                // We ask for Strong Redstone Power (getDirectSignal substitutes getStrongRedstonePower)
                maxStrong = Math.max(maxStrong, sourceState.getDirectSignal(world, mutable, dir));
            }

        }
        return maxStrong;
    }


    private CobaltWireNode getOrAddWireNode(Level world, BlockPos pos, @Nullable BlockState knownState) {
        long posLong = pos.asLong(); // Obtains the long primitive without boxing
        CobaltNode node = nodes.get(posLong);

        if (node == null) {
            // Skipping known computation
            BlockState state = knownState != null ? knownState : world.getBlockState(pos);

            if (state.getBlock() instanceof CobaltWireBlock) {
                node = new CobaltWireNode(pos, state);
            } else {
                node = new CobaltNode(pos, state);
            }
            nodes.put(posLong, node); // Insertion without boxing
        }

        return node.isWire() ? node.asWire() : null;
    }

    // Helper to evaluate which neighbors should update their state by a Cobalt Wire Node update
    private void updateNeighbors(Level world, CobaltWireNode node) {

        Block block = world.getBlockState(node.pos).getBlock();

        // 1. Six Neighbours
        boolean isSolidD = false, isSolidU = false, isSolidN = false, isSolidS = false, isSolidW = false, isSolidE = false;

        // North
        reusableMutable.setWithOffset(node.pos, Direction.NORTH);
        BlockState stateN = world.getBlockState(reusableMutable);
        if (!(stateN.getBlock() instanceof CobaltWireBlock)) { // Skipping Cobalt Wire Block
            if (stateN.getBlock() instanceof ObserverBlock) {
                stateN.updateShape(
                        world,                       // LevelReader
                        world,                       // ScheduledTickAccess
                        reusableMutable,             // BlockPos (Posizione dell'Observer)
                        Direction.SOUTH,             // Direction (Da dove arriva l'aggiornamento) NORHT -> SOUTH
                        node.pos,                    // BlockPos2 (Posizione del cavo)
                        node.state,                  // BlockState (Stato del cavo)
                        world.getRandom()            // RandomSource
                );
            } else if (stateN.getBlock() instanceof CobaltSignalSource || SignalHelper.isWirePointingTo(node.state, Direction.NORTH)) {
                world.neighborChanged(reusableMutable.immutable(), block, null);
                isSolidN = stateN.isRedstoneConductor(world, reusableMutable) || stateN.getBlock() instanceof PoweredBlock;
            }
        }

        // South
        reusableMutable.setWithOffset(node.pos, Direction.SOUTH);
        BlockState stateS = world.getBlockState(reusableMutable);
        if (!(stateS.getBlock() instanceof CobaltWireBlock)) { // Skipping Cobalt Wire Block
            if (stateS.getBlock() instanceof ObserverBlock) {
                stateS.updateShape(world, world, reusableMutable, Direction.NORTH, node.pos, node.state, world.getRandom());
            } else if (stateS.getBlock() instanceof CobaltSignalSource || SignalHelper.isWirePointingTo(node.state, Direction.SOUTH)) {
                world.neighborChanged(reusableMutable.immutable(), block, null);
                isSolidS = stateS.isRedstoneConductor(world, reusableMutable) || stateS.getBlock() instanceof PoweredBlock;
            }
        }

        // East
        reusableMutable.setWithOffset(node.pos, Direction.EAST);
        BlockState stateE = world.getBlockState(reusableMutable);
        if (!(stateE.getBlock() instanceof CobaltWireBlock)) { // Skipping Cobalt Wire Block
            if (stateE.getBlock() instanceof ObserverBlock) {
                stateE.updateShape(world, world, reusableMutable, Direction.WEST, node.pos, node.state, world.getRandom());
            } else if (stateE.getBlock() instanceof CobaltSignalSource || SignalHelper.isWirePointingTo(node.state, Direction.EAST)) {
                world.neighborChanged(reusableMutable.immutable(), block, null);
                isSolidE = stateE.isRedstoneConductor(world, reusableMutable) || stateE.getBlock() instanceof PoweredBlock;
            }
        }

        // West
        reusableMutable.setWithOffset(node.pos, Direction.WEST);
        BlockState stateW = world.getBlockState(reusableMutable);
        if (!(stateW.getBlock() instanceof CobaltWireBlock)) { // Skipping Cobalt Wire Block
            if (stateW.getBlock() instanceof ObserverBlock) {
                stateW.updateShape(world, world, reusableMutable, Direction.EAST, node.pos, node.state, world.getRandom());
            } else if (stateW.getBlock() instanceof CobaltSignalSource || SignalHelper.isWirePointingTo(node.state, Direction.WEST)) {
                world.neighborChanged(reusableMutable.immutable(), block, null);
                isSolidW = stateW.isRedstoneConductor(world, reusableMutable) || stateW.getBlock() instanceof PoweredBlock;
            }
        }

        // Up
        reusableMutable.setWithOffset(node.pos, Direction.UP);
        BlockState stateU = world.getBlockState(reusableMutable);
        if (!(stateU.getBlock() instanceof CobaltWireBlock)) { // Skipping Cobalt Wire Block
            if (stateU.getBlock() instanceof ObserverBlock) {
                stateU.updateShape(world, world, reusableMutable, Direction.DOWN, node.pos, node.state, world.getRandom());
            } else if (stateU.getBlock() instanceof CobaltSignalSource || block instanceof CobaltRelayBlock) {
                world.neighborChanged(reusableMutable.immutable(), block, null);
                isSolidU = stateU.isRedstoneConductor(world, reusableMutable) || stateU.getBlock() instanceof PoweredBlock;
            }
        }

        // Down
        reusableMutable.setWithOffset(node.pos, Direction.DOWN);
        BlockState stateD = world.getBlockState(reusableMutable);
        if (!(stateD.getBlock() instanceof CobaltWireBlock)) { // Skipping Cobalt Wire Block
            if (stateD.getBlock() instanceof ObserverBlock) {
                stateD.updateShape(world, world, reusableMutable, Direction.UP, node.pos, node.state, world.getRandom());
            }else {
                world.neighborChanged(reusableMutable.immutable(), block, null);
                isSolidD = stateD.isRedstoneConductor(world, reusableMutable) || stateD.getBlock() instanceof PoweredBlock;
            }
        }
        // Far Updates
        if (isSolidN) world.neighborChanged(node.pos.offset(0, 0, -2), block, null);
        if (isSolidS) world.neighborChanged(node.pos.offset(0, 0, 2), block, null);
        if (isSolidE) world.neighborChanged(node.pos.offset(2, 0, 0), block, null);
        if (isSolidW) world.neighborChanged(node.pos.offset(-2, 0, 0), block, null);
        if (isSolidU) world.neighborChanged(node.pos.offset(0, 2, 0), block, null);
        if (isSolidD) world.neighborChanged(node.pos.offset(0, -2, 0), block, null);

        // Diagonal Updates
        // Diagonali Orizzontali
        if (isSolidN || isSolidE) world.neighborChanged(node.pos.offset(1, 0, -1), block, null);
        if (isSolidN || isSolidW) world.neighborChanged(node.pos.offset(-1, 0, -1), block, null);
        if (isSolidS || isSolidE) world.neighborChanged(node.pos.offset(1, 0, 1), block, null);
        if (isSolidS || isSolidW) world.neighborChanged(node.pos.offset(-1, 0, 1), block, null);

        // Diagonali Verticali
        if (isSolidU || isSolidN) world.neighborChanged(node.pos.offset(0, 1, -1), block, null);
        if (isSolidU || isSolidS) world.neighborChanged(node.pos.offset(0, 1, 1), block, null);
        if (isSolidU || isSolidE) world.neighborChanged(node.pos.offset(1, 1, 0), block, null);
        if (isSolidU || isSolidW) world.neighborChanged(node.pos.offset(-1, 1, 0), block, null);
        if (isSolidD || isSolidN) world.neighborChanged(node.pos.offset(0, -1, -1), block, null);
        if (isSolidD || isSolidS) world.neighborChanged(node.pos.offset(0, -1, 1), block, null);
        if (isSolidD || isSolidE) world.neighborChanged(node.pos.offset(1, -1, 0), block, null);
        if (isSolidD || isSolidW) world.neighborChanged(node.pos.offset(-1, -1, 0), block, null);
    }
}