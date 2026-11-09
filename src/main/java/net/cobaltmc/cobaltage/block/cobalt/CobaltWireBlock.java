package net.cobaltmc.cobaltage.block.cobalt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import net.cobaltmc.cobaltage.CobaltAge;
import net.cobaltmc.cobaltage.block.abstracts.WireBlock;
import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltColorUtil;
import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltSignalSource;
import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltWireShape;
import net.cobaltmc.cobaltage.block.signal.engine.legacy.DefaultWireEvaluator;
import net.cobaltmc.cobaltage.block.signal.engine.legacy.ExperimentalWireEvaluator;
import net.cobaltmc.cobaltage.block.signal.engine.legacy.WireEvaluator;
import net.cobaltmc.cobaltage.util.interfaces.mixin.IServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Map;

public class CobaltWireBlock extends WireBlock implements SimpleWaterloggedBlock, CobaltSignalSource {
    public static final MapCodec<CobaltWireBlock> CODEC = simpleCodec(CobaltWireBlock::new);
    public static final BooleanProperty WATERLOGGED;
    public static final BooleanProperty RETRACTED;
    private final WireEvaluator evaluator = new DefaultWireEvaluator(this);
    private static final VoxelShape DOT_SHAPE;
    private static final Map<Direction, VoxelShape> SHAPES_BY_DIRECTION;
    private static final VoxelShape UP_NORTH_STUB;
    private static final VoxelShape UP_SOUTH_STUB;
    private static final VoxelShape UP_EAST_STUB;
    private static final VoxelShape UP_WEST_STUB;
    private static final VoxelShape[] SHAPE_CACHE;

    public CobaltWireBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(POWER, 0)).setValue(NORTH, RedstoneSide.SIDE)).setValue(SOUTH, RedstoneSide.SIDE)).setValue(EAST, RedstoneSide.SIDE)).setValue(WEST, RedstoneSide.SIDE)).setValue(RETRACTED, false)).setValue(WATERLOGGED, false));
    }

    private static int getShapeIndex(BlockState state) {
        return ((RedstoneSide)state.getValue(NORTH)).ordinal() + ((RedstoneSide)state.getValue(SOUTH)).ordinal() * 3 + ((RedstoneSide)state.getValue(EAST)).ordinal() * 9 + ((RedstoneSide)state.getValue(WEST)).ordinal() * 27;
    }

    protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return SHAPE_CACHE[getShapeIndex(state)];
    }

    public BlockState getWireShapeState(BlockGetter world, BlockPos pos, BlockState state) {
        return CobaltWireShape.getUpdatedState(world, pos, state);
    }

    private void addPoweredParticles(Level world, RandomSource random, BlockPos pos, int colorInt, Direction direction, Direction direction2, float f, float g) {
        float h = g - f;
        if (!(random.nextFloat() > 0.2F * h)) {
            float j = f + h * random.nextFloat();
            double d = (double)pos.getX() + (double)0.5F + (double)(0.4375F * (float)direction.getStepX() + j * (float)direction2.getStepX());
            double e = (double)pos.getY() + (double)0.5F + (double)(0.4375F * (float)direction.getStepY() + j * (float)direction2.getStepY());
            double k = (double)pos.getZ() + (double)0.5F + (double)(0.4375F * (float)direction.getStepZ() + j * (float)direction2.getStepZ());
            DustParticleOptions particleEffect = new DustParticleOptions(colorInt, 1.0F);
            world.addParticle(particleEffect, d, e, k, (double)0.0F, (double)0.0F, (double)0.0F);
        }

    }

    public void animateTick(BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        int power = (Integer)state.getValue(POWER);
        if (power != 0) {
            int colorInt = CobaltColorUtil.getCobaltColor(power);

            for(Direction direction : Direction.Plane.HORIZONTAL) {
                EnumProperty<RedstoneSide> property = (EnumProperty)PROPERTY_BY_DIRECTION.get(direction);
                switch ((RedstoneSide)state.getValue(property)) {
                    case UP:
                        this.addPoweredParticles(world, random, pos, colorInt, direction, Direction.UP, -0.5F, 0.5F);
                        break;
                    case SIDE:
                        this.addPoweredParticles(world, random, pos, colorInt, Direction.DOWN, direction, 0.0F, 0.5F);
                        break;
                    case NONE:
                    default:
                        this.addPoweredParticles(world, random, pos, colorInt, Direction.DOWN, direction, 0.0F, 0.3F);
                }
            }

        }
    }

    public boolean canSurvive(@NonNull BlockState state, LevelReader world, @NonNull BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        mutable.setWithOffset(pos, Direction.DOWN);
        BlockState floorState = world.getBlockState(mutable);
        return floorState.isFaceSturdy(world, mutable, Direction.UP) || floorState.is(Blocks.HOPPER);
    }

    public @NonNull FluidState getFluidState(BlockState state) {
        return (Boolean)state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{POWER, NORTH, SOUTH, EAST, WEST, RETRACTED, WATERLOGGED});
    }

    public @NonNull RenderShape getRenderShape(@NonNull BlockState state) {
        return RenderShape.MODEL;
    }

    private void updatePowerStrengthByType(Level level, BlockPos blockPos, BlockState blockState, @Nullable Orientation orientation) {
        if (!CobaltAge.MODERN_SIGNAL_ENGINE) {
            if (useExperimentalEvaluator(level)) {
                (new ExperimentalWireEvaluator(this)).updatePowerStrengthByType(SignalType.COBALT, level, blockPos, blockState, orientation, false);
            } else {
                this.evaluator.updatePowerStrengthByType(SignalType.COBALT, level, blockPos, blockState, orientation, false);
            }
        }

    }

    public void neighborChanged(@NonNull BlockState state, Level world, @NonNull BlockPos pos, @NonNull Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!world.isClientSide() && (neighborBlock != this || !useExperimentalEvaluator(world))) {
            if (state.canSurvive(world, pos)) {
                if (CobaltAge.MODERN_SIGNAL_ENGINE) {
                    ((IServerLevel)world).cobaltage$getWireHandler().onWireUpdated(pos, state, orientation);
                } else {
                    this.updatePowerStrengthByType(world, pos, state, orientation);
                }
            } else {
                dropResources(state, world, pos);
                world.removeBlock(pos, false);
            }
        }

    }

    protected void affectNeighborsAfterRemoval(@NonNull BlockState blockState, @NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, boolean bl) {
        if (!bl) {
            for(Direction direction : Direction.values()) {
                serverLevel.updateNeighborsAt(blockPos.relative(direction), this);
            }

            if (CobaltAge.MODERN_SIGNAL_ENGINE) {
                ((IServerLevel)serverLevel).cobaltage$getWireHandler().onWireRemoved(blockPos, blockState);
            } else {
                this.updatePowerStrengthByType(serverLevel, blockPos, blockState, (Orientation)null);
            }

            this.updateAllNeighbors(serverLevel, blockPos);
            this.updateDiagonalShapes(serverLevel, blockPos);
        }

    }

    private static boolean useExperimentalEvaluator(Level level) {
        return level.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS);
    }

    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if ((Boolean)state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        if (!state.canSurvive(world, pos)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            BlockState newState = this.getWireShapeState(world, pos, state);
            return (BlockState)newState.setValue(WATERLOGGED, (Boolean)state.getValue(WATERLOGGED));
        }
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = this.getWireShapeState(world, pos, this.defaultBlockState());
        FluidState fluidState = world.getFluidState(pos);
        boolean waterlogged = fluidState.getType() == Fluids.WATER;
        return (BlockState)state.setValue(WATERLOGGED, waterlogged);
    }

    public boolean isSignalSource(@NonNull BlockState state) {
        return this.shouldSignal;
    }

    protected int getDirectSignal(@NonNull BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        return !this.shouldSignal ? 0 : this.getSignal(blockState, blockGetter, blockPos, direction);
    }

    protected int getSignal(@NonNull BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        if (this.shouldSignal && direction != Direction.DOWN) {
            int i = (Integer)blockState.getValue(POWER);
            if (i == 0) {
                return 0;
            } else {
                return direction != Direction.UP && !((RedstoneSide)blockState.getValue((Property)PROPERTY_BY_DIRECTION.get(direction.getOpposite()))).isConnected() ? 0 : i;
            }
        } else {
            return 0;
        }
    }

    public int getDirectCobaltSignal(BlockState blockState, Level world, BlockPos pos, Direction direction) {
        return !this.shouldSignal ? 0 : this.getCobaltSignal(blockState, world, pos, direction);
    }

    public int getCobaltSignal(BlockState blockState, Level world, BlockPos pos, Direction direction) {
        if (this.shouldSignal && direction != Direction.DOWN) {
            int i = (Integer)blockState.getValue(POWER);
            if (i == 0) {
                return 0;
            } else {
                return direction != Direction.UP && !((RedstoneSide)blockState.getValue((Property)PROPERTY_BY_DIRECTION.get(direction.getOpposite()))).isConnected() ? 0 : i;
            }
        } else {
            return 0;
        }
    }

    public void onPlace(@NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos, @NonNull BlockState oldState, boolean bl) {
        if (!oldState.is(state.getBlock()) && !level.isClientSide()) {
            if (CobaltAge.MODERN_SIGNAL_ENGINE) {
                ((IServerLevel)level).cobaltage$getWireHandler().onWireAdded(pos, state);
            } else {
                this.updatePowerStrengthByType(level, pos, state, (Orientation)null);
            }

            this.updateAllNeighbors(level, pos);
            this.updateDiagonalShapes(level, pos);
        }

    }

    private void updateAllNeighbors(Level world, BlockPos pos) {
        world.updateNeighborsAt(pos, this, (Orientation)null);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for(Direction dir : Direction.Plane.HORIZONTAL) {
            mutable.setWithOffset(pos, dir);
            BlockPos immutableSide = mutable.immutable();
            world.updateNeighborsAt(immutableSide, this, (Orientation)null);
            world.updateNeighborsAt(immutableSide.above(), this, (Orientation)null);
            world.updateNeighborsAt(immutableSide.below(), this, (Orientation)null);
        }

    }

    private void updateDiagonalShapes(Level world, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for(Direction dir : Direction.values()) {
            this.forceShapeUpdate(world, mutable.setWithOffset(pos, dir));
        }

        for(Direction dir : Direction.Plane.HORIZONTAL) {
            mutable.setWithOffset(pos, dir);
            this.forceShapeUpdate(world, mutable.move(Direction.UP));
            mutable.setWithOffset(pos, dir);
            this.forceShapeUpdate(world, mutable.move(Direction.DOWN));
        }

    }

    private void forceShapeUpdate(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block var5 = state.getBlock();
        if (var5 instanceof CobaltWireBlock wireBlock) {
            BlockState newState = wireBlock.getWireShapeState(world, pos, state);
            if (state != newState) {
                world.setBlock(pos, newState, 3);

                for(Direction dir : Direction.values()) {
                    world.updateNeighborsAt(pos.relative(dir), wireBlock);
                }
            }
        }

    }

    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level world, @NonNull BlockPos pos, Player player, @NonNull BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else if (!this.isCross(state) && !this.isDot(state)) {
            if (isFreeLine(world, pos)) {
                BlockState newState = (BlockState)state.cycle(RETRACTED);
                world.setBlock(pos, CobaltWireShape.getUpdatedState(world, pos, newState), 3);
                this.updatesOnShapeChange(world, pos, state, newState);
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.PASS;
            }
        } else if (this.isConnected(world, pos)) {
            return InteractionResult.PASS;
        } else {
            BlockState newState = this.isCross(state) ? this.setDot(state) : this.setCross(state);
            newState = (BlockState)newState.setValue(POWER, (Integer)state.getValue(POWER));
            world.setBlock(pos, newState, 3);
            this.updatesOnShapeChange(world, pos, state, newState);
            return InteractionResult.SUCCESS;
        }
    }

    private void updatesOnShapeChange(Level level, BlockPos blockPos, BlockState blockState, BlockState blockState2) {
        Direction front = CobaltAge.MODERN_SIGNAL_ENGINE ? Direction.WEST : null;
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, front, Direction.UP);

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockPos2 = blockPos.relative(direction);
            if (((RedstoneSide)blockState.getValue((Property)PROPERTY_BY_DIRECTION.get(direction))).isConnected() != ((RedstoneSide)blockState2.getValue((Property)PROPERTY_BY_DIRECTION.get(direction))).isConnected() && (level.getBlockState(blockPos2).isRedstoneConductor(level, blockPos2) || level.getBlockState(blockPos2).getBlock() instanceof PoweredBlock)) {
                level.updateNeighborsAtExceptFromFacing(blockPos2, blockState2.getBlock(), direction.getOpposite(), ExperimentalRedstoneUtils.withFront(orientation, direction));
            }
        }

    }

    private boolean isConnected(Level world, BlockPos pos) {
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            if (CobaltWireShape.getRenderConnection(world, pos, direction) != RedstoneSide.NONE) {
                return true;
            }
        }

        return false;
    }

    public static boolean isFreeLine(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        int i = 0;
        int j = 0;
        if (state.getValue(NORTH) != RedstoneSide.NONE) {
            ++i;
        }

        if (state.getValue(SOUTH) != RedstoneSide.NONE) {
            ++i;
        }

        if (state.getValue(EAST) != RedstoneSide.NONE) {
            ++j;
        }

        if (state.getValue(WEST) != RedstoneSide.NONE) {
            ++j;
        }

        if (i != 0 && j != 0) {
            return false;
        } else if (i > 0) {
            i = 0;
            if (CobaltWireShape.getRenderConnection(world, pos, Direction.NORTH) != RedstoneSide.NONE) {
                ++i;
            }

            if (CobaltWireShape.getRenderConnection(world, pos, Direction.SOUTH) != RedstoneSide.NONE) {
                ++i;
            }

            return i == 1;
        } else {
            j = 0;
            if (CobaltWireShape.getRenderConnection(world, pos, Direction.EAST) != RedstoneSide.NONE) {
                ++j;
            }

            if (CobaltWireShape.getRenderConnection(world, pos, Direction.WEST) != RedstoneSide.NONE) {
                ++j;
            }

            return j == 1;
        }
    }

    private boolean isDot(BlockState state) {
        return state.getValue(NORTH) == RedstoneSide.NONE && state.getValue(SOUTH) == RedstoneSide.NONE && state.getValue(EAST) == RedstoneSide.NONE && state.getValue(WEST) == RedstoneSide.NONE;
    }

    private boolean isCross(BlockState state) {
        return state.getValue(NORTH) == RedstoneSide.SIDE && state.getValue(SOUTH) == RedstoneSide.SIDE && state.getValue(EAST) == RedstoneSide.SIDE && state.getValue(WEST) == RedstoneSide.SIDE;
    }

    private BlockState setDot(BlockState state) {
        return (BlockState)((BlockState)((BlockState)((BlockState)state.setValue(NORTH, RedstoneSide.NONE)).setValue(SOUTH, RedstoneSide.NONE)).setValue(EAST, RedstoneSide.NONE)).setValue(WEST, RedstoneSide.NONE);
    }

    private BlockState setCross(BlockState state) {
        return (BlockState)((BlockState)((BlockState)((BlockState)state.setValue(NORTH, RedstoneSide.SIDE)).setValue(SOUTH, RedstoneSide.SIDE)).setValue(EAST, RedstoneSide.SIDE)).setValue(WEST, RedstoneSide.SIDE);
    }

    static {
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        RETRACTED = BooleanProperty.create("retracted");
        DOT_SHAPE = Block.box((double)3.0F, (double)0.0F, (double)3.0F, (double)13.0F, (double)1.0F, (double)13.0F);
        SHAPES_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(Direction.NORTH, Block.box((double)3.0F, (double)0.0F, (double)0.0F, (double)13.0F, (double)1.0F, (double)13.0F), Direction.SOUTH, Block.box((double)3.0F, (double)0.0F, (double)3.0F, (double)13.0F, (double)1.0F, (double)16.0F), Direction.EAST, Block.box((double)3.0F, (double)0.0F, (double)3.0F, (double)16.0F, (double)1.0F, (double)13.0F), Direction.WEST, Block.box((double)0.0F, (double)0.0F, (double)3.0F, (double)13.0F, (double)1.0F, (double)13.0F), Direction.UP, Block.box((double)3.0F, (double)0.0F, (double)3.0F, (double)13.0F, (double)16.0F, (double)13.0F)));
        UP_NORTH_STUB = Block.box((double)3.0F, (double)0.0F, (double)0.0F, (double)13.0F, (double)16.0F, (double)1.0F);
        UP_SOUTH_STUB = Block.box((double)3.0F, (double)0.0F, (double)15.0F, (double)13.0F, (double)16.0F, (double)16.0F);
        UP_EAST_STUB = Block.box((double)15.0F, (double)0.0F, (double)3.0F, (double)16.0F, (double)16.0F, (double)13.0F);
        UP_WEST_STUB = Block.box((double)0.0F, (double)0.0F, (double)3.0F, (double)1.0F, (double)16.0F, (double)13.0F);
        SHAPE_CACHE = new VoxelShape[81];

        for(RedstoneSide north : RedstoneSide.values()) {
            for(RedstoneSide south : RedstoneSide.values()) {
                for(RedstoneSide east : RedstoneSide.values()) {
                    for(RedstoneSide west : RedstoneSide.values()) {
                        VoxelShape shape = DOT_SHAPE;
                        if (north == RedstoneSide.SIDE) {
                            shape = Shapes.or(shape, (VoxelShape)SHAPES_BY_DIRECTION.get(Direction.NORTH));
                        } else if (north == RedstoneSide.UP) {
                            shape = Shapes.or(shape, (VoxelShape)SHAPES_BY_DIRECTION.get(Direction.NORTH));
                            shape = Shapes.or(shape, UP_NORTH_STUB);
                        }

                        if (south == RedstoneSide.SIDE) {
                            shape = Shapes.or(shape, (VoxelShape)SHAPES_BY_DIRECTION.get(Direction.SOUTH));
                        } else if (south == RedstoneSide.UP) {
                            shape = Shapes.or(shape, (VoxelShape)SHAPES_BY_DIRECTION.get(Direction.SOUTH));
                            shape = Shapes.or(shape, UP_SOUTH_STUB);
                        }

                        if (east == RedstoneSide.SIDE) {
                            shape = Shapes.or(shape, (VoxelShape)SHAPES_BY_DIRECTION.get(Direction.EAST));
                        } else if (east == RedstoneSide.UP) {
                            shape = Shapes.or(shape, (VoxelShape)SHAPES_BY_DIRECTION.get(Direction.EAST));
                            shape = Shapes.or(shape, UP_EAST_STUB);
                        }

                        if (west == RedstoneSide.SIDE) {
                            shape = Shapes.or(shape, (VoxelShape)SHAPES_BY_DIRECTION.get(Direction.WEST));
                        } else if (west == RedstoneSide.UP) {
                            shape = Shapes.or(shape, (VoxelShape)SHAPES_BY_DIRECTION.get(Direction.WEST));
                            shape = Shapes.or(shape, UP_WEST_STUB);
                        }

                        int index = north.ordinal() + south.ordinal() * 3 + east.ordinal() * 9 + west.ordinal() * 27;
                        SHAPE_CACHE[index] = shape.optimize();
                    }
                }
            }
        }

    }
}