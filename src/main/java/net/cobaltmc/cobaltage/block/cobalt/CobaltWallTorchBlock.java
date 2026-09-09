package net.cobaltmc.cobaltage.block.cobalt;

import com.mojang.serialization.MapCodec;
import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class CobaltWallTorchBlock extends CobaltTorchBlock {
    public static final MapCodec<CobaltWallTorchBlock> CODEC = simpleCodec(CobaltWallTorchBlock::new);
    public static final EnumProperty<Direction> FACING;
    public static final BooleanProperty LIT;
    public static final BooleanProperty WATERLOGGED;

    public CobaltWallTorchBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(LIT, true)).setValue(WATERLOGGED, false));
    }

    protected @NonNull VoxelShape getShape(@NonNull BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull CollisionContext collisionContext) {
        return WallTorchBlock.getShape(blockState);
    }

    protected boolean canSurvive(BlockState blockState, @NonNull LevelReader levelReader, @NonNull BlockPos blockPos) {
        return WallTorchBlock.canSurvive(levelReader, blockPos, (Direction)blockState.getValue(FACING));
    }

    public void animateTick(BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull RandomSource randomSource) {
        if ((Boolean)blockState.getValue(LIT)) {
            Direction direction = ((Direction)blockState.getValue(FACING)).getOpposite();
            double e = (double)blockPos.getX() + (double)0.5F + (randomSource.nextDouble() - (double)0.5F) * 0.2 + 0.27 * (double)direction.getStepX();
            double f = (double)blockPos.getY() + 0.7 + (randomSource.nextDouble() - (double)0.5F) * 0.2 + 0.22;
            double g = (double)blockPos.getZ() + (double)0.5F + (randomSource.nextDouble() - (double)0.5F) * 0.2 + 0.27 * (double)direction.getStepZ();
            int cobaltBlue = 39423;
            DustParticleOptions cobaltDust = new DustParticleOptions(cobaltBlue, 1.0F);
            level.addParticle(cobaltDust, e, f, g, (double)0.0F, (double)0.0F, (double)0.0F);
        }
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
        builder.add(new Property[]{LIT});
        builder.add(new Property[]{WATERLOGGED});
    }

    public @NonNull FluidState getFluidState(BlockState state) {
        return (Boolean)state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public BlockState getStateForPlacement(@NonNull BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state == null) {
            return null;
        } else {
            BlockState wallTorchState = Blocks.WALL_TORCH.getStateForPlacement(ctx);
            FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
            return wallTorchState == null ? null : (BlockState)((BlockState)state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER)).setValue(FACING, (Direction)wallTorchState.getValue(FACING));
        }
    }

    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if ((Boolean)state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        BlockState stateInherited = super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        return direction.getOpposite() == stateInherited.getValue(FACING) && !stateInherited.canSurvive(world, pos) ? Blocks.AIR.defaultBlockState() : stateInherited;
    }

    public int getDirectCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return direction == Direction.DOWN && (Boolean)state.getValue(LIT) ? 15 : 0;
    }

    public int getCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return (Boolean)state.getValue(LIT) && direction != state.getValue(FACING) ? 15 : 0;
    }

    protected int getSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        return (Boolean)state.getValue(LIT) && direction != state.getValue(FACING) ? 15 : 0;
    }

    protected int getDirectSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        return direction == Direction.DOWN && (Boolean)state.getValue(LIT) ? 15 : 0;
    }

    protected boolean hasNeighborSignal(@NonNull Level world, BlockPos pos, @NonNull BlockState state) {
        Direction facing = (Direction)state.getValue(FACING);
        BlockPos attachedPos = pos.relative(facing.getOpposite());
        return ((SignalGetterByType)world).cobaltage$hasSignalByType(SignalType.COBALT, attachedPos, facing.getOpposite());
    }

    protected @NonNull BlockState rotate(BlockState blockState, Rotation rotation) {
        return (BlockState)blockState.setValue(FACING, rotation.rotate((Direction)blockState.getValue(FACING)));
    }

    protected @NonNull BlockState mirror(BlockState blockState, Mirror mirror) {
        return blockState.rotate(mirror.getRotation((Direction)blockState.getValue(FACING)));
    }

    protected @Nullable Orientation randomOrientation(Level level, BlockState blockState) {
        return ExperimentalRedstoneUtils.initialOrientation(level, ((Direction)blockState.getValue(FACING)).getOpposite(), Direction.UP);
    }

    static {
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        FACING = HorizontalDirectionalBlock.FACING;
        LIT = CobaltTorchBlock.LIT;
    }
}