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



    // --- LOGICA DI INPUT (DIETRO) ---
    @Override
    protected int getPower(World world, BlockPos pos, BlockState state) {
        // FACING STA PUNTANDO IN REALTA' DIETRO IL REPEATER.
        Direction outDirection = state.get(FACING);
        BlockPos outPos = pos.offset(outDirection);
        BlockState outState = world.getBlockState(outPos);

        // L'input è dietro il repeater
        Direction inDirection = outDirection.getOpposite();
        BlockPos inputPos = pos.offset(inDirection);

        BlockState inputState = world.getBlockState(inputPos);

        // 🟦 INPUT DA CAVO
        if (outState.getBlock() instanceof CobaltWireBlock) {
            return outState.get(CobaltWireBlock.POWER);
        }

        // 🟦 INPUT DA ALTRE SORGENTI (Torce, Leve, ecc.)
        if (outState.getBlock() instanceof CobaltPowerSource source) {
            return source.getCobaltPower(outState, world, outPos);
        }

        return 0;
    }

    // --- LOGICA DI BLOCCAGGIO (LATERALE) ---
    @Override
    protected int getMaxInputLevelSides(RedstoneView world, BlockPos pos, BlockState state) {
        Direction facing = state.get(FACING);
        Direction side1 = facing.rotateYClockwise();
        Direction side2 = facing.rotateYCounterclockwise();
        return Math.max(getCobaltSidePower(world, pos.offset(side1)), getCobaltSidePower(world, pos.offset(side2)));
    }

    private int getCobaltSidePower(RedstoneView world, BlockPos sidePos) {
        BlockState state = world.getBlockState(sidePos);
        // Solo Repeater/Comparatori Cobalt possono bloccare un Repeater Cobalt
        if (state.getBlock() instanceof CobaltRepeaterBlock || state.getBlock() instanceof CobaltComparatorBlock) {
            return ((CobaltPowerSource)state.getBlock()).getCobaltPower(state, (World)world, sidePos);
        }
        return 0;
    }

    @Override
    public int getCobaltPower(BlockState state, World world, BlockPos pos) {
        // Il repeater emette energia SOLO se è acceso (POWERED)
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongCobaltPower(BlockState state, World world, BlockPos pos, Direction direction) {
        // direction è la faccia del blocco adiacente che viene colpita.
        // Il repeater emette "Strong Power" solo dalla sua faccia anteriore.
        return (state.get(FACING).getOpposite() == direction && state.get(POWERED)) ? 15 : 0;
    }

    // --- ISOLAMENTO TOTALE ---
    @Override public boolean emitsRedstonePower(BlockState state) { return false; }

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

    // Lista dei blocchi vanilla da tenere isolati
    private static boolean isVanillaRedstone(BlockState state) {
        return state.isOf(Blocks.REDSTONE_WIRE) ||
                state.isOf(Blocks.REPEATER) ||
                state.isOf(Blocks.COMPARATOR) ||
                state.isOf(Blocks.REDSTONE_TORCH) ||
                state.isOf(Blocks.REDSTONE_WALL_TORCH) ||
                state.isOf(Blocks.REDSTONE_BLOCK);
    }
}