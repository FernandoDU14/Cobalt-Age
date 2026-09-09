package net.cobaltmc.cobaltage.block.cobalt;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import net.cobaltmc.cobaltage.CobaltAgeConstants;
import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltSignalSource;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
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

public class CobaltTorchBlock extends BaseTorchBlock implements SimpleWaterloggedBlock, CobaltSignalSource{

    public static final MapCodec<CobaltTorchBlock> CODEC = simpleCodec(CobaltTorchBlock::new);
    public static final BooleanProperty LIT;
    private static final Map<BlockGetter, List<Toggle>> RECENT_TOGGLES;
    public static final BooleanProperty WATERLOGGED;
    public static final long RECENT_TOGGLE_TIMER = 60L;
    public static final int MAX_RECENT_TOGGLES = 8;
    public static final int RESTART_DELAY = 160;
    private static final int UPDATE_DELAY = 2;

    public @NonNull MapCodec<? extends CobaltTorchBlock> codec() {
        return CODEC;
    }

    public CobaltTorchBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(LIT, true)).setValue(WATERLOGGED, false));
    }

    protected void onPlace(@NonNull BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull BlockState blockState2, boolean bl) {
        this.notifyNeighbors(level, blockPos, blockState);
    }

    private void notifyNeighbors(Level level, BlockPos blockPos, BlockState blockState) {
        Orientation orientation = this.randomOrientation(level, blockState);

        for(Direction direction : Direction.values()) {
            level.neighborChanged(blockPos.relative(direction), this, ExperimentalRedstoneUtils.withFront(orientation, direction));
        }

    }

    protected void onStateReplaced(@NonNull BlockState blockState, @NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, boolean bl) {
        if (!bl) {
            this.notifyNeighbors(serverLevel, blockPos, blockState);
        }

    }

    protected void onRemove(@NonNull BlockState blockState, @NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, boolean bl) {
        if (!bl) {
            this.notifyNeighbors(serverLevel, blockPos, blockState);
        }

    }

    public void animateTick(BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        if ((Boolean)state.getValue(LIT)) {
            double d = (double)pos.getX() + (double)0.5F + (random.nextDouble() - (double)0.5F) * 0.2;
            double e = (double)pos.getY() + 0.7;
            double f = (double)pos.getZ() + (double)0.5F + (random.nextDouble() - (double)0.5F) * 0.2;
            int cobaltBlue = 39423;
            DustParticleOptions cobaltDust = new DustParticleOptions(cobaltBlue, 1.0F);
            world.addParticle(cobaltDust, d, e, f, (double)0.0F, (double)0.0F, (double)0.0F);
        }

    }

    private static boolean isToggledTooFrequently(Level level, BlockPos blockPos, boolean bl) {
        List<Toggle> list = (List)RECENT_TOGGLES.computeIfAbsent(level, (blockGetter) -> Lists.newArrayList());
        if (bl) {
            list.add(new Toggle(blockPos.immutable(), level.getGameTime()));
        }

        int i = 0;

        for(Toggle toggle : list) {
            if (toggle.pos.equals(blockPos)) {
                ++i;
                if (i >= 8) {
                    return true;
                }
            }
        }

        return false;
    }

    protected void neighborChanged(BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull Block block, @Nullable Orientation orientation, boolean bl) {
        if ((Boolean)blockState.getValue(LIT) == this.hasNeighborSignal(level, blockPos, blockState) && !level.getBlockTicks().hasScheduledTick(blockPos, this)) {
            level.scheduleTick(blockPos, this, 2);
        }

    }

    protected @Nullable Orientation randomOrientation(Level level, BlockState blockState) {
        Direction front = CobaltAgeConstants.MODERN_SIGNAL_ENGINE ? Direction.WEST : null;
        return ExperimentalRedstoneUtils.initialOrientation(level, front, Direction.UP);
    }

    protected boolean isSignalSource(@NonNull BlockState blockState) {
        return true;
    }

    protected boolean hasNeighborSignal(@NonNull Level world, BlockPos pos, @NonNull BlockState state) {
        return ((SignalGetterByType)world).cobaltage$hasSignalByType(SignalType.COBALT, pos.below(), Direction.DOWN);
    }

    protected int getSignal(BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        return blockState.getValue(LIT) && Direction.UP != direction ? 15 : 0;
    }

    protected int getDirectSignal(@NonNull BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        return direction == Direction.DOWN && blockState.getValue(LIT) ? 15 : 0;
    }

    public int getDirectCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return direction == Direction.DOWN && state.getValue(LIT) ? 15 : 0;
    }

    public int getCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return state.getValue(LIT) && direction != Direction.UP ? 15 : 0;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{LIT, WATERLOGGED});
    }

    public @NonNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return (Objects.requireNonNull(super.getStateForPlacement(ctx))).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if ((Boolean)state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        BlockState newState = super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        return newState.is(this) ? (BlockState)newState.setValue(WATERLOGGED, (Boolean)state.getValue(WATERLOGGED)) : newState;
    }

    static {
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        LIT = BlockStateProperties.LIT;
        RECENT_TOGGLES = new WeakHashMap();
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