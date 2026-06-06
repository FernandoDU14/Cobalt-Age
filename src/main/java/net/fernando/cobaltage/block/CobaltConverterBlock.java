package net.fernando.cobaltage.block;

import com.mojang.serialization.MapCodec;
import net.fernando.cobaltage.block.wire.CobaltSignalEmitter;
import net.fernando.cobaltage.util.signal.FlowingSide;
import net.fernando.cobaltage.util.signal.SignalTypeLevelExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import static net.fernando.cobaltage.util.signal.SignalType.*;

public class CobaltConverterBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, CobaltSignalEmitter {

    public static final MapCodec<CobaltConverterBlock> CODEC = simpleCodec(CobaltConverterBlock::new);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    public static final BooleanProperty COBALT_LIT = BooleanProperty.create("cobalt_lit");
    public static final BooleanProperty REDSTONE_LIT = BooleanProperty.create("redstone_lit");
    public static final EnumProperty<FlowingSide> FLOWING_SIDE = EnumProperty.create("flowing_side", FlowingSide.class);

    public CobaltConverterBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWER, 0)
                .setValue(COBALT_LIT, false)
                .setValue(REDSTONE_LIT, false)
                .setValue(FLOWING_SIDE, FlowingSide.NONE)
                .setValue(WATERLOGGED, false));
    }

    public @NonNull MapCodec<CobaltConverterBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, @NonNull Level world, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);

        // 1. Troviamo il centro esatto del blocco
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.4; // L'altezza delle torce
        double centerZ = pos.getZ() + 0.5;

        // 2. Creiamo l'oscillazione casuale (il tremolio del fumo)
        double randX = (random.nextDouble() - 0.5) * 0.2;
        double randY = (random.nextDouble() - 0.5) * 0.2;
        double randZ = (random.nextDouble() - 0.5) * 0.2;

        // 3. Offset di distanza dal centro: 4 pixel (0.25 blocchi)
        double offsetDistance = 0.25;

        if (state.getValue(COBALT_LIT)) {
            // La torcia Cobalt è spostata "IN AVANTI" rispetto al centro
            double x = centerX + (facing.getStepX() * offsetDistance) + randX;
            double y = centerY + randY;
            double z = centerZ + (facing.getStepZ() * offsetDistance) + randZ;

            int cobaltBlue = (0) | (153 << 8) | 255;
            DustParticleOptions cobaltDust = new DustParticleOptions(cobaltBlue, 1.0f);
            world.addParticle(cobaltDust, x, y, z, 0.0, 0.0, 0.0);
        }

        if (state.getValue(REDSTONE_LIT)) {
            // La torcia Redstone è spostata "ALL'INDIETRO" rispetto al centro (nota il segno meno)
            double x = centerX - (facing.getStepX() * offsetDistance) + randX;
            double y = centerY + randY;
            double z = centerZ - (facing.getStepZ() * offsetDistance) + randZ;

            world.addParticle(DustParticleOptions.REDSTONE, x, y, z, 0.0F, 0.0F, 0.0F);
        }
    }

    @Override
    public @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER, COBALT_LIT, REDSTONE_LIT, FLOWING_SIDE, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public @NonNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canSurvive(@NonNull BlockState state, LevelReader world, BlockPos pos) {
        BlockPos floorPos = pos.below();
        BlockState floorState = world.getBlockState(floorPos);
        return floorState.isFaceSturdy(world, floorPos, Direction.UP) || floorState.is(Blocks.HOPPER);
    }

    @Override
    public void onPlace(BlockState state, @NonNull Level world, @NonNull BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock()) && !world.isClientSide()) {
            updateConverterState(state, world, pos);
        }
    }

    @Override
    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        BlockState newState =  super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (newState.is(this)) {
            return direction == Direction.DOWN && !(neighborState.isFaceSturdy(world, neighborPos, Direction.UP, SupportType.RIGID)) ? Blocks.AIR.defaultBlockState() : newState.setValue(WATERLOGGED, state.getValue(WATERLOGGED));
        }
        return newState;
    }

    private void updateConverterState(BlockState state, Level world, BlockPos pos){
        if (world.isClientSide()) return;

        Direction cobaltDirection = state.getValue(FACING);
        Direction redstoneDirection = cobaltDirection.getOpposite();
        BlockPos RedstoneSidePos = pos.relative(redstoneDirection);
        BlockState redstoneSideState = world.getBlockState(RedstoneSidePos);
        BlockPos cobaltSidePos = pos.relative(cobaltDirection);
        BlockState cobaltSideState = world.getBlockState(cobaltSidePos);

        // Reading Redstone signals like a Redstone Repeater does
        int newRed;
        int i = ((SignalTypeLevelExtensions) world).getSignalByType(REDSTONE, RedstoneSidePos, redstoneDirection);
        if (i >= 15) {
            newRed = i;
        } else {
            newRed = Math.max(i, redstoneSideState.is(Blocks.REDSTONE_WIRE) ? redstoneSideState.getValue(RedStoneWireBlock.POWER) : 0);
        }
        // Reading Cobalt signal like Cobalt Repeater does
        int newCob;
        i = ((SignalTypeLevelExtensions) world).getSignalByType(COBALT, cobaltSidePos, cobaltDirection);
        if (i >= 15) {
            newCob = i;
        } else {
            newCob = Math.max(i, cobaltSideState.is(ModBlocks.COBALT_DUST) ? cobaltSideState.getValue(CobaltWireBlock.POWER) : 0);
        }

        FlowingSide currentFlow = state.getValue(FLOWING_SIDE);
        int currentPower = state.getValue(POWER);
        int nextPower = 0;
        FlowingSide nextFlow = currentFlow;

        if (currentFlow == FlowingSide.TOWARDS_COBALT) {
            nextPower = newRed;
            if (nextPower == 0) nextFlow = FlowingSide.NONE;

        } else if (currentFlow == FlowingSide.TOWARDS_REDSTONE) {
            nextPower = newCob;
            if (nextPower == 0) nextFlow = FlowingSide.NONE;
        }else {
            if (newRed > 0 && newRed > newCob) {
                nextPower = newRed;
                nextFlow = FlowingSide.TOWARDS_COBALT;
            } else if (newCob > 0) {
                nextPower = newCob;
                nextFlow = FlowingSide.TOWARDS_REDSTONE;
            }
        }
        if (currentPower != nextPower || currentFlow != nextFlow) {

            if (currentFlow == FlowingSide.NONE && nextFlow != FlowingSide.NONE && !state.getValue(WATERLOGGED)) {
                world.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, 0.55F);
            }

            BlockState newState = state
                    .setValue(POWER, nextPower)
                    .setValue(FLOWING_SIDE, nextFlow)
                    .setValue(COBALT_LIT, nextFlow == FlowingSide.TOWARDS_COBALT)
                    .setValue(REDSTONE_LIT, nextFlow == FlowingSide.TOWARDS_REDSTONE);

            world.setBlock(pos, newState, Block.UPDATE_ALL);
            updateNeighbors(world, pos, newState);
        }
    }

    @Override
    public void neighborChanged(@NonNull BlockState state, Level world, @NonNull BlockPos pos, @NonNull Block sourceBlock, @Nullable Orientation orientation, boolean notify) {
        if (world.isClientSide()) return;
        if (!state.canSurvive(world, pos)) {
            dropResources(state, world, pos);
            world.removeBlock(pos, false);
        }
        updateConverterState(state, world, pos);
    }

    private void updateNeighbors(Level world, BlockPos pos, BlockState state) {
        Direction cobaltSide = state.getValue(FACING);
        Direction redstoneSide = cobaltSide.getOpposite();

        // Aggiorna Tutti i Primi Vicini e Primi Vicini Diagonali
        world.updateNeighborsAt(pos, this, null);
        world.updateNeighborsAt(pos.relative(redstoneSide), this, null);
        world.updateNeighborsAt(pos.relative(cobaltSide), this, null);

    }


    // --- Signal overrides ---
    @Override
    public boolean isSignalSource(@NonNull BlockState state) {
        return true;
    }
    @Override
    protected int getDirectSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        return this.getSignal(state, world, pos, direction);
    }
    @Override
    // direction is the direction of the EMISSION
    protected int getSignal(BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        // (F) Cobalt -> Redston (F.opposite) only from Facing direction
        return (state.getValue(FLOWING_SIDE) == FlowingSide.TOWARDS_REDSTONE && direction == state.getValue(FACING)) ? state.getValue(POWER) : 0;
    }
    @Override
    public int getCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        // Giving you signal if i'm not in cobalt input mode and i'm the opposite of my facing respect to you
        return (state.getValue(FLOWING_SIDE).equals(FlowingSide.TOWARDS_COBALT) && direction == state.getValue(FACING).getOpposite()) ? state.getValue(POWER) : 0;
    }
    @Override
    public int getDirectCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        // Giving you the direct signal as I would've given as a signal
        return this.getCobaltSignal(state, world, pos, direction);
    }
}
