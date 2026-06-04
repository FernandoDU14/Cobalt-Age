package net.fernando.cobaltage.block;

import com.mojang.serialization.MapCodec;
import net.fernando.cobaltage.block.blockentities.CobaltComparatorBlockEntity;
import net.fernando.cobaltage.block.wire.CobaltSignalSource;
import net.fernando.cobaltage.util.signal.SignalTypeLevelExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static net.fernando.cobaltage.util.signal.SignalType.COBALT;
import static net.minecraft.world.level.block.state.properties.ComparatorMode.COMPARE;

public class CobaltComparatorBlock extends DiodeBlock implements SimpleWaterloggedBlock, CobaltSignalSource, EntityBlock {

    public static final MapCodec<CobaltComparatorBlock> CODEC = simpleCodec(CobaltComparatorBlock::new);
    public static final EnumProperty<ComparatorMode> MODE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    static {
        MODE = BlockStateProperties.MODE_COMPARATOR;
    }

    public @NonNull MapCodec<CobaltComparatorBlock> codec() {
        return CODEC;
    }

    public CobaltComparatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(MODE, COMPARE)
                .setValue(WATERLOGGED, false));
    }

    protected int getDelay(@NonNull BlockState blockState) {
        return 2;
    }

    @Override
    public @NonNull BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        BlockState state = super.getStateForPlacement(ctx);
        return state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public @NonNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    // All rewritten vanilla:
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

    protected int getOutputSignal(BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        BlockEntity blockEntity = blockGetter.getBlockEntity(blockPos);
        return blockEntity instanceof CobaltComparatorBlockEntity ? ((CobaltComparatorBlockEntity)blockEntity).getOutputSignal() : 0;
    }

    private int calculateOutputSignal(Level level, BlockPos blockPos, BlockState blockState) {
        int i = this.getInputSignal(level, blockPos, blockState);
        if (i == 0) {
            return 0;
        } else {
            int j = this.getAlternateSignal(level, blockPos, blockState);
            if (j > i) {
                return 0;
            } else {
                return blockState.getValue(MODE) == ComparatorMode.SUBTRACT ? i - j : i;
            }
        }
    }

    protected boolean shouldTurnOn(@NonNull Level level, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        int i = this.getInputSignal(level, blockPos, blockState);
        if (i == 0) {
            return false;
        } else {
            int j = this.getAlternateSignal(level, blockPos, blockState);
            if (i > j) {
                return true;
            } else {
                return i == j && blockState.getValue(MODE) == COMPARE;
            }
        }
    }

    protected int getInputSignal(@NonNull Level level, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        int i = getCobaltInputSignal(level, blockPos, blockState);
        Direction direction = blockState.getValue(FACING);
        BlockPos blockPos2 = blockPos.relative(direction);
        BlockState blockState2 = level.getBlockState(blockPos2);
        if (blockState2.hasAnalogOutputSignal()) {
            i = blockState2.getAnalogOutputSignal(level, blockPos2, direction.getOpposite());
        } else if (i < 15 && (blockState2.isRedstoneConductor(level, blockPos2) || blockState2.getBlock() instanceof PoweredBlock)) {
            blockPos2 = blockPos2.relative(direction);
            blockState2 = level.getBlockState(blockPos2);
            ItemFrame itemFrame = getItemFrame(level, direction, blockPos2);
            int j = Math.max(itemFrame == null ? Integer.MIN_VALUE : itemFrame.getAnalogOutput(), blockState2.hasAnalogOutputSignal() ? blockState2.getAnalogOutputSignal(level, blockPos2, direction.getOpposite()) : Integer.MIN_VALUE);
            if (j != Integer.MIN_VALUE) {
                i = j;
            }
        }
        return i;
    }

    private @Nullable ItemFrame getItemFrame(Level level, Direction direction, BlockPos blockPos) {
        List<ItemFrame> list = level.getEntitiesOfClass(ItemFrame.class, new AABB(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ() + 1), (itemFrame) -> itemFrame.getDirection() == direction);
        return list.size() == 1 ? list.get(0) : null;
    }

    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, Player player, @NonNull BlockHitResult blockHitResult) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            blockState = blockState.cycle(MODE);
            float f = blockState.getValue(MODE) == ComparatorMode.SUBTRACT ? 0.55F : 0.5F;
            level.playSound(player, blockPos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, f);
            level.setBlock(blockPos, blockState, 2);
            this.refreshOutputState(level, blockPos, blockState);
            return InteractionResult.SUCCESS;
        }
    }

    protected void checkTickOnNeighbor(Level level, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        if (!level.getBlockTicks().willTickThisTick(blockPos, this)) {
            int i = this.calculateOutputSignal(level, blockPos, blockState);
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            int j = blockEntity instanceof CobaltComparatorBlockEntity ? ((CobaltComparatorBlockEntity)blockEntity).getOutputSignal() : 0;
            if (i != j || blockState.getValue(POWERED) != this.shouldTurnOn(level, blockPos, blockState)) {
                TickPriority tickPriority = this.shouldPrioritize(level, blockPos, blockState) ? TickPriority.HIGH : TickPriority.NORMAL;
                level.scheduleTick(blockPos, this, 2, tickPriority);
            }
        }
    }

    private void refreshOutputState(Level level, BlockPos blockPos, BlockState blockState) {
        int i = this.calculateOutputSignal(level, blockPos, blockState);
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        int j = 0;
        if (blockEntity instanceof CobaltComparatorBlockEntity cobaltComparatorBlockEntity) {
            j = cobaltComparatorBlockEntity.getOutputSignal();
            cobaltComparatorBlockEntity.setOutputSignal(i);
        }
        if (j != i || blockState.getValue(MODE) == COMPARE) {
            boolean bl = this.shouldTurnOn(level, blockPos, blockState);
            boolean bl2 = blockState.getValue(POWERED);
            if (bl2 && !bl) {
                level.setBlock(blockPos, blockState.setValue(POWERED, false), 2);
            } else if (!bl2 && bl) {
                level.setBlock(blockPos, blockState.setValue(POWERED, true), 2);
            }
            this.updateNeighborsInFront(level, blockPos, blockState);
        }
    }

    protected void tick(@NonNull BlockState blockState, @NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, @NonNull RandomSource randomSource) {
        this.refreshOutputState(serverLevel, blockPos, blockState);
    }

    protected boolean triggerEvent(@NonNull BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, int i, int j) {
        super.triggerEvent(blockState, level, blockPos, i, j);
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        return blockEntity != null && blockEntity.triggerEvent(i, j);
    }

    public @NonNull BlockEntity newBlockEntity(@NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        return new CobaltComparatorBlockEntity(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, MODE, WATERLOGGED);
    }

    // Fast extraction from vanilla which would've required more extensions
    protected int getCobaltInputSignal(Level level, BlockPos blockPos, BlockState blockState) {
        Direction direction = blockState.getValue(FACING);
        BlockPos blockPos2 = blockPos.relative(direction);
        int i = ((SignalTypeLevelExtensions) level).getSignalByType(COBALT, blockPos2, direction);
        if (i >= 15) {
            return i;
        } else {
            BlockState blockState2 = level.getBlockState(blockPos2);
            return Math.max(i, blockState2.is(ModBlocks.COBALT_DUST) ? blockState2.getValue(CobaltWireBlock.POWER) : 0);
        }
    }

    @Override
    protected int getAlternateSignal(@NonNull SignalGetter world, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction side1 = facing.getClockWise();
        Direction side2 = facing.getCounterClockWise();

        // Now we get the power from the sides
        return Math.max(
                ((SignalTypeLevelExtensions) world).getSignalByType(COBALT, pos.relative(side1), side1),
                ((SignalTypeLevelExtensions) world).getSignalByType(COBALT, pos.relative(side2), side2)
        );
    }

    // --- Output Signal ---
    @Override
    public int getCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        // Giving you the signal if i'm the direction of the facing respect to you.
        if (world.getBlockEntity(pos) instanceof CobaltComparatorBlockEntity be && direction == state.getValue(FACING)) {
            return be.getOutputSignal();
        }
        return 0;
    }

    @Override
    public int getDirectCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        // Same as normal signal
        return this.getCobaltSignal(state, world, pos, direction);
    }

    @Override public boolean isSignalSource(@NonNull BlockState state) { return false; }
    @Override
    protected int getSignal(@NonNull BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction.getOpposite());
        BlockState neighborState = world.getBlockState(neighborPos);
        if (isVanillaRedstone(neighborState)) {
            return 0;
        }
        return super.getSignal(state, world, pos, direction);
    }

    @Override
    protected int getDirectSignal(@NonNull BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos targetPos = pos.relative(direction.getOpposite());
        BlockState targetState = world.getBlockState(targetPos);
        if (targetState.isRedstoneConductor(world, targetPos)) {
            for (Direction side : Direction.values()) {
                if (side == direction) continue;
                BlockPos checkPos = targetPos.relative(side);
                BlockState checkState = world.getBlockState(checkPos);
                if (isVanillaRedstone(checkState)) {
                    return 0;
                }
            }
        }
        return super.getDirectSignal(state, world, pos, direction);
    }

    private static boolean isVanillaRedstone(BlockState state) {
        return state.is(Blocks.REDSTONE_WIRE) ||
                state.is(Blocks.REPEATER) ||
                state.is(Blocks.POWERED_RAIL) ||
                state.is(Blocks.ACTIVATOR_RAIL) ||
                state.is(Blocks.COMPARATOR) ||
                state.is(Blocks.REDSTONE_TORCH) ||
                state.is(Blocks.REDSTONE_WALL_TORCH);
    }
}