package net.fernando.cobaltrails.block;

import net.fernando.cobaltrails.block.entity.CobaltComparatorBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CobaltComparatorBlock extends ComparatorBlock implements Waterloggable, CobaltPowerSource {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public CobaltComparatorBlock(Settings settings) {
        super(settings);
        // Impostiamo il default: non sommerso
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWERED, false)
                .with(MODE, ComparatorMode.COMPARE)
                .with(WATERLOGGED, false));
    }


    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        // Fondamentale: aggiungiamo la proprietà al builder altrimenti crasha
        builder.add(FACING, POWERED, MODE, WATERLOGGED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Controlla se c'è acqua dove stiamo piazzando il blocco
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        BlockState state = super.getPlacementState(ctx);
        if (state != null) {
            return state.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
        }
        return null;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        // Se è waterlogged, mostra l'acqua, altrimenti no
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        if (state.get(WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        // Flicker issue fix: We take the new computed vanilla state, and we inject the waterlog property
        BlockState newState =  super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (newState.isOf(this)) {
            return newState.with(WATERLOGGED, state.get(WATERLOGGED));
        }
        return newState;
    }





    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CobaltComparatorBlockEntity(pos, state);
    }

    // 1. Read the signal of the block entity
    @Override
    protected int getOutputLevel(BlockView world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CobaltComparatorBlockEntity cobaltBE) {
            return cobaltBE.getOutputSignal();
        }
        return 0;
    }

    // 2. Calcola e aggiorna il segnale (Logica custom per 1.21.11)
    @Override
    protected void updatePowered(World world, BlockPos pos, BlockState state) {
        if (world.getBlockTickScheduler().isQueued(pos, this)) {
            return;
        }
        int i = this.calculateOutputSignal(world, pos, state);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        int j = blockEntity instanceof CobaltComparatorBlockEntity cobaltBE ? cobaltBE.getOutputSignal() : 0;

        if (i != j || state.get(POWERED) != i > 0) {
            // Se il segnale è cambiato, programmiamo un tick per aggiornare
            world.getBlockTickScheduler().scheduleTick(

                    new net.minecraft.world.tick.OrderedTick<>(
                            this,           // Il blocco (CobaltComparatorBlock)
                            pos,            // La posizione
                            world.getTime() + 2, // La priorità
                            net.minecraft.world.tick.TickPriority.NORMAL, // Il tempo esatto in cui deve scattare (Tempo attuale + 2 tick)
                            0L
                    )

            );

        }

    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        int expectedPower = this.calculateOutputSignal(world, pos, state);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        int currentPower = 0;

        // Risolviamo il bug del cast vanilla usando la NOSTRA entità Cobalt
        if (blockEntity instanceof CobaltComparatorBlockEntity cobaltEntity) {
            currentPower = cobaltEntity.getOutputSignal();
            cobaltEntity.setOutputSignal(expectedPower);
        }

        if (currentPower != expectedPower || state.get(MODE) == ComparatorMode.COMPARE) {
            boolean shouldBePowered = this.hasPower(world, pos, state);
            boolean isPowered = state.get(POWERED);
            if (isPowered && !shouldBePowered) {
                world.setBlockState(pos, state.with(POWERED, false), 2);
            } else if (!isPowered && shouldBePowered) {
                world.setBlockState(pos, state.with(POWERED, true), 2);
            }
            this.updateTarget(world, pos, state);
        }
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        // 1. Controlliamo se il blocco può sopravvivere (es. se il blocco sotto viene rimosso)
        if (!state.canPlaceAt(world, pos)) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            dropStacks(state, world, pos, blockEntity);
            world.removeBlock(pos, false);
            return;
        }

        // 2. Chiamiamo la logica di aggiornamento del segnale
        // Questo metodo (ereditato da ComparatorBlock) ricalcola l'output
        this.updatePowered(world, pos, state);
    }

    private int calculateOutputSignal(World world, BlockPos pos, BlockState state) {
        int i = this.getPowerOnBack(world, pos, state);
        if (i == 0) {
            return 0;
        } else {
            int j = this.getPowerOnSides(world, pos, state);
            if (j > i) {
                return 0;
            } else {
                return state.get(MODE) == ComparatorMode.SUBTRACT ? i - j : i;
            }
        }
    }



    // Servono anche questi due piccoli metodi di supporto perché spesso sono private o final
    private int getPowerOnBack(World world, BlockPos pos, BlockState state) {
        Direction direction = state.get(FACING);
        BlockPos blockPos = pos.offset(direction);
        return this.getPowerOnSide(world, blockPos, direction);
    }

    protected int getPowerOnSides(WorldView world, BlockPos pos, BlockState state) {
        int maxSidePower = 0;
        Direction facing = state.get(FACING);

        // Controlla i due lati (Destra e Sinistra rispetto alla direzione)
        for (Direction side : Direction.Type.HORIZONTAL) {
            if (side != facing && side != facing.getOpposite()) {
                BlockPos sidePos = pos.offset(side);
                BlockState sideState = world.getBlockState(sidePos);

                int p = 0;
                if (sideState.getBlock() instanceof CobaltWireBlock) {
                    p = sideState.get(CobaltWireBlock.POWER);
                } else if (sideState.getBlock() instanceof CobaltPowerSource source) {
                    p = source.getCobaltPower(sideState, (World)world, sidePos);
                }

                if (p > maxSidePower) maxSidePower = p;
            }
        }
        return maxSidePower;
    }

    private int getPowerOnSide(World world, BlockPos pos, Direction side) {
        BlockState state = world.getBlockState(pos);

        // If the neighbor emits the signal (f.i. dust, torches, leve)
        if (this.isRedstoneConductor(world, pos, state)) {
            return world.getReceivedRedstonePower(pos);
        }

        // Else it will read the signal emitted by the block
        return state.getWeakRedstonePower(world, pos, side);
    }



    // To understand if this block can conduct power
    private boolean isRedstoneConductor(World world, BlockPos pos, BlockState state) {
        return state.isSolidBlock(world, pos);
    }


    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(POWERED)) {
            // Blue Particle Logic, as we did for the Repeater and Torch
            Direction direction = state.get(FACING);
            double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double e = (double)pos.getY() + 0.4 + (random.nextDouble() - 0.5) * 0.2;
            double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;

            int cobaltBlue = (0 << 16) | (153 << 8) | 255;
            DustParticleEffect cobaltDust = new DustParticleEffect(cobaltBlue, 1.0f);

            world.addParticleClient(cobaltDust, d, e, f, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected int getPower(World world, BlockPos pos, BlockState state) {
        int power = 0;
        Direction direction = state.get(FACING);
        BlockPos rearPos = pos.offset(direction);
        BlockState rearState = world.getBlockState(rearPos);

        // --- 1. LEGGE RETE COBALT ---
        if (rearState.getBlock() instanceof CobaltPowerSource source) {
            if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                power = source.getCobaltPower(rearState, world, rearPos);
            }
        } else if (rearState.getBlock() instanceof CobaltWireBlock) {
            power = rearState.get(CobaltWireBlock.POWER);
        }

        // --- 2. LEGGE INVENTARI, SCULK SENSOR E ITEM FRAME ---
        if (rearState.hasComparatorOutput()) {
            power = Math.max(power, rearState.getComparatorOutput(world, rearPos, direction));
        } else if (power < 15 && rearState.isSolidBlock(world, rearPos)) {
            // Se c'è un blocco solido, guarda cosa c'è dietro (es. Blocco di Pietra con Item Frame attaccato)
            BlockPos furtherPos = rearPos.offset(direction);
            BlockState furtherState = world.getBlockState(furtherPos);

            int behindPower = 0;
            if (furtherState.hasComparatorOutput()) {
                behindPower = furtherState.getComparatorOutput(world, furtherPos, direction);
            }

            // Cerca Item Frame appesi al blocco solido
            Box box = new Box(furtherPos);
            List<ItemFrameEntity> list = world.getEntitiesByClass(
                    ItemFrameEntity.class,
                    box,
                    (entity) -> entity != null && entity.getFacing() == direction
            );

            if (list.size() == 1) {
                behindPower = Math.max(behindPower, list.get(0).getComparatorPower());
            }

            power = Math.max(power, behindPower);
        }

        return power;
    }

    /**
     * Calcola la potenza Cobalt che entra nel comparatore dal lato posteriore.
     */
    private int getCobaltInputFromBehind(World world, BlockPos pos, BlockState state) {
        Direction direction = state.get(FACING);
        BlockPos blockPos = pos.offset(direction);
        BlockState blockState = world.getBlockState(blockPos);

        // Se dietro c'è un cavo Cobalt
        if (blockState.getBlock() instanceof CobaltWireBlock) {
            return blockState.get(CobaltWireBlock.POWER);
        }

        // Se dietro c'è una sorgente Cobalt (Leva, Torcia Cobalt, ecc.)
        if (blockState.getBlock() instanceof CobaltPowerSource source) {
            return source.getCobaltPower(blockState, world, blockPos);
        }

        return 0;
    }

    /**
     * Cerca un'entità Cornice (Item Frame) attaccata al lato corretto del blocco specificato.
     */
    private ItemFrameEntity getAttachedItemFrame(World world, Direction facing, BlockPos pos) {
        // Cerchiamo entità ItemFrameEntity nella zona del blocco bersaglio
        List<ItemFrameEntity> list = world.getEntitiesByClass(
                ItemFrameEntity.class,
                new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
                (itemFrame) -> itemFrame != null && itemFrame.getHorizontalFacing() == facing
        );

        return list.size() == 1 ? list.get(0) : null;
    }

    @Override
    protected int getMaxInputLevelSides(RedstoneView world, BlockPos pos, BlockState state) {
        Direction facing = state.get(FACING);
        Direction side1 = facing.rotateYClockwise();
        Direction side2 = facing.rotateYCounterclockwise();

        return Math.max(
                getCobaltSidePower(world, pos.offset(side1)),
                getCobaltSidePower(world, pos.offset(side2))
        );
    }

    private int getCobaltSidePower(RedstoneView world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof CobaltWireBlock) return state.get(CobaltWireBlock.POWER);
        if (state.getBlock() instanceof CobaltPowerSource source) return source.getCobaltPower(state, (World)world, pos);
        return 0;
    }

    // --- OUTPUT ---
    @Override
    public int getCobaltPower(BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof CobaltComparatorBlockEntity be) {
            return be.getOutputSignal();
        }
        return 0;
    }

    @Override
    public int getStrongCobaltPower(BlockState state, World world, BlockPos pos, Direction direction) {
        return direction == state.get(FACING).getOpposite() ? this.getCobaltPower(state, world, pos) : 0;
    }

    @Override public boolean emitsRedstonePower(BlockState state) { return false; }
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
        // 1. Identifichiamo il blocco che sta ricevendo l'energia (quello sopra la torcia)
        BlockPos targetPos = pos.offset(direction.getOpposite());
        BlockState targetState = world.getBlockState(targetPos);

        // 2. Se il blocco sopra è un blocco solido (Pietra, Cobblestone, ecc.)
        if (targetState.isSolidBlock(world, targetPos)) {
            // Controlliamo i vicini del blocco di pietra!
            for (Direction side : Direction.values()) {
                // Non controlliamo la torcia stessa (sotto)
                if (side == direction) continue;

                BlockPos checkPos = targetPos.offset(side);
                BlockState checkState = world.getBlockState(checkPos);

                // Se la pietra tocca Redstone Vanilla, la torcia "spegne" la Strong Power
                // per evitare che la polvere si accenda.
                if (isVanillaRedstone(checkState)) {
                    return 0;
                }
            }
        }

        // Se non c'è redstone vanilla attorno al blocco caricato, procedi normalmente
        return super.getStrongRedstonePower(state, world, pos, direction);
    }

    // Lista dei blocchi vanilla da tenere isolati
    private static boolean isVanillaRedstone(BlockState state) {
        return state.isOf(Blocks.REDSTONE_WIRE) ||
                state.isOf(Blocks.REPEATER) ||
                state.isOf(Blocks.COMPARATOR) ||
                state.isOf(Blocks.REDSTONE_TORCH) ||
                state.isOf(Blocks.REDSTONE_WALL_TORCH) ||
                state.isOf(Blocks.REDSTONE_BLOCK);
    }
}