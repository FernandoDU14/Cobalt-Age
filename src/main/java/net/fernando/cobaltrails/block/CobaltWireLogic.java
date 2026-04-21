package net.fernando.cobaltrails.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class CobaltWireLogic {

    // Lista dei blocchi vanilla da tenere isolati
    private static boolean isVanillaRedstone(BlockState state) {
        return state.isOf(Blocks.REDSTONE_WIRE) ||
                state.isOf(Blocks.REPEATER) ||
                state.isOf(Blocks.COMPARATOR) ||
                state.isOf(Blocks.REDSTONE_TORCH) ||
                state.isOf(Blocks.REDSTONE_WALL_TORCH) ||
                state.isOf(Blocks.REDSTONE_BLOCK);
    }


    public static int calculate(World world, BlockPos pos) {
        int power = 0;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // 1. INPUT DIRETTO (Solo i 6 vicini immediati: N, S, E, W, Sopra, Sotto)
            // Qui la torcia può accendere la polvere se le è accanto.
            power = Math.max(power, getInput(world, neighborPos, neighborState));

            // 2. ENERGIA ATTRAVERSO I BLOCCHI (Strong Power)
            // Controlla se il blocco sotto la polvere è caricato "fortemente" (es. torcia sotto)
            if (neighborState.isSolidBlock(world, neighborPos)) {
                power = Math.max(power, getStrongPowerThroughBlock(world, neighborPos));
            }

            // 3. PROPAGAZIONE VERTICALE (Climbing - Salita/Discesa)
            if (dir.getAxis().isHorizontal()) {
                // Diagonale in Alto: Controlla solo se c'è un altro CAVO
                if (!world.getBlockState(pos.up()).isSolidBlock(world, pos.up())) {
                    BlockPos upPos = neighborPos.up();
                    BlockState upState = world.getBlockState(upPos);
                    if (upState.getBlock() instanceof CobaltWireBlock) { // <--- SOLO SE È UN CAVO
                        power = Math.max(power, upState.get(CobaltWireBlock.POWER));
                    }
                }

                // Diagonale in Basso: Controlla solo se c'è un altro CAVO
                if (!neighborState.isSolidBlock(world, neighborPos)) {
                    BlockPos downPos = neighborPos.down();
                    BlockState downState = world.getBlockState(downPos);
                    if (downState.getBlock() instanceof CobaltWireBlock) { // <--- SOLO SE È UN CAVO
                        power = Math.max(power, downState.get(CobaltWireBlock.POWER));
                    }
                }
            }
        }

        return Math.max(0, power - 1);
    }

    private static int getInput(World world, BlockPos pos, BlockState state) {
        // 🟦 1. Rete cobalt
        if (state.getBlock() instanceof CobaltWireBlock) {
            return state.get(CobaltWireBlock.POWER);
        }

        // 🟦 2. Sorgenti cobalt SOLO dirette
        if (state.getBlock() instanceof CobaltPowerSource source) {
            if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                return source.getCobaltPower(state, world, pos);
            }
        }

        return 0;
    }

    // 🟦 3. NUOVO: Scansiona chi sta dando "Energia Forte" al blocco solido
    private static int getStrongPowerThroughBlock(World world, BlockPos solidPos) {
        int maxStrongPower = 0;

        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = solidPos.offset(dir);
            BlockState adjacentState = world.getBlockState(adjacentPos);

            if (adjacentState.getBlock() instanceof CobaltPowerSource source) {
                if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                    // Passiamo 'dir.getOpposite()' perché chiediamo alla sorgente in che direzione spara energia.
                    // Esempio: se la torcia è SOTTO (Down) rispetto al blocco solido,
                    // chiederemo alla torcia se sta sparando energia in SU (Up).
                    maxStrongPower = Math.max(maxStrongPower, source.getStrongCobaltPower(adjacentState, world, adjacentPos, dir.getOpposite()));
                }
            }
        }
        return maxStrongPower;
    }
}