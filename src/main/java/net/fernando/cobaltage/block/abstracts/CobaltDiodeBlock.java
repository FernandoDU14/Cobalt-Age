package net.fernando.cobaltage.block.abstracts;

import com.mojang.serialization.MapCodec;
import net.fernando.cobaltage.block.CobaltWireBlock;
import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.block.signal.cobalt.CobaltSource;
import net.fernando.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static net.fernando.cobaltage.block.signal.SignalType.COBALT;

public abstract class CobaltDiodeBlock extends HorizontalDirectionalBlock implements CobaltSource {
    public static final BooleanProperty POWERED;
    private static final VoxelShape SHAPE;

    static {
        POWERED = BlockStateProperties.POWERED;
        SHAPE = Block.column(16.0F, 0.0F, 2.0F);
    }

    protected abstract @NonNull MapCodec<? extends CobaltDiodeBlock> codec();

    protected CobaltDiodeBlock(Properties properties) {
        super(properties);
    }

    protected @NonNull VoxelShape getShape(@NonNull BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull CollisionContext collisionContext) {
        return SHAPE;
    }

    protected boolean canSurvive(@NonNull BlockState blockState, @NonNull LevelReader levelReader, BlockPos blockPos) {
        BlockPos blockPos2 = blockPos.below();
        return this.canSurviveOn(levelReader, blockPos2, levelReader.getBlockState(blockPos2));
    }

    protected boolean canSurviveOn(LevelReader levelReader, BlockPos blockPos, BlockState blockState) {
        return blockState.isFaceSturdy(levelReader, blockPos, Direction.UP, SupportType.RIGID);
    }

    protected void tick(@NonNull BlockState blockState, @NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, @NonNull RandomSource randomSource) {
        if (!this.isLocked(serverLevel, blockPos, blockState)) {
            boolean bl = blockState.getValue(POWERED);
            boolean bl2 = this.shouldTurnOn(serverLevel, blockPos, blockState);
            if (bl && !bl2) {
                serverLevel.setBlock(blockPos, blockState.setValue(POWERED, false), 2);
            } else if (!bl) {
                serverLevel.setBlock(blockPos, blockState.setValue(POWERED, true), 2);
                if (!bl2) {
                    serverLevel.scheduleTick(blockPos, this, this.getDelay(blockState), TickPriority.VERY_HIGH);
                }
            }

        }
    }

    protected int getDirectSignal(BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        return blockState.getSignal(blockGetter, blockPos, direction);
    }

    protected int getSignal(BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        if (!(Boolean)blockState.getValue(POWERED)) {
            return 0;
        } else {
            return blockState.getValue(FACING) == direction ? this.getOutputSignal(blockGetter, blockPos, blockState) : 0;
        }
    }


    protected void neighborChanged(BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull Block block, @Nullable Orientation orientation, boolean bl) {
        if (blockState.canSurvive(level, blockPos)) {
            this.checkTickOnNeighbor(level, blockPos, blockState);
        } else {
            BlockEntity blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(blockPos) : null;
            dropResources(blockState, level, blockPos, blockEntity);
            level.removeBlock(blockPos, false);

            for(Direction direction : Direction.values()) {
                level.updateNeighborsAt(blockPos.relative(direction), this);
            }

        }
    }

    protected void checkTickOnNeighbor(Level level, BlockPos blockPos, BlockState blockState) {
        if (!this.isLocked(level, blockPos, blockState)) {
            boolean bl = blockState.getValue(POWERED);
            boolean bl2 = this.shouldTurnOn(level, blockPos, blockState);
            if (bl != bl2 && !level.getBlockTicks().willTickThisTick(blockPos, this)) {
                TickPriority tickPriority = TickPriority.HIGH;
                if (this.shouldPrioritize(level, blockPos, blockState)) {
                    tickPriority = TickPriority.EXTREMELY_HIGH;
                } else if (bl) {
                    tickPriority = TickPriority.VERY_HIGH;
                }

                level.scheduleTick(blockPos, this, this.getDelay(blockState), tickPriority);
            }

        }
    }

    public boolean isLocked(LevelReader levelReader, BlockPos blockPos, BlockState blockState) {
        return false;
    }

    protected boolean shouldTurnOn(Level level, BlockPos blockPos, BlockState blockState) {
        return this.getInputSignal(level, blockPos, blockState) > 0;
    }

    @Override
    public int getDirectCobaltSignal(BlockState blockState, Level world, BlockPos blockPos, Direction direction) {
        return this.getCobaltSignal(blockState, world, blockPos, direction);
    }
    @Override
    public int getCobaltSignal(BlockState blockState, Level world, BlockPos blockPos, Direction direction) {
        if (!blockState.getValue(POWERED)) {
            return 0;
        } else {
            return blockState.getValue(FACING) == direction ? this.getOutputSignal(world, blockPos, blockState) : 0;
        }
    }

    protected int getInputSignal(@NonNull Level level, BlockPos blockPos, BlockState blockState) {
        Direction direction = blockState.getValue(FACING);
        BlockPos rearPos = blockPos.relative(direction);
        int i = ((SignalGetterByType) level).cobaltage$getSignalByType(COBALT, rearPos, direction);
        if (i >= 15) {
            return i;
        } else {
            BlockState rearState = level.getBlockState(rearPos);
            return Math.max(i, rearState.is(ModBlocks.COBALT_DUST) ? rearState.getValue(CobaltWireBlock.POWER) : 0);
        }
    }

    protected int getAlternateSignal(@NonNull SignalGetter signalGetter, BlockPos blockPos, BlockState blockState) {
        Direction direction = blockState.getValue(FACING);
        Direction direction2 = direction.getClockWise();
        Direction direction3 = direction.getCounterClockWise();
        boolean bl = this.sideInputDiodesOnly();
        return Math.max(((SignalGetterByType) signalGetter).cobaltage$getControlInputSignalByType(COBALT, blockPos.relative(direction2), direction2, bl), ((SignalGetterByType) signalGetter).cobaltage$getControlInputSignalByType(COBALT, blockPos.relative(direction3), direction3, bl));
    }

    protected boolean isSignalSource(@NonNull BlockState blockState) {
        return true;
    }

    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return this.defaultBlockState().setValue(FACING, blockPlaceContext.getHorizontalDirection().getOpposite());
    }

    public void setPlacedBy(@NonNull Level level, @NonNull BlockPos blockPos, @NonNull BlockState blockState, @Nullable LivingEntity livingEntity, @NonNull ItemStack itemStack) {
        if (this.shouldTurnOn(level, blockPos, blockState)) {
            level.scheduleTick(blockPos, this, 1);
        }

    }

    protected void onPlace(@NonNull BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull BlockState blockState2, boolean bl) {
        this.updateNeighborsInFront(level, blockPos, blockState);
    }

    protected void affectNeighborsAfterRemoval(@NonNull BlockState blockState, @NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, boolean bl) {
        if (!bl) {
            this.updateNeighborsInFront(serverLevel, blockPos, blockState);
        }
    }

    protected void updateNeighborsInFront(Level level, BlockPos blockPos, BlockState blockState) {
        Direction direction = blockState.getValue(FACING);
        BlockPos blockPos2 = blockPos.relative(direction.getOpposite());
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, direction.getOpposite(), Direction.UP);
        level.neighborChanged(blockPos2, this, orientation);
        level.updateNeighborsAtExceptFromFacing(blockPos2, this, direction, orientation);
    }

    protected boolean sideInputDiodesOnly() {
        return false;
    }

    protected int getOutputSignal(BlockGetter blockGetter, BlockPos blockPos, BlockState blockState) {
        return 15;
    }

    public static boolean isDiode(BlockState blockState) {
        return blockState.getBlock() instanceof CobaltDiodeBlock;
    }

    public boolean shouldPrioritize(BlockGetter blockGetter, BlockPos blockPos, BlockState blockState) {
        Direction direction = (blockState.getValue(FACING)).getOpposite();
        BlockState blockState2 = blockGetter.getBlockState(blockPos.relative(direction));
        return isDiode(blockState2) && blockState2.getValue(FACING) != direction;
    }

    protected abstract int getDelay(BlockState blockState);

}
