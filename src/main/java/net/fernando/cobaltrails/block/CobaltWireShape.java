package net.fernando.cobaltrails.block;

import net.minecraft.block.*;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

public class CobaltWireShape {

    public static BlockState getUpdatedState(BlockView world, BlockPos pos, BlockState state) {
        WireConnection north = getRenderConnection(world, pos, Direction.NORTH);
        WireConnection south = getRenderConnection(world, pos, Direction.SOUTH);
        WireConnection east = getRenderConnection(world, pos, Direction.EAST);
        WireConnection west = getRenderConnection(world, pos, Direction.WEST);

        boolean hasNorth = north.isConnected();
        boolean hasSouth = south.isConnected();
        boolean hasEast = east.isConnected();
        boolean hasWest = west.isConnected();

        // 🛑 FIX: LOGICA DI RITORNO AL DEFAULT 🛑
        if (!hasNorth && !hasSouth && !hasEast && !hasWest) {
            // Se lo stato attuale è GIÀ un puntino (tutti NONE), lo lasciamo così.
            // Questo preserva il toggle manuale e ignora i pistoni.
            if (isNotConnected(state)) {
                return state;
            }
            // Se non è un puntino e non ha vicini (es. ho appena rotto l'ultimo cavo vicino),
            // torniamo al CROSS (il nostro default di piazzamento).
            // Questo forzerà un aggiornamento del blocco e dei suoi vicini!
            return state
                    .with(CobaltWireBlock.NORTH, WireConnection.SIDE)
                    .with(CobaltWireBlock.SOUTH, WireConnection.SIDE)
                    .with(CobaltWireBlock.EAST, WireConnection.SIDE)
                    .with(CobaltWireBlock.WEST, WireConnection.SIDE);
        }

        // Se ci sono connessioni, ricalcoliamo la forma normalmente
        // (La logica delle linee automatiche va qui sotto)
        if (!hasNorth && !hasSouth) {
            if (!hasEast) east = WireConnection.SIDE;
            if (!hasWest) west = WireConnection.SIDE;
        } else if (!hasEast && !hasWest) {
            if (!hasNorth) north = WireConnection.SIDE;
            if (!hasSouth) south = WireConnection.SIDE;
        }

        return state
                .with(CobaltWireBlock.NORTH, north)
                .with(CobaltWireBlock.SOUTH, south)
                .with(CobaltWireBlock.EAST, east)
                .with(CobaltWireBlock.WEST, west);
    }

    private static boolean isNotConnected(BlockState state) {
        return state.get(CobaltWireBlock.NORTH) == WireConnection.NONE &&
                state.get(CobaltWireBlock.SOUTH) == WireConnection.NONE &&
                state.get(CobaltWireBlock.EAST) == WireConnection.NONE &&
                state.get(CobaltWireBlock.WEST) == WireConnection.NONE;
    }

    private static WireConnection getRenderConnection(BlockView world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.offset(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        BlockState stateBelowMe = world.getBlockState(pos.down());
        BlockState stateAboveNeighbor = world.getBlockState(neighborPos.up());

        // 1. CONNESSIONE ORIZZONTALE (SIDE)
        // Accetta connessioni da Cavi E Sorgenti (Torce, Repeater, ecc.)
        if (canConnectTo(neighborState, direction)) {
            return WireConnection.SIDE;
        }
        // 1.BIS: CONNESSIONE ORIZZONTALE (SIDE) PER CONNETTERSI SE CI SONO BLOCCHI CON SLAB E SOPRA WIRE
        if (stateAboveNeighbor.getBlock() instanceof CobaltWireBlock) { // <--- RESTRIZIONE
            if (neighborState.getBlock() instanceof SlabBlock) {
                    return WireConnection.SIDE;
            }
        }

        // 2. CONNESSIONE VERSO L'ALTO (UP)
        // La polvere sale SOLO se sopra il vicino c'è un'altra POLVERE.
        // Ignora Torce o altre sorgenti poste in alto.
        if (!world.getBlockState(pos.up()).isSolidBlock(world, pos.up())) {
            if (stateAboveNeighbor.getBlock() instanceof CobaltWireBlock) { // <--- RESTRIZIONE

                if (neighborState.getBlock() instanceof TransparentBlock ||
                        neighborState.isSolidBlock(world, neighborPos)) {
                    return WireConnection.UP;
                }

            }
        }

        // 3. CONNESSIONE VERSO IL BASSO (SIDE)
        // La polvere scende SOLO se sotto il vicino c'è un'altra POLVERE.
        if (!neighborState.isSolidBlock(world, neighborPos)) {
            BlockState stateBelowNeighbor = world.getBlockState(neighborPos.down());

            if (stateBelowNeighbor.getBlock() instanceof CobaltWireBlock) { // <--- RESTRIZIONE
                // --- FIX VETRO / SLAB (Vertical Diode) ---
                // Il cavo cerca di collegarsi (DA SOPRA) solo se il blocco SOTTO il cavo attuale è solido o glass.
                if (stateBelowMe.getBlock() instanceof TransparentBlock ||
                        stateBelowMe.getBlock() instanceof SlabBlock ||
                        stateBelowMe.isSolidBlock(world, pos.down())) {
                    return WireConnection.SIDE;
                }
            }
        }

        return WireConnection.NONE;
    }

    private static boolean canConnectTo(BlockState state, @Nullable Direction dir) {
        if (state.getBlock() instanceof CobaltWireBlock) return true;

        if (state.getBlock() instanceof CobaltRepeaterBlock) {
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            // Se dir è null, stiamo solo controllando se il blocco è compatibile in generale
            if (dir == null) return true;
            // La polvere si connette solo se è davanti o dietro al repeater/comparator
            return dir == facing || dir == facing.getOpposite();
        }

        return state.getBlock() instanceof CobaltPowerSource ||
                CobaltWireLogic.compatibleCobaltPowerSource(state);
    }

}