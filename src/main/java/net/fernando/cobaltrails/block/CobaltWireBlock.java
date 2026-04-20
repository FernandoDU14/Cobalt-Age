package net.fernando.cobaltrails.block;

import net.minecraft.block.*;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;
public class CobaltWireBlock extends Block {

    public static final IntProperty POWER = Properties.POWER;

    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty EAST  = Properties.EAST;
    public static final BooleanProperty WEST  = Properties.WEST;

    public CobaltWireBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(POWER, 0)
                .with(NORTH, false)
                .with(SOUTH, false)
                .with(EAST, false)
                .with(WEST, false));
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int power = state.get(POWER);
        if (power != 0) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                // If Cobalt Dust is active, particles will spawn
                // Vector3f(0.2f, 0.6f, 1.0f) is a good blue for the cobalt
                double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
                double e = (double)pos.getY() + 0.0625;
                double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;

                int cobaltBlue = (0 << 16) | (153 << 8) | 255;
                DustParticleEffect cobaltDust = new DustParticleEffect(cobaltBlue, 1.0f);

                world.addParticleClient(cobaltDust, d, e, f, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos floorPos = pos.down();
        BlockState floorState = world.getBlockState(floorPos);

        // This will allow the placement only and only if the block under is valid and is not another Cobalt Dust
        return floorState.isSideSolidFullSquare(world, floorPos, Direction.UP) || floorState.isOf(Blocks.HOPPER);
    }


    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWER, NORTH, SOUTH, EAST, WEST);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos,
                               Block sourceBlock, @Nullable WireOrientation orientation,
                               boolean notify) {

        world.scheduleBlockTick(pos, this, 1);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        CobaltWireNetwork.schedule(world, pos);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         @Nullable LivingEntity placer, ItemStack stack) {

        CobaltWireNetwork.schedule(world, pos);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return false;
    }

    /*
    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world,
                                    BlockPos pos, Direction direction) {

        return state.get(POWER);
    }*/

    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.offset(direction.getOpposite());
        BlockState neighborState = world.getBlockState(neighborPos);

        if (isVanillaRedstone(neighborState)) {
            return 0; // Niente energia per te, redstone rossa!
        }
        return super.getWeakRedstonePower(state, world, pos, direction);
    }

    @Override
    protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.offset(direction.getOpposite());
        BlockState neighborState = world.getBlockState(neighborPos);

        if (isVanillaRedstone(neighborState)) {
            return 0;
        }
        return super.getStrongRedstonePower(state, world, pos, direction);
    }

    private boolean isVanillaRedstone(BlockState state) {
        return state.isOf(Blocks.REDSTONE_WIRE) ||
                state.isOf(Blocks.REPEATER) ||
                state.isOf(Blocks.COMPARATOR) ||
                state.isOf(Blocks.REDSTONE_TORCH) ||
                state.isOf(Blocks.REDSTONE_WALL_TORCH) ||
                state.isOf(Blocks.REDSTONE_BLOCK);
    }

}