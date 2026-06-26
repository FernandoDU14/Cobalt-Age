package net.fernando.cobaltage.block;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import net.fernando.cobaltage.block.signal.cobalt.CobaltSource;
import net.fernando.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import static net.fernando.cobaltage.block.signal.SignalType.COBALT;

public class CobaltTorchBlock extends BaseTorchBlock implements SimpleWaterloggedBlock, CobaltSource {
    public static final MapCodec<CobaltTorchBlock> CODEC = simpleCodec(CobaltTorchBlock::new);
    public static final BooleanProperty LIT;
    private static final Map<BlockGetter, List<CobaltTorchBlock.Toggle>> RECENT_TOGGLES;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final long RECENT_TOGGLE_TIMER = 60; // Temporal window of registering toggles (ticks) -> 20 ticks = 1 second
    public static final int MAX_RECENT_TOGGLES = 8; // Number of filps between on/off state before burnout (1)
    public static final int RESTART_DELAY = 160; // Number of ticks to wait before trying to repower (ticks) -> 160 = 8 seconds
    private static final int UPDATE_DELAY = 2; // Number of tick delay when updated 2 ticks = 0.1 seconds = 1 redstone tick

    public @NonNull MapCodec<? extends CobaltTorchBlock> codec() {
        return CODEC;
    }

    public CobaltTorchBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true).setValue(WATERLOGGED, false));
    }

    protected void onPlace(@NonNull BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull BlockState blockState2, boolean bl) {
        this.notifyNeighbors(level, blockPos, blockState);
    }

    private void notifyNeighbors(Level level, BlockPos blockPos, BlockState blockState) {
        Orientation orientation = this.randomOrientation(level, blockState);

        for(Direction direction : Direction.values()) {
            level.updateNeighborsAt(blockPos.relative(direction), this, ExperimentalRedstoneUtils.withFront(orientation, direction));
        }

    }
    protected void affectNeighborsAfterRemoval(@NonNull BlockState blockState, @NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, boolean bl) {
        if (!bl) {
            this.notifyNeighbors(serverLevel, blockPos, blockState);
        }

    }

    public void animateTick(BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        if (state.getValue(LIT)) {
            double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double e = (double)pos.getY() + 0.7;
            double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            int cobaltBlue = (0) | (153 << 8) | 255;
            DustParticleOptions cobaltDust = new DustParticleOptions(cobaltBlue, 1.0f);
            world.addParticle(cobaltDust, d, e, f, 0.0, 0.0, 0.0);
        }
    }

    private static boolean isToggledTooFrequently(Level level, BlockPos blockPos, boolean bl) {
        List<CobaltTorchBlock.Toggle> list = RECENT_TOGGLES.computeIfAbsent(level, (blockGetter) -> Lists.newArrayList());
        if (bl) {
            list.add(new CobaltTorchBlock.Toggle(blockPos.immutable(), level.getGameTime()));
        }
        int i = 0;
        for(CobaltTorchBlock.Toggle toggle : list) {
            if (toggle.pos.equals(blockPos)) {
                ++i;
                if (i >= MAX_RECENT_TOGGLES) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void tick(@NonNull BlockState blockState, @NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, @NonNull RandomSource randomSource) {
        boolean bl = this.hasNeighborSignal(serverLevel, blockPos, blockState);
        List<CobaltTorchBlock.Toggle> list = RECENT_TOGGLES.get(serverLevel);

        while(list != null && !list.isEmpty() && serverLevel.getGameTime() - (list.get(0)).when > RECENT_TOGGLE_TIMER) {
            list.remove(0);
        }

        if (blockState.getValue(LIT)) {
            if (bl) {
                serverLevel.setBlock(blockPos, blockState.setValue(LIT, false), 3);
                if (isToggledTooFrequently(serverLevel, blockPos, true)) {
                    serverLevel.levelEvent(1502, blockPos, 0);
                    serverLevel.scheduleTick(blockPos, serverLevel.getBlockState(blockPos).getBlock(), RESTART_DELAY);
                }
            }
        } else if (!bl && !isToggledTooFrequently(serverLevel, blockPos, false)) {
            serverLevel.setBlock(blockPos, blockState.setValue(LIT, true), 3);
        }

    }

    protected void neighborChanged(BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull Block block, @Nullable Orientation orientation, boolean bl) {
        if (blockState.getValue(LIT) == this.hasNeighborSignal(level, blockPos, blockState) && !level.getBlockTicks().willTickThisTick(blockPos, this)) {
            level.scheduleTick(blockPos, this, UPDATE_DELAY);
        }
    }

    protected @Nullable Orientation randomOrientation(Level level, BlockState blockState) {
        return ExperimentalRedstoneUtils.initialOrientation(level, null, Direction.UP);
    }

    protected boolean isSignalSource(@NonNull BlockState blockState) {
        return true;
    }
    protected boolean hasNeighborSignal(@NonNull Level world, BlockPos pos, @NonNull BlockState state) {
        return ((SignalGetterByType) world).cobaltage$hasSignalByType(COBALT, pos.below(), Direction.DOWN);
    }
    protected int getSignal(BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        return blockState.getValue(LIT) && Direction.UP != direction ? 15 : 0;
    }
    protected int getDirectSignal(@NonNull BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        return (direction == Direction.DOWN && blockState.getValue(LIT)) ? 15 : 0;
    }
    @Override
    public int getDirectCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return (direction == Direction.DOWN && state.getValue(LIT)) ? 15 : 0;
    }
    @Override
    public int getCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return (state.getValue(LIT) && direction != Direction.UP)? 15 : 0;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, WATERLOGGED);
    }

    @Override
    public @NonNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        BlockState newState =  super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (newState.is(this)) {
            return newState.setValue(WATERLOGGED, state.getValue(WATERLOGGED));
        }
        return newState;
    }

    static {
        LIT = BlockStateProperties.LIT;
        RECENT_TOGGLES = new WeakHashMap<>();
    }

    public static class Toggle {
        final BlockPos pos;
        final long when;

        public Toggle(BlockPos blockPos, long l) {
            this.pos = blockPos;
            this.when = l;
        }
    }
}