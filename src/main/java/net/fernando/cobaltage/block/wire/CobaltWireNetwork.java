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

    // BUCKET QUEUE: Inserimento O(1). Mantenuto per la massima performance!
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

            // ⚡ FAST ABORT: Se non c'è energia prima e non ce n'è ora, fermati subito.
            if (oldPower == 0 && newExt == 0) return;

            startNode.externalPower = newExt;
            startNode.externalCalculated = true;

            Queue<CobaltWireNode> depletionQueue = new ArrayDeque<>();

            if (newExt > oldPower) {
                // INCREMENTO: Salta la deplezione, andiamo diretti in propagazione
                startNode.virtualPower = newExt;
                addToBucket(startNode, newExt);
            } else if (newExt < oldPower) {
                // DECREMENTO (Observer si spegne/Cavo rotto): Abbattiamo SOLO il ramo che dipendeva da noi
                startNode.oldPower = oldPower;
                startNode.virtualPower = newExt;
                startNode.discoveredAsDependent = true;
                depletionQueue.add(startNode);
            } else {
                // Stessa energia (forse è cambiata la forma del filo adiacente). Mettiamo in bucket per aggiornare i vicini.
                startNode.virtualPower = newExt;
                addToBucket(startNode, newExt);
            }

            // --- FASE 1: DEPLEZIONE (Crollo energetico mirato) ---
            while (!depletionQueue.isEmpty()) {
                CobaltWireNode node = depletionQueue.poll();

                if (node.virtualPower > 0) {
                    addToBucket(node, node.virtualPower);
                }

                findAndCacheConnectedWires(world, node);

                // Variabili di cache pulite per questo nodo specifico
                boolean isSolidBelow = false;
                boolean hasCheckedSolidBelow = false;

                for (CobaltWireNode neighbor : node.connectedWires) {
                    if (neighbor.discoveredAsDependent) continue;

                    // Possiamo INVIARE energia a lui? (Serve per capire se dipendeva da noi)
                    boolean weCanSendToNeighbor = true;
                    boolean differentXZ = node.pos.getX() != neighbor.pos.getX() || node.pos.getZ() != neighbor.pos.getZ();
                    if (neighbor.pos.getY() < node.pos.getY() && differentXZ) {
                        if (!hasCheckedSolidBelow) {
                            isSolidBelow = world.getBlockState(node.pos.below()).isRedstoneConductor(world, node.pos.below());
                            hasCheckedSolidBelow = true;
                        }
                        weCanSendToNeighbor = isSolidBelow;
                    }

                    // Lui può INVIARE energia a noi? (Serve per il Backfill)
                    boolean neighborCanSendToUs = true;
                    if (neighbor.pos.getY() > node.pos.getY() && differentXZ){
                        neighborCanSendToUs = world.getBlockState(neighbor.pos.below()).isRedstoneConductor(world, neighbor.pos.below());
                    }

                    if (neighbor.currentPower > 0 && neighbor.currentPower <= node.oldPower - 1) {
                        // Se il vetro ci impediva di passargli energia, non poteva dipendere da noi!
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
                        // BACKFILL: Il vicino ha energia ALTA!
                        // Ma può aiutarci SOLO se il suo blocco gli permette di inviare energia in basso.
                        if (!neighborCanSendToUs) continue;

                        neighbor.virtualPower = neighbor.currentPower;
                        addToBucket(neighbor, neighbor.virtualPower);
                    }
                }
            }

            // --- FASE 2: PROPAGAZIONE IN RAM ---
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

                    // Cache isolata per prevenire leak sui blocchi diagonali
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

            // --- FASE 3: SCRITTURA E AGGIORNAMENTO VANILLA ---
            applyPowerChanges(world);

        } finally {
            // Svuota cache in sicurezza
            nodes.clear();
            for (Queue<CobaltWireNode> q : priorityBuckets) q.clear();
            isUpdating = false;
        }
    }

    // Helper O(1) per gestire l'inserimento univoco nei bucket
    private void addToBucket(CobaltWireNode node, int power) {
        if (!node.addedToBucket) {
            node.addedToBucket = true;
            priorityBuckets[power].add(node);
        }
    }

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

    private void findAndCacheConnectedWires(Level world, CobaltWireNode node) {
        if (!node.connectedWires.isEmpty()) return; // Se già riempito, salta! Costo ZERO.

        BlockPos pos = node.pos;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        // ---- NUOVO: CONNESSIONE DIRETTA VERTICALE (Per i Relay Block) ----
        mutable.setWithOffset(pos, Direction.UP);
        checkAndLink(world, node, mutable.immutable());

        mutable.setWithOffset(pos, Direction.DOWN);
        checkAndLink(world, node, mutable.immutable());

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            mutable.setWithOffset(pos, dir);
            BlockState neighborState = world.getBlockState(mutable);

            // Orizzontale
            checkAndLink(world, node, mutable.immutable());

            // 2. DISCESA
            if (!neighborState.isRedstoneConductor(world, mutable)) { // The Diagonal Block That Could Interrupt The Wire Web
                // 🛑 FIX: Non controlliamo 'canGoDown' qui!
                // Memorizziamo il vicino a prescindere per poter ricevere eventuale Backfill
                mutable.setWithOffset(pos, dir).move(Direction.DOWN);
                checkAndLink(world, node, mutable.immutable());
            }

            // 3. SALITA
            mutable.setWithOffset(pos, Direction.UP);
            if (!world.getBlockState(mutable).isRedstoneConductor(world, mutable)) { // The Diagonal Block That Could Interrupt The Wire Web
                mutable.setWithOffset(pos, dir).move(Direction.UP);
                checkAndLink(world, node, mutable.immutable());
            }
        }
    }

    private void checkAndLink(Level world, CobaltWireNode source, BlockPos targetPos) {
        CobaltWireNode target = getOrAddWireNode(world, targetPos);
        if (target == null) return;

        boolean sourceIsRelay = source.state.is(ModBlocks.COBALT_RELAY);
        boolean targetIsRelay = target.state.is(ModBlocks.COBALT_RELAY);
        boolean sourceIsDust = source.state.is(ModBlocks.COBALT_DUST);
        boolean targetIsDust = target.state.is(ModBlocks.COBALT_DUST);

        boolean diffXZ = source.pos.getX() != target.pos.getX() || source.pos.getZ() != target.pos.getZ();
        boolean diffY = source.pos.getY() != target.pos.getY();

        // 1. Relay <-> Relay: Solo verticale puro
        if (sourceIsRelay && targetIsRelay) {
            if (diffXZ) return;
        }

        // 2. Relay <-> Dust: Isolamento verticale inferiore
        if ((sourceIsRelay && targetIsDust) || (sourceIsDust && targetIsRelay)) {
            // Niente diagonali se c'è un relay di mezzo
            if (diffY && diffXZ) return;

            // Logica richiesta: Se sono uno sopra l'altro (Verticale puro)
            if (!diffXZ && diffY) {
                // Se il Relay sta SOPRA la Dust, blocchiamo il link in ENTRAMBE le direzioni.
                // Così il Relay non alimenta sotto e non riceve da sotto.
                if (sourceIsRelay && source.pos.getY() > target.pos.getY()) return;
                if (targetIsRelay && target.pos.getY() > source.pos.getY()) return;
            }
        }

        source.connectedWires.add(target);
    }


    private int calculateExternalPower(Level world, BlockPos pos) {
        int maxPower = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(); // Istanza unica riutilizzata

        for (Direction dir : Direction.values()) {
            mutable.setWithOffset(pos, dir); // Sposta il cursore, niente nuova RAM
            BlockState neighborState = world.getBlockState(mutable);

            // 1. Ignora le altre polveri di cobalto!
            // L'energia tra polveri viene gestita dalla Fase 2 (Propagazione), non qui.
            if (neighborState.getBlock() instanceof CobaltWireBlock) {
                continue;
            }

            // 2. Se è una fonte Cobalt diretta (Repeater Cobalt, Torcia Cobalt, ecc.)
            if (neighborState.getBlock() instanceof CobaltPowerSource source) {
                if (neighborState.getBlock() instanceof CobaltRepeaterBlock ||
                        neighborState.getBlock() instanceof CobaltComparatorBlock) {

                    Direction facing = neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING);

                    // Dir è la direzione dal CAVO verso il BLOCCO.
                    // Il blocco punta verso il cavo solo se la sua direzione (facing)
                    // è l'opposto di 'dir'.
                    // Esempio: se il blocco è a NORD del cavo (dir=NORTH),
                    // deve guardare a SUD (facing=SOUTH) per alimentarlo.
                    if (facing == dir) {
                        maxPower = Math.max(maxPower, source.getCobaltPower(neighborState, world, mutable)); // È un repeater ma guarda altrove, quindi 0 energia
                    }
                }
                else if (neighborState.getBlock() instanceof CobaltConverterBlock) {
                    Direction cobaltSide = neighborState.getValue(CobaltConverterBlock.FACING);
                    // Il convertitore cede Cobalt SOLO dalla faccia "Cobalt".
                    // dir è la direzione dal cavo verso il convertitore.
                    if (cobaltSide == dir.getOpposite()) {
                        maxPower = Math.max(maxPower, source.getCobaltPower(neighborState, world, mutable));
                    }
                }
                else{
                    maxPower = Math.max(maxPower, source.getCobaltPower(neighborState, world, mutable));
                }
            }
            // 3. Se è un blocco solido, controlla se è alimentato FORTEMENTE da una fonte Cobalt
            // MAI controllare world.getReceivedRedstonePower() perché includerebbe le polveri!
            else if (neighborState.isRedstoneConductor(world, mutable) || neighborState.getBlock() instanceof PoweredBlock) {
                maxPower = Math.max(maxPower, getStrongPowerFromNeighbors(world, mutable));
            }

            if(compatibleCobaltPowerSource(neighborState)){
                maxPower = Math.max(maxPower, neighborState.getSignal(world, pos, dir.getOpposite()));
            }

            // SE IL VICINO È UN OBSERVER
            if (neighborState.is(Blocks.OBSERVER)) {
                // L'Observer dà energia solo se la sua faccia posteriore punta verso il cavo.
                // dirFromWire è la direzione dal cavo verso observer,
                // quindi dobbiamo controllare se observer "guarda" dalla parte opposta.
                if (neighborState.getValue(ObserverBlock.FACING) == dir) {
                    return neighborState.getValue(ObserverBlock.POWERED) ? 15 : 0;
                }
            }

        }
        return maxPower;
    }

    // Funzione helper per vedere se un blocco è alimentato da Leve/Torce (Escludendo polveri)
    public static int getStrongPowerFromNeighbors(Level world, BlockPos blockPos) {
        int maxStrong = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (Direction dir : Direction.values()) {

            mutable.setWithOffset(blockPos, dir);
            BlockState sourceState = world.getBlockState(mutable);

            if (sourceState.getBlock() instanceof CobaltPowerSource source) {
                if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                    // Chiediamo solo la "Strong Power" (es. leva o torcia sotto il blocco)
                    maxStrong = Math.max(maxStrong, source.getStrongCobaltPower(sourceState, world, mutable, dir.getOpposite()));
                }
            }

            if (compatibleCobaltPowerSource(sourceState)) {
                // Chiediamo solo la "Strong Power" (es. leva o torcia sotto il blocco)
                maxStrong = Math.max(maxStrong, sourceState.getDirectSignal(world, mutable, dir));
            }

        }
        return maxStrong;
    }

    public static boolean isSolidBlockPoweredByCobalt(Level world, BlockPos solidPos, Direction exceptDir) {


        // Se è il blocco di alimentazione (sorgente), ritorna true a prescindere dalla "solidità"
        if (world.getBlockState(solidPos).is(ModBlocks.COBALT_DUST_BLOCK)) { // Usa il riferimento corretto al tuo blocco solido
            return true;
        }
        if(!world.getBlockState(solidPos).isRedstoneConductor(world, solidPos) && !(world.getBlockState(solidPos).getBlock() instanceof PoweredBlock)){
            return false;
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (Direction dir : Direction.values()) {
            if (dir == exceptDir) continue;

            mutable.setWithOffset(solidPos, dir);
            BlockState neighborState = world.getBlockState(mutable);

            // A. Controllo Componenti Meccanici (Leve, bottoni attaccati al blocco solido)
            if (CobaltWireNetwork.compatibleCobaltPowerSource(neighborState)) {
                // Se la leva sta iniettando energia forte nel blocco di pietra,
                // la torcia di cobalto attaccata ad esso deve spegnersi!
                if (neighborState.getDirectSignal(world, mutable, dir) > 0) return true;
            }

            // B. Controllo Cavo di Cobalto
            if (neighborState.getBlock() instanceof CobaltWireBlock) {
                if (neighborState.getSignal(world, mutable, dir) > 0) return true;
                if (neighborState.getDirectSignal(world, mutable, dir) > 0) return true;
            }
            // C. Controllo altre Sorgenti Cobalt
            else if (neighborState.getBlock() instanceof CobaltPowerSource source) {
                if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                    if (source.getStrongCobaltPower(neighborState, world, mutable, dir.getOpposite()) > 0) return true;
                }
            }
        }

        return false;
    }


    private void updateNeighbors(Level world, CobaltWireNode node) {

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos farMutable = new BlockPos.MutableBlockPos();
        Block block = world.getBlockState(node.pos).getBlock();

        for (Direction dir : Direction.values()) {
            mutable.setWithOffset(node.pos, dir);
            BlockState neighborState = world.getBlockState(mutable);

            // Cobalt Firewall
            if (isVanillaRedstone(neighborState)) continue;

            // Salta le polveri di Cobalto (sono già aggiornate dal Network Handler)
            if (neighborState.getBlock() instanceof CobaltWireBlock) continue;

            // Notifica il blocco vicino che l'energia è cambiata
            world.neighborChanged(mutable.immutable(), block, null);

            // Quasi-Neighbor update
            if (dir == Direction.DOWN) {
                // 1. Notifica il blocco a 2 blocchi di distanza in basso (Pistone dritto sotto, separato da aria/vetro)
                farMutable.setWithOffset(mutable, Direction.DOWN);
                if (!isVanillaRedstone(world.getBlockState(farMutable))) {
                    world.neighborChanged(farMutable.immutable(), block, null);
                }

                // 2. Notifica i blocchi in diagonale in basso (Pistoni laterali sotto il cavo)
                for (Direction horizontalDir : Direction.Plane.HORIZONTAL) {
                    farMutable.setWithOffset(mutable, horizontalDir);
                    if (!isVanillaRedstone(world.getBlockState(farMutable))) {
                        world.neighborChanged(farMutable.immutable(), block, null);
                    }
                }
            }

            // Aggiornamento anche i blocchi DOPO di lui (Strong Power)
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
        long posLong = pos.asLong(); // Ottiene il long primitivo senza boxing
        CobaltNode node = nodes.get(posLong);

        if (node == null) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof CobaltWireBlock) {
                node = new CobaltWireNode(pos, state);
            } else {
                node = new CobaltNode(pos, state);
            }
            nodes.put(posLong, node); // Inserimento senza boxing
        }

        return node.isWire() ? node.asWire() : null;
    }

    public static boolean isVanillaRedstone(BlockState state) {
        // Here we DO NOT want to add REDSTONE BLOCK
        return state.is(net.minecraft.world.level.block.Blocks.REDSTONE_WIRE) ||
                state.is(net.minecraft.world.level.block.Blocks.REPEATER) ||
                state.is(net.minecraft.world.level.block.Blocks.COMPARATOR) ||
                state.is(net.minecraft.world.level.block.Blocks.REDSTONE_TORCH) ||
                state.is(net.minecraft.world.level.block.Blocks.REDSTONE_WALL_TORCH);
    }

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