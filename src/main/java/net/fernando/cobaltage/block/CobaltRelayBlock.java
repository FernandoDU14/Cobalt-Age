package net.fernando.cobaltage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

import static net.fernando.cobaltage.wire.CobaltWireShape.canSourceConnectToTarget;

public class CobaltRelayBlock extends CobaltWireBlock {
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public CobaltRelayBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(UP, true)
                .setValue(DOWN, true)
                .setValue(NORTH, RedstoneSide.SIDE)
                .setValue(SOUTH, RedstoneSide.SIDE)
                .setValue(EAST, RedstoneSide.SIDE)
                .setValue(WEST, RedstoneSide.SIDE)
                .setValue(POWER, 0)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // Aggiunge POWER, NORTH, ecc.
        builder.add(UP, DOWN);
    }

    @Override
    public @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public boolean canSurvive(@NonNull BlockState state, LevelReader world, @NonNull BlockPos pos) {
        return true;
    }

    private BlockState getUpdatedState(BlockGetter world, BlockPos pos, BlockState state) {
        boolean up = true; // forced state
        boolean down = true; // forced state
        RedstoneSide north = getRenderConnection(world, pos, Direction.NORTH);
        RedstoneSide south = getRenderConnection(world, pos, Direction.SOUTH);
        RedstoneSide east = getRenderConnection(world, pos, Direction.EAST);
        RedstoneSide west = getRenderConnection(world, pos, Direction.WEST);

        boolean hasNorth = north.isConnected();
        boolean hasSouth = south.isConnected();
        boolean hasEast = east.isConnected();
        boolean hasWest = west.isConnected();

        return state
                .setValue(UP, up)
                .setValue(DOWN, down)
                .setValue(NORTH, hasNorth ? RedstoneSide.SIDE : RedstoneSide.NONE)
                .setValue(SOUTH, hasSouth ? RedstoneSide.SIDE : RedstoneSide.NONE)
                .setValue(EAST, hasEast ? RedstoneSide.SIDE : RedstoneSide.NONE)
                .setValue(WEST, hasWest ? RedstoneSide.SIDE : RedstoneSide.NONE);
    }

    public static RedstoneSide getRenderConnection(BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        BlockState sourceState = world.getBlockState(pos);

        mutable.setWithOffset(pos, direction);
        BlockState neighborState = world.getBlockState(mutable);

        if (canSourceConnectToTarget(sourceState, neighborState, direction)) {
            return RedstoneSide.SIDE;
        }
        return RedstoneSide.NONE;
    }

    @Override
    public BlockState getWireShapeState(BlockGetter world, BlockPos pos, BlockState state) {
        return this.getUpdatedState(world, pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state != null) {
            return this.getUpdatedState(ctx.getLevel(), ctx.getClickedPos(), state);
        }
        return null;
    }

    @Override
    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, net.minecraft.world.level.@NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        BlockState baseUpdate = super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (baseUpdate.is(Blocks.AIR)) return baseUpdate; // Eredita la logica dell'acqua/distruzione

        return this.getUpdatedState(world, pos, baseUpdate);
    }

    @Override
    protected int getSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        if (this.shouldSignal) {
            int i = state.getValue(POWER);
            if (i == 0) {
                return 0;
            }
            else if(direction == Direction.UP && state.getValue(UP) || direction == Direction.DOWN && state.getValue(DOWN)){
                return i;
            } else {
                return !(state.getValue(getProperty(direction.getOpposite())).isConnected()) ? 0 : i;
            }
        } else {
            return 0;
        }
    }
    @Override
    public int getCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (this.shouldSignal) {
            int i = state.getValue(POWER);
            if (i == 0) {
                return 0;
            }
            else if(direction == Direction.UP && state.getValue(UP) || direction == Direction.DOWN && state.getValue(DOWN)){
                return i;
            } else {
                return !(state.getValue(getProperty(direction.getOpposite())).isConnected()) ? 0 : i;
            }
        } else {
            return 0;
        }
    }
    @Override
    public int getDirectCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return !this.shouldSignal ? 0 : this.getCobaltSignal(state, world, pos, direction);
    }
    @Override
    protected int getDirectSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        return !this.shouldSignal ? 0 : this.getSignal(state, world, pos, direction);
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level world, @NonNull BlockPos pos, Player player, @NonNull BlockHitResult hit) {
        // Nessun interruttore manuale (Cross/Dot) per il relè: è sempre automatico!
        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        int power = state.getValue(POWER);
        if (power == 0) return;

        float f = (float)power / 15.0F;
        float r = f * 0.1f + 0.1f;
        float g = f * 0.5f + 0.3f;
        float b = f * 1.1f + 0.4f;
        if(f != 0){ b += 0.1f; g += 0.1f; }

        int red = Mth.clamp((int)(r * 255.0F), 0, 255);
        int green = Mth.clamp((int)(g * 255.0F), 0, 255);
        int blue = Mth.clamp((int)(b * 255.0F), 0, 255);
        int colorInt = red << 16 | green << 8 | blue;

        // Emette particelle dal centro esatto del blocco (dentro il vetro)
        if (random.nextFloat() < 0.5F) {
            double dX = pos.getX() + 0.5D + (random.nextDouble() - 0.5) * 0.4;
            double dY = pos.getY() + 0.5D + (random.nextDouble() - 0.5) * 0.4;
            double dZ = pos.getZ() + 0.5D + (random.nextDouble() - 0.5) * 0.4;
            world.addParticle(new DustParticleOptions(colorInt, 1.0F), dX, dY, dZ, 0.0, 0.0, 0.0);
        }
    }
}