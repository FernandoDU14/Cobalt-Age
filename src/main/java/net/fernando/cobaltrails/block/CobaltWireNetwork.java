package net.fernando.cobaltrails.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation; // Aggiungi questo import

import java.util.*;

public class CobaltWireNetwork {
    private static final Queue<BlockPos> queue = new ArrayDeque<>();

    public static void schedule(World world, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        queue.clear();
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
    }

    public static void update(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof CobaltWireBlock)) return;

        int newPower = CobaltWireLogic.calculate(world, pos);
        int oldPower = state.get(CobaltWireBlock.POWER);

        if (newPower != oldPower) {
            world.setBlockState(pos, state.with(CobaltWireBlock.POWER, newPower), Block.NOTIFY_ALL);

            // 🔥 Notifichiamo i vicini e i vicini dei vicini
            updateNeighborsOfNeighbors(world, pos);

            world.scheduleBlockTick(pos, state.getBlock(), 1);
        }
    }

    // NUOVO METODO con supporto a WireOrientation (null per aggiornamenti standard)
    private static void updateNeighborsOfNeighbors(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();

        // Aggiorna i 6 vicini diretti
        world.updateNeighborsAlways(pos, block, null);

        // Aggiorna i vicini di ogni vicino (fondamentale per i pistoni sotto i blocchi)
        for (Direction direction : Direction.values()) {
            world.updateNeighborsAlways(pos.offset(direction), block, null);
        }
    }

}