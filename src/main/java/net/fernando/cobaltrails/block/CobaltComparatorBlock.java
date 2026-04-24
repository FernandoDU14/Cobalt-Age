package net.fernando.cobaltrails.block;

import net.fernando.cobaltrails.block.entity.CobaltComparatorBlockEntity;
import net.fernando.cobaltrails.block.entity.ModBlockEntities;
import net.fernando.cobaltrails.block.wire.CobaltWireNetwork;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
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
    private static final net.fernando.cobaltrails.block.wire.CobaltWireNetwork NETWORK_HANDLER = new net.fernando.cobaltrails.block.wire.CobaltWireNetwork();

    public CobaltComparatorBlock(Settings settings) {
        super(settings);
        // Impostiamo il default: non sommerso
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWERED, false)
                .with(MODE, ComparatorMode.COMPARE)
                .with(WATERLOGGED, false));
    }

    // Aggiungi questo metodo per attivare il Ticker
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // Il ticker serve solo sul server per elaborare la logica
        if (world.isClient()) return null;

        // Verifichiamo che la BlockEntity sia quella corretta
        if (type == ModBlockEntities.COBALT_COMPARATOR_ENTITY) {
            return (world1, pos1, state1, blockEntity) ->
                    CobaltComparatorBlockEntity.tick(world1, pos1, state1, (CobaltComparatorBlockEntity) blockEntity);
        }
        return null;
    }

    // FIX IMPORTANTE: Sovrascrivi updateTarget per avvisare la tua rete Cobalt
    @Override
    protected void updateTarget(World world, BlockPos pos, BlockState state) {
        Direction direction = state.get(FACING);
        BlockPos outputPos = pos.offset(direction.getOpposite());

        // 1. Notifica i blocchi vanilla (opzionale)
        world.updateNeighbor(outputPos, this, null);
        world.updateNeighborsExcept(outputPos, this, direction, null);

        // 2. SVEGLIA LA RETE COBALT!
        // Se non facciamo questo, il comparatore cambia ma la Cobalt Dust non lo sa.
        if (world.getBlockState(outputPos).getBlock() instanceof CobaltWireBlock) {
            NETWORK_HANDLER.updateNetwork(world, pos);
        }
    }


    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        // Fondamentale: aggiungiamo la proprietà al builder altrimenti vado in crash
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
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Eseguiamo prima la logica vanilla (che cambia la modalità e produce il suono "click")
        ActionResult result = super.onUse(state, world, pos, player, hit);

        if (!world.isClient() && result.isAccepted()) {
            // Il cambio di modalità è avvenuto con successo.
            // Ora dobbiamo "svegliare" la rete Cobalt circostante.

            // 1. Notifichiamo la rete Cobalt partendo da ogni lato del comparatore.
            // Questo forza i cavi vicini a ricalcolare la loro potenza basandosi
            // sulla nuova modalità (Sottrazione o Addizione).
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.offset(dir);
                NETWORK_HANDLER.updateNetwork(world, neighborPos);
            }

            // 2. Forziamo un tick del blocco immediato.
            // Il flickering del comparatore dipende dai tick programmati (scheduled ticks).
            // Senza questo, il comparatore potrebbe "congelarsi" nell'ultimo stato calcolato.
            world.scheduleBlockTick(pos, this, 1);
        }

        return result;
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
        // 1. Controlliamo se il blocco può sopravvivere (es, se il blocco sotto viene rimosso)
        if (!state.canPlaceAt(world, pos)) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            dropStacks(state, world, pos, blockEntity);
            world.removeBlock(pos, false);
            return;
        }

        // 2. Chiamiamo la logica di aggiornamento del segnale
        // Questo metodo (ereditato da ComparatorBlock) ricalcola uscita
        this.updatePowered(world, pos, state);
    }

    private int calculateOutputSignal(World world, BlockPos pos, BlockState state) {
        // 🛑 FIX 1: Usa il tuo metodo getPower() che contiene tutta la logica degli inventari!
        // Prima qui c'era this.getPowerOnBack(...) che ignorava le chest.
        int i = this.getPower(world, pos, state);
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




    @Override
    public int getPower(World world, BlockPos pos, BlockState state) {
        int power = 0;
        Direction direction = state.get(FACING);
        BlockPos rearPos = pos.offset(direction);
        BlockState rearState = world.getBlockState(rearPos);

        // --- 1. LEGGE RETE COBALT E BLOCCHI COMPATIBILI ---
        if (rearState.getBlock() instanceof CobaltPowerSource source) {
            if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                power = source.getCobaltPower(rearState, world, rearPos);
            }
        } else if (rearState.getBlock() instanceof CobaltWireBlock) {
            power = rearState.get(CobaltWireBlock.POWER);
        } else if (CobaltWireNetwork.compatibleCobaltPowerSource(rearState)) {
            // Legge leve o bottoni piazzati direttamente dietro
            power = rearState.getWeakRedstonePower(world, rearPos, direction);
        } else if (rearState.isSolidBlock(world, rearPos)) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = rearPos.offset(dir);
                BlockState neighborState = world.getBlockState(neighborPos);

                // CONTROLLO RICHIESTO: Polvere di cobalto che punta al blocco
                if (neighborState.getBlock() instanceof CobaltWireBlock) {
                    // Verifichiamo se il MODELLO della dust punta verso il blocco solido
                    if (isDustPointingTo(neighborState, dir.getOpposite())) {
                        power = Math.max(power, neighborState.get(CobaltWireBlock.POWER));
                    }
                }
                // Altre sorgenti che caricano il blocco (Strong Power)
                else if (neighborState.getBlock() instanceof CobaltPowerSource src) {
                    if (src.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                        power = Math.max(power, src.getStrongCobaltPower(neighborState, world, neighborPos, dir.getOpposite()));
                    }
                } else if (CobaltWireNetwork.compatibleCobaltPowerSource(neighborState)) {
                    power = Math.max(power, neighborState.getStrongRedstonePower(world, neighborPos, dir));
                }
            }
        }

        // --- 2. LEGGE INVENTARI E ITEM FRAME ---
        if (rearState.hasComparatorOutput()) {
            power = Math.max(power, rearState.getComparatorOutput(world, rearPos, direction.getOpposite()));
        } else if (power < 15 && rearState.isSolidBlock(world, rearPos)) {
            // Se c'è un blocco solido, guarda cosa c'è dietro
            BlockPos furtherPos = rearPos.offset(direction);
            BlockState furtherState = world.getBlockState(furtherPos);

            int behindPower = 0;
            if (furtherState.hasComparatorOutput()) {
                behindPower = furtherState.getComparatorOutput(world, furtherPos, direction.getOpposite());
            }

            // Cerca Item Frame appesi al blocco solido
            Box box = new Box(furtherPos);
            List<ItemFrameEntity> list = world.getEntitiesByClass(
                    ItemFrameEntity.class,
                    box,
                    (entity) -> entity != null && entity.getFacing() == direction
            );

            if (list.size() == 1) {
                behindPower = Math.max(behindPower, list.getFirst().getComparatorPower());
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
        return property != null && dustState.get(property).isConnected();
    }



    @Override
    protected int getMaxInputLevelSides(RedstoneView world, BlockPos pos, BlockState state) {
        Direction facing = state.get(FACING);
        Direction side1 = facing.rotateYClockwise();
        Direction side2 = facing.rotateYCounterclockwise();

        // Ora passiamo anche la direzione verso cui stiamo guardando il lato
        return Math.max(
                getCobaltSidePower(world, pos.offset(side1), side1),
                getCobaltSidePower(world, pos.offset(side2), side2)
        );
    }

    private int getCobaltSidePower(RedstoneView world, BlockPos sidePos, Direction sideDir) {
        BlockState state = world.getBlockState(sidePos);
        int power = 0;

        if (state.getBlock() instanceof CobaltWireBlock) {
            power = state.get(CobaltWireBlock.POWER);
        } else if (state.getBlock() instanceof CobaltPowerSource source) {
            power = source.getCobaltPower(state, (World)world, sidePos);
        } else if (CobaltWireNetwork.compatibleCobaltPowerSource(state)) {
            // Legge i componenti compatibili attaccati ai lati
            power = state.getWeakRedstonePower(world, sidePos, sideDir);
        } else if (state.isSolidBlock(world, sidePos)) {
            // I lati possono anche essere alimentati da blocchi solidi energizzati
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = sidePos.offset(dir);
                BlockState neighborState = world.getBlockState(neighborPos);

                if (neighborState.getBlock() instanceof CobaltPowerSource src) {
                    if (src.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                        power = Math.max(power, src.getStrongCobaltPower(neighborState, (World)world, neighborPos, dir.getOpposite()));
                    }
                } else if (CobaltWireNetwork.compatibleCobaltPowerSource(neighborState)) {
                    power = Math.max(power, neighborState.getStrongRedstonePower(world, neighborPos, dir));
                }
            }
        }

        return power;
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