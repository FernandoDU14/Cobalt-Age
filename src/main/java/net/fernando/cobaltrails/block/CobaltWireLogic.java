package net.fernando.cobaltrails.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class CobaltWireLogic {

    public static int calculate(World world, BlockPos pos) {

        int power = 0;

        // 🔥 controlla tutti i vicini (rete + input)
        for (Direction dir : Direction.values()) {

            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            power = Math.max(power, getInput(world, neighborPos, neighborState));
        }

        // 🔥 decay stile redstone vanilla
        return Math.max(0, power - 1);
    }

    private static int getInput(World world, BlockPos pos, BlockState state) {

        // 🟦 1. rete cobalt
        if (state.getBlock() instanceof CobaltWireBlock) {
            return state.get(CobaltWireBlock.POWER);
        }

        // 🟦 2. sorgenti cobalt SOLO
        if (state.getBlock() instanceof CobaltPowerSource source) {

            if (source.getSignalType() != CobaltPowerSource.CobaltSignalType.COBALT) {
                return 0; // 🔥 IGNORA REDSTONE MASCHERATA
            }

            return source.getCobaltPower(state, world, pos);
        }

        // ❌ tutto il resto ignorato
        return 0;
    }
}