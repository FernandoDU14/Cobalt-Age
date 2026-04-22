package net.fernando.cobaltrails.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import java.util.*;

public class CobaltWireNetwork {
    // 🛡️ La Guardia: impedisce alla rete di chiamare se stessa all'infinito
    private static boolean isUpdating = false;

    public static void schedule(World world, BlockPos start) {
        // Se stiamo già calcolando la rete, ignoriamo le nuove chiamate.
        // Il ciclo 'while' qui sotto si occuperà comunque di aggiornare tutti i blocchi connessi.
        if (isUpdating) return;

        isUpdating = true;
        try {
            // 📦 Coda LOCALE: evita conflitti tra aggiornamenti diversi
            Queue<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();

            queue.add(start);

            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                if (!visited.add(pos)) continue;

                update(world, pos);

                for (Direction dir : Direction.values()) {
                    BlockPos next = pos.offset(dir);
                    if (world.getBlockState(next).getBlock() instanceof CobaltWireBlock) {
                        queue.add(next);
                    }
                }
            }
        } finally {
            // Fondamentale: liberiamo la guardia alla fine, anche se c'è un errore
            isUpdating = false;
        }
    }

    public static void update(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof CobaltWireBlock)) return;

        int newPower = CobaltWireLogic.calculate(world, pos);
        int oldPower = state.get(CobaltWireBlock.POWER);

        if (newPower != oldPower) {
            // NOTA: setBlockState scatena i neighborUpdate,
            // ma ora la nostra guardia 'isUpdating' li bloccherà!
            world.setBlockState(pos, state.with(CobaltWireBlock.POWER, newPower), Block.NOTIFY_ALL);

            // Notifichiamo i vicini (torce, pistoni, ecc.)
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