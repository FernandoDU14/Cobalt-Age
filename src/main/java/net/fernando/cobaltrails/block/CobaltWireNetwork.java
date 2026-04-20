package net.fernando.cobaltrails.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;
public class CobaltWireNetwork {

    private static final Queue<BlockPos> queue = new ArrayDeque<>();

    public static void schedule(World world, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        queue.clear();
        visited.clear();

        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) continue;

            update(world, pos);

            for (Direction dir : Direction.values()) {
                BlockPos next = pos.offset(dir);
                BlockState state = world.getBlockState(next);

                if (state.getBlock() instanceof CobaltWireBlock) {
                    queue.add(next);
                }
            }
        }
    }

    public static void update(World world, BlockPos origin) {

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(origin);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) continue;

            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof CobaltWireBlock)) continue;

            int newPower = CobaltWireLogic.calculate(world, pos);

            int oldPower = state.get(CobaltWireBlock.POWER);

            if (newPower != oldPower) {

                world.setBlockState(pos,
                        state.with(CobaltWireBlock.POWER, newPower),
                        Block.NOTIFY_ALL);

                // 🔥 ESSENZIALE: continua propagazione
                world.scheduleBlockTick(pos, state.getBlock(), 1);
            }

            for (Direction dir : Direction.values()) {
                BlockPos next = pos.offset(dir);
                BlockState nextState = world.getBlockState(next);

                if (nextState.getBlock() instanceof CobaltWireBlock) {
                    queue.add(next);
                }
            }
        }
    }
}