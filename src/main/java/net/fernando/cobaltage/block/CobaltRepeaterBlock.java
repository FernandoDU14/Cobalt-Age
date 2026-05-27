package net.fernando.cobaltage.block;

import net.fernando.cobaltage.block.wire.CobaltWireNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.NonNull;

public class CobaltRepeaterBlock extends RepeaterBlock implements SimpleWaterloggedBlock, CobaltPowerSource {
    public CobaltRepeaterBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(DELAY, 1)
                .setValue(LOCKED, false)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false)); // Aggiungi questo
    }

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    @Override
    public void animateTick(BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        if (state.getValue(POWERED)) {
            Direction direction = state.getValue(FACING);

            // 1. Punto di partenza al centro del blocco (con leggera oscillazione casuale)
            double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double e = (double)pos.getY() + 0.4 + (random.nextDouble() - 0.5) * 0.2;
            double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;

            // 2. Calcolo della posizione della torcia
            float g = -5.0F; // Offset della torcia fissa (quella posteriore)

            if (random.nextBoolean()) {
                // Offset della torcia mobile: cambia in base allo stato DELAY (1, 2, 3 o 4)
                g = (float)(state.getValue(DELAY) * 2 - 1);
            }

            // Dividiamo per 16 per convertire i "pixel" del modello 3D in coordinate del blocco
            g /= 16.0F;

            // 3. Spostiamo la particella lungo l'asse X e Z corretto in base a dove guarda il repeater
            double offsetX = (g * (float)direction.getStepX());
            double offsetZ = (g * (float)direction.getStepZ());

            // 4. Creiamo la tua particella personalizzata
            int cobaltBlue = (0) | (153 << 8) | 255;
            DustParticleOptions cobaltDust = new DustParticleOptions(cobaltBlue, 1.0f);

            // 5. Spawnamo la particella aggiungendo l'offset calcolato
            world.addParticle(cobaltDust, d + offsetX, e, f + offsetZ, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DELAY, LOCKED, POWERED); // All properties we have in the .json file.
        builder.add(WATERLOGGED);
    }

    @Override
    public @NonNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public @NonNull BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Unwrapped if statement assuming NonNull state
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        BlockState state = super.getStateForPlacement(ctx);
        return state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
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
    // --- LOGICA DI INPUT (DIETRO) ---
    @Override
    protected int getInputSignal(Level world, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        BlockPos rearPos = pos.relative(direction);
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
            power = rearState.getValue(CobaltWireBlock.POWER);
        }
        // 🟦 3. Sorgente Vanilla Compatibile Diretta (Leva, Bottone attaccato direttamente dietro)
        else if (CobaltWireNetwork.compatibleCobaltPowerSource(rearState)) {
            // FIX: Usiamo rearState e rearPos invece di state e pos!
            power = rearState.getSignal(world, rearPos, direction);
        }
        // 🟦 4. Blocco Solido caricato da energia forte
        else if (rearState.isRedstoneConductor(world, rearPos) || rearState.getBlock() instanceof PoweredBlock) {
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
        return property != null && dustState.getValue(property).isConnected();
    }

    // --- LOGICA DI BLOCCAGGIO (LATERALE) ---
    @Override
    protected int getAlternateSignal(@NonNull SignalGetter world, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction side1 = facing.getClockWise();
        Direction side2 = facing.getCounterClockWise();
        return Math.max(getCobaltSidePower(world, pos.relative(side1), facing), getCobaltSidePower(world, pos.relative(side2), facing));
    }

    private int getCobaltSidePower(SignalGetter world, BlockPos sidePos, Direction facing) {
        BlockState state = world.getBlockState(sidePos);
        // Solo Repeater/Comparatori Cobalt possono bloccare un Repeater Cobalt
        if (state.getBlock() instanceof CobaltRepeaterBlock || state.getBlock() instanceof CobaltComparatorBlock) {
            if((state.getValue(FACING) == facing.getClockWise()) || (state.getValue(FACING) == facing.getCounterClockWise()))
                return ((CobaltPowerSource) state.getBlock()).getCobaltPower(state, (Level) world, sidePos);
        }
        return 0;
    }

    @Override
    public int getCobaltPower(BlockState state, Level world, BlockPos pos) {
        // Il repeater emette energia SOLO se è acceso (POWERED)
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongCobaltPower(BlockState state, Level world, BlockPos pos, Direction direction) {
        // Direction è la faccia del blocco adiacente che viene colpita.
        // Il repeater emette "Strong Power" solo dalla sua faccia anteriore.
        return (state.getValue(FACING).getOpposite() == direction && state.getValue(POWERED)) ? 15 : 0;
    }

    // --- ISOLAMENTO TOTALE ---
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
                // Non controlliamo il repeater stesso (lato)
                if (side == direction) continue;

                BlockPos checkPos = targetPos.relative(side);
                BlockState checkState = world.getBlockState(checkPos);

                // Se la pietra tocca Redstone Vanilla, il cobalt repeater "spegne" la Strong Power
                // per evitare che la polvere si accenda.
                if (isVanillaRedstone(checkState)) {
                    return 0;
                }
            }
        }

        // Se non c'è redstone vanilla attorno al blocco caricato, procedi normalmente
        return super.getDirectSignal(state, world, pos, direction);
    }

    // Lista dei blocchi vanilla da tenere isolati
    private static boolean isVanillaRedstone(BlockState state) {
        // Here we need REDSTONE BLOCK
        return state.is(Blocks.REDSTONE_WIRE) ||
                state.is(Blocks.REPEATER) ||
                state.is(Blocks.COMPARATOR) ||
                state.is(Blocks.REDSTONE_TORCH) ||
                state.is(Blocks.REDSTONE_WALL_TORCH) ||
                state.is(Blocks.REDSTONE_BLOCK);
    }
}