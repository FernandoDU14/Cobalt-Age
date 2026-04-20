package net.fernando.cobaltrails.block;

import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
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
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.tick.ScheduledTickView;
import net.minecraft.block.enums.WireConnection;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

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
        BlockPos neighborPos = pos.offset(direction.getOpposite());
        BlockState neighborState = world.getBlockState(neighborPos);

        if (isVanillaRedstone(neighborState)) {
            return 0;
        }
        return super.getStrongRedstonePower(state, world, pos, direction);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        if (state.get(WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        // Usiamo la logica base di RedstoneWireBlock.
        // Non chiamare MAI filterConnections o setBlockState qui!
        BlockState newState = super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);

        if (newState.isOf(this)) {
            return newState.with(WATERLOGGED, state.get(WATERLOGGED));
        }
        return newState;
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable net.minecraft.world.block.WireOrientation wireOrientation, boolean notify) {
        // 1. Lasciamo che Vanilla faccia i suoi calcoli e magari la accenda per sbaglio
        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);

        // 2. Facciamo scattare il nostro scudo per neutralizzare l'energia illegale
        this.isolatePower(world, pos);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);

        // Stessa cosa: controlliamo appena il blocco viene piazzato a terra
        this.isolatePower(world, pos);
    }

    // Devi filtrare i fili anche quando piazzi il blocco la prima volta!
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (state != null) {
            state = state.with(WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER);
        }
        return state;
    }


    private boolean isVanillaRedstone(BlockState state) {
        return state.isOf(Blocks.REDSTONE_WIRE) ||
                state.isOf(Blocks.REPEATER) ||
                state.isOf(Blocks.COMPARATOR) ||
                state.isOf(Blocks.REDSTONE_TORCH) ||
                state.isOf(Blocks.REDSTONE_WALL_TORCH) ||
                state.isOf(Blocks.REDSTONE_BLOCK);
    }

    private void isolatePower(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        // Se è già spenta, non dobbiamo preoccuparci di nulla
        if (!state.contains(POWER) || state.get(POWER) == 0) return;

        boolean hasValidSource = false;

        // Controlliamo in tutte le direzioni (Nord, Sud, Est, Ovest, Su, Giù)
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            // Se c'è Redstone Vanilla vicina, la ignoriamo come fonte valida
            if (isVanillaRedstone(neighborState)) continue;

            // Fonte valida 1: Altra polvere di cobalto accesa
            if (neighborState.isOf(this) && neighborState.get(POWER) > 0) {
                hasValidSource = true;
                break;
            }

            // Fonte valida 2: Un blocco che emette energia (es. Leve, Pulsanti, Blocchi solidi alimentati)
            // Usiamo dir.getOpposite() perché vogliamo l'energia emessa DAL vicino VERSO di noi
            if (world.getEmittedRedstonePower(neighborPos, dir.getOpposite()) > 0) {
                hasValidSource = true;
                break;
            }
        }

        // Se la polvere risulta accesa, ma non ha nessuna "Fonte Valida" attorno a sé...
        // Significa che la Redstone Vanilla l'ha accesa di nascosto! Spegniamo tutto.
        if (!hasValidSource) {
            world.setBlockState(pos, state.with(POWER, 0), Block.NOTIFY_ALL);
        }
    }

}