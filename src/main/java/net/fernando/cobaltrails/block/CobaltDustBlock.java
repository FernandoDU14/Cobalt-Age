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
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import net.minecraft.block.enums.WireConnection;

public class CobaltDustBlock extends RedstoneWireBlock implements Waterloggable {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public CobaltDustBlock(Settings settings) {
        super(settings);
        // Initialization of default dust state
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
        // Waterlogged Property of Cobalt Dust (as Block ofc, not skill issues here!)
        super.appendProperties(builder);
        builder.add(WATERLOGGED);
    }

    // Particle Spawning Section. Here we manage the redstone-like particle on Cobalt Dust (as block)
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int power = state.get(POWER);
        if (power != 0) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                // If Cobalt Dust is active, particles will spawn
                // Vector3f(0.2f, 0.6f, 1.0f) is a good blue for the cobalt
                double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
                double e = (double)pos.getY() + 0.0625;
                double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;

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
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos floorPos = pos.down();
        BlockState floorState = world.getBlockState(floorPos);

        // This will allow the placement only and only if the block under is valid and is not another Cobalt Dust
        return floorState.isSideSolidFullSquare(world, floorPos, Direction.UP) || floorState.isOf(Blocks.HOPPER);
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
        // Flicker issue fix: We take the new computed vanilla state, and we inject the waterlog property
        BlockState newState =  super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (newState.isOf(this)) {
            return newState.with(WATERLOGGED, state.get(WATERLOGGED));
        }
        return newState;
    }

}