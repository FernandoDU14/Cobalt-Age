package net.fernando.cobaltage.block;

import net.fernando.cobaltage.block.wire.CobaltPowerSource;
import net.fernando.cobaltage.util.SignalTypeLevelExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.NonNull;

import static net.fernando.cobaltage.util.SignalType.COBALT;

public class CobaltWallTorchBlock extends RedstoneWallTorchBlock implements SimpleWaterloggedBlock, CobaltPowerSource {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public CobaltWallTorchBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, true)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public int getDirectCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        // La torcia dà Energia Forte SOLO verso l'alto (al blocco che ha sulla testa)
        return (direction == Direction.UP && state.getValue(LIT)) ? 15 : 0;
    }

    @Override
    public int getCobaltSignal(BlockState state, Level world, BlockPos pos) {
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override
    public void animateTick(BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        if (state.getValue(LIT)) {
            double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double e = (double)pos.getY() + 0.7;
            double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            // Usiamo Dust blu invece di quella rossa
            int cobaltBlue = (0) | (153 << 8) | 255;
            DustParticleOptions cobaltDust = new DustParticleOptions(cobaltBlue, 1.0f);

            world.addParticle(cobaltDust, d, e, f, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
        builder.add(WATERLOGGED);
    }

    @Override
    public @NonNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForPlacement(@NonNull BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state == null) return null;

        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        // Flicker issue fix: We take the new computed vanilla state, and we inject the waterlog property
        BlockState newState =  super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (newState.is(this)) {
            return newState.setValue(WATERLOGGED, state.getValue(WATERLOGGED));
        }
        return newState;
    }


    @Override
    protected int getSignal(@NonNull BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction.getOpposite());
        BlockState neighborState = world.getBlockState(neighborPos);

        if (isVanillaRedstone(neighborState)) {
            return 0; // Niente energia per te, redstone rossa!
        }
        return super.getSignal(state, world, pos, direction);
    }

    @Override
    protected int getDirectSignal(@NonNull BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        // 1. Identifichiamo il blocco che sta ricevendo l'energia (quello sopra la torcia)
        BlockPos targetPos = pos.relative(direction.getOpposite());
        BlockState targetState = world.getBlockState(targetPos);

        // 2. Se il blocco sopra è un blocco solido (Pietra, Cobblestone, ecc.)
        if (targetState.isRedstoneConductor(world, targetPos)) {
            // Controlliamo i vicini del blocco di pietra!
            for (Direction side : Direction.values()) {
                // Non controlliamo la torcia stessa (sotto)
                if (side == direction) continue;

                BlockPos checkPos = targetPos.relative(side);
                BlockState checkState = world.getBlockState(checkPos);

                // Se la pietra tocca Redstone Vanilla, la torcia "spegne" la Strong Power
                // per evitare che la polvere si accenda.
                if (isVanillaRedstone(checkState)) {
                    return 0;
                }
            }
        }

        // Se non c'è redstone vanilla attorno al blocco caricato, procedi normalmente
        return super.getDirectSignal(state, world, pos, direction);
    }

    @Override
    public boolean isSignalSource(@NonNull BlockState state) {
        return false;
    }

    private boolean isVanillaRedstone(BlockState state) {
        // Here we need REDSTONE BLOCK
        return state.is(Blocks.REDSTONE_WIRE) ||
                state.is(Blocks.REPEATER) ||
                state.is(Blocks.COMPARATOR) ||
                state.is(Blocks.REDSTONE_TORCH) ||
                state.is(Blocks.REDSTONE_WALL_TORCH) ||
                state.is(Blocks.REDSTONE_BLOCK);
    }

    @Override
    protected boolean hasNeighborSignal(@NonNull Level world, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos attachedPos = pos.relative(facing.getOpposite());
        return ((SignalTypeLevelExtensions) world).hasSignalByType(COBALT, attachedPos, facing);
    }


}