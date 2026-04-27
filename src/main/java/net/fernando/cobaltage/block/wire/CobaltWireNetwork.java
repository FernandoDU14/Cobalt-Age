package net.fernando.cobaltage.block.wire;

import net.fernando.cobaltage.block.*;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

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

    public void updateNetwork(World world, BlockPos startPos) {
        if (world.isClient() || isUpdating) return;
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

                // Se questo blocco ha ancora energia residua (es. da un'altra fonte), la immettiamo per propagarla
                if (node.virtualPower > 0) {
                    addToBucket(node, node.virtualPower);
                }

                // Trova vicini FISICI (senza scatenare reazioni a catena, solo caching)
                findAndCacheConnectedWires(world, node);

                for (CobaltWireNode neighbor : node.connectedWires) {
                    if (neighbor.discoveredAsDependent) continue;

                    // Se il vicino aveva un'energia compatibile col fatto di essere stato alimentato da noi (massimo node.oldPower - 1)
                    if (neighbor.currentPower > 0 && neighbor.currentPower <= node.oldPower - 1) {
                        // Lo tiriamo giù temporaneamente
                        neighbor.discoveredAsDependent = true;
                        neighbor.oldPower = neighbor.currentPower;

                        if (!neighbor.externalCalculated) {
                            neighbor.externalPower = calculateExternalPower(world, neighbor.pos);
                            neighbor.externalCalculated = true;
                        }
                        neighbor.virtualPower = neighbor.externalPower;
                        depletionQueue.add(neighbor);

                    } else if (neighbor.currentPower > node.oldPower - 1) {
                        // Il vicino ha energia ALTA. Non dipendeva da noi.
                        // Significa che è alimentato da un'altra parte della rete! Gli chiediamo di darci energia ("Backfill")
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
                    node.addedToBucket = false; // Reset per future collisioni

                    if (node.virtualPower != p) continue; // Un altro nodo lo ha sovrascritto meglio

                    int powerToTransmit = node.virtualPower - 1;
                    if (powerToTransmit <= 0) continue;

                    if (node.connectedWires.isEmpty()) {
                        findAndCacheConnectedWires(world, node);
                    }

                    for (CobaltWireNode neighbor : node.connectedWires) {
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

    private void applyPowerChanges(World world) {
        if (!(world instanceof ServerWorld serverLevel)) return;
        List<CobaltWireNode> changedWires = new ArrayList<>();

        for (CobaltNode node : nodes.values()) {
            if (node.isWire()) {
                CobaltWireNode wireNode = node.asWire();
                if (wireNode.virtualPower != wireNode.currentPower) {
                    BlockState newState = wireNode.state.with(CobaltWireBlock.POWER, wireNode.virtualPower);
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

    private void findAndCacheConnectedWires(World world, CobaltWireNode node) {
        if (!node.connectedWires.isEmpty()) return; // Se già riempito nella Fase 1, salta! Costo ZERO.

        BlockPos pos = node.pos;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockState stateBelowMe = world.getBlockState(mutable.set(pos, Direction.DOWN));

        for (Direction dir : Direction.Type.HORIZONTAL) {
            mutable.set(pos, dir);
            BlockState neighborState = world.getBlockState(mutable);

            // 1. CONNESSIONE ORIZZONTALE
            checkAndLink(world, node, mutable.toImmutable(), true);

            // 2. DISCESA
            if (!neighborState.isSolidBlock(world, mutable)) {
                boolean canGoDown = stateBelowMe.isSolidBlock(world, mutable.set(pos, Direction.DOWN));
                mutable.set(pos, dir).move(Direction.DOWN);
                checkAndLink(world, node, mutable.toImmutable(), canGoDown);
            }

            // 3. SALITA
            mutable.set(pos, Direction.UP);
            if (!world.getBlockState(mutable).isSolidBlock(world, mutable)) {
                mutable.set(pos, dir).move(Direction.UP);
                checkAndLink(world, node, mutable.toImmutable(), true);
            }
        }
    }

    // NESSUNA LOGICA COMPLESSA QUI. Collega e basta.
    private void checkAndLink(World world, CobaltWireNode source, BlockPos targetPos, boolean flowAllowed) {
        if (!flowAllowed) return;

        CobaltWireNode target = getOrAddWireNode(world, targetPos);
        if (target != null) {
            source.connectedWires.add(target);
        }
    }

    // --- MANUTENZIONE DEI TUOI METODI ESISTENTI ---

    private int calculateExternalPower(World world, BlockPos pos) {
        int maxPower = 0;
        BlockPos.Mutable mutable = new BlockPos.Mutable(); // Istanza unica riutilizzata

        for (Direction dir : Direction.values()) {
            mutable.set(pos, dir); // Sposta il cursore, niente nuova RAM
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

                    Direction facing = neighborState.get(Properties.HORIZONTAL_FACING);

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
                    Direction cobaltSide = neighborState.get(CobaltConverterBlock.FACING);
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
            else if (neighborState.isSolidBlock(world, mutable)) {
                maxPower = Math.max(maxPower, getStrongPowerFromNeighbors(world, mutable));
            }

            if(compatibleCobaltPowerSource(neighborState)){
                maxPower = Math.max(maxPower, neighborState.getWeakRedstonePower(world, pos, dir.getOpposite()));
            }

            // SE IL VICINO È UN OBSERVER
            if (neighborState.isOf(Blocks.OBSERVER)) {
                // L'Observer dà energia solo se la sua faccia posteriore punta verso il cavo.
                // dirFromWire è la direzione dal cavo verso observer,
                // quindi dobbiamo controllare se observer "guarda" dalla parte opposta.
                if (neighborState.get(ObserverBlock.FACING) == dir) {
                    return neighborState.get(ObserverBlock.POWERED) ? 15 : 0;
                }
            }

        }
        return maxPower;
    }

    // Funzione helper per vedere se un blocco è alimentato da Leve/Torce (Escludendo polveri)
    public static int getStrongPowerFromNeighbors(World world, BlockPos blockPos) {
        int maxStrong = 0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (Direction dir : Direction.values()) {

            mutable.set(blockPos, dir);
            BlockState sourceState = world.getBlockState(mutable);

            if (sourceState.getBlock() instanceof CobaltPowerSource source) {
                if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                    // Chiediamo solo la "Strong Power" (es. leva o torcia sotto il blocco)
                    maxStrong = Math.max(maxStrong, source.getStrongCobaltPower(sourceState, world, mutable, dir.getOpposite()));
                }
            }

            if (compatibleCobaltPowerSource(sourceState)) {
                // Chiediamo solo la "Strong Power" (es. leva o torcia sotto il blocco)
                maxStrong = Math.max(maxStrong, sourceState.getStrongRedstonePower(world, mutable, dir));
            }

        }
        return maxStrong;
    }

    public static boolean isSolidBlockPoweredByCobalt(World world, BlockPos solidPos, Direction exceptDir) {


        // Se è il blocco di alimentazione (sorgente), ritorna true a prescindere dalla "solidità"
        if (world.getBlockState(solidPos).isOf(ModBlocks.COBALT_DUST_BLOCK)) { // Usa il riferimento corretto al tuo blocco solido
            return true;
        }
        if(!world.getBlockState(solidPos).isSolidBlock(world, solidPos)){
            return false;
        }

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (Direction dir : Direction.values()) {
            if (dir == exceptDir) continue;

            mutable.set(solidPos, dir);
            BlockState neighborState = world.getBlockState(mutable);

            // A. Controllo Componenti Meccanici (Leve, bottoni attaccati al blocco solido)
            if (CobaltWireNetwork.compatibleCobaltPowerSource(neighborState)) {
                // Se la leva sta iniettando energia forte nel blocco di pietra,
                // la torcia di cobalto attaccata ad esso deve spegnersi!
                if (neighborState.getStrongRedstonePower(world, mutable, dir) > 0) return true;
            }

            // B. Controllo Cavo di Cobalto
            if (neighborState.getBlock() instanceof CobaltWireBlock) {
                if (neighborState.getWeakRedstonePower(world, mutable, dir) > 0) return true;
                if (neighborState.getStrongRedstonePower(world, mutable, dir) > 0) return true;
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


    private void updateNeighbors(World world, CobaltWireNode node) {

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockPos.Mutable farMutable = new BlockPos.Mutable();

        for (Direction dir : Direction.values()) {
            mutable.set(node.pos, dir);
            BlockState neighborState = world.getBlockState(mutable);

            // Cobalt Firewall
            if (isVanillaRedstone(neighborState)) continue;

            // ⚡ LAG FIX DEFINITIVO: Salta le polveri di Cobalto! ⚡
            // Le polveri sono GIÀ state aggiornate dal nostro algoritmo (Fase 2 e 3).
            // Se le avvisiamo, scateniamo un nuovo updateNetwork a catena.
            if (neighborState.getBlock() instanceof CobaltWireBlock) continue;

            // Notifica il blocco vicino che l'energia è cambiata
            world.updateNeighbor(mutable.toImmutable(), world.getBlockState(node.pos).getBlock(), null);

            // Se è un blocco solido, dobbiamo aggiornare anche i blocchi DOPO di lui (Strong Power)
            if (neighborState.isSolidBlock(world, mutable)) {
                for (Direction sideDir : Direction.values()) {
                    if (sideDir == dir.getOpposite()) continue;
                    farMutable.set(mutable, sideDir);
                    if (!isVanillaRedstone(world.getBlockState(farMutable))) {
                        world.updateNeighbor(farMutable.toImmutable(), world.getBlockState(node.pos).getBlock(), null);
                    }
                }
            }
        }
    }

    private CobaltWireNode getOrAddWireNode(World world, BlockPos pos) {
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
        return state.isOf(net.minecraft.block.Blocks.REDSTONE_WIRE) ||
                state.isOf(net.minecraft.block.Blocks.REPEATER) ||
                state.isOf(net.minecraft.block.Blocks.COMPARATOR) ||
                state.isOf(net.minecraft.block.Blocks.REDSTONE_TORCH) ||
                state.isOf(net.minecraft.block.Blocks.REDSTONE_WALL_TORCH) ||
                state.isOf(net.minecraft.block.Blocks.REDSTONE_BLOCK);
    }

    public static boolean compatibleCobaltPowerSource(BlockState state) {
        return state.getBlock() instanceof ButtonBlock ||
                state.getBlock() instanceof LeverBlock ||
                state.getBlock() instanceof PressurePlateBlock ||
                state.getBlock() instanceof WeightedPressurePlateBlock ||
                state.getBlock() instanceof SculkSensorBlock ||
                state.getBlock() instanceof TargetBlock ||
                state.getBlock() instanceof TripwireHookBlock ||
                state.getBlock() instanceof DaylightDetectorBlock ||
                state.getBlock() instanceof JukeboxBlock ||
                state.getBlock() instanceof LightningRodBlock ||
                state.getBlock() instanceof TrappedChestBlock;
    }

}