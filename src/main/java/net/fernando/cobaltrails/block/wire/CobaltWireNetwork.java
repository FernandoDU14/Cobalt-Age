package net.fernando.cobaltrails.block.wire;

import net.fernando.cobaltrails.block.*;
import net.minecraft.block.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

public class CobaltWireNetwork {

    private final Map<BlockPos, CobaltNode> nodes = new HashMap<>();
    private final Queue<CobaltWireNode> searchQueue = new ArrayDeque<>();

    // BUCKET QUEUE: 16 code (0-15). Inserimento ed estrazione O(1). Zero lag.
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

            startNode.discovered = true;
            searchQueue.add(startNode);

            // FASE 1: Esplora e azzera
            searchAndDepowerNetwork(world);

            // FASE 2: Propaga in RAM
            propagatePower();

            // FASE 3: Applica al mondo
            applyPowerChanges(world);

        } finally {
            nodes.clear();
            searchQueue.clear();
            for (Queue<CobaltWireNode> q : priorityBuckets) q.clear();
            isUpdating = false;
        }
    }

    private void searchAndDepowerNetwork(World world) {
        while (!searchQueue.isEmpty()) {
            CobaltWireNode node = searchQueue.poll();
            node.searched = true;

            // Calcola potenza esterna usando la tua logica preesistente
            node.externalPower = calculateExternalPower(world, node.pos);
            node.virtualPower = node.externalPower;

            // Trova vicini e mettili in cache nel nodo!
            findAndCacheConnectedWires(world, node);

            // Mettiamo il nodo nel bucket giusto per la propagazione iniziale
            if (node.virtualPower > 0) {
                priorityBuckets[node.virtualPower].add(node);
            }
        }
    }

    private void propagatePower() {
        // Leggiamo dai bucket partendo dal più alto (15) a scendere (1).
        for (int p = 15; p > 0; p--) {
            Queue<CobaltWireNode> currentBucket = priorityBuckets[p];

            while (!currentBucket.isEmpty()) {
                CobaltWireNode node = currentBucket.poll();

                // Se la potenza virtuale è scesa (es. un altro nodo lo ha sovrascritto male), ignoriamo
                if (node.virtualPower != p) continue;

                int powerToTransmit = node.virtualPower - 1;
                if (powerToTransmit <= 0) continue;

                // Usiamo la cache, niente chiamate al World!
                for (CobaltWireNode neighbor : node.connectedWires) {
                    if (powerToTransmit > neighbor.virtualPower) {
                        neighbor.virtualPower = powerToTransmit;
                        // Aggiungiamo al bucket corrispondente. È O(1)!
                        priorityBuckets[powerToTransmit].add(neighbor);
                    }
                }
            }
        }
    }

    private void applyPowerChanges(World world) {
        // Lista dei blocchi da aggiornare alla fine
        List<CobaltWireNode> changedWires = new ArrayList<>();

        for (CobaltNode node : nodes.values()) {
            if (node.isWire()) {
                CobaltWireNode wireNode = node.asWire();
                if (wireNode.virtualPower != wireNode.currentPower) {

                    // Applica in silenzio (senza causare reaction Vanilla)
                    BlockState newState = wireNode.state.with(CobaltWireBlock.POWER, wireNode.virtualPower);
                    world.setBlockState(wireNode.pos, newState, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);

                    changedWires.add(wireNode);
                }
            }
        }

        // AGGIORNAMENTI: Avvengono SOLO alla fine, quando la rete è stabile.
        for (CobaltWireNode wire : changedWires) {
            updateNeighbors(world, wire);
        }
    }

    private void findAndCacheConnectedWires(World world, CobaltWireNode node) {
        BlockPos pos = node.pos;
        BlockState stateBelowMe = world.getBlockState(pos.down());

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // 1. CONNESSIONE ORIZZONTALE (Sempre permessa)
            checkAndLink(world, node, neighborPos, true);

            // 2. DISCESA (Il nodo attuale è SOPRA, il target è SOTTO)
            // Flusso permesso solo se il blocco sotto il cavo attuale è SOLIDO.
            if (!neighborState.isSolidBlock(world, neighborPos)) { // Controllo che non ci sia un blocco in mezzo
                boolean canGoDown = stateBelowMe.isSolidBlock(world, pos.down());
                checkAndLink(world, node, neighborPos.down(), canGoDown);
            }

            // 3. SALITA (Il nodo attuale è SOTTO, il target è SOPRA)
            // Flusso permesso sempre, a meno che non ci sia un blocco solido sopra la testa.
            if (!world.getBlockState(pos.up()).isSolidBlock(world, pos.up())) {
                checkAndLink(world, node, neighborPos.up(), true);
            }
        }
    }

    private void checkAndLink(World world, CobaltWireNode source, BlockPos targetPos, boolean flowAllowed) {
        CobaltWireNode target = getOrAddWireNode(world, targetPos);
        if (target != null) {
            // --- 1. SCOPERTA (Sempre bidirezionale) ---
            // Aggiungiamo il vicino alla coda di ricerca a prescindere dal diodo.
            // Questo serve per "trovare" il cavo e aggiornarlo se la corrente viene troncata.
            if (!target.discovered) {
                target.discovered = true;
                searchQueue.add(target);
            }

            // --- 2. PROPAGAZIONE (Logica del Diodo) ---
            // Aggiungiamo il target alla lista dei collegamenti d'uscita del nodo attuale
            // SOLO se la logica (solido/vetro) permette il passaggio.
            if (flowAllowed) {
                source.connectedWires.add(target);
            }
        }
    }

    // --- MANUTENZIONE DEI TUOI METODI ESISTENTI ---

    private int calculateExternalPower(World world, BlockPos pos) {
        int maxPower = 0;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

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
                        maxPower = Math.max(maxPower, source.getCobaltPower(neighborState, world, neighborPos)); // È un repeater ma guarda altrove, quindi 0 energia
                    }
                }
                else if (neighborState.getBlock() instanceof CobaltConverterBlock) {
                    Direction cobaltSide = neighborState.get(CobaltConverterBlock.FACING);
                    // Il convertitore cede Cobalt SOLO dalla faccia "Cobalt".
                    // dir è la direzione dal cavo verso il convertitore.
                    if (cobaltSide == dir.getOpposite()) {
                        maxPower = Math.max(maxPower, source.getCobaltPower(neighborState, world, neighborPos));
                    }
                }
                else{
                    maxPower = Math.max(maxPower, source.getCobaltPower(neighborState, world, neighborPos));
                }
            }
            // 3. Se è un blocco solido, controlla se è alimentato FORTEMENTE da una fonte Cobalt
            // MAI controllare world.getReceivedRedstonePower() perché includerebbe le polveri!
            else if (neighborState.isSolidBlock(world, neighborPos)) {
                maxPower = Math.max(maxPower, getStrongPowerFromNeighbors(world, neighborPos));
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
        for (Direction dir : Direction.values()) {

            BlockPos sourcePos = blockPos.offset(dir);
            BlockState sourceState = world.getBlockState(sourcePos);

            if (sourceState.getBlock() instanceof CobaltPowerSource source) {
                if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                    // Chiediamo solo la "Strong Power" (es. leva o torcia sotto il blocco)
                    maxStrong = Math.max(maxStrong, source.getStrongCobaltPower(sourceState, world, sourcePos, dir.getOpposite()));
                }
            }

            if (compatibleCobaltPowerSource(sourceState)) {
                // Chiediamo solo la "Strong Power" (es. leva o torcia sotto il blocco)
                maxStrong = Math.max(maxStrong, sourceState.getStrongRedstonePower(world, sourcePos, dir));
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

        for (Direction dir : Direction.values()) {
            if (dir == exceptDir) continue;

            BlockPos neighborPos = solidPos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // A. Controllo Componenti Meccanici (Leve, bottoni attaccati al blocco solido)
            if (CobaltWireNetwork.compatibleCobaltPowerSource(neighborState)) {
                // Se la leva sta iniettando energia forte nel blocco di pietra,
                // la torcia di cobalto attaccata ad esso deve spegnersi!
                if (neighborState.getStrongRedstonePower(world, neighborPos, dir) > 0) return true;
            }

            // B. Controllo Cavo di Cobalto
            if (neighborState.getBlock() instanceof CobaltWireBlock) {
                if (neighborState.getWeakRedstonePower(world, neighborPos, dir) > 0) return true;
                if (neighborState.getStrongRedstonePower(world, neighborPos, dir) > 0) return true;
            }
            // C. Controllo altre Sorgenti Cobalt
            else if (neighborState.getBlock() instanceof CobaltPowerSource source) {
                if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                    if (source.getStrongCobaltPower(neighborState, world, neighborPos, dir.getOpposite()) > 0) return true;
                }
            }
        }

        return false;
    }


    private void updateNeighbors(World world, CobaltWireNode node) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = node.pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // Cobalt Firewall
            if (isVanillaRedstone(neighborState)) continue;

            // Notifica il blocco vicino che l'energia è cambiata
            world.updateNeighbor(neighborPos, world.getBlockState(node.pos).getBlock(), null);

            // Se è un blocco solido, dobbiamo aggiornare anche i blocchi DOPO di lui (Strong Power)
            if (neighborState.isSolidBlock(world, neighborPos)) {
                for (Direction sideDir : Direction.values()) {
                    if (sideDir == dir.getOpposite()) continue;
                    BlockPos farPos = neighborPos.offset(sideDir);
                    if (!isVanillaRedstone(world.getBlockState(farPos))) {
                        world.updateNeighbor(farPos, world.getBlockState(node.pos).getBlock(), null);
                    }
                }
            }
        }
    }

    private CobaltWireNode getOrAddWireNode(World world, BlockPos pos) {
        CobaltNode node = nodes.computeIfAbsent(pos, p -> {
            BlockState state = world.getBlockState(p);
            if (state.getBlock() instanceof CobaltWireBlock) {
                return new CobaltWireNode(p, state);
            }
            return new CobaltNode(p, state);
        });
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