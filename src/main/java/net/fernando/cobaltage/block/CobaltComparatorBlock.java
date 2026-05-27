package net.fernando.cobaltage.block;

import net.fernando.cobaltage.block.blockentities.CobaltComparatorBlockEntity;
import net.fernando.cobaltage.block.blockentities.ModBlockEntities;
import net.fernando.cobaltage.block.wire.CobaltWireNetwork;
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
        // Impostiamo il default: non sommerso
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(MODE, ComparatorMode.COMPARE)
                .setValue(WATERLOGGED, false));
    }

    // Aggiungi questo metodo per attivare il Ticker
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, @NonNull BlockState state, @NonNull BlockEntityType<T> type) {
        // Il ticker serve solo sul server per elaborare la logica
        if (world.isClientSide()) return null;

        // Verifichiamo che la BlockEntity sia quella corretta
        if (type == ModBlockEntities.COBALT_COMPARATOR_ENTITY) {
            return (world1, pos1, state1, blockEntity) ->
                    CobaltComparatorBlockEntity.tick(world1, pos1, state1, (CobaltComparatorBlockEntity) blockEntity);
        }
        return null;
    }

    // FIX IMPORTANTE: Sovrascrivi updateTarget per avvisare la tua rete Cobalt
    @Override
    protected void updateNeighborsInFront(Level world, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        BlockPos outputPos = pos.relative(direction.getOpposite());

        // 1. Notifica i blocchi vanilla (opzionale)
        world.neighborChanged(outputPos, this, null);
        world.updateNeighborsAtExceptFromFacing(outputPos, this, direction, null);

        // 2. SVEGLIA LA RETE COBALT_INGOT!
        // Se non facciamo questo, il comparatore cambia ma la Cobalt Dust non lo sa.
        if (world.getBlockState(outputPos).getBlock() instanceof CobaltWireBlock) {
            NETWORK_HANDLER.updateNetwork(world, pos);
        }
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // Fondamentale: aggiungiamo la proprietà al builder altrimenti vado in crash
        builder.add(FACING, POWERED, MODE, WATERLOGGED);
    }

    @Override
    public @NonNull BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Controlla se c'è acqua dove stiamo piazzando il blocco
        // Unwrapped method return (si assume che super.getStateForPlacement non restituisca null)
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        BlockState state = super.getStateForPlacement(ctx);
        return state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull Player player, @NonNull BlockHitResult hit) {
        // Eseguiamo prima la logica vanilla (che cambia la modalità e produce il suono "click")
        InteractionResult result = super.useWithoutItem(state, world, pos, player, hit);

        if (!world.isClientSide() && result.consumesAction()) {
            // Il cambio di modalità è avvenuto con successo.
            // Ora dobbiamo "svegliare" la rete Cobalt circostante.

            // 1. Notifichiamo la rete Cobalt partendo da ogni lato del comparatore.
            // Questo forza i cavi vicini a ricalcolare la loro potenza basandosi
            // sulla nuova modalità (Sottrazione o Addizione).
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                NETWORK_HANDLER.updateNetwork(world, neighborPos);
            }

            // 2. Forziamo un tick del blocco immediato.
            // Il flickering del comparatore dipende dai tick programmati (scheduled ticks).
            // Senza questo, il comparatore potrebbe "congelarsi" nell'ultimo stato calcolato.
            world.scheduleTick(pos, this, 1);
        }

        return result;
    }

    @Override
    public @NonNull FluidState getFluidState(BlockState state) {
        // Se è waterlogged, mostra l'acqua, altrimenti no
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        // Flicker issue fix: We take the new computed vanilla state, and we inject the waterlog property
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

    // 2. Calcola e aggiorna il segnale (Logica custom per 1.21.11)
    @Override
    protected void checkTickOnNeighbor(Level world, @NonNull BlockPos pos, @NonNull BlockState state) {
        if (world.getBlockTicks().hasScheduledTick(pos, this)) {
            return;
        }
        int i = this.calculateOutputSignal(world, pos, state);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        int j = blockEntity instanceof CobaltComparatorBlockEntity cobaltBE ? cobaltBE.getOutputSignal() : 0;

        if (i != j || state.getValue(POWERED) != i > 0) {
            // Se il segnale è cambiato, programmiamo un tick per aggiornare
            world.getBlockTicks().schedule(

                    new ScheduledTick<>(this, pos, world.getGameTime() + 2,
                            TickPriority.NORMAL, 0L
                    )

            );

        }

    }

    @Override
    public void tick(@NonNull BlockState state, @NonNull ServerLevel world, @NonNull BlockPos pos, net.minecraft.util.@NonNull RandomSource random) {
        int expectedPower = this.calculateOutputSignal(world, pos, state);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        int currentPower = 0;

        // Risolviamo il bug del cast vanilla usando la NOSTRA entità Cobalt
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
        // 1. Controlliamo se il blocco può sopravvivere (es, se il blocco sotto viene rimosso)
        if (!state.canSurvive(world, pos)) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            dropResources(state, world, pos, blockEntity);
            world.removeBlock(pos, false);
            return;
        }

        // 2. Chiamiamo la logica di aggiornamento del segnale
        // Questo metodo (ereditato da ComparatorBlock) ricalcola uscita
        this.checkTickOnNeighbor(world, pos, state);
    }

    private int calculateOutputSignal(Level world, BlockPos pos, BlockState state) {
        // 🛑 FIX 1: Usa il tuo metodo getPower() che contiene tutta la logica degli inventari!
        // Prima qui c'era this.getPowerOnBack(...) che ignorava le chest.
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


    protected int getPowerOnSides(LevelReader world, BlockPos pos, BlockState state) {
        int maxSidePower = 0;
        Direction facing = state.getValue(FACING);

        // Controlla i due lati (Destra e Sinistra rispetto alla direzione)
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (side != facing && side != facing.getOpposite()) {
                BlockPos sidePos = pos.relative(side);
                BlockState sideState = world.getBlockState(sidePos);

                int p = 0;
                if (sideState.getBlock() instanceof CobaltWireBlock) {
                    p = sideState.getValue(CobaltWireBlock.POWER);
                } else if (sideState.getBlock() instanceof CobaltPowerSource source) {
                    p = source.getCobaltPower(sideState, (Level)world, sidePos);
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

        // --- 1. LEGGE RETE COBALT_INGOT E BLOCCHI COMPATIBILI ---
        if (rearState.getBlock() instanceof CobaltPowerSource source) {
            if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                power = source.getCobaltPower(rearState, world, rearPos);
            }
        } else if (rearState.getBlock() instanceof CobaltWireBlock) {
            power = rearState.getValue(CobaltWireBlock.POWER);
        } else if (CobaltWireNetwork.compatibleCobaltPowerSource(rearState)) {
            // Legge leve o bottoni piazzati direttamente dietro
            power = rearState.getSignal(world, rearPos, direction);
        } else if (rearState.isRedstoneConductor(world, rearPos) || rearState.getBlock() instanceof PoweredBlock) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = rearPos.relative(dir);
                BlockState neighborState = world.getBlockState(neighborPos);

                // CONTROLLO RICHIESTO: Polvere di cobalto che punta al blocco
                if (neighborState.getBlock() instanceof CobaltWireBlock) {
                    // Verifichiamo se il MODELLO della dust punta verso il blocco solido
                    if (isDustPointingTo(neighborState, dir.getOpposite())) {
                        power = Math.max(power, neighborState.getValue(CobaltWireBlock.POWER));
                    }
                }
                // Altre sorgenti che caricano il blocco (Strong Power)
                else if (neighborState.getBlock() instanceof CobaltPowerSource src) {
                    if (src.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                        power = Math.max(power, src.getStrongCobaltPower(neighborState, world, neighborPos, dir.getOpposite()));
                    }
                } else if (CobaltWireNetwork.compatibleCobaltPowerSource(neighborState)) {
                    power = Math.max(power, neighborState.getDirectSignal(world, neighborPos, dir));
                }
            }
        }

        // --- 2. LEGGE INVENTARI E ITEM FRAME ---
        if (rearState.hasAnalogOutputSignal()) {
            power = Math.max(power, rearState.getAnalogOutputSignal(world, rearPos, direction.getOpposite()));
        } else if (power < 15 && rearState.isRedstoneConductor(world, rearPos)) {
            // Se c'è un blocco solido, guarda cosa c'è dietro
            BlockPos furtherPos = rearPos.relative(direction);
            BlockState furtherState = world.getBlockState(furtherPos);

            int behindPower = 0;
            if (furtherState.hasAnalogOutputSignal()) {
                behindPower = furtherState.getAnalogOutputSignal(world, furtherPos, direction.getOpposite());
            }

            // Cerca Item Frame appesi al blocco solido
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
        if (directionToBlock == Direction.DOWN) return true; // Sopra il blocco: alimenta sempre
        if (directionToBlock == Direction.UP) return false;   // Sotto il blocco: non alimenta

        // Controlliamo le proprietà NORTH, SOUTH, EAST, WEST del CobaltWireBlock
        var property = switch (directionToBlock) {
            case NORTH -> CobaltWireBlock.NORTH;
            case SOUTH -> CobaltWireBlock.SOUTH;
            case EAST -> CobaltWireBlock.EAST;
            case WEST -> CobaltWireBlock.WEST;
            default -> null;
        };

        // isConnected() restituisce true se lo stato è SIDE o UP (quindi punta verso il blocco)
        return property != null && dustState.getValue(property).isConnected();
    }



    @Override
    protected int getAlternateSignal(@NonNull SignalGetter world, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction side1 = facing.getClockWise();
        Direction side2 = facing.getCounterClockWise();

        // Ora passiamo anche la direzione verso cui stiamo guardando il lato
        return Math.max(
                getCobaltSidePower(world, pos.relative(side1), side1),
                getCobaltSidePower(world, pos.relative(side2), side2)
        );
    }

    private int getCobaltSidePower(SignalGetter world, BlockPos sidePos, Direction sideDir) {
        BlockState state = world.getBlockState(sidePos);
        int power = 0;

        if (state.getBlock() instanceof CobaltWireBlock) {
            power = state.getValue(CobaltWireBlock.POWER);
        } else if (state.getBlock() instanceof CobaltPowerSource source) {
            power = source.getCobaltPower(state, (Level)world, sidePos);
        } else if (CobaltWireNetwork.compatibleCobaltPowerSource(state)) {
            // Legge i componenti compatibili attaccati ai lati
            power = state.getSignal(world, sidePos, sideDir);
        } else if (state.isRedstoneConductor(world, sidePos)) {
            // I lati possono anche essere alimentati da blocchi solidi energizzati
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = sidePos.relative(dir);
                BlockState neighborState = world.getBlockState(neighborPos);

                if (neighborState.getBlock() instanceof CobaltPowerSource src) {
                    if (src.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                        power = Math.max(power, src.getStrongCobaltPower(neighborState, (Level)world, neighborPos, dir.getOpposite()));
                    }
                } else if (CobaltWireNetwork.compatibleCobaltPowerSource(neighborState)) {
                    power = Math.max(power, neighborState.getDirectSignal(world, neighborPos, dir));
                }
            }
        }

        return power;
    }

    // --- OUTPUT ---
    @Override
    public int getCobaltPower(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof CobaltComparatorBlockEntity be) {
            return be.getOutputSignal();
        }
        return 0;
    }

    @Override
    public int getStrongCobaltPower(BlockState state, Level world, BlockPos pos, Direction direction) {
        return direction == state.getValue(FACING).getOpposite() ? this.getCobaltPower(state, world, pos) : 0;
    }

    @Override public boolean isSignalSource(@NonNull BlockState state) { return false; }
    @Override
    protected int getSignal(@NonNull BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction.getOpposite());
        BlockState neighborState = world.getBlockState(neighborPos);

        if (isVanillaRedstone(neighborState)) {
            return 0; // Niente energia per te, redstone rossa!
        }
        return super.getSignal(state, world, pos, direction);
    }

    @Override
    protected int getDirectSignal(@NonNull BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        // 1. Identifichiamo il blocco che sta ricevendo l'energia (quello sopra la torcia)
        BlockPos targetPos = pos.relative(direction.getOpposite());
        BlockState targetState = world.getBlockState(targetPos);

        // 2. Se il blocco sopra è un blocco solido (Pietra, Cobblestone, ecc.)
        if (targetState.isRedstoneConductor(world, targetPos)) {
            // Controlliamo i vicini del blocco di pietra!
            for (Direction side : Direction.values()) {
                // Non controlliamo la torcia stessa (sotto)
                if (side == direction) continue;

                BlockPos checkPos = targetPos.relative(side);
                BlockState checkState = world.getBlockState(checkPos);

                // Se la pietra tocca Redstone Vanilla, la torcia "spegne" la Strong Power
                // per evitare che la polvere si accenda.
                if (isVanillaRedstone(checkState)) {
                    return 0;
                }
            }
        }

        // Se non c'è redstone vanilla attorno al blocco caricato, procedi normalmente
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