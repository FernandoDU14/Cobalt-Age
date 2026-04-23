package net.fernando.cobaltrails.block;

import net.minecraft.block.*;
import net.minecraft.state.property.Properties;
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

            // 1. INPUT DIRETTO
            int neighborPower = getInput(world, neighborPos, neighborState, dir);

            // 🛑 FIX ISSUE 2: Sottraiamo 1 SOLO se l'energia arriva da un altro cavo.
            // Se l'energia arriva da Torce, Leve, o Weighted Pressure Plates,
            // il cavo assorbe l'energia per intero senza perderne un pezzo!
            if (neighborState.getBlock() instanceof CobaltWireBlock) {
                neighborPower = Math.max(0, neighborPower - 1);
            }

            power = Math.max(power, neighborPower);

            // 2. ENERGIA ATTRAVERSO I BLOCCHI (Strong Power)
            if (neighborState.isSolidBlock(world, neighborPos)) {
                // L'energia forte passa interamente dal blocco solido al cavo
                power = Math.max(power, getStrongPowerThroughBlock(world, neighborPos));
            }

            // 3. PROPAGAZIONE VERTICALE (Climbing - Salita/Discesa)
            if (dir.getAxis().isHorizontal()) {

                // --- SALITA DEL SEGNALE (Sto cercando di prendere energia dal cavo SOPRA il mio vicino) ---
                // In Vanilla: L'energia NON scende se lo scalino è di vetro/slab.
                if (!world.getBlockState(pos.up()).isSolidBlock(world, pos.up())) {
                    BlockPos upPos = neighborPos.up();
                    BlockState upState = world.getBlockState(upPos);

                    if (upState.getBlock() instanceof CobaltWireBlock) {
                        // FIX: Controlliamo lo "scalino" (neighborState).
                        // Se è solido (Pietra, Cobble), l'energia può scendere verso di noi.
                        // Se NON è solido (Vetro, Slab), l'energia è bloccata (Diodo).
                        if (neighborState.isSolidBlock(world, neighborPos)) {
                            power = Math.max(power, upState.get(CobaltWireBlock.POWER) - 1);
                        }
                    }
                }

                // --- DISCESA DEL SEGNALE (Sto cercando di prendere energia dal cavo SOTTO il mio vicino) ---
                // In Vanilla: L'energia sale SEMPRE, anche se io (pos) sono su un blocco di vetro.
                if (!neighborState.isSolidBlock(world, neighborPos)) {
                    BlockPos downPos = neighborPos.down();
                    BlockState downState = world.getBlockState(downPos);

                    if (downState.getBlock() instanceof CobaltWireBlock) {
                        // Rimosso il controllo su 'downStateOfMe':
                        // Il segnale può sempre salire da un blocco inferiore a uno superiore
                        // se il percorso (neighborPos) è libero.
                        power = Math.max(power, downState.get(CobaltWireBlock.POWER) - 1);
                    }
                }
            }
        }

        // Restituiamo il potere ESATTO calcolato
        return power;
    }

    private static int getInput(World world, BlockPos pos, BlockState state, Direction dir) {

        if (state.getBlock() instanceof CobaltPowerSource source) {

            // --- INIZIO FIX DIREZIONALE ---
            // Se il vicino è un Repeater o un Comparator (blocchi direzionali)
            if (state.getBlock() instanceof CobaltRepeaterBlock ||
                    state.getBlock() instanceof CobaltComparatorBlock) {

                Direction facing = state.get(Properties.HORIZONTAL_FACING);

                // 'dir' è la direzione dal CAVO verso il BLOCCO.
                // Il blocco punta verso il cavo solo se la sua direzione (facing)
                // è l'opposto di 'dir'.
                // Esempio: se il blocco è a NORD del cavo (dir=NORTH),
                // deve guardare a SUD (facing=SOUTH) per alimentarlo.
                if (facing.getOpposite() != dir.getOpposite()) {
                    return 0; // È un repeater ma guarda altrove, quindi 0 energia
                }
            }
            // --- FINE FIX DIREZIONALE ---

            return source.getCobaltPower(state, world, pos);
        }

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

        // 🟦 3. Sorgenti cobalt compatibili dirette
        if (compatibleCobaltPowerSource(state)){
            return state.getWeakRedstonePower(world, pos, dir.getOpposite());
        }

        return 0;
    }

    private static int getStrongPowerThroughBlock(World world, BlockPos solidPos) {
        int maxStrongPower = 0;

        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = solidPos.offset(dir);
            BlockState adjacentState = world.getBlockState(adjacentPos);

            if (adjacentState.getBlock() instanceof CobaltPowerSource source) {
                if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                    maxStrongPower = Math.max(maxStrongPower, source.getStrongCobaltPower(adjacentState, world, adjacentPos, dir.getOpposite()));
                }
            }

            if (compatibleCobaltPowerSource(adjacentState)){
                // 🛑 FIX ISSUE 1: Invece di WeakRedstonePower, usiamo StrongRedstonePower.
                // Una leva inietta "Energia Forte" SOLO nel blocco su cui è appoggiata.
                // Chiedendo la StrongPower, ignoriamo l'energia "debole" che si disperde inutilmente
                // e che causava accensioni fantasma a distanza.
                maxStrongPower = Math.max(maxStrongPower, adjacentState.getStrongRedstonePower(world, adjacentPos, dir));
            }
        }
        return maxStrongPower;
    }

    public static int getStrongPowerReaching(World world, BlockPos solidBlockPos, Direction sideToFace) {
        int maxPower = 0;
        for (Direction dir : Direction.values()) {
            // Ignoriamo la faccia che guarda il componente
            if (dir == sideToFace.getOpposite()) continue;

            BlockPos neighborPos = solidBlockPos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof CobaltPowerSource source) {
                // Cerchiamo energia forte Cobalt che entra nel blocco
                maxPower = Math.max(maxPower, source.getStrongCobaltPower(neighborState, world, neighborPos, dir.getOpposite()));
            }
        }
        return maxPower;
    }

    public static boolean isSolidBlockPoweredByCobalt(World world, BlockPos solidPos, Direction exceptDir) {
        for (Direction dir : Direction.values()) {
            if (dir == exceptDir) continue;

            BlockPos neighborPos = solidPos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // A. Controllo Componenti Meccanici (Leve, bottoni attaccati al blocco solido)
            if (compatibleCobaltPowerSource(neighborState)) {
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


    public static boolean compatibleCobaltPowerSource(BlockState state) {
        return state.getBlock() instanceof ButtonBlock ||
                state.getBlock() instanceof PressurePlateBlock ||
                state.getBlock() instanceof WeightedPressurePlateBlock ||
                state.getBlock() instanceof SculkSensorBlock ||
                state.getBlock() instanceof TargetBlock ||
                state.getBlock() instanceof TripwireHookBlock ||
                state.getBlock() instanceof DaylightDetectorBlock ||
                state.getBlock() instanceof JukeboxBlock ||
                state.getBlock() instanceof LightningRodBlock ||
                state.getBlock() instanceof LeverBlock;
    }

}