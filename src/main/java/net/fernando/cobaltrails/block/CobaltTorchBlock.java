package net.fernando.cobaltrails.block;

import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

public class CobaltTorchBlock extends RedstoneTorchBlock implements Waterloggable, CobaltPowerSource {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public CobaltTorchBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(LIT, true).with(WATERLOGGED, false));
    }


    @Override
    public int getStrongCobaltPower(BlockState state, World world, BlockPos pos, Direction direction) {
        // La torcia dà Energia Forte SOLO verso l'alto (al blocco che ha sulla testa)
        return (direction == Direction.UP && state.get(LIT)) ? 15 : 0;
    }

    @Override
    public int getCobaltPower(BlockState state, World world, BlockPos pos) {
        return state.get(LIT) ? 15 : 0;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(LIT)) {
            double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double e = (double)pos.getY() + 0.7;
            double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            // Same of Dust - Particle Section
            int cobaltBlue = (0 << 16) | (153 << 8) | 255;
            DustParticleEffect cobaltDust = new DustParticleEffect(cobaltBlue, 1.0f);

            world.addParticleClient(cobaltDust, d, e, f, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIT, WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        return super.getPlacementState(ctx).with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
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
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.offset(direction.getOpposite());
        BlockState neighborState = world.getBlockState(neighborPos);

        if (isVanillaRedstone(neighborState)) {
            return 0; // Niente energia per te, redstone rossa!
        }
        return super.getWeakRedstonePower(state, world, pos, direction);
    }

    @Override
    protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        // 1. Identifichiamo il blocco che sta ricevendo l'energia (quello sopra la torcia)
        BlockPos targetPos = pos.offset(direction.getOpposite());
        BlockState targetState = world.getBlockState(targetPos);

        // 2. Se il blocco sopra è un blocco solido (Pietra, Cobblestone, ecc.)
        if (targetState.isSolidBlock(world, targetPos)) {
            // Controlliamo i vicini del blocco di pietra!
            for (Direction side : Direction.values()) {
                // Non controlliamo la torcia stessa (sotto)
                if (side == direction) continue;

                BlockPos checkPos = targetPos.offset(side);
                BlockState checkState = world.getBlockState(checkPos);

                // Se la pietra tocca Redstone Vanilla, la torcia "spegne" la Strong Power
                // per evitare che la polvere si accenda.
                if (isVanillaRedstone(checkState)) {
                    return 0;
                }
            }
        }

        // Se non c'è redstone vanilla attorno al blocco caricato, procedi normalmente
        return super.getStrongRedstonePower(state, world, pos, direction);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return false;
    }

    private boolean isVanillaRedstone(BlockState state) {
        return state.isOf(Blocks.REDSTONE_WIRE) ||
                state.isOf(Blocks.REPEATER) ||
                state.isOf(Blocks.COMPARATOR) ||
                state.isOf(Blocks.REDSTONE_TORCH) ||
                state.isOf(Blocks.REDSTONE_WALL_TORCH) ||
                state.isOf(Blocks.REDSTONE_BLOCK);
    }

    @Override
    protected boolean shouldUnpower(World world, BlockPos pos, BlockState state) {

        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);

        BlockPos belowPos2 = belowPos.down();
        BlockState belowState2 = world.getBlockState(belowPos2);

        /*
        // 🟦 1. sorgente cobalt diretta
        if (belowState.getBlock() instanceof CobaltPowerSource source) {
            if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                return source.getCobaltPower(belowState, world, belowPos) > 0;
            }
        }
        */

        // 🟦 1. sorgente cobalt diretta
        if (belowState2.getBlock() instanceof CobaltPowerSource source) {
            if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                return source.getCobaltPower(belowState2, world, belowPos2) > 0;
            }
        }

        // ❌ NON controllare i vicini → causa flicker
        return false;
    }

}