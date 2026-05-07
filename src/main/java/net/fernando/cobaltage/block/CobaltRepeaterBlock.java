package net.fernando.cobaltage.block;

import net.fernando.cobaltage.block.wire.CobaltWireNetwork;
import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

public class CobaltRepeaterBlock extends RepeaterBlock implements Waterloggable, CobaltPowerSource {
    public CobaltRepeaterBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(DELAY, 1)
                .with(LOCKED, false)
                .with(POWERED, false)
                .with(WATERLOGGED, false)); // Aggiungi questo
    }

    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(POWERED)) {
            Direction direction = state.get(FACING);

            // 1. Punto di partenza al centro del blocco (con leggera oscillazione casuale)
            double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double e = (double)pos.getY() + 0.4 + (random.nextDouble() - 0.5) * 0.2;
            double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;

            // 2. Calcolo della posizione della torcia
            float g = -5.0F; // Offset della torcia fissa (quella posteriore)

            if (random.nextBoolean()) {
                // Offset della torcia mobile: cambia in base allo stato DELAY (1, 2, 3 o 4)
                g = (float)(state.get(DELAY) * 2 - 1);
            }

            // Dividiamo per 16 per convertire i "pixel" del modello 3D in coordinate del blocco
            g /= 16.0F;

            // 3. Spostiamo la particella lungo l'asse X e Z corretto in base a dove guarda il repeater
            double offsetX = (g * (float)direction.getOffsetX());
            double offsetZ = (g * (float)direction.getOffsetZ());

            // 4. Creiamo la tua particella personalizzata
            int cobaltBlue = (0) | (153 << 8) | 255;
            DustParticleEffect cobaltDust = new DustParticleEffect(cobaltBlue, 1.0f);

            // 5. Spawnamo la particella aggiungendo l'offset calcolato
            world.addParticleClient(cobaltDust, d + offsetX, e, f + offsetZ, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, DELAY, LOCKED, POWERED); // All properties we have in the .json file.
        builder.add(WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        BlockState state = super.getPlacementState(ctx);
        if (state != null) {
            return state.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
        }
        return null;
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
    // --- LOGICA DI INPUT (DIETRO) ---
    @Override
    protected int getPower(World world, BlockPos pos, BlockState state) {
        Direction direction = state.get(FACING);
        BlockPos rearPos = pos.offset(direction);
        BlockState rearState = world.getBlockState(rearPos);

        int power = 0;

        // 🟦 1. Sorgenti Cobalt
        if (rearState.getBlock() instanceof CobaltPowerSource source) {
            if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                power = source.getCobaltPower(rearState, world, rearPos);
            }
        }
        // 🟦 2. Cavo Cobalt
        else if (rearState.getBlock() instanceof CobaltWireBlock) {
            power = rearState.get(CobaltWireBlock.POWER);
        }
        // 🟦 3. Sorgente Vanilla Compatibile Diretta (Leva, Bottone attaccato direttamente dietro)
        else if (CobaltWireNetwork.compatibleCobaltPowerSource(rearState)) {
            // FIX: Usiamo rearState e rearPos invece di state e pos!
            power = rearState.getWeakRedstonePower(world, rearPos, direction);
        }
        // 🟦 4. Blocco Solido caricato da energia forte
        else if (rearState.isSolidBlock(world, rearPos) || rearState.getBlock() instanceof RedstoneBlock) {
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

        return power;
    }

    // Metodo Helper per verificare la connessione visuale
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

    // --- LOGICA DI BLOCCAGGIO (LATERALE) ---
    @Override
    protected int getMaxInputLevelSides(RedstoneView world, BlockPos pos, BlockState state) {
        Direction facing = state.get(FACING);
        Direction side1 = facing.rotateYClockwise();
        Direction side2 = facing.rotateYCounterclockwise();
        return Math.max(getCobaltSidePower(world, pos.offset(side1), facing), getCobaltSidePower(world, pos.offset(side2), facing));
    }

    private int getCobaltSidePower(RedstoneView world, BlockPos sidePos, Direction facing) {
        BlockState state = world.getBlockState(sidePos);
        // Solo Repeater/Comparatori Cobalt possono bloccare un Repeater Cobalt
        if (state.getBlock() instanceof CobaltRepeaterBlock || state.getBlock() instanceof CobaltComparatorBlock) {
            if((state.get(FACING) == facing.rotateYClockwise()) || (state.get(FACING) == facing.rotateYCounterclockwise()))
                return ((CobaltPowerSource) state.getBlock()).getCobaltPower(state, (World) world, sidePos);
        }
        return 0;
    }

    @Override
    public int getCobaltPower(BlockState state, World world, BlockPos pos) {
        // Il repeater emette energia SOLO se è acceso (POWERED)
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongCobaltPower(BlockState state, World world, BlockPos pos, Direction direction) {
        // Direction è la faccia del blocco adiacente che viene colpita.
        // Il repeater emette "Strong Power" solo dalla sua faccia anteriore.
        return (state.get(FACING).getOpposite() == direction && state.get(POWERED)) ? 15 : 0;
    }

    // --- ISOLAMENTO TOTALE ---
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
                // Non controlliamo il repeater stesso (lato)
                if (side == direction) continue;

                BlockPos checkPos = targetPos.offset(side);
                BlockState checkState = world.getBlockState(checkPos);

                // Se la pietra tocca Redstone Vanilla, il cobalt repeater "spegne" la Strong Power
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
        // Here we need REDSTONE BLOCK
        return state.isOf(Blocks.REDSTONE_WIRE) ||
                state.isOf(Blocks.REPEATER) ||
                state.isOf(Blocks.COMPARATOR) ||
                state.isOf(Blocks.REDSTONE_TORCH) ||
                state.isOf(Blocks.REDSTONE_WALL_TORCH) ||
                state.isOf(Blocks.REDSTONE_BLOCK);
    }
}