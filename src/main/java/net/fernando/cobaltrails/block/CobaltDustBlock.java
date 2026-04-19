package net.fernando.cobaltrails.block;

import net.minecraft.block.*;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.random.Random;
import net.minecraft.state.StateManager;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.joml.Vector3f;
import net.minecraft.block.enums.WireConnection;

public class CobaltDustBlock extends RedstoneWireBlock implements Waterloggable {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public CobaltDustBlock(Settings settings) {
        super(settings);
        // IMPORTANTE: Non chiamare metodi complessi qui se non necessario.
        // Inizializza solo lo stato predefinito.
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(WIRE_CONNECTION_NORTH, WireConnection.NONE)
                .with(WIRE_CONNECTION_SOUTH, WireConnection.NONE)
                .with(WIRE_CONNECTION_EAST, WireConnection.NONE)
                .with(WIRE_CONNECTION_WEST, WireConnection.NONE)
                .with(POWER, 0)
                .with(WATERLOGGED, false));
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        // Aggiungiamo WATERLOGGED alle proprietà della Redstone standard
        super.appendProperties(builder);
        builder.add(WATERLOGGED);
    }

    // GESTIONE PARTICELLE: Cambiamo il colore da rosso a blu cobalto
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int power = state.get(POWER);
        if (power != 0) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                // Se la redstone è attiva, creiamo particelle Dust blu
                // Vector3f(0.2f, 0.6f, 1.0f) è un bel blu brillante
                double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
                double e = (double)pos.getY() + 0.0625;
                double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
                float g = (float)power / 15.0f;
                float h = g * 0.6f + 0.4f;
                float i = Math.max(0.0f, g * g * 0.7f - 0.5f);
                float j = Math.max(0.0f, g * g * 0.6f - 0.7f);

                int cobaltBlue = (0 << 16) | (153 << 8) | 255;
                DustParticleEffect cobaltDust = new DustParticleEffect(cobaltBlue, 1.0f);

                world.addParticleClient(cobaltDust, d, e, f, 0.0, 0.0, 0.0);
            }
        }
    }


    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (state == null) return null;
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        return state.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        if (state.get(WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        // CORREZIONE FLICKER: Prendiamo il nuovo stato calcolato dalla redstone vanilla
        // e gli iniettiamo IMMEDIATAMENTE il valore corrente di WATERLOGGED.
        BlockState newState =  super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (newState.isOf(this)) {
            return newState.with(WATERLOGGED, state.get(WATERLOGGED));
        }
        return newState;
    }
}