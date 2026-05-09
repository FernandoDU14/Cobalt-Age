package net.fernando.cobaltage.block;

import net.fernando.cobaltage.block.wire.CobaltWireNetwork;
import net.minecraft.block.*;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class CobaltRelayBlock extends CobaltWireBlock {

    // Aggiungiamo le direzioni verticali. (Orizzontali e POWER sono eredidate)
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;

    public CobaltRelayBlock(Settings settings) {
        super(settings);
        // Sovrascriviamo il default state per includere UP e DOWN
        this.setDefaultState(this.getDefaultState()
                .with(UP, true)
                .with(DOWN, true)
                .with(NORTH, WireConnection.SIDE)
                .with(SOUTH, WireConnection.SIDE)
                .with(EAST, WireConnection.SIDE)
                .with(WEST, WireConnection.SIDE)
                .with(POWER, 0)
                .with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder); // Aggiunge POWER, NORTH, ecc.
        builder.add(UP, DOWN);
    }

    // Essendo un blocco di vetro/relè, occupa l'intero spazio
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    // Il relè può fluttuare nell'aria o essere impilato, a differenza della polvere
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return true;
    }

    // Calcolo delle 6 connessioni per la grafica/logica direzionale
    private BlockState calculateConnections(WorldView world, BlockPos pos, BlockState state) {
        boolean up = true; // forced state
        boolean down = true; // forced state
        boolean north = canConnectTo(world.getBlockState(pos.north()), Direction.SOUTH);
        boolean south = canConnectTo(world.getBlockState(pos.south()), Direction.NORTH);
        boolean east = canConnectTo(world.getBlockState(pos.east()), Direction.WEST);
        boolean west = canConnectTo(world.getBlockState(pos.west()), Direction.EAST);

        return state
                .with(UP, up)
                .with(DOWN, down)
                .with(NORTH, north ? WireConnection.SIDE : WireConnection.NONE)
                .with(SOUTH, south ? WireConnection.SIDE : WireConnection.NONE)
                .with(EAST, east ? WireConnection.SIDE : WireConnection.NONE)
                .with(WEST, west ? WireConnection.SIDE : WireConnection.NONE);
    }

    // Controlla se il vicino supporta la rete Cobalt
    private boolean canConnectTo(BlockState state, Direction from) {
        if (state.isOf(ModBlocks.COBALT_DUST)) return true; // Include anche altri Relay Block!
        if (state.getBlock() instanceof CobaltPowerSource) return true;
        return CobaltWireNetwork.compatibleCobaltPowerSource(state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (state != null) {
            return calculateConnections(ctx.getWorld(), ctx.getBlockPos(), state);
        }
        return null;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, net.minecraft.world.tick.ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        BlockState baseUpdate = super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (baseUpdate.isOf(Blocks.AIR)) return baseUpdate; // Eredita la logica dell'acqua/distruzione

        return calculateConnections(world, pos, baseUpdate);
    }

    // Override dell'energia: Questo blocco emette sia sopra che sotto
    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        int power = state.get(POWER);
        if (power == 0) return 0;

        BlockPos neighborPos = pos.offset(direction.getOpposite());
        if (isVanillaRedstone(world.getBlockState(neighborPos))) return 0;

        Direction outDir = direction.getOpposite();

        // Emette in verticale se c'è connessione grafica
        if (outDir == Direction.UP && state.get(UP)) return power;
        if (outDir == Direction.DOWN && state.get(DOWN)) return power;

        // Emette in orizzontale se c'è connessione grafica
        if (outDir.getAxis().isHorizontal()) {
            if (state.get(getProperty(outDir)).isConnected()) {
                return power;
            }
        }

        return 0;
    }

    @Override
    protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        // La logica forte è simile a quella debole ma con i controlli isolamento Vanilla che già usi
        int power = getWeakRedstonePower(state, world, pos, direction);
        if (power == 0) return 0;

        BlockPos targetPos = pos.offset(direction.getOpposite());
        if (world.getBlockState(targetPos).isSolidBlock(world, targetPos)) {
            for (Direction dir : Direction.values()) {
                if (dir == direction) continue;
                if (isVanillaRedstone(world.getBlockState(targetPos.offset(dir)))) {
                    return 0; // Protezione isolamento Vanilla
                }
            }
        }
        return power;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Nessun interruttore manuale (Cross/Dot) per il relè: è sempre automatico!
        return ActionResult.PASS;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int power = state.get(POWER);
        if (power == 0) return;

        float f = (float)power / 15.0F;
        float r = f * 0.1f + 0.1f;
        float g = f * 0.5f + 0.3f;
        float b = f * 1.1f + 0.4f;
        if(f != 0){ b += 0.1f; g += 0.1f; }

        int red = MathHelper.clamp((int)(r * 255.0F), 0, 255);
        int green = MathHelper.clamp((int)(g * 255.0F), 0, 255);
        int blue = MathHelper.clamp((int)(b * 255.0F), 0, 255);
        int colorInt = red << 16 | green << 8 | blue;

        // Emette particelle dal centro esatto del blocco (dentro il vetro)
        if (random.nextFloat() < 0.5F) {
            double dX = pos.getX() + 0.5D + (random.nextDouble() - 0.5) * 0.4;
            double dY = pos.getY() + 0.5D + (random.nextDouble() - 0.5) * 0.4;
            double dZ = pos.getZ() + 0.5D + (random.nextDouble() - 0.5) * 0.4;
            world.addParticleClient(new DustParticleEffect(colorInt, 1.0F), dX, dY, dZ, 0.0, 0.0, 0.0);
        }
    }

    private boolean isVanillaRedstone(BlockState state) {
        return state.isOf(Blocks.REDSTONE_WIRE) || state.isOf(Blocks.REPEATER) ||
                state.isOf(Blocks.COMPARATOR) || state.isOf(Blocks.REDSTONE_TORCH) ||
                state.isOf(Blocks.REDSTONE_WALL_TORCH) || state.isOf(Blocks.REDSTONE_BLOCK);
    }
}