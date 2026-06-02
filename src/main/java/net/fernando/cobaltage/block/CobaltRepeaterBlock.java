package net.fernando.cobaltage.block;

import net.fernando.cobaltage.block.wire.CobaltSignalSource;
import net.fernando.cobaltage.util.signal.SignalTypeLevelExtensions;
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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.NonNull;

import static net.fernando.cobaltage.util.signal.SignalType.COBALT;

public class CobaltRepeaterBlock extends RepeaterBlock implements SimpleWaterloggedBlock, CobaltSignalSource {
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
    protected int getInputSignal(@NonNull Level world, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        BlockPos rearPos = pos.relative(direction);
        // Reading Cobalt signal like A Vanilla Repeater does
        int cobaltIn;
        int i = ((SignalTypeLevelExtensions) world).getSignalByType(COBALT, rearPos, direction);
        if (i >= 15) {
            cobaltIn = i;
        } else {
            BlockState rearState = world.getBlockState(rearPos);
            cobaltIn = Math.max(i, rearState.is(ModBlocks.COBALT_DUST) ? rearState.getValue(CobaltWireBlock.POWER) : 0);
        }
        return cobaltIn;
    }

    // --- LOGICA DI BLOCCAGGIO (LATERALE) ---
    @Override
    protected int getAlternateSignal(@NonNull SignalGetter world, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction side1 = facing.getClockWise();
        Direction side2 = facing.getCounterClockWise();
        return Math.max(
                getCobaltSideDiodeBlockPower(world, pos.relative(side1), side1),
                getCobaltSideDiodeBlockPower(world, pos.relative(side2), side2)
        );
    }
    private int getCobaltSideDiodeBlockPower(SignalGetter world, BlockPos sidePos, Direction sideDir) {
        BlockState state = world.getBlockState(sidePos);
        // Solo Repeater/Comparatori/Converter Cobalt possono bloccare un Repeater Cobalt
        if (state.getBlock() instanceof CobaltRepeaterBlock || state.getBlock() instanceof CobaltComparatorBlock || state.getBlock() instanceof CobaltConverterBlock) {
            return ((CobaltSignalSource) state.getBlock()).getDirectCobaltSignal(state, (Level) world, sidePos, sideDir);
        }
        return 0;
    }

    @Override
    public int getCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        // Instruction to comprehend the getSignal method:                          FACING COUNTERWISE / CLOCKWISE
        //                                                          (block A)    FACING   -----REPEATER----->  OPPOSITE FACING      (block B)
        //                                                                           FACING CLOCKWISE / COUNTERWISE
        // The reading in this example is:
        // To give you the signal, i must be powered and i must point such that respect to you, i'm at my FACING.
        // In other words, "when the repeater is at the FACING of the Block", so Block B will be powered
        // If I had put state.getValue(FACING), then it would've been "when the repeater is at FACING OPPOSITE of Block, so Block A would've been be powered.
        // direction must be so the opposite of where you are respect to me
        return (state.getValue(FACING) == direction && state.getValue(POWERED)) ? 15 : 0;
    }

    @Override
    public int getDirectCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        // Giving you the direct power the same direction and the same level of power as the normal signal.
        return this.getCobaltSignal(state, world, pos, direction);
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
                state.is(Blocks.POWERED_RAIL) ||
                state.is(Blocks.ACTIVATOR_RAIL) ||
                state.is(Blocks.COMPARATOR) ||
                state.is(Blocks.REDSTONE_TORCH) ||
                state.is(Blocks.REDSTONE_WALL_TORCH) ||
                state.is(Blocks.REDSTONE_BLOCK);
    }
}