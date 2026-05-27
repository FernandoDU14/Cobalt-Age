package net.fernando.cobaltage.block;

import net.fernando.cobaltage.block.wire.CobaltWireNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

public class CobaltRelayBlock extends CobaltWireBlock {

    // Aggiungiamo le direzioni verticali. (Orizzontali e POWER sono eredidate)
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public CobaltRelayBlock(Properties settings) {
        super(settings);
        // Sovrascriviamo il default state per includere UP e DOWN
        this.registerDefaultState(this.defaultBlockState()
                .setValue(UP, true)
                .setValue(DOWN, true)
                .setValue(NORTH, RedstoneSide.SIDE)
                .setValue(SOUTH, RedstoneSide.SIDE)
                .setValue(EAST, RedstoneSide.SIDE)
                .setValue(WEST, RedstoneSide.SIDE)
                .setValue(POWER, 0)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // Aggiunge POWER, NORTH, ecc.
        builder.add(UP, DOWN);
    }

    // Essendo un blocco di vetro/relè, occupa l'intero spazio
    @Override
    public @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return Shapes.block();
    }

    // Il relè può fluttuare nell'aria o essere impilato, a differenza della polvere
    @Override
    public boolean canSurvive(@NonNull BlockState state, LevelReader world, @NonNull BlockPos pos) {
        return true;
    }

    // Calcolo delle 6 connessioni per la grafica/logica direzionale
    private BlockState calculateConnections(BlockGetter world, BlockPos pos, BlockState state) {
        boolean up = true; // forced state
        boolean down = true; // forced state
        boolean north = canConnectTo(world.getBlockState(pos.north()), Direction.SOUTH);
        boolean south = canConnectTo(world.getBlockState(pos.south()), Direction.NORTH);
        boolean east = canConnectTo(world.getBlockState(pos.east()), Direction.WEST);
        boolean west = canConnectTo(world.getBlockState(pos.west()), Direction.EAST);

        return state
                .setValue(UP, up)
                .setValue(DOWN, down)
                .setValue(NORTH, north ? RedstoneSide.SIDE : RedstoneSide.NONE)
                .setValue(SOUTH, south ? RedstoneSide.SIDE : RedstoneSide.NONE)
                .setValue(EAST, east ? RedstoneSide.SIDE : RedstoneSide.NONE)
                .setValue(WEST, west ? RedstoneSide.SIDE : RedstoneSide.NONE);
    }

    // Questo farà sì che forceShapeUpdate e i neighbor update usino la logica del Relay!
    @Override
    public BlockState getWireShapeState(BlockGetter world, BlockPos pos, BlockState state) {
        return calculateConnections(world, pos, state);
    }

    // Controlla se il vicino supporta la rete Cobalt
    private boolean canConnectTo(BlockState state, Direction from) {
        if (state.is(ModBlocks.COBALT_DUST)) return true; // Includes only Dust Wires and not Relays
        if (state.getBlock() instanceof CobaltPowerSource) return true;
        return CobaltWireNetwork.compatibleCobaltPowerSource(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state != null) {
            return calculateConnections(ctx.getLevel(), ctx.getClickedPos(), state);
        }
        return null;
    }

    @Override
    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, net.minecraft.world.level.@NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        BlockState baseUpdate = super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (baseUpdate.is(Blocks.AIR)) return baseUpdate; // Eredita la logica dell'acqua/distruzione

        return calculateConnections(world, pos, baseUpdate);
    }

    // Override dell'energia: Questo blocco emette sia sopra che sotto
    @Override
    protected int getSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        int power = state.getValue(POWER);
        if (power == 0) return 0;

        BlockPos neighborPos = pos.relative(direction.getOpposite());
        if (isVanillaRedstone(world.getBlockState(neighborPos))) return 0;

        Direction outDir = direction.getOpposite();

        // Emette in verticale se c'è connessione grafica
        if (outDir == Direction.UP && state.getValue(UP)) return power;
        if (outDir == Direction.DOWN && state.getValue(DOWN)) return power;

        // Emette in orizzontale se c'è connessione grafica
        if (outDir.getAxis().isHorizontal()) {
            if (state.getValue(getProperty(outDir)).isConnected()) {
                return power;
            }
        }

        return 0;
    }

    @Override
    protected int getDirectSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        // La logica forte è simile a quella debole ma con i controlli isolamento Vanilla che già usi
        int power = getSignal(state, world, pos, direction);
        if (power == 0) return 0;

        BlockPos targetPos = pos.relative(direction.getOpposite());
        if (world.getBlockState(targetPos).isRedstoneConductor(world, targetPos)) {
            for (Direction dir : Direction.values()) {
                if (dir == direction) continue;
                if (isVanillaRedstone(world.getBlockState(targetPos.relative(dir)))) {
                    return 0; // Protezione isolamento Vanilla
                }
            }
        }
        return power;
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level world, @NonNull BlockPos pos, Player player, @NonNull BlockHitResult hit) {
        // Nessun interruttore manuale (Cross/Dot) per il relè: è sempre automatico!
        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        int power = state.getValue(POWER);
        if (power == 0) return;

        float f = (float)power / 15.0F;
        float r = f * 0.1f + 0.1f;
        float g = f * 0.5f + 0.3f;
        float b = f * 1.1f + 0.4f;
        if(f != 0){ b += 0.1f; g += 0.1f; }

        int red = Mth.clamp((int)(r * 255.0F), 0, 255);
        int green = Mth.clamp((int)(g * 255.0F), 0, 255);
        int blue = Mth.clamp((int)(b * 255.0F), 0, 255);
        int colorInt = red << 16 | green << 8 | blue;

        // Emette particelle dal centro esatto del blocco (dentro il vetro)
        if (random.nextFloat() < 0.5F) {
            double dX = pos.getX() + 0.5D + (random.nextDouble() - 0.5) * 0.4;
            double dY = pos.getY() + 0.5D + (random.nextDouble() - 0.5) * 0.4;
            double dZ = pos.getZ() + 0.5D + (random.nextDouble() - 0.5) * 0.4;
            world.addParticle(new DustParticleOptions(colorInt, 1.0F), dX, dY, dZ, 0.0, 0.0, 0.0);
        }
    }

    private boolean isVanillaRedstone(BlockState state) {
        return state.is(Blocks.REDSTONE_WIRE) || state.is(Blocks.REPEATER) ||
                state.is(Blocks.COMPARATOR) || state.is(Blocks.REDSTONE_TORCH) ||
                state.is(Blocks.REDSTONE_WALL_TORCH) || state.is(Blocks.REDSTONE_BLOCK);
    }
}