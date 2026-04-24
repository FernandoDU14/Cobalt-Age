package net.fernando.cobaltrails.block;

import net.fernando.cobaltrails.block.wire.CobaltWireNetwork;
import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
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

public class CobaltConverterBlock extends Block implements Waterloggable, CobaltPowerSource {
    
    public static final EnumProperty<Direction> FACING = null;
    public static final IntProperty POWER = Properties.POWER;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    // TRUE: Flusso Cobalt -> Redstone
    // FALSE: Flusso Redstone -> Cobalt
    public static final BooleanProperty COBALT_INPUT = BooleanProperty.of("cobalt_input");

    private static final CobaltWireNetwork NETWORK_HANDLER = new CobaltWireNetwork();

    public CobaltConverterBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWER, 0)
                .with(COBALT_INPUT, true)
                .with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER, COBALT_INPUT, WATERLOGGED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
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
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    // --- DYNAMIC POWER ROUTING ---
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation orientation, boolean notify) {
        if (world.isClient()) return;

        Direction redstoneSide = state.get(FACING);
        Direction cobaltSide = redstoneSide.getOpposite();

        int currentPower = state.get(POWER);
        boolean isCobaltInput = state.get(COBALT_INPUT);

        int cobaltPower = getCobaltInputPower(world, pos, cobaltSide);
        int redstonePower = getRedstoneInputPower(world, pos, redstoneSide);

        int newPower = currentPower;
        boolean newCobaltInput = isCobaltInput;

        if (isCobaltInput) {
            // Aspettiamo energia dal lato COBALT
            if (cobaltPower > 0) {
                newPower = cobaltPower;
            } else {
                // Il Cobalt è sceso a 0.
                if (currentPower > 0) {
                    // STEP DI DEPRESSURIZZAZIONE: Spegniamo la nostra emissione PRIMA di cambiare lato.
                    // Questo impedisce al convertitore di leggere la sua stessa energia residua dalla redstone.
                    newPower = 0;
                } else {
                    // Rete scarica! Ora possiamo ascoltare il lato Redstone in totale sicurezza.
                    if (redstonePower > 0) {
                        newPower = redstonePower;
                        newCobaltInput = false; // Inversione di flusso!
                    } else {
                        newPower = 0;
                    }
                }
            }
        } else {
            // Aspettiamo energia dal lato REDSTONE
            if (redstonePower > 0) {
                newPower = redstonePower;
            } else {
                // La Redstone è scesa a 0.
                if (currentPower > 0) {
                    // STEP DI DEPRESSURIZZAZIONE
                    newPower = 0;
                } else {
                    // Rete scarica! Ascoltiamo il lato Cobalt.
                    if (cobaltPower > 0) {
                        newPower = cobaltPower;
                        newCobaltInput = true; // Inversione di flusso!
                    } else {
                        newPower = 0;
                    }
                }
            }
        }

        // Applica i cambiamenti solo se lo stato è effettivamente mutato
        if (currentPower != newPower || isCobaltInput != newCobaltInput) {
            BlockState newState = state.with(POWER, newPower).with(COBALT_INPUT, newCobaltInput);
            world.setBlockState(pos, newState, Block.NOTIFY_ALL);

            // Svegliamo la redstone Vanilla
            world.updateNeighbor(pos.offset(redstoneSide), this, null);
            // Svegliamo la rete Cobalt
            NETWORK_HANDLER.updateNetwork(world, pos.offset(cobaltSide));
        }
    }

    private int getCobaltInputPower(World world, BlockPos pos, Direction cobaltSide) {
        BlockPos inputPos = pos.offset(cobaltSide);
        BlockState inputState = world.getBlockState(inputPos);

        if (inputState.getBlock() instanceof CobaltWireBlock) {
            return inputState.get(CobaltWireBlock.POWER);
        } else if (inputState.getBlock() instanceof CobaltPowerSource source) {
            return source.getCobaltPower(inputState, world, inputPos);
        }
        return 0;
    }

    private int getRedstoneInputPower(World world, BlockPos pos, Direction redstoneSide) {
        BlockPos neighborPos = pos.offset(redstoneSide);
        // FIX IMPORTANTISSIMO: Chiediamo al blocco adiacente quanta energia spara verso la NOSTRA faccia.
        return world.getEmittedRedstonePower(neighborPos, redstoneSide.getOpposite());
    }

    // --- VANILLA REDSTONE OUTPUT ---
    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        // 'direction' è il lato da cui il vicino sta interrogando il blocco.
        if (direction.getOpposite() == state.get(FACING) && state.get(COBALT_INPUT)) {
            return state.get(POWER);
        }
        return 0;
    }

    @Override
    protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return getWeakRedstonePower(state, world, pos, direction);
    }

    // --- COBALT OUTPUT ---
    @Override
    public int getCobaltPower(BlockState state, World world, BlockPos pos) {
        // Emette verso Cobalt solo se il flusso è Redstone -> Cobalt
        if (!state.get(COBALT_INPUT)) {
            return state.get(POWER);
        }
        return 0;
    }

    @Override
    public int getStrongCobaltPower(BlockState state, World world, BlockPos pos, Direction direction) {
        if (direction == state.get(FACING).getOpposite() && !state.get(COBALT_INPUT)) {
            return state.get(POWER);
        }
        return 0;
    }
}