package net.cobaltmc.cobaltage.block.cobalt;

import com.mojang.serialization.MapCodec;
import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltSignalSource;
import net.cobaltmc.cobaltage.util.signal.FlowingSide;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class CobaltConverterBlock extends DiodeBlock implements SimpleWaterloggedBlock, CobaltSignalSource{

    public static final MapCodec<CobaltConverterBlock> CODEC = simpleCodec(CobaltConverterBlock::new);
    public static final IntegerProperty POWER;
    public static final BooleanProperty WATERLOGGED;
    private static final VoxelShape SHAPE;
    public static final BooleanProperty COBALT_LIT;
    public static final BooleanProperty REDSTONE_LIT;
    public static final EnumProperty<FlowingSide> FLOWING_SIDE;
    private static final int UPDATE_DELAY = 2;
    protected boolean shouldSignal = true;

    public CobaltConverterBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(POWER, 0)).setValue(COBALT_LIT, false)).setValue(REDSTONE_LIT, false)).setValue(FLOWING_SIDE, FlowingSide.NONE)).setValue(WATERLOGGED, false));
    }

    public @NonNull MapCodec<CobaltConverterBlock> codec() {
        return CODEC;
    }

    public void animateTick(BlockState state, @NonNull Level world, BlockPos pos, RandomSource random) {
        Direction facing = (Direction)state.getValue(FACING);
        double centerX = (double)pos.getX() + (double)0.5F;
        double centerY = (double)pos.getY() + 0.4;
        double centerZ = (double)pos.getZ() + (double)0.5F;
        double randX = (random.nextDouble() - (double)0.5F) * 0.2;
        double randY = (random.nextDouble() - (double)0.5F) * 0.2;
        double randZ = (random.nextDouble() - (double)0.5F) * 0.2;
        double offsetDistance = (double)0.25F;
        if ((Boolean)state.getValue(COBALT_LIT)) {
            double x = centerX + (double)facing.getStepX() * offsetDistance + randX;
            double y = centerY + randY;
            double z = centerZ + (double)facing.getStepZ() * offsetDistance + randZ;
            int cobaltBlue = 39423;
            DustParticleOptions cobaltDust = new DustParticleOptions(cobaltBlue, 1.0F);
            world.addParticle(cobaltDust, x, y, z, (double)0.0F, (double)0.0F, (double)0.0F);
        }

        if ((Boolean)state.getValue(REDSTONE_LIT)) {
            double x = centerX - (double)facing.getStepX() * offsetDistance + randX;
            double y = centerY + randY;
            double z = centerZ - (double)facing.getStepZ() * offsetDistance + randZ;
            world.addParticle(DustParticleOptions.REDSTONE, x, y, z, (double)0.0F, (double)0.0F, (double)0.0F);
        }

    }

    public @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return SHAPE;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, POWER, COBALT_LIT, REDSTONE_LIT, FLOWING_SIDE, WATERLOGGED});
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite())).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    public @NonNull FluidState getFluidState(BlockState state) {
        return (Boolean)state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public boolean canSurvive(@NonNull BlockState state, LevelReader world, BlockPos pos) {
        BlockPos floorPos = pos.below();
        BlockState floorState = world.getBlockState(floorPos);
        return floorState.isFaceSturdy(world, floorPos, Direction.UP) || floorState.is(Blocks.HOPPER);
    }

    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if ((Boolean)state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        BlockState newState = super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (!newState.is(this)) {
            return newState;
        } else {
            return direction == Direction.DOWN && !neighborState.isFaceSturdy(world, neighborPos, Direction.UP, net.minecraft.world.level.block.SupportType.FULL) ? Blocks.AIR.defaultBlockState() : (BlockState)newState.setValue(WATERLOGGED, (Boolean)state.getValue(WATERLOGGED));
        }
    }

    protected void tick(@NonNull BlockState state, @NonNull ServerLevel world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        this.updateConverterState(state, world, pos);
    }

    public void onPlace(BlockState state, @NonNull Level world, @NonNull BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock()) && !world.isClientSide() && !world.getBlockTicks().hasScheduledTick(pos, this)) {
            world.scheduleTick(pos, this, 2);
        }
    }

    @Override
    protected int getDelay(BlockState blockState) {
        return 0;
    }

    private void updateConverterState(BlockState state, Level world, BlockPos pos) {
        if (!world.isClientSide()) {
            Direction cobaltDirection = (Direction)state.getValue(FACING);
            Direction redstoneDirection = cobaltDirection.getOpposite();
            BlockPos RedstoneSidePos = pos.relative(redstoneDirection);
            BlockState redstoneSideState = world.getBlockState(RedstoneSidePos);
            BlockPos cobaltSidePos = pos.relative(cobaltDirection);
            BlockState cobaltSideState = world.getBlockState(cobaltSidePos);
            this.shouldSignal = false;
            int i = ((SignalGetterByType)world).cobaltage$getSignalByType(SignalType.REDSTONE, RedstoneSidePos, redstoneDirection);
            int newExternalRed;
            if (i >= 15) {
                newExternalRed = i;
            } else {
                newExternalRed = Math.max(i, redstoneSideState.is(Blocks.REDSTONE_WIRE) ? (Integer)redstoneSideState.getValue(RedStoneWireBlock.POWER) : 0);
            }

            i = ((SignalGetterByType)world).cobaltage$getSignalByType(SignalType.COBALT, cobaltSidePos, cobaltDirection);
            int newExternalCob;
            if (i >= 15) {
                newExternalCob = i;
            } else {
                newExternalCob = Math.max(i, cobaltSideState.is(ModBlocks.COBALT_DUST) ? (Integer)cobaltSideState.getValue(CobaltWireBlock.POWER) : 0);
            }

            this.shouldSignal = true;
            FlowingSide currentFlow = (FlowingSide)state.getValue(FLOWING_SIDE);
            int currentPower = (Integer)state.getValue(POWER);
            int effectiveRed = newExternalRed;
            int effectiveCob = newExternalCob;
            if (currentFlow == FlowingSide.TOWARDS_COBALT && newExternalCob <= currentPower) {
                effectiveCob = 0;
            } else if (currentFlow == FlowingSide.TOWARDS_REDSTONE && newExternalRed <= currentPower) {
                effectiveRed = 0;
            }

            int nextPower = 0;
            FlowingSide nextFlow;
            if (effectiveRed > effectiveCob) {
                nextPower = effectiveRed;
                nextFlow = FlowingSide.TOWARDS_COBALT;
            } else if (effectiveCob > effectiveRed) {
                nextPower = effectiveCob;
                nextFlow = FlowingSide.TOWARDS_REDSTONE;
            } else if (effectiveRed > 0) {
                nextPower = effectiveRed;
                nextFlow = currentFlow == FlowingSide.NONE ? FlowingSide.TOWARDS_REDSTONE : currentFlow;
            } else {
                nextFlow = FlowingSide.NONE;
            }

            if (currentPower != nextPower || currentFlow != nextFlow) {
                if (currentFlow == FlowingSide.NONE && nextFlow != FlowingSide.NONE && !(Boolean)state.getValue(WATERLOGGED)) {
                    world.playSound((Entity)null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, 0.55F);
                }

                BlockState newState = (BlockState)((BlockState)((BlockState)((BlockState)state.setValue(POWER, nextPower)).setValue(FLOWING_SIDE, nextFlow)).setValue(COBALT_LIT, nextFlow == FlowingSide.TOWARDS_COBALT)).setValue(REDSTONE_LIT, nextFlow == FlowingSide.TOWARDS_REDSTONE);
                world.setBlock(pos, newState, 3);
                this.updateNeighbors(world, pos, newState);
            }
        }
    }

    public void neighborChanged(@NonNull BlockState state, Level world, @NonNull BlockPos pos, @NonNull Block sourceBlock, @Nullable Orientation orientation, boolean notify) {
        if (!world.isClientSide()) {
            if (!state.canSurvive(world, pos)) {
                dropResources(state, world, pos);
                world.removeBlock(pos, false);
            }

            if (!world.getBlockTicks().hasScheduledTick(pos, this)) {
                world.scheduleTick(pos, this, 2);
            }

        }
    }

    private void updateNeighbors(Level world, BlockPos pos, BlockState state) {
        Direction cobaltSide = (Direction)state.getValue(FACING);
        Direction redstoneSide = cobaltSide.getOpposite();
        world.updateNeighborsAt(pos, this, (Orientation)null);
        world.updateNeighborsAt(pos.relative(redstoneSide), this, (Orientation)null);
        world.updateNeighborsAt(pos.relative(cobaltSide), this, (Orientation)null);
    }

    public boolean isSignalSource(@NonNull BlockState state) {
        return this.shouldSignal;
    }

    protected int getSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        return this.shouldSignal ? this.getDirectSignal(state, world, pos, direction) : 0;
    }

    protected int getDirectSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        return this.shouldSignal && state.getValue(FLOWING_SIDE) == FlowingSide.TOWARDS_REDSTONE && direction == state.getValue(FACING) ? (Integer)state.getValue(POWER) : 0;
    }

    public int getCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return this.shouldSignal && ((FlowingSide)state.getValue(FLOWING_SIDE)).equals(FlowingSide.TOWARDS_COBALT) && direction == ((Direction)state.getValue(FACING)).getOpposite() ? (Integer)state.getValue(POWER) : 0;
    }

    public int getDirectCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return this.shouldSignal ? this.getCobaltSignal(state, world, pos, direction) : 0;
    }

    static {
        POWER = BlockStateProperties.POWER;
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        SHAPE = Block.box((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)2.0F, (double)16.0F);
        COBALT_LIT = BooleanProperty.create("cobalt_lit");
        REDSTONE_LIT = BooleanProperty.create("redstone_lit");
        FLOWING_SIDE = EnumProperty.create("flowing_side", FlowingSide.class);
    }
}


