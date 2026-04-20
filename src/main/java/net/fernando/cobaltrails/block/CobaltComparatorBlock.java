package net.fernando.cobaltrails.block;

import net.fernando.cobaltrails.block.entity.CobaltComparatorBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ComparatorMode;
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

public class CobaltComparatorBlock extends ComparatorBlock implements Waterloggable, CobaltPowerSource {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public CobaltComparatorBlock(Settings settings) {
        super(settings);
        // Impostiamo il default: non sommerso
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWERED, false)
                .with(MODE, ComparatorMode.COMPARE)
                .with(WATERLOGGED, false));
    }

    @Override
    public int getCobaltPower(BlockState state, World world, BlockPos pos) {
        return state.get(POWERED) ? 15 : 0;
    }



    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        // Fondamentale: aggiungiamo la proprietà al builder altrimenti crasha
        builder.add(FACING, POWERED, MODE, WATERLOGGED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Controlla se c'è acqua dove stiamo piazzando il blocco
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        BlockState state = super.getPlacementState(ctx);
        if (state != null) {
            return state.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
        }
        return null;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        // Se è waterlogged, mostra l'acqua, altrimenti no
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





    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CobaltComparatorBlockEntity(pos, state);
    }

    // 1. Read the signal of the block entity
    @Override
    protected int getOutputLevel(BlockView world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CobaltComparatorBlockEntity cobaltBE) {
            return cobaltBE.getOutputSignal();
        }
        return 0;
    }

    // 2. Calcola e aggiorna il segnale (Logica custom per 1.21.11)
    @Override
    protected void updatePowered(World world, BlockPos pos, BlockState state) {
        if (world.getBlockTickScheduler().isQueued(pos, this)) {
            return;
        }
        int i = this.calculateOutputSignal(world, pos, state);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        int j = blockEntity instanceof CobaltComparatorBlockEntity cobaltBE ? cobaltBE.getOutputSignal() : 0;

        if (i != j || state.get(POWERED) != i > 0) {
            // Se il segnale è cambiato, programmiamo un tick per aggiornare
            world.getBlockTickScheduler().scheduleTick(

                    new net.minecraft.world.tick.OrderedTick<>(
                            this,           // Il blocco (CobaltComparatorBlock)
                            pos,            // La posizione
                            world.getTime() + 2, // La priorità
                            net.minecraft.world.tick.TickPriority.NORMAL, // Il tempo esatto in cui deve scattare (Tempo attuale + 2 tick)
                            0L
                    )

            );

        }

    }

    // 3. Gestisce il tick programmato per cambiare effettivamente il segnale
    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int i = this.calculateOutputSignal(world, pos, state);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        int j = 0;

        if (blockEntity instanceof CobaltComparatorBlockEntity cobaltBE) {
            j = cobaltBE.getOutputSignal();
            cobaltBE.setOutputSignal(i);
        }

        if (j != i || state.get(MODE) != net.minecraft.block.enums.ComparatorMode.COMPARE) {
            boolean haPotenza = this.hasPower(world, pos, state);
            boolean eAcceso = state.get(POWERED);

            if (eAcceso && !haPotenza) {
                world.setBlockState(pos, state.with(POWERED, false), 2);
            } else if (!eAcceso && haPotenza) {
                world.setBlockState(pos, state.with(POWERED, true), 2);
            }

            // CORREZIONE QUI:
            world.updateNeighborsAlways(pos, this, (net.minecraft.world.block.WireOrientation)null);
        }
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        // 1. Controlliamo se il blocco può sopravvivere (es. se il blocco sotto viene rimosso)
        if (!state.canPlaceAt(world, pos)) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            dropStacks(state, world, pos, blockEntity);
            world.removeBlock(pos, false);
            return;
        }

        // 2. Chiamiamo la logica di aggiornamento del segnale
        // Questo metodo (ereditato da ComparatorBlock) ricalcola l'output
        this.updatePowered(world, pos, state);
    }

    private int calculateOutputSignal(World world, BlockPos pos, BlockState state) {
        // Calcola la potenza che arriva dal retro (es. da una cesta o polvere)
        int i = this.getPowerOnSides(world, pos, state);

        // Se c'è un'entità (come una cesta) direttamente dietro, legge quella
        int j = this.getPowerOnBack(world, pos, state);

        if (state.get(MODE) == net.minecraft.block.enums.ComparatorMode.SUBTRACT) {
            // Modalità Sottrazione (Torcia davanti accesa)
            return Math.max(j - i, 0);
        }
        // Modalità Comparazione (Torcia davanti spenta)
        return j >= i ? j : 0;
    }

    // Servono anche questi due piccoli metodi di supporto perché spesso sono private o final
    private int getPowerOnBack(World world, BlockPos pos, BlockState state) {
        Direction direction = state.get(FACING);
        BlockPos blockPos = pos.offset(direction);
        return this.getPowerOnSide(world, blockPos, direction);
    }

    private int getPowerOnSides(World world, BlockPos pos, BlockState state) {
        int i = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (direction == state.get(FACING)) continue;
            int j = this.getPowerOnSide(world, pos.offset(direction), direction);
            if (j > i) i = j;
        }
        return i;
    }

    private int getPowerOnSide(World world, BlockPos pos, Direction side) {
        BlockState state = world.getBlockState(pos);

        // If the neighbor emits the signal (f.i. dust, torches, leve)
        if (this.isRedstoneConductor(world, pos, state)) {
            return world.getReceivedRedstonePower(pos);
        }

        // Else it will read the signal emitted by the block
        return state.getWeakRedstonePower(world, pos, side);
    }

    // To understand if this block can conduct power
    private boolean isRedstoneConductor(World world, BlockPos pos, BlockState state) {
        return state.isSolidBlock(world, pos);
    }


    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(POWERED)) {
            // Blue Particle Logic, as we did for the Repeater and Torch
            Direction direction = state.get(FACING);
            double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double e = (double)pos.getY() + 0.4 + (random.nextDouble() - 0.5) * 0.2;
            double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;

            int cobaltBlue = (0 << 16) | (153 << 8) | 255;
            DustParticleEffect cobaltDust = new DustParticleEffect(cobaltBlue, 1.0f);

            world.addParticleClient(cobaltDust, d, e, f, 0.0, 0.0, 0.0);
        }
    }



}