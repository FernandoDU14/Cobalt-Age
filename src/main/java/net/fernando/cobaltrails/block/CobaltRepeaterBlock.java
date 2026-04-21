package net.fernando.cobaltrails.block;

import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

public class CobaltRepeaterBlock extends RepeaterBlock implements Waterloggable, CobaltPowerSource {
    public CobaltRepeaterBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(DELAY, 1)
                .with(LOCKED, false)
                .with(POWERED, false)
                .with(WATERLOGGED, false)); // Aggiungi questo
    }

    @Override
    public int getStrongCobaltPower(BlockState state, World world, BlockPos pos, Direction direction) {
        // Il ripetitore dà Energia Forte verso la direzione in cui sta puntando
        return (state.get(FACING) == direction && state.get(POWERED)) ? 15 : 0;
    }


    @Override
    public int getCobaltPower(BlockState state, World world, BlockPos pos) {
        return state.get(POWERED) ? 15 : 0;
    }

    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(POWERED)) {
            // Cobalt Particles like the one we did in CobaltDustBlock.java
            Direction direction = state.get(FACING);
            double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double e = (double)pos.getY() + 0.4 + (random.nextDouble() - 0.5) * 0.2;
            double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;

            int cobaltBlue = (0 << 16) | (153 << 8) | 255;
            DustParticleEffect cobaltDust = new DustParticleEffect(cobaltBlue, 1.0f);

            world.addParticleClient(cobaltDust, d, e, f, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, DELAY, LOCKED, POWERED); // All properties we have in the .json file.
        builder.add(WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        BlockState state = super.getPlacementState(ctx);
        if (state != null) {
            return state.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
        }
        return null;
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



    @Override
    protected int getPower(World world, BlockPos pos, BlockState state) {

        Direction direction = state.get(FACING);
        BlockPos inputPos = pos.offset(direction);

        BlockState inputState = world.getBlockState(inputPos);

        // 🟦 SOLO COBALT
        if (inputState.getBlock() instanceof CobaltPowerSource source) {
            if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                return source.getCobaltPower(inputState, world, inputPos);
            }
        }

        // 🟦 COBALT WIRE
        if (inputState.getBlock() instanceof CobaltWireBlock) {
            return inputState.get(CobaltWireBlock.POWER);
        }

        // ❌ IGNORA COMPLETAMENTE REDSTONE
        return 0;
    }

    @Override
    protected int getMaxInputLevelSides(RedstoneView world, BlockPos pos, BlockState state) {
        return 0; // 🔥 blocca input laterali vanilla
    }


}