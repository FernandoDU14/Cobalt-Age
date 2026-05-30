package net.fernando.cobaltage.block.wire;
import net.fernando.cobaltage.block.*;
import net.fernando.cobaltage.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
    private final BlockPos.MutableBlockPos reusableFarMutable = new BlockPos.MutableBlockPos();
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
                    if (CobaltLevelHelper.setWireState(serverLevel, wireNode.pos, newState)) {
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

            // 2. Direct Cobalt Power Source detected (Repeater Cobalt, Torcia Cobalt, ecc.)
            if (neighborState.getBlock() instanceof CobaltPowerSource source) {

                // Special Facing Blocks - Cobalt Comparators and Repeaters
                if (neighborState.getBlock() instanceof CobaltRepeaterBlock ||
                        neighborState.getBlock() instanceof CobaltComparatorBlock) {
                    // 'dir' is from the cable towards the block, it follows 'facing' must be the opposite
                    Direction facing = neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    if (facing == dir) {
                        maxPower = Math.max(maxPower, source.getCobaltPower(neighborState, world, reusableMutable)); // È un repeater ma guarda altrove, quindi 0 energia
                    }
                }
                // Special Facing Blocks - Cobalt Converter
                else if (neighborState.getBlock() instanceof CobaltConverterBlock) {
                    Direction cobaltSide = neighborState.getValue(CobaltConverterBlock.FACING);
                    // I get cobalt power only if he is facing towards me with the cobalt side of the block (its facing)
                    if (cobaltSide == dir.getOpposite()) {
                        maxPower = Math.max(maxPower, source.getCobaltPower(neighborState, world, reusableMutable));
                    }
                }
                else{
                    maxPower = Math.max(maxPower, source.getCobaltPower(neighborState, world, reusableMutable));
                }
            }
            // 3. When we met a solid block (isRedstoneConductor substitutes isSolidBlock in Mojang Mappings)
            // we check if it is Strong Powered (getDirectSignal substitutes getStrongRedstonePower
            // and getSignal substitutes getWeakRedstonePower in Mojang Mappings)
            // by a Cobalt Source (PoweredBlock substitues the instance of the RedstoneBlock in Mojang Mappings)
            else if (neighborState.isRedstoneConductor(world, reusableMutable) || neighborState.getBlock() instanceof PoweredBlock) {
                maxPower = Math.max(maxPower, getStrongPowerFromNeighbors(world, reusableMutable));
            }

            // 4. Compatible Cobalt Power Source (It does not care about the emission direction - Lectern, Lever, Button...)
            if(compatibleCobaltPowerSource(neighborState)){
                maxPower = Math.max(maxPower, neighborState.getSignal(world, pos, dir));
            }

            // 5. Restricted Cobalt Power Source (It cares about the emission direction - Observer, Calibrated Sculk Sensor...)
            if(restrictedCobaltPowerSource(neighborState)){
                maxPower = Math.max(maxPower, neighborState.getSignal(world, pos, dir));
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

            if (sourceState.getBlock() instanceof CobaltPowerSource source) {
                if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                    // We ask for Strong Cobalt Power
                    maxStrong = Math.max(maxStrong, source.getStrongCobaltPower(sourceState, world, mutable, dir.getOpposite()));
                }
            }

            if (compatibleCobaltPowerSource(sourceState)) {
                // We ask for Strong Redstone Power (getDirectSignal substitutes getStrongRedstonePower)
                maxStrong = Math.max(maxStrong, sourceState.getDirectSignal(world, mutable, dir));
            }

            if (restrictedCobaltPowerSource(sourceState)) {
                // We ask for Strong Redstone Power (getDirectSignal substitutes getStrongRedstonePower)
                maxStrong = Math.max(maxStrong, sourceState.getDirectSignal(world, mutable, dir));
            }

        }
        return maxStrong;
    }

    // Boolean helper to check if a solid block is powered by cobalt (or compatible)
    // in any direction except the specified one (to combine with ShouldUnpower for Cobalt Torches)
    public static boolean isSolidBlockPoweredByCobalt(Level world, BlockPos solidPos, Direction exceptDir) {


        // Cobalt Dust Block always emits (bro has Strong Power inside, nice bro)
        if (world.getBlockState(solidPos).is(ModBlocks.COBALT_DUST_BLOCK)) {
            return true;
        }
        // If it isn't either solid and a redstone dust block, it cannot have energy
        if(!world.getBlockState(solidPos).isRedstoneConductor(world, solidPos) && !(world.getBlockState(solidPos).getBlock() instanceof PoweredBlock)){
            return false;
        }

        BlockPos.MutableBlockPos mutable = STATIC_MUTABLE.get();

        for (Direction dir : Direction.values()) {
            if (dir == exceptDir) continue;

            mutable.setWithOffset(solidPos, dir);
            BlockState neighborState = world.getBlockState(mutable);

            // A. Compatible Cobalt Power Source (Levers, Buttons, Pressure Plates - All of them attached to the block)
            if (CobaltWireNetwork.compatibleCobaltPowerSource(neighborState)) {
                // we check if it is Strong Powered (getDirectSignal substitutes getStrongRedstonePower)
                if (neighborState.getDirectSignal(world, mutable, dir) > 0) return true;
            }
            if (CobaltWireNetwork.restrictedCobaltPowerSource(neighborState)) {
                // we check if it is Strong Powered (getDirectSignal substitutes getStrongRedstonePower)
                if (neighborState.getDirectSignal(world, mutable, dir) > 0) return true;
            }

            // B. Cobalt Wire pointing to or Powering from the top
            if (neighborState.getBlock() instanceof CobaltWireBlock) {
                if (neighborState.getSignal(world, mutable, dir) > 0) return true; // When pointing towards the block itself
                if (neighborState.getDirectSignal(world, mutable, dir) > 0) return true; // When over the block itself
            }
            // C. Other Cobalt Sources (Cobalt Repeater, Cobalt Comparator, Cobal Converter)
            else if (neighborState.getBlock() instanceof CobaltPowerSource source) {
                if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                    if (source.getStrongCobaltPower(neighborState, world, mutable, dir.getOpposite()) > 0) return true;
                }
            }
        }

        return false;
    }

    // Boolean helper for the converter
    public static boolean isWeakOrStrongPoweredByCompatibleOrRestrictedCobalt(Level world, BlockPos solidPos, Direction exceptDir) {
        BlockPos.MutableBlockPos mutable = STATIC_MUTABLE.get();
        // Credits DiodeBlock class
        mutable.set(solidPos);
        BlockState redstoneSideState = world.getBlockState(mutable);
        if(compatibleCobaltPowerSource(redstoneSideState) || restrictedCobaltPowerSource(redstoneSideState)){
        int i = world.getSignal(solidPos, exceptDir);
            if (i >= 15) {
                return true; // Credits -> return i; is Redstone Classical Diode Block Function
            } else {
                return (Math.max(i, redstoneSideState.is(Blocks.REDSTONE_WIRE) ? redstoneSideState.getValue(RedStoneWireBlock.POWER) : 0)) > 0;
            }
        }
        return false;
    }


    // Helper to evaluate which neighbors should update their state by a Cobalt Wire Node update
    private void updateNeighbors(Level world, CobaltWireNode node) {

        Block block = world.getBlockState(node.pos).getBlock();

        for (Direction dir : Direction.values()) {
            reusableMutable.setWithOffset(node.pos, dir);
            BlockState neighborState = world.getBlockState(reusableMutable);

            // Skipping vanilla (efficiency)
            if (isVanillaRedstone(neighborState)) continue;

            // Skipping cobalt wires (efficiency)
            if (neighborState.getBlock() instanceof CobaltWireBlock) continue;

            if (neighborState.is(Blocks.OBSERVER)) {
                // Gli Observer vanilla ignorano completamente 'neighborChanged', rispondono solo a 'updateShape'.
                // Chiamiamo updateShape direttamente sull'Observer che abbiamo già intercettato.
                neighborState.updateShape(
                        world,                       // LevelReader
                        world,                       // ScheduledTickAccess
                        reusableMutable,             // BlockPos (Posizione dell'Observer)
                        dir.getOpposite(),           // Direction (Da dove arriva l'aggiornamento)
                        node.pos,                    // BlockPos2 (Posizione del cavo)
                        node.state,                  // BlockState (Stato del cavo)
                        world.getRandom()            // RandomSource
                );
            }else{
                world.neighborChanged(reusableMutable.immutable(), block, null);
            }
            // All the rest

            // Quasi-Neighbor update (Specifically for Pistons)
            // 🛑 Consider removing 'isVanillaRedstone ifs' for efficiency
            if (dir == Direction.DOWN) {
                // 1. Updates the block at theirY = ourY - 2 (Piston down 2 blocks)
                reusableFarMutable.setWithOffset(reusableMutable, Direction.DOWN);
                if (!isVanillaRedstone(world.getBlockState(reusableFarMutable))) {
                    world.neighborChanged(reusableFarMutable.immutable(), block, null);
                }

                // 2. Updates the block at theirY = ourY - 1 and theirXZ differs by 1 (Piston down 1 block in oblique shape)
                for (Direction horizontalDir : Direction.Plane.HORIZONTAL) {
                    reusableFarMutable.setWithOffset(reusableMutable, horizontalDir);
                    if (!isVanillaRedstone(world.getBlockState(reusableFarMutable))) {
                        world.neighborChanged(reusableFarMutable.immutable(), block, null);
                    }
                }
            }

            // Updating also the neighbors next to the neighborState when it is a solid block (or Redstone Dust Block) (for Strong Power Updates)
            if (neighborState.isRedstoneConductor(world, reusableMutable) || neighborState.getBlock() instanceof PoweredBlock) {
                for (Direction sideDir : Direction.values()) {
                    if (sideDir == dir.getOpposite()) continue;
                    reusableFarMutable.setWithOffset(reusableMutable, sideDir);
                    if (!isVanillaRedstone(world.getBlockState(reusableFarMutable))) {
                        world.neighborChanged(reusableFarMutable.immutable(), world.getBlockState(node.pos).getBlock(), null);
                    }
                }
            }
        }
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

    // Helper to check if a block is vanilla redstone (redstone blocks are not here because they are considered solid by cobalt energy blocks)
    public static boolean isVanillaRedstone(BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.REDSTONE_WIRE) ||
                state.is(net.minecraft.world.level.block.Blocks.REPEATER) ||
                state.is(net.minecraft.world.level.block.Blocks.COMPARATOR) ||
                state.is(net.minecraft.world.level.block.Blocks.REDSTONE_TORCH) ||
                state.is(net.minecraft.world.level.block.Blocks.REDSTONE_WALL_TORCH);
    }


    public static boolean restrictedCobaltPowerSource(BlockState state) {
        // The Observer emits only in its opposite facing
        // The Calibrated Sculk Sensor does emit in its opposite facing
        return( state.is(Blocks.OBSERVER) ||
                state.is(Blocks.CALIBRATED_SCULK_SENSOR)
        );
    }


    public static boolean compatibleCobaltPowerSource(BlockState state) {
        return state.is(ModTags.Blocks.COMPATIBLE_COBALT_SOURCES);
    }
}