package net.fernando.cobaltage.block;

import net.fernando.cobaltage.block.blockentities.CobaltComparatorBlockEntity;
import net.fernando.cobaltage.block.blockentities.ModBlockEntities;
import net.fernando.cobaltage.block.wire.CobaltPowerSource;
import net.fernando.cobaltage.util.SignalHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.PoweredBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class CobaltComparatorBlock extends ComparatorBlock implements SimpleWaterloggedBlock, CobaltPowerSource {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final net.fernando.cobaltage.block.wire.CobaltWireNetwork NETWORK_HANDLER = new net.fernando.cobaltage.block.wire.CobaltWireNetwork();

    public CobaltComparatorBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(MODE, ComparatorMode.COMPARE)
                .setValue(WATERLOGGED, false));
    }

    // This method is called to get the ticker for the block entity
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, @NonNull BlockState state, @NonNull BlockEntityType<T> type) {
        if (world.isClientSide()) return null;

        // We check if the block entity type matches
        if (type == ModBlockEntities.COBALT_COMPARATOR_ENTITY) {
            return (world1, pos1, state1, blockEntity) ->
                    CobaltComparatorBlockEntity.tick(world1, pos1, state1, (CobaltComparatorBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected void updateNeighborsInFront(Level world, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        BlockPos outputPos = pos.relative(direction.getOpposite());

        world.neighborChanged(outputPos, this, null);
        world.updateNeighborsAtExceptFromFacing(outputPos, this, direction, null);
        if (world.getBlockState(outputPos).getBlock() instanceof CobaltWireBlock) {
            NETWORK_HANDLER.updateNetwork(world, pos);
        }
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, MODE, WATERLOGGED);
    }

    @Override
    public @NonNull BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        BlockState state = super.getStateForPlacement(ctx);
        return state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull Player player, @NonNull BlockHitResult hit) {
        // Execute the vanilla logic first (which changes the mode and produces the sounds)
        InteractionResult result = super.useWithoutItem(state, world, pos, player, hit);

        if (!world.isClientSide() && result.consumesAction()) {
            // Notify the Cobalt network starting from each side of the comparator.
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                NETWORK_HANDLER.updateNetwork(world, neighborPos);
            }
            world.scheduleTick(pos, this, 1);
        }
        return result;
    }

    @Override
    public @NonNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        BlockState newState =  super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (newState.is(this)) {
            return newState.setValue(WATERLOGGED, state.getValue(WATERLOGGED));
        }
        return newState;
    }





    @Override
    public @NonNull BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new CobaltComparatorBlockEntity(pos, state);
    }

    // 1. Read the signal of the block entity
    @Override
    protected int getOutputSignal(BlockGetter world, @NonNull BlockPos pos, @NonNull BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CobaltComparatorBlockEntity cobaltBE) {
            return cobaltBE.getOutputSignal();
        }
        return 0;
    }

    // 2. Compute the signal
    @Override
    protected void checkTickOnNeighbor(Level world, @NonNull BlockPos pos, @NonNull BlockState state) {
        if (world.getBlockTicks().hasScheduledTick(pos, this)) {
            return;
        }
        int i = this.calculateOutputSignal(world, pos, state);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        int j = blockEntity instanceof CobaltComparatorBlockEntity cobaltBE ? cobaltBE.getOutputSignal() : 0;
        if (i != j || state.getValue(POWERED) != i > 0) {
            // Update on change
            world.getBlockTicks().schedule(
                    new ScheduledTick<>(this, pos, world.getGameTime() + 2, TickPriority.NORMAL, 0L)
            );
        }
    }

    @Override
    public void tick(@NonNull BlockState state, @NonNull ServerLevel world, @NonNull BlockPos pos, net.minecraft.util.@NonNull RandomSource random) {
        int expectedPower = this.calculateOutputSignal(world, pos, state);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        int currentPower = 0;

        if (blockEntity instanceof CobaltComparatorBlockEntity cobaltEntity) {
            currentPower = cobaltEntity.getOutputSignal();
            cobaltEntity.setOutputSignal(expectedPower);
        }

        if (currentPower != expectedPower || state.getValue(MODE) == ComparatorMode.COMPARE) {
            boolean shouldBePowered = this.shouldTurnOn(world, pos, state);
            boolean isPowered = state.getValue(POWERED);
            if (isPowered && !shouldBePowered) {
                world.setBlock(pos, state.setValue(POWERED, false), 2);
            } else if (!isPowered && shouldBePowered) {
                world.setBlock(pos, state.setValue(POWERED, true), 2);
            }
            this.updateNeighborsInFront(world, pos, state);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        // 1. Check on survive block event
        if (!state.canSurvive(world, pos)) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            dropResources(state, world, pos, blockEntity);
            world.removeBlock(pos, false);
            return;
        }
        this.checkTickOnNeighbor(world, pos, state);
    }

    private int calculateOutputSignal(Level world, BlockPos pos, BlockState state) {
        // Custom Cobalt Rule for calculating output signals
        int i = this.getInputSignal(world, pos, state);
        if (i == 0) {
            return 0;
        } else {
            int j = this.getPowerOnSides(world, pos, state);
            if (j > i) {
                return 0;
            } else {
                return state.getValue(MODE) == ComparatorMode.SUBTRACT ? i - j : i;
            }
        }
    }


    // Helper to manage power changes
    protected int getPowerOnSides(LevelReader world, BlockPos pos, BlockState state) {
        int maxSidePower = 0;
        Direction facing = state.getValue(FACING);

        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (side != facing && side != facing.getOpposite()) {
                BlockPos sidePos = pos.relative(side);
                BlockState sideState = world.getBlockState(sidePos);

                int p = 0;
                if (sideState.getBlock() instanceof CobaltWireBlock) {
                    p = sideState.getValue(CobaltWireBlock.POWER);
                } else if (sideState.getBlock() instanceof CobaltPowerSource source) {
                    p = source.getCobaltSignal(sideState, (Level)world, sidePos);
                }

                if (p > maxSidePower) maxSidePower = p;
            }
        }
        return maxSidePower;
    }


    @Override
    public int getInputSignal(Level world, BlockPos pos, BlockState state) {
        int power = 0;
        Direction direction = state.getValue(FACING);
        BlockPos rearPos = pos.relative(direction);
        BlockState rearState = world.getBlockState(rearPos);

        // --- 1. Cobalt Network Management ------
        if (rearState.getBlock() instanceof CobaltPowerSource source) {
            if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                power = source.getCobaltSignal(rearState, world, rearPos);
            }
        } else if (rearState.getBlock() instanceof CobaltWireBlock) {
            power = rearState.getValue(CobaltWireBlock.POWER);
        } else if (SignalHelper.compatibleCobaltPowerSource(rearState)) {
            power = rearState.getSignal(world, rearPos, direction);
        }else if (SignalHelper.restrictedCobaltPowerSource(rearState)) {
            power = rearState.getSignal(world, rearPos, direction);
        } else if (rearState.isRedstoneConductor(world, rearPos) || rearState.getBlock() instanceof PoweredBlock) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = rearPos.relative(dir);
                BlockState neighborState = world.getBlockState(neighborPos);

                // Cobalt Dust Pointing To Solid Block
                if (neighborState.getBlock() instanceof CobaltWireBlock) {
                    if (isDustPointingTo(neighborState, dir.getOpposite())) {
                        power = Math.max(power, neighborState.getValue(CobaltWireBlock.POWER));
                    }
                }
                // Other Sources
                else if (neighborState.getBlock() instanceof CobaltPowerSource src) {
                    if (src.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                        power = Math.max(power, src.getDirectCobaltSignal(neighborState, world, neighborPos, dir.getOpposite()));
                    }
                } else if (SignalHelper.compatibleCobaltPowerSource(neighborState)) {
                    power = Math.max(power, neighborState.getDirectSignal(world, neighborPos, dir));
                }else if (SignalHelper.restrictedCobaltPowerSource(neighborState)) {
                    power = Math.max(power, neighborState.getDirectSignal(world, neighborPos, dir));
                }
            }
        }

        // --- 2. Comparator Signals Management ---
        if (rearState.hasAnalogOutputSignal()) {
            power = Math.max(power, rearState.getAnalogOutputSignal(world, rearPos, direction.getOpposite()));
        } else if (power < 15 && rearState.isRedstoneConductor(world, rearPos)) {
            // Behind the solid block
            BlockPos furtherPos = rearPos.relative(direction);
            BlockState furtherState = world.getBlockState(furtherPos);

            int behindPower = 0;
            if (furtherState.hasAnalogOutputSignal()) {
                behindPower = furtherState.getAnalogOutputSignal(world, furtherPos, direction.getOpposite());
            }

            // Item Frames
            AABB box = new AABB(furtherPos);
            List<ItemFrame> list = world.getEntitiesOfClass(
                    ItemFrame.class,
                    box,
                    (entity) -> entity.getNearestViewDirection() == direction
            );

            if (list.size() == 1) {
                behindPower = Math.max(behindPower, list.getFirst().getAnalogOutput());
            }

            power = Math.max(power, behindPower);
        }

        return power;
    }

    private boolean isDustPointingTo(BlockState dustState, Direction directionToBlock) {
        if (directionToBlock == Direction.DOWN) return true;
        if (directionToBlock == Direction.UP) return false;

        var property = switch (directionToBlock) {
            case NORTH -> CobaltWireBlock.NORTH;
            case SOUTH -> CobaltWireBlock.SOUTH;
            case EAST -> CobaltWireBlock.EAST;
            case WEST -> CobaltWireBlock.WEST;
            default -> null;
        };

        // isConnected() is a helper method that checks if the dust is visually connected in that direction, which means it's pointing towards the block.
        return property != null && dustState.getValue(property).isConnected();
    }



    @Override
    protected int getAlternateSignal(@NonNull SignalGetter world, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction side1 = facing.getClockWise();
        Direction side2 = facing.getCounterClockWise();

        // Now we get the power from the sides
        return Math.max(
                getCobaltSidePower(world, pos.relative(side1), side1),
                getCobaltSidePower(world, pos.relative(side2), side2)
        );
    }

    // Helper to get the Cobalt Power
    private int getCobaltSidePower(SignalGetter world, BlockPos sidePos, Direction sideDir) {
        BlockState state = world.getBlockState(sidePos);
        int power = 0;

        if (state.getBlock() instanceof CobaltWireBlock) {
            power = state.getValue(CobaltWireBlock.POWER);
        } else if (state.getBlock() instanceof CobaltPowerSource source) {
            power = source.getCobaltSignal(state, (Level)world, sidePos);
        } else if (SignalHelper.compatibleCobaltPowerSource(state)) {
            power = state.getSignal(world, sidePos, sideDir);
        } else if (SignalHelper.restrictedCobaltPowerSource(state)) {
            power = state.getSignal(world, sidePos, sideDir);
        } else if (state.isRedstoneConductor(world, sidePos)) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = sidePos.relative(dir);
                BlockState neighborState = world.getBlockState(neighborPos);

                if (neighborState.getBlock() instanceof CobaltPowerSource src) {
                    if (src.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                        power = Math.max(power, src.getDirectCobaltSignal(neighborState, (Level)world, neighborPos, dir.getOpposite()));
                    }
                } else if (SignalHelper.compatibleCobaltPowerSource(neighborState)) {
                    power = Math.max(power, neighborState.getDirectSignal(world, neighborPos, dir));
                } else if (SignalHelper.restrictedCobaltPowerSource(neighborState)) {
                    power = Math.max(power, neighborState.getDirectSignal(world, neighborPos, dir));
                }
            }
        }

        return power;
    }

    // --- Output Signal ---
    @Override
    public int getCobaltSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof CobaltComparatorBlockEntity be) {
            return be.getOutputSignal();
        }
        return 0;
    }

    @Override
    public int getDirectCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return direction == state.getValue(FACING).getOpposite() ? this.getCobaltSignal(state, world, pos) : 0;
    }

    @Override public boolean isSignalSource(@NonNull BlockState state) { return false; }
    @Override
    protected int getSignal(@NonNull BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction.getOpposite());
        BlockState neighborState = world.getBlockState(neighborPos);

        if (isVanillaRedstone(neighborState)) {
            return 0; // I do not emit if redstone is near me (🛑 Although all these methods must change to only use mixins over redstone components, ig 🛑)
        }
        return super.getSignal(state, world, pos, direction);
    }

    @Override
    protected int getDirectSignal(@NonNull BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        // 1. Identifying the block that is receiving the energy (that is above the torch)
        BlockPos targetPos = pos.relative(direction.getOpposite());
        BlockState targetState = world.getBlockState(targetPos);

        // 2. If the block above is a solid block
        if (targetState.isRedstoneConductor(world, targetPos)) {
            for (Direction side : Direction.values()) {
                // We do not check the torch itself (below)
                if (side == direction) continue;

                BlockPos checkPos = targetPos.relative(side);
                BlockState checkState = world.getBlockState(checkPos);

                // If the stone touches vanilla redstone, the torch "turns off" the Strong Power
                // to avoid the dust from lighting up.
                if (isVanillaRedstone(checkState)) {
                    return 0;
                }
            }
        }

        // If there is no vanilla redstone around the block, proceed normally
        return super.getDirectSignal(state, world, pos, direction);
    }

    private static boolean isVanillaRedstone(BlockState state) {
        return state.is(Blocks.REDSTONE_WIRE) ||
                state.is(Blocks.REPEATER) ||
                state.is(Blocks.COMPARATOR) ||
                state.is(Blocks.REDSTONE_TORCH) ||
                state.is(Blocks.REDSTONE_WALL_TORCH);
    }
}