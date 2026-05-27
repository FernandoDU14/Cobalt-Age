package net.fernando.cobaltage.block.wire;

import net.fernando.cobaltage.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.jetbrains.annotations.Nullable;

public class CobaltWireShape {

    public static BlockState getUpdatedState(BlockGetter world, BlockPos pos, BlockState state) {
        RedstoneSide north = getRenderConnection(world, pos, Direction.NORTH);
        RedstoneSide south = getRenderConnection(world, pos, Direction.SOUTH);
        RedstoneSide east = getRenderConnection(world, pos, Direction.EAST);
        RedstoneSide west = getRenderConnection(world, pos, Direction.WEST);

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
            // Se non è un puntino e non ha vicini (es, se ho appena rotto l'ultimo cavo vicino),
            // torniamo al CROSS (il nostro default di piazzamento).
            // Questo forzerà un aggiornamento del blocco e dei suoi vicini!
            return state
                    .setValue(CobaltWireBlock.NORTH, RedstoneSide.SIDE)
                    .setValue(CobaltWireBlock.SOUTH, RedstoneSide.SIDE)
                    .setValue(CobaltWireBlock.EAST, RedstoneSide.SIDE)
                    .setValue(CobaltWireBlock.WEST, RedstoneSide.SIDE);
        }

        // Se ci sono connessioni, ricalcoliamo la forma normalmente
        // (La logica delle linee automatiche va qui sotto)
        if (!hasNorth && !hasSouth) {
            if (!hasEast) east = RedstoneSide.SIDE;
            if (!hasWest) west = RedstoneSide.SIDE;
        } else if (!hasEast && !hasWest) {
            if (!hasNorth) north = RedstoneSide.SIDE;
            if (!hasSouth) south = RedstoneSide.SIDE;
        }

        return state
                .setValue(CobaltWireBlock.NORTH, north)
                .setValue(CobaltWireBlock.SOUTH, south)
                .setValue(CobaltWireBlock.EAST, east)
                .setValue(CobaltWireBlock.WEST, west);
    }

    private static boolean isNotConnected(BlockState state) {
        return state.getValue(CobaltWireBlock.NORTH) == RedstoneSide.NONE &&
                state.getValue(CobaltWireBlock.SOUTH) == RedstoneSide.NONE &&
                state.getValue(CobaltWireBlock.EAST) == RedstoneSide.NONE &&
                state.getValue(CobaltWireBlock.WEST) == RedstoneSide.NONE;
    }

    public static RedstoneSide getRenderConnection(BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        BlockState sourceState = world.getBlockState(pos);

        mutable.setWithOffset(pos, direction);
        BlockState neighborState = world.getBlockState(mutable);

        mutable.setWithOffset(pos, Direction.DOWN);
        BlockState stateBelowMe = world.getBlockState(mutable);

        mutable.setWithOffset(pos, direction).move(Direction.UP);
        BlockState stateAboveNeighbor = world.getBlockState(mutable);

        // 1. CONNESSIONE ORIZZONTALE (SIDE)
        // Accetta connessioni da Cavi E Sorgenti (Torce, Repeater, ecc.)
        if (canConnectTo(sourceState, neighborState, direction)) {
            return RedstoneSide.SIDE;
        }
        // 1.BIS: CONNESSIONE ORIZZONTALE (SIDE) PER CONNETTERSI SE CI SONO BLOCCHI CON SLAB E SOPRA WIRE
        if (stateAboveNeighbor.is(ModBlocks.COBALT_DUST)) {
            if (isSlabbyRedstoneBehaviour(neighborState)) {
                return RedstoneSide.SIDE;
            }
        }


        // 2. CONNESSIONE VERSO L'ALTO (UP) - RUN ON TOP CONNECTION
        // La polvere sale SOLO se sopra il vicino c'è un'altra POLVERE.
        mutable.setWithOffset(pos, Direction.UP);
        if (!world.getBlockState(mutable).isRedstoneConductor(world, mutable)) {
            if (stateAboveNeighbor.is(ModBlocks.COBALT_DUST)) {
                mutable.setWithOffset(pos, direction); // Ritorna alla posizione del vicino
                if (isGlassyRedstoneBehaviour(neighborState) ||
                        neighborState.isRedstoneConductor(world, mutable)) {
                    return RedstoneSide.UP;
                }
            }
        }

        // 3. CONNESSIONE VERSO IL BASSO (SIDE)
        // La polvere scende SOLO se sotto il vicino c'è un'altra POLVERE.
        mutable.setWithOffset(pos, direction);
        if (!neighborState.isRedstoneConductor(world, mutable)) {
            mutable.move(Direction.DOWN);
            BlockState stateBelowNeighbor = world.getBlockState(mutable);

            if (stateBelowNeighbor.is(ModBlocks.COBALT_DUST)) {
                mutable.setWithOffset(pos, Direction.DOWN);
                if (isSlabbyRedstoneBehaviour(stateBelowMe) ||
                        isGlassyRedstoneBehaviour(stateBelowMe) ||
                        stateBelowMe.isRedstoneConductor(world, mutable)) {
                    return RedstoneSide.SIDE;
                }
            }
        }

        return RedstoneSide.NONE;
    }

    private static boolean canConnectTo(BlockState sourceState, BlockState targetState, @Nullable Direction dir) {

        if (sourceState.is(ModBlocks.COBALT_RELAY) && targetState.is(ModBlocks.COBALT_RELAY)) {
            return false;
        }

        // Da qui in poi la tua logica originale
        if (targetState.getBlock() instanceof CobaltWireBlock) return true;

        if (targetState.getBlock() instanceof CobaltRepeaterBlock) {
            Direction facing = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (dir == null) return true;
            return dir == facing || dir == facing.getOpposite();
        }

        if(targetState.getBlock() instanceof CobaltConverterBlock){
            Direction facing = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (dir == null) return true;
            return dir == facing.getOpposite();
        }

        if (targetState.is(Blocks.OBSERVER)) return dir == targetState.getValue(ObserverBlock.FACING);

        return targetState.getBlock() instanceof CobaltPowerSource ||
                CobaltWireNetwork.compatibleCobaltPowerSource(targetState);
    }

    // Prolly to change in isGlassyCobaltBehaviour
    public static boolean isGlassyRedstoneBehaviour(BlockState state){
        return state.getBlock() instanceof TransparentBlock ||
                state.getBlock() instanceof CopperBulbBlock ||
                state.is(Blocks.GLOWSTONE) ||
                state.getBlock() instanceof TntBlock ||
                state.getBlock() instanceof IceBlock;
    }

    public static boolean isSlabbyRedstoneBehaviour(BlockState state){
        return state.getBlock() instanceof SlabBlock ||
                state.getBlock() instanceof HopperBlock ||
                state.getBlock() instanceof PistonBaseBlock ||
                state.getBlock() instanceof StairBlock ||
                state.getBlock() instanceof ScaffoldingBlock;
    }

}