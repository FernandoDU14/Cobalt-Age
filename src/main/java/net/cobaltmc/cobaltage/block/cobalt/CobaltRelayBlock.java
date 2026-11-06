package net.cobaltmc.cobaltage.block.cobalt;

import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltColorUtil;
import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltWireShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
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

public class CobaltRelayBlock extends CobaltWireBlock {
    public static final BooleanProperty UP;
    public static final BooleanProperty DOWN;

    public CobaltRelayBlock(Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(UP, true)).setValue(DOWN, true)).setValue(NORTH, RedstoneSide.SIDE)).setValue(SOUTH, RedstoneSide.SIDE)).setValue(EAST, RedstoneSide.SIDE)).setValue(WEST, RedstoneSide.SIDE)).setValue(POWER, 0)).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
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
        boolean up = true;
        boolean down = true;
        RedstoneSide north = getRenderConnection(world, pos, Direction.NORTH);
        RedstoneSide south = getRenderConnection(world, pos, Direction.SOUTH);
        RedstoneSide east = getRenderConnection(world, pos, Direction.EAST);
        RedstoneSide west = getRenderConnection(world, pos, Direction.WEST);
        boolean hasNorth = north.isConnected();
        boolean hasSouth = south.isConnected();
        boolean hasEast = east.isConnected();
        boolean hasWest = west.isConnected();
        return (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)state.setValue(UP, up))
                .setValue(DOWN, down)).setValue(NORTH, hasNorth ? RedstoneSide.SIDE : RedstoneSide.NONE))
                .setValue(SOUTH, hasSouth ? RedstoneSide.SIDE : RedstoneSide.NONE))
                .setValue(EAST, hasEast ? RedstoneSide.SIDE : RedstoneSide.NONE))
                .setValue(WEST, hasWest ? RedstoneSide.SIDE : RedstoneSide.NONE);
    }

    public static RedstoneSide getRenderConnection(BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockState sourceState = world.getBlockState(pos);
        mutable.setWithOffset(pos, direction);
        BlockState neighborState = world.getBlockState(mutable);
        return CobaltWireShape.canSourceConnectToTarget(sourceState, neighborState, direction) ? RedstoneSide.SIDE : RedstoneSide.NONE;
    }

    public BlockState getWireShapeState(BlockGetter world, BlockPos pos, BlockState state) {
        return this.getUpdatedState(world, pos, state);
    }

    protected int getSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        return !this.shouldSignal ? 0 : this.getDirectSignal(state, world, pos, direction);
    }

    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level world, @NonNull BlockPos pos, Player player, @NonNull BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    public void animateTick(BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        int power = (Integer)state.getValue(POWER);
        if (power != 0) {
            int colorInt = CobaltColorUtil.getCobaltColor(power);
            if (random.nextFloat() < 0.5F) {
                double dX = (double)pos.getX() + (double)0.5F + (random.nextDouble() - (double)0.5F) * 0.4;
                double dY = (double)pos.getY() + (double)0.5F + (random.nextDouble() - (double)0.5F) * 0.4;
                double dZ = (double)pos.getZ() + (double)0.5F + (random.nextDouble() - (double)0.5F) * 0.4;
                world.addParticle(new DustParticleOptions(colorInt, 1.0F), dX, dY, dZ, (double)0.0F, (double)0.0F, (double)0.0F);
            }

        }
    }

    static {
        UP = BlockStateProperties.UP;
        DOWN = BlockStateProperties.DOWN;
    }
}
