package net.fernando.cobaltage.block.wire;

import net.fernando.cobaltage.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PoweredBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class CobaltWireNetwork {

    private final Long2ObjectOpenHashMap<CobaltNode> nodes = new Long2ObjectOpenHashMap<>();

    // BUCKET QUEUE: Insert O(1) for every node to update, ordered by virtual power (0-15)
    private final Queue<CobaltWireNode>[] priorityBuckets = new Queue[16];

    private boolean isUpdating = false;

    public CobaltWireNetwork() {
        for (int i = 0; i < 16; i++) {
            priorityBuckets[i] = new ArrayDeque<>();
        }
    }

    public void updateNetwork(Level world, BlockPos startPos) {
        if (world.isClientSide() || isUpdating) return;
        isUpdating = true;

        try {
            CobaltWireNode startNode = getOrAddWireNode(world, startPos);
            if (startNode == null) return;

            int oldPower = startNode.currentPower;
            int newExt = calculateExternalPower(world, startNode.pos);

            // ⚡ fast quit when no energy now and before
            if (oldPower == 0 && newExt == 0) return;

            startNode.externalPower = newExt;
            startNode.externalCalculated = true;

            Queue<CobaltWireNode> depletionQueue = new ArrayDeque<>();

            if (newExt > oldPower) {
                // Increment: Skip depletion, go directly to propagation
                startNode.virtualPower = newExt;
                addToBucket(startNode, newExt);
            } else if (newExt < oldPower) {
                // Decrement: (Observer if it turns off/broken cable): Cutting only the branch was depending on us
                startNode.oldPower = oldPower;
                startNode.virtualPower = newExt;
                startNode.discoveredAsDependent = true;
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
                    if (neighbor.discoveredAsDependent) continue;

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

                        neighbor.discoveredAsDependent = true;
                        neighbor.oldPower = neighbor.currentPower;

                        if (!neighbor.externalCalculated) {
                            neighbor.externalPower = calculateExternalPower(world, neighbor.pos);
                            neighbor.externalCalculated = true;
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
                    node.addedToBucket = false;

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
        if (!node.addedToBucket) {
            node.addedToBucket = true;
            priorityBuckets[power].add(node);
        }
    }

    // Helper to apply power changes
    private void applyPowerChanges(Level world) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        List<CobaltWireNode> changedWires = new ArrayList<>();

        for (CobaltNode node : nodes.values()) {
            if (node.isWire()) {
                CobaltWireNode wireNode = node.asWire();
                if (wireNode.virtualPower != wireNode.currentPower) {
                    BlockState newState = wireNode.state.setValue(CobaltWireBlock.POWER, wireNode.virtualPower);
                    if (CobaltLevelHelper.setWireState(serverLevel, wireNode.pos, newState)) {
                        wireNode.state = newState;
                        wireNode.currentPower = wireNode.virtualPower;
                        changedWires.add(wireNode);
                    }
                }
            }
        }

        for (CobaltWireNode wire : changedWires) {
            updateNeighbors(world, wire);
        }
    }

    // Helper to checkAndLink connected wires and cache them.
    private void findAndCacheConnectedWires(Level world, CobaltWireNode node) {
        if (!node.connectedWires.isEmpty()) return; // Se già riempito, salta! Costo ZERO.

        BlockPos pos = node.pos;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        // ---- Vertical Wire Connections for Relay Blocks ----
        mutable.setWithOffset(pos, Direction.UP);
        checkAndLink(world, node, mutable.immutable());
        mutable.setWithOffset(pos, Direction.DOWN);
        checkAndLink(world, node, mutable.immutable());

        // ---- Horizontal Wire Connections for both Relay Blocks and Cobalt Wire Blocks ----
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            mutable.setWithOffset(pos, dir);
            BlockState neighborState = world.getBlockState(mutable);

            // Horizontal link
            checkAndLink(world, node, mutable.immutable());

            // 2. Block descent
            if (!neighborState.isRedstoneConductor(world, mutable)) { // The Diagonal Block That Could Interrupt The Wire Web
                // 🛑 Important: We do not check 'canGoDown' here, as the wire web could still be
                // influenced by a (diagonal) above cobalt wire.
                // Memorizing the neighbor regardless to receive potential Backfill
                mutable.setWithOffset(pos, dir).move(Direction.DOWN);
                checkAndLink(world, node, mutable.immutable());
            }

            // 3. Block ascent
            mutable.setWithOffset(pos, Direction.UP);
            if (!world.getBlockState(mutable).isRedstoneConductor(world, mutable)) { // The Diagonal Block That Could Interrupt The Wire Web
                mutable.setWithOffset(pos, dir).move(Direction.UP);
                checkAndLink(world, node, mutable.immutable());
            }
        }
    }

    // Helper to check and link between wires and relays, implementing here the logic for their mixed behavior
    private void checkAndLink(Level world, CobaltWireNode source, BlockPos targetPos) {
        CobaltWireNode target = getOrAddWireNode(world, targetPos);
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
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(); // One instance that we reuse (efficiency)

        for (Direction dir : Direction.values()) {
            mutable.setWithOffset(pos, dir);
            BlockState neighborState = world.getBlockState(mutable);

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
                        maxPower = Math.max(maxPower, source.getCobaltPower(neighborState, world, mutable)); // È un repeater ma guarda altrove, quindi 0 energia
                    }
                }
                // Special Facing Blocks - Cobalt Converter
                else if (neighborState.getBlock() instanceof CobaltConverterBlock) {
                    Direction cobaltSide = neighborState.getValue(CobaltConverterBlock.FACING);
                    // I get cobalt power only if he is facing towards me with the cobalt side of the block (its facing)
                    if (cobaltSide == dir.getOpposite()) {
                        maxPower = Math.max(maxPower, source.getCobaltPower(neighborState, world, mutable));
                    }
                }
                else{
                    maxPower = Math.max(maxPower, source.getCobaltPower(neighborState, world, mutable));
                }
            }
            // 3. When we met a solid block (isRedstoneConductor substitutes isSolidBlock in Mojang Mappings)
            // we check if it is Strong Powered (getDirectSignal substitutes getStrongRedstonePower
            // and getSignal substitutes getWeakRedstonePower in Mojang Mappings)
            // by a Cobalt Source (PoweredBlock substitues the instance of the RedstoneBlock in Mojang Mappings)
            else if (neighborState.isRedstoneConductor(world, mutable) || neighborState.getBlock() instanceof PoweredBlock) {
                maxPower = Math.max(maxPower, getStrongPowerFromNeighbors(world, mutable));
            }

            // 4. Compatible Cobalt Power Source
            if(compatibleCobaltPowerSource(neighborState)){
                maxPower = Math.max(maxPower, neighborState.getSignal(world, pos, dir.getOpposite()));
            }

            // 5. Special Case - Observer
            if (neighborState.is(Blocks.OBSERVER)) {
                // The Observer emits only in its opposite facing
                if (neighborState.getValue(ObserverBlock.FACING) == dir) {
                    return neighborState.getValue(ObserverBlock.POWERED) ? 15 : 0;
                }
            }

        }
        return maxPower;
    }

    // Helper to evaluate the Strong Cobalt Power or a Compatible Strong Redstone Power of a Block.
    public static int getStrongPowerFromNeighbors(Level world, BlockPos blockPos) {
        int maxStrong = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

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

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (Direction dir : Direction.values()) {
            if (dir == exceptDir) continue;

            mutable.setWithOffset(solidPos, dir);
            BlockState neighborState = world.getBlockState(mutable);

            // A. Compatible Cobalt Power Source (Levers, Buttons, Pressure Plates - All of them attached to the block)
            if (CobaltWireNetwork.compatibleCobaltPowerSource(neighborState)) {
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


    // Helper to evaluate which neighbors should update their state by a Cobalt Wire Node update
    private void updateNeighbors(Level world, CobaltWireNode node) {

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos farMutable = new BlockPos.MutableBlockPos();
        Block block = world.getBlockState(node.pos).getBlock();

        for (Direction dir : Direction.values()) {
            mutable.setWithOffset(node.pos, dir);
            BlockState neighborState = world.getBlockState(mutable);

            // Skipping vanilla (efficiency)
            if (isVanillaRedstone(neighborState)) continue;

            // Skipping cobalt wires (efficiency)
            if (neighborState.getBlock() instanceof CobaltWireBlock) continue;

            // All the rest
            world.neighborChanged(mutable.immutable(), block, null);

            // Quasi-Neighbor update (Specifically for Pistons)
            // 🛑 Consider removing 'isVanillaRedstone ifs' for efficiency
            if (dir == Direction.DOWN) {
                // 1. Updates the block at theirY = ourY - 2 (Piston down 2 blocks)
                farMutable.setWithOffset(mutable, Direction.DOWN);
                if (!isVanillaRedstone(world.getBlockState(farMutable))) {
                    world.neighborChanged(farMutable.immutable(), block, null);
                }

                // 2. Updates the block at theirY = ourY - 1 and theirXZ differs by 1 (Piston down 1 block in oblique shape)
                for (Direction horizontalDir : Direction.Plane.HORIZONTAL) {
                    farMutable.setWithOffset(mutable, horizontalDir);
                    if (!isVanillaRedstone(world.getBlockState(farMutable))) {
                        world.neighborChanged(farMutable.immutable(), block, null);
                    }
                }
            }

            // Updating also the neighbors next to the neighborState when it is a solid block (or Redstone Dust Block) (for Strong Power Updates)
            if (neighborState.isRedstoneConductor(world, mutable) || neighborState.getBlock() instanceof PoweredBlock) {
                for (Direction sideDir : Direction.values()) {
                    if (sideDir == dir.getOpposite()) continue;
                    farMutable.setWithOffset(mutable, sideDir);
                    if (!isVanillaRedstone(world.getBlockState(farMutable))) {
                        world.neighborChanged(farMutable.immutable(), world.getBlockState(node.pos).getBlock(), null);
                    }
                }
            }
        }
    }

    private CobaltWireNode getOrAddWireNode(Level world, BlockPos pos) {
        long posLong = pos.asLong(); // Obtains the long primitive without boxing
        CobaltNode node = nodes.get(posLong);

        if (node == null) {
            BlockState state = world.getBlockState(pos);
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

    // Helper to check if a block can power a Cobalt Energy Block
    public static boolean compatibleCobaltPowerSource(BlockState state) {
        return state.getBlock() instanceof ButtonBlock ||
                state.getBlock() instanceof LeverBlock ||
                state.getBlock() instanceof PressurePlateBlock ||
                state.getBlock() instanceof WeightedPressurePlateBlock ||
                state.getBlock() instanceof SculkSensorBlock ||
                state.getBlock() instanceof TargetBlock ||
                state.getBlock() instanceof TripWireHookBlock ||
                state.getBlock() instanceof DaylightDetectorBlock ||
                state.getBlock() instanceof JukeboxBlock ||
                state.getBlock() instanceof LightningRodBlock ||
                state.getBlock() instanceof TrappedChestBlock ||
                state.getBlock() instanceof LecternBlock;
    }

}