package net.fernando.cobaltage.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import net.fernando.cobaltage.CobaltAge;
import net.fernando.cobaltage.block.signal.cobalt.CobaltSource;
import net.fernando.cobaltage.util.interfaces.mixin.IServerLevel;
import net.fernando.cobaltage.block.signal.cobalt.CobaltWireShape;
import net.fernando.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.fernando.cobaltage.block.signal.engine.legacy.CobaltWireEvaluator;
import net.fernando.cobaltage.block.signal.engine.legacy.DefaultCobaltWireEvaluator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static net.fernando.cobaltage.block.signal.SignalType.COBALT;

public class CobaltWireBlock extends Block  implements SimpleWaterloggedBlock, CobaltSource {
    public static final MapCodec<CobaltWireBlock> CODEC = simpleCodec(CobaltWireBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final BooleanProperty RETRACTED = BooleanProperty.create("retracted");
    private final CobaltWireEvaluator evaluator = new DefaultCobaltWireEvaluator(this);
    protected boolean shouldSignal = true;

    public CobaltWireBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any()
                .setValue(POWER, 0)
                .setValue(NORTH, RedstoneSide.SIDE)
                .setValue(SOUTH, RedstoneSide.SIDE)
                .setValue(EAST, RedstoneSide.SIDE)
                .setValue(WEST, RedstoneSide.SIDE)
                .setValue(RETRACTED, false)
                .setValue(WATERLOGGED, false));
    }
    
    private static final VoxelShape DOT_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 13.0);
    private static final Map<Direction, VoxelShape> SHAPES_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, Block.box(3.0, 0.0, 0.0, 13.0, 1.0, 13.0),
            Direction.SOUTH, Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 16.0),
            Direction.EAST,  Block.box(3.0, 0.0, 3.0, 16.0, 1.0, 13.0),
            Direction.WEST,  Block.box(0.0, 0.0, 3.0, 13.0, 1.0, 13.0),
            Direction.UP,    Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0)
    ));

    private static final VoxelShape UP_NORTH_STUB = Block.box(3.0, 0.0, 0.0, 13.0, 16.0, 1.0);
    private static final VoxelShape UP_SOUTH_STUB = Block.box(3.0, 0.0, 15.0, 13.0, 16.0, 16.0);
    private static final VoxelShape UP_EAST_STUB = Block.box(15.0, 0.0, 3.0, 16.0, 16.0, 13.0);
    private static final VoxelShape UP_WEST_STUB = Block.box(0.0, 0.0, 3.0, 1.0, 16.0, 13.0);

    private static final VoxelShape[] SHAPE_CACHE = new VoxelShape[81];

    static {
        for (RedstoneSide north : RedstoneSide.values()) {
            for (RedstoneSide south : RedstoneSide.values()) {
                for (RedstoneSide east : RedstoneSide.values()) {
                    for (RedstoneSide west : RedstoneSide.values()) {
                        VoxelShape shape = DOT_SHAPE;

                        // Composizione NORTH
                        if (north == RedstoneSide.SIDE) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.NORTH));
                        } else if (north == RedstoneSide.UP) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.NORTH));
                            shape = Shapes.or(shape, UP_NORTH_STUB);
                        }

                        // Composizione SOUTH
                        if (south == RedstoneSide.SIDE) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.SOUTH));
                        } else if (south == RedstoneSide.UP) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.SOUTH));
                            shape = Shapes.or(shape, UP_SOUTH_STUB);
                        }

                        // Composizione EAST
                        if (east == RedstoneSide.SIDE) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.EAST));
                        } else if (east == RedstoneSide.UP) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.EAST));
                            shape = Shapes.or(shape, UP_EAST_STUB);
                        }

                        // Composizione WEST
                        if (west == RedstoneSide.SIDE) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.WEST));
                        } else if (west == RedstoneSide.UP) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.WEST));
                            shape = Shapes.or(shape, UP_WEST_STUB);
                        }

                        // Calcolo matematico dell'indice (0 - 80)
                        int index = north.ordinal() +
                                south.ordinal() * 3 +
                                east.ordinal() * 9 +
                                west.ordinal() * 27;

                        // .optimize() unisce le intersezioni interne riducendo i poligoni da renderizzare
                        SHAPE_CACHE[index] = shape.optimize();
                    }
                }
            }
        }
    }

    private static int getShapeIndex(BlockState state) {
        return state.getValue(NORTH).ordinal() +
                state.getValue(SOUTH).ordinal() * 3 +
                state.getValue(EAST).ordinal() * 9 +
                state.getValue(WEST).ordinal() * 27;
    }

    @Override
    protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return SHAPE_CACHE[getShapeIndex(state)]; // O(1)
    }

    public static EnumProperty<RedstoneSide> getProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("Invalid direction when calling getProperty in CobaltWireBlock: found %s but only NESW allowed".formatted(direction));
        };
    }

    public BlockState getWireShapeState(BlockGetter world, BlockPos pos, BlockState state) {
        return CobaltWireShape.getUpdatedState(world, pos, state);
    }

    // Function to compute the colour of the Cobalt Dust
    private static int getCobaltColor(int power) {
        float f = (float)power / 15.0F;
        float r = f * 0.1f + 0.1f;
        float g = f * 0.5f + 0.3f;
        float b = f * 1.1f + 0.4f;
        if(f!=0){
            b = b + 0.1f;
            g = g + 0.1f;
        }
        int red = Mth.clamp((int)(r * 255.0F), 0, 255);
        int green = Mth.clamp((int)(g * 255.0F), 0, 255);
        int blue = Mth.clamp((int)(b * 255.0F), 0, 255);

        return red << 16 | green << 8 | blue;
    }

    private void addPoweredParticles(Level world, RandomSource random, BlockPos pos, int colorInt, Direction direction, Direction direction2, float f, float g) {
        float h = g - f;
        if (!(random.nextFloat() > 0.2F * h)) {
            float j = f + h * random.nextFloat();
            double d = (double)pos.getX() + 0.5 + (double)(0.4375F * (float)direction.getStepX() + j * (float)direction2.getStepX());
            double e = (double)pos.getY() + 0.5 + (double)(0.4375F * (float)direction.getStepY() + j * (float)direction2.getStepY());
            double k = (double)pos.getZ() + 0.5 + (double)(0.4375F * (float)direction.getStepZ() + j * (float)direction2.getStepZ());
            DustParticleOptions particleEffect = new DustParticleOptions(colorInt, 1.0F);
            world.addParticle(particleEffect, d, e, k, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void animateTick(BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        int power = state.getValue(POWER);
        if (power == 0) return;
        int colorInt = getCobaltColor(power);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            EnumProperty<RedstoneSide> property = getProperty(direction);
            switch (state.getValue(property)) {
                case UP:
                    addPoweredParticles(world, random, pos, colorInt, direction, Direction.UP, -0.5F, 0.5F);
                    break;
                case SIDE:
                    addPoweredParticles(world, random, pos, colorInt, Direction.DOWN, direction, 0.0F, 0.5F);
                    break;
                case NONE:
                default:
                    addPoweredParticles(world, random, pos, colorInt, Direction.DOWN, direction, 0.0F, 0.3F);
                    break;
            }
        }
    }

    @Override
    public boolean canSurvive(@NonNull BlockState state, LevelReader world, @NonNull BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        mutable.setWithOffset(pos, Direction.DOWN);
        BlockState floorState = world.getBlockState(mutable);
        return floorState.isFaceSturdy(world, mutable, Direction.UP) || floorState.is(Blocks.HOPPER);
    }

    @Override
    public @NonNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER, NORTH, SOUTH, EAST, WEST, RETRACTED, WATERLOGGED);
    }

    @Override
    public @NonNull RenderShape getRenderShape(@NonNull BlockState state) {
        return RenderShape.MODEL;
    }

    private void updatePowerStrength(Level level, BlockPos blockPos, BlockState blockState, @Nullable Orientation orientation) {
        if (!CobaltAge.ModernSignalEngine) { // Skipping update power strength method when modern engine is enabled
            //if (useExperimentalEvaluator(level)) {
            // (new ExperimentalRedstoneWireEvaluator(this)).updatePowerStrength(level, blockPos, blockState, orientation, bl);
            //} else {
            this.evaluator.updatePowerStrength(level, blockPos, blockState, orientation, false);
            //}
        }
    }

    @Override
    public void neighborChanged(@NonNull BlockState state, Level world, @NonNull BlockPos pos, @NonNull Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!world.isClientSide()) {
            if (neighborBlock != this || !useExperimentalEvaluator(world)) {
                if (state.canSurvive(world, pos)) {
                    // Modern Signal Engine Switch
                    if (CobaltAge.ModernSignalEngine){
                        ((IServerLevel)world).cobaltage$getWireHandler().onWireUpdated(pos, state, orientation);
                    }else{
                        this.updatePowerStrength(world, pos, state, orientation);
                    }
                } else {
                    dropResources(state, world, pos);
                    world.removeBlock(pos, false);
                }
            }
        }
    }

    public int getBlockSignal(Level level, BlockPos blockPos) {
        this.shouldSignal = false;
        int i = ((SignalGetterByType)level).cobaltage$getBestNeighborSignalByType(COBALT, blockPos);
        this.shouldSignal = true;
        return i;
    }

    protected void affectNeighborsAfterRemoval(@NonNull BlockState blockState, @NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, boolean bl) {
        if (!bl) {
            for(Direction direction : Direction.values()) {
                serverLevel.updateNeighborsAt(blockPos.relative(direction), this);
            }
            // Modern Signal Engine Switch
            if (CobaltAge.ModernSignalEngine){
                ((IServerLevel)serverLevel).cobaltage$getWireHandler().onWireRemoved(blockPos, blockState);
            }else{
                this.updatePowerStrength(serverLevel, blockPos, blockState, null);
            }
            this.updateAllNeighbors(serverLevel, blockPos);
            this.updateDiagonalShapes(serverLevel, blockPos);
        }
    }

    private static boolean useExperimentalEvaluator(Level level) {
        return level.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS);
    }

    @Override
    public @NonNull BlockState updateShape(
            BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView,
            @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos,
            @NonNull BlockState neighborState, @NonNull RandomSource random
    ) {
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        if (!state.canSurvive(world, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState newState = this.getWireShapeState(world, pos, state);
        return newState.setValue(WATERLOGGED, state.getValue(WATERLOGGED));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = this.getWireShapeState(world, pos, this.defaultBlockState());
        FluidState fluidState = world.getFluidState(pos);
        boolean waterlogged = fluidState.getType() == Fluids.WATER;
        return state.setValue(WATERLOGGED, waterlogged);
    }

    @Override
    public boolean isSignalSource(@NonNull BlockState state) {
        return this.shouldSignal;
    }

    @Override
    protected int getDirectSignal(@NonNull BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        return !this.shouldSignal ? 0 : this.getSignal(blockState, blockGetter, blockPos, direction);
    }
    @Override
    protected int getSignal(@NonNull BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        if (this.shouldSignal && direction != Direction.DOWN) {
            int i = blockState.getValue(POWER);
            if (i == 0) {
                return 0;
            } else {
                return direction != Direction.UP && !(blockState.getValue(getProperty(direction.getOpposite())).isConnected()) ? 0 : i;
            }
        } else {
            return 0;
        }
    }

    @Override
    public int getDirectCobaltSignal(BlockState blockState, Level world, BlockPos pos, Direction direction) {
        return !this.shouldSignal ? 0 : this.getCobaltSignal(blockState, world, pos, direction);
    }

    @Override
    public int getCobaltSignal(BlockState blockState, Level world, BlockPos pos, Direction direction) {
        if (this.shouldSignal && direction != Direction.DOWN) {
            int i = blockState.getValue(POWER);
            if (i == 0) {
                return 0;
            } else {
                return direction != Direction.UP && !(blockState.getValue(getProperty(direction.getOpposite())).isConnected()) ? 0 : i;
            }
        } else {
            return 0;
        }
    }

    @Override
    public void onPlace(@NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos, @NonNull BlockState oldState, boolean bl) {
        if (!oldState.is(state.getBlock()) && !level.isClientSide()) {
            // Modern Signal Engine Switch
            if (CobaltAge.ModernSignalEngine){
                ((IServerLevel)level).cobaltage$getWireHandler().onWireAdded(pos, state);
            }else{
                this.updatePowerStrength(level, pos, state, null);
            }
            this.updateAllNeighbors(level,pos);
            this.updateDiagonalShapes(level, pos);
        }
    }

    private void updateAllNeighbors(Level world, BlockPos pos) {
        world.updateNeighborsAt(pos, this, null);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            mutable.setWithOffset(pos, dir);
            BlockPos immutableSide = mutable.immutable();
            world.updateNeighborsAt(immutableSide, this, null);
            world.updateNeighborsAt(immutableSide.above(), this, null);
            world.updateNeighborsAt(immutableSide.below(), this, null);
        }
    }

    private void updateDiagonalShapes(Level world, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        // 6 Prime Neighbours
        for (Direction dir : Direction.values()) {
            forceShapeUpdate(world, mutable.setWithOffset(pos, dir));
        }
        // 8 Diagonal Neighbours (4 UP, 4 DOWN)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
        mutable.setWithOffset(pos, dir);
        forceShapeUpdate(world, mutable.move(Direction.UP));
        mutable.setWithOffset(pos, dir);
        forceShapeUpdate(world, mutable.move(Direction.DOWN));
        }
    }

    private void forceShapeUpdate(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof CobaltWireBlock wireBlock) {
            BlockState newState = wireBlock.getWireShapeState(world, pos, state);
            if (state != newState) {
                world.setBlock(pos, newState, Block.UPDATE_ALL);
                for (Direction dir : Direction.values()) {
                    world.updateNeighborsAt(pos.relative(dir), wireBlock);
                }
            }
        }
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level world, @NonNull BlockPos pos, Player player, @NonNull BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        // + or .
        if (isFullyConnected(state) || isNotConnected(state)) {
            if(hasForcedConnection(world, pos)){
                return InteractionResult.PASS;
            }
            BlockState newState = isFullyConnected(state) ? setDotState(state) : setCrossState(state);
            newState = newState.setValue(POWER, state.getValue(POWER));
            world.setBlock(pos, newState, Block.UPDATE_ALL);
            this.updateAllNeighbors(world, pos);
            return InteractionResult.SUCCESS;
        }
        // - with 1 free side
        if (hasOneFreeConnectionInALineShape(world, pos)) {
            BlockState newState = state.cycle(RETRACTED);
            world.setBlock(pos, CobaltWireShape.getUpdatedState(world, pos, newState), Block.UPDATE_ALL);
            this.updateAllNeighbors(world, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // has at least 1 connection?
    private boolean hasForcedConnection(Level world, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (CobaltWireShape.getRenderConnection(world, pos, direction) != RedstoneSide.NONE) {
                return true;
            }
        }
        return false;
    }

    // is - state with only 1 free connection on the same axis?
    public static boolean hasOneFreeConnectionInALineShape(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        int i = 0;
        int j = 0;
        if(state.getValue(NORTH) != (RedstoneSide.NONE)) i++;
        if(state.getValue(SOUTH) != (RedstoneSide.NONE)) i++;
        if(state.getValue(EAST) != (RedstoneSide.NONE)) j++;
        if(state.getValue(WEST) != (RedstoneSide.NONE)) j++;
        if(i==0 || j==0){ // One axis Connection (Line or Ramp)
            if(i>0){ // North-South Axis
                i = 0;
                if(CobaltWireShape.getRenderConnection(world, pos, Direction.NORTH) != RedstoneSide.NONE) i++;
                if(CobaltWireShape.getRenderConnection(world, pos, Direction.SOUTH) != RedstoneSide.NONE) i++;
                return i==1; // Requires 1 axis to be forced
            }else{  // East-West Axis
                j = 0;
                if(CobaltWireShape.getRenderConnection(world, pos, Direction.EAST) != RedstoneSide.NONE) j++;
                if(CobaltWireShape.getRenderConnection(world, pos, Direction.WEST) != RedstoneSide.NONE) j++;
                return j==1; // Requires 1 axis to be forced
            }
        }
        return false;
    }

    // is . State?
    private boolean isNotConnected(BlockState state) {
        return state.getValue(NORTH) == RedstoneSide.NONE &&
                state.getValue(SOUTH) == RedstoneSide.NONE &&
                state.getValue(EAST) == RedstoneSide.NONE &&
                state.getValue(WEST) == RedstoneSide.NONE;
    }

    // is + State?
    private boolean isFullyConnected(BlockState state) {
        return state.getValue(NORTH) == RedstoneSide.SIDE &&
                state.getValue(SOUTH) == RedstoneSide.SIDE &&
                state.getValue(EAST) == RedstoneSide.SIDE &&
                state.getValue(WEST) == RedstoneSide.SIDE;
    }

    // . State
    private BlockState setDotState(BlockState state) {
        return state.setValue(NORTH, RedstoneSide.NONE)
                .setValue(SOUTH, RedstoneSide.NONE)
                .setValue(EAST, RedstoneSide.NONE)
                .setValue(WEST, RedstoneSide.NONE);
    }

    // + State
    private BlockState setCrossState(BlockState state) {
        return state.setValue(NORTH, RedstoneSide.SIDE)
                .setValue(SOUTH, RedstoneSide.SIDE)
                .setValue(EAST, RedstoneSide.SIDE)
                .setValue(WEST, RedstoneSide.SIDE);
    }
}