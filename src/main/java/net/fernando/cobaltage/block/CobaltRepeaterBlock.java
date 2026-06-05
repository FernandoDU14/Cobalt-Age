package net.fernando.cobaltage.block;

import com.mojang.serialization.MapCodec;
import net.fernando.cobaltage.block.abstracts.CobaltDiodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;

public class CobaltRepeaterBlock extends CobaltDiodeBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CobaltRepeaterBlock> CODEC = simpleCodec(CobaltRepeaterBlock::new);
    public static final BooleanProperty LOCKED;
    public static final IntegerProperty DELAY;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public @NonNull MapCodec<CobaltRepeaterBlock> codec() {
        return CODEC;
    }

    static {
        LOCKED = BlockStateProperties.LOCKED;
        DELAY = BlockStateProperties.DELAY;
    }

    public CobaltRepeaterBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(DELAY, 1)
                .setValue(LOCKED, false)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false));
    }

    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, Player player, @NonNull BlockHitResult blockHitResult) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            level.setBlock(blockPos, blockState.cycle(DELAY), 3);
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected boolean sideInputDiodesOnly() {
        return true;
    }

    @Override
    protected int getDelay(BlockState blockState) {
        return blockState.getValue(DELAY) * 2;
    }

    @Override
    public boolean isLocked(@NonNull LevelReader levelReader, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        return super.getAlternateSignal(levelReader, blockPos, blockState) > 0;
    }

    public void animateTick(BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull RandomSource randomSource) {
        if (blockState.getValue(POWERED)) {
            Direction direction = blockState.getValue(FACING);
            double d = (double)blockPos.getX() + (double)0.5F + (randomSource.nextDouble() - (double)0.5F) * 0.2;
            double e = (double)blockPos.getY() + 0.4 + (randomSource.nextDouble() - (double)0.5F) * 0.2;
            double f = (double)blockPos.getZ() + (double)0.5F + (randomSource.nextDouble() - (double)0.5F) * 0.2;
            float g = -5.0F;
            if (randomSource.nextBoolean()) {
                g = (float)(blockState.getValue(DELAY) * 2 - 1);
            }

            g /= 16.0F;
            double h = (g * (float)direction.getStepX());
            double i = (g * (float)direction.getStepZ());
            int cobaltBlue = (0) | (153 << 8) | 255;
            DustParticleOptions cobaltDust = new DustParticleOptions(cobaltBlue, 1.0f);
            level.addParticle(cobaltDust, d + h, e, f + i, 0.0F, 0.0F, 0.0F);
        }
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DELAY, LOCKED, POWERED);
        builder.add(WATERLOGGED);
    }

    public @NonNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public @NonNull BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        BlockState state = super.getStateForPlacement(ctx);
        assert state != null;
        return state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER)
                .setValue(LOCKED, this.isLocked(ctx.getLevel(), ctx.getClickedPos(), state));
    }

    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        BlockState newState =  super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (newState.is(this)) {
            if (direction == Direction.DOWN && !this.canSurviveOn(world, neighborPos, neighborState)) {
                return Blocks.AIR.defaultBlockState();
            } else {
                return !world.isClientSide() && direction.getAxis() != state.getValue(FACING).getAxis() ? state.setValue(LOCKED, this.isLocked(world, pos, state)).setValue(WATERLOGGED, state.getValue(WATERLOGGED)) : super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
            }
        }
        return newState;
    }
}