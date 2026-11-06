package net.cobaltmc.cobaltage.block.cobalt;

import com.mojang.serialization.MapCodec;
import net.cobaltmc.cobaltage.block.abstracts.CobaltDiodeBlock;
import net.cobaltmc.cobaltage.block.entity.CobaltComparatorBlockEntity;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class CobaltComparatorBlock extends CobaltDiodeBlock implements EntityBlock, SimpleWaterloggedBlock{

    public static final MapCodec<CobaltComparatorBlock> CODEC = simpleCodec(CobaltComparatorBlock::new);
    public static final EnumProperty<ComparatorMode> MODE;
    public static final BooleanProperty WATERLOGGED;
    private static final int UPDATE_DELAY = 2;

    public @NonNull MapCodec<CobaltComparatorBlock> codec() {
        return CODEC;
    }

    public CobaltComparatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition
                .any()).setValue(FACING, Direction.NORTH)).setValue(POWERED, false))
                .setValue(MODE, ComparatorMode.COMPARE)).setValue(WATERLOGGED, false));
    }

    protected int getDelay(@NonNull BlockState blockState) {
        return 2;
    }

    public @NonNull BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        BlockState state = super.getStateForPlacement(ctx);

        assert state != null;

        return (BlockState)state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    public @NonNull FluidState getFluidState(BlockState state) {
        return (Boolean)state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if ((Boolean)state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        BlockState newState = super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (!newState.is(this)) {
            return newState;
        } else {
            return direction == Direction.DOWN && !this.canSurviveOn(world, neighborPos, neighborState) ? Blocks.AIR.defaultBlockState() : (BlockState)newState.setValue(WATERLOGGED, (Boolean)state.getValue(WATERLOGGED));
        }
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
                return i == j && blockState.getValue(MODE) == ComparatorMode.COMPARE;
            }
        }
    }

    protected int getInputSignal(@NonNull Level level, BlockPos blockPos, BlockState blockState) {
        // 1. Get standard redstone power from the rear block
        int i = super.getInputSignal(level, blockPos, blockState);
        Direction direction = blockState.getValue(FACING);
        BlockPos blockPos2 = blockPos.relative(direction);
        BlockState blockState2 = level.getBlockState(blockPos2);

        // 2. Check if the block directly behind can provide an analog signal (e.g., Lecterns, Chests, Composter, etc.)
        if (blockState2.hasAnalogOutputSignal()) {
            i = blockState2.getAnalogOutputSignal(level, blockPos2, direction.getOpposite());
        }
        // 3. Check through a solid conductor block if necessary
        else if (i < 15 && blockState2.isRedstoneConductor(level, blockPos2)) {
            blockPos2 = blockPos2.relative(direction);
            blockState2 = level.getBlockState(blockPos2);
            ItemFrame itemFrame = this.getItemFrame(level, direction, blockPos2);
            int j = Math.max(itemFrame == null ? Integer.MIN_VALUE : itemFrame.getAnalogOutput(), blockState2.hasAnalogOutputSignal() ? blockState2.getAnalogOutputSignal(level, blockPos2, direction.getOpposite()) : Integer.MIN_VALUE);
            if (j != Integer.MIN_VALUE) {
                i = j;
            }
        }

        return i;
    }

    private @Nullable ItemFrame getItemFrame(Level level, Direction direction, BlockPos blockPos) {
        List<ItemFrame> list = level.getEntitiesOfClass(ItemFrame.class, new AABB((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), (double)(blockPos.getX() + 1), (double)(blockPos.getY() + 1), (double)(blockPos.getZ() + 1)), (itemFrame) -> itemFrame != null && itemFrame.getDirection() == direction);
        return list.size() == 1 ? (ItemFrame)list.get(0) : null;
    }

    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, Player player, @NonNull BlockHitResult blockHitResult) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            blockState = (BlockState)blockState.cycle(MODE);
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
            if (i != j || (Boolean)blockState.getValue(POWERED) != this.shouldTurnOn(level, blockPos, blockState)) {
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

        if (j != i || blockState.getValue(MODE) == ComparatorMode.COMPARE) {
            boolean bl = this.shouldTurnOn(level, blockPos, blockState);
            boolean bl2 = (Boolean)blockState.getValue(POWERED);
            if (bl2 && !bl) {
                level.setBlock(blockPos, (BlockState)blockState.setValue(POWERED, false), 2);
            } else if (!bl2 && bl) {
                level.setBlock(blockPos, (BlockState)blockState.setValue(POWERED, true), 2);
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

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, POWERED, MODE, WATERLOGGED});
    }

    static {
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        MODE = BlockStateProperties.MODE_COMPARATOR;
    }
}