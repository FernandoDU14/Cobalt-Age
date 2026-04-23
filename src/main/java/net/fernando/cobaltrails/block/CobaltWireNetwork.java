package net.fernando.cobaltrails.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import java.util.*;

public class CobaltWireNetwork {

    // 🛡️ FIX 1: Sostituiamo il boolean statico con un ThreadLocal.
    // Questo crea una guardia separata e sicura per il Server e il Client,
    // e previene blocchi della variabile tra i vari riavvii del mondo o tra le dimensioni.
    private static final ThreadLocal<Boolean> IS_UPDATING = ThreadLocal.withInitial(() -> false);

    public static void schedule(World world, BlockPos start) {
        // 🛡️ FIX 2: Blocchiamo immediatamente l'esecuzione sul client.
        // La logica della redstone DEVE girare esclusivamente sul Server.
        if (world.isClient()) return;

        // Se il server sta già calcolando la rete, ignoriamo.
        if (IS_UPDATING.get()) return;

        IS_UPDATING.set(true);
        try {
            Queue<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();

            queue.add(start);

            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                if (!visited.add(pos)) continue;

                update(world, pos);

                for (Direction dir : Direction.values()) {
                    BlockPos next = pos.offset(dir);

                    // 🛡️ FIX 3: Evitiamo che la rete legga chunk scaricati (es. durante il caricamento del mondo)
                    // Se il chunk non è caricato, lo ignoriamo per evitare che legga "Aria" e rompa il circuito.
                    if (world.isChunkLoaded(next)) {
                        if (world.getBlockState(next).getBlock() instanceof CobaltWireBlock) {
                            queue.add(next);
                        }
                    }
                }
            }
        } finally {
            // Rilasciamo la guardia in modo sicuro
            IS_UPDATING.set(false);
        }
    }

    public static void update(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof CobaltWireBlock)) return;

        int newPower = CobaltWireLogic.calculate(world, pos);
        int oldPower = state.get(CobaltWireBlock.POWER);

        if (newPower != oldPower) {
            world.setBlockState(pos, state.with(CobaltWireBlock.POWER, newPower), Block.NOTIFY_ALL);
            updateNeighborsOfNeighbors(world, pos);
            world.scheduleBlockTick(pos, state.getBlock(), 1);
        }
    }

    private static void updateNeighborsOfNeighbors(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        world.updateNeighborsAlways(pos, block, null);
        for (Direction direction : Direction.values()) {
            world.updateNeighborsAlways(pos.offset(direction), block, null);
        }
    }
}