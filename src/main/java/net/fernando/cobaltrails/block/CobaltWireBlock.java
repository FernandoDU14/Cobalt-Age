package net.fernando.cobaltrails.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.block.*;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static net.fernando.cobaltrails.block.ModBlocks.*;
import static net.minecraft.block.CaveVines.SHAPE;

public class CobaltWireBlock extends Block  implements Waterloggable {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final IntProperty POWER = Properties.POWER;
    public static final EnumProperty<WireConnection> NORTH = Properties.NORTH_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> SOUTH = Properties.SOUTH_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> EAST = Properties.EAST_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> WEST = Properties.WEST_WIRE_CONNECTION;

    public CobaltWireBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(POWER, 0)
                .with(NORTH, WireConnection.SIDE)
                .with(SOUTH, WireConnection.SIDE)
                .with(EAST, WireConnection.SIDE)
                .with(WEST, WireConnection.SIDE)
                .with(WATERLOGGED, false));
    }

    private static final VoxelShape DOT_SHAPE = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 1.0, 13.0);
    private static final Map<Direction, VoxelShape> SHAPES_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, Block.createCuboidShape(3.0, 0.0, 0.0, 13.0, 1.0, 13.0),
            Direction.SOUTH, Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 1.0, 16.0),
            Direction.EAST,  Block.createCuboidShape(3.0, 0.0, 3.0, 16.0, 1.0, 13.0),
            Direction.WEST,  Block.createCuboidShape(0.0, 0.0, 3.0, 13.0, 1.0, 13.0),
            Direction.UP,    Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 16.0, 13.0)
    ));

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = DOT_SHAPE;

        for (Direction direction : Direction.Type.HORIZONTAL) {
            WireConnection connection = state.get(CobaltWireBlock.getProperty(direction));

            if (connection == WireConnection.SIDE) {
                shape = VoxelShapes.union(shape, SHAPES_BY_DIRECTION.get(direction));
            } else if (connection == WireConnection.UP) {
                shape = VoxelShapes.union(shape, SHAPES_BY_DIRECTION.get(direction));
                shape = VoxelShapes.union(shape, getVerticalStub(direction));
            }
        }

        return shape;
    }

    public static EnumProperty<WireConnection> getProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        };
    }

    // Helper to create the vertical wall hitboxes
    private VoxelShape getVerticalStub(Direction direction) {
        return switch (direction) {
            case NORTH -> Block.createCuboidShape(3.0, 0.0, 0.0, 13.0, 16.0, 1.0);
            case SOUTH -> Block.createCuboidShape(3.0, 0.0, 15.0, 13.0, 16.0, 16.0);
            case EAST -> Block.createCuboidShape(15.0, 0.0, 3.0, 16.0, 16.0, 13.0);
            case WEST -> Block.createCuboidShape(0.0, 0.0, 3.0, 1.0, 16.0, 13.0);
            default -> VoxelShapes.empty();
        };
    }



    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int power = state.get(POWER);
        if (power != 0) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                // If Cobalt Dust is active, particles will spawn
                // Vector3f(0.2f, 0.6f, 1.0f) is a good blue for the cobalt
                double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
                double e = (double)pos.getY() + 0.0625;
                double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;

                int cobaltBlue = (0 << 16) | (153 << 8) | 255;
                DustParticleEffect cobaltDust = new DustParticleEffect(cobaltBlue, 1.0f);

                world.addParticleClient(cobaltDust, d, e, f, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos floorPos = pos.down();
        BlockState floorState = world.getBlockState(floorPos);

        // This will allow the placement only and only if the block under is valid and is not another Cobalt Dust
        return floorState.isSideSolidFullSquare(world, floorPos, Direction.UP) || floorState.isOf(Blocks.HOPPER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }


    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWER, NORTH, SOUTH, EAST, WEST, WATERLOGGED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos,
                               Block sourceBlock, @Nullable WireOrientation orientation,
                               boolean notify) {
        if (!world.isClient()) {
            // 1. aggiorna la tua rete (logica custom)
            CobaltWireNetwork.update(world, pos);
        }
        // 2. schedula il tick per ricalcolare la potenza
        world.scheduleBlockTick(pos, this, 0);
    }

    @Override
    public BlockState getStateForNeighborUpdate(
            BlockState state, WorldView world, ScheduledTickView tickView,
            BlockPos pos, Direction direction, BlockPos neighborPos,
            BlockState neighborState, Random random
    ) {
        // 💧 waterlogged handling
        if (state.get(WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        // ❌ se non può stare lì → aria
        if (!state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }

        // 🔌 aggiorna connessioni (la TUA logica, non vanilla)
        BlockState newState = CobaltWireShape.getUpdatedState(world, pos, state);

        // 💧 preserva waterlogged
        return newState.with(WATERLOGGED, state.get(WATERLOGGED));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();

        // 1. stato base con shape corretta (connessioni)
        BlockState state = CobaltWireShape.getUpdatedState(world, pos, this.getDefaultState());

        // 2. gestione waterlogged
        FluidState fluidState = world.getFluidState(pos);
        boolean waterlogged = fluidState.getFluid() == Fluids.WATER;

        return state.with(WATERLOGGED, waterlogged);
    }


    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int power = getCobaltPower(world, pos);
        if (state.get(POWER) != power) {
            world.setBlockState(pos, state.with(POWER, power), Block.NOTIFY_ALL);
        }

        CobaltWireNetwork.schedule(world, pos);
    }

    private int getCobaltPower(World world, BlockPos pos) {
        int power = 0;

        for (Direction dir : Direction.values()) {
            BlockPos target = pos.offset(dir);
            BlockState state = world.getBlockState(target);

            if (state.getBlock() instanceof CobaltPowerSource source) {
                if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                    power = Math.max(power, source.getCobaltPower(state, world, target));
                }
            }
        }

        return power;
    }


    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }


    // 🔥 ENERGIA DEBOLE (Attiva pistoni, porte, ecc.)
    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        // In Minecraft, 'direction' è la faccia del cavo da cui il vicino sta chiedendo energia.
        // Se il vicino è SOPRA il cavo, chiederà energia alla faccia superiore (Direction.DOWN).

        // 1. Il cavo NON attiva mai i blocchi posti esattamente sopra di esso.
        if (direction == Direction.DOWN) return 0;

        int power = state.get(POWER);
        if (power == 0) return 0;

        // Se è un puntino (tutti i lati su NONE), non alimenta i vicini orizzontali.
        if (isNotConnected(state)) {
            return 0;
        }

        BlockPos neighborPos = pos.offset(direction.getOpposite());
        if (isVanillaRedstone(world.getBlockState(neighborPos))) return 0; // Isolamento Vanilla

        // 2. Alimenta sempre verso il basso (il blocco di supporto)
        if (direction == Direction.UP) return power;

        // 3. Controllo Direzionale (il modello tocca il blocco?)
        Direction outDir = direction.getOpposite(); // La direzione verso il blocco adiacente
        EnumProperty<WireConnection> property = getProperty(outDir);

        // Se c'è una connessione grafica (SIDE o UP) verso quel lato, passiamo l'energia
        if (state.get(property).isConnected()) {
            return power;
        }

        // Se è una linea dritta che non punta verso il pistone, non si attiva!
        return 0;
    }

    // 🔥 ENERGIA FORTE (Carica i blocchi solidi)
    @Override
    protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        // La redstone dà energia forte SOLO verso il basso (al blocco su cui poggia)
        if (direction != Direction.UP) return 0;

        int power = state.get(POWER);
        if (power == 0) return 0;

        // Se è un puntino, non diamo energia forte al blocco sotto (comportamento Vanilla 1.16+)
        // if (isNotConnected(state)) return 0;

        BlockPos blockBelowPos = pos.down();
        BlockState blockBelowState = world.getBlockState(blockBelowPos);

        // 🛑 LA NUOVA VEGGENZA 🛑
        // Se il blocco sotto di noi è solido, controlliamo se ha redstone vanilla attorno
        if (blockBelowState.isSolidBlock(world, blockBelowPos)) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP) continue; // Ignoriamo la polvere stessa (che è sopra)

                BlockPos checkPos = blockBelowPos.offset(dir);
                if (isVanillaRedstone(world.getBlockState(checkPos))) {
                    // "Vedo della redstone Vanilla attorno al blocco di supporto!
                    // Non inietto Energia Forte per non accenderla."
                    return 0;
                }
            }
        }

        return power;
    }

    private boolean isVanillaRedstone(BlockState state) {
        return state.isOf(Blocks.REDSTONE_WIRE) ||
                state.isOf(Blocks.REPEATER) ||
                state.isOf(Blocks.COMPARATOR) ||
                state.isOf(Blocks.REDSTONE_TORCH) ||
                state.isOf(Blocks.REDSTONE_WALL_TORCH) ||
                state.isOf(Blocks.REDSTONE_BLOCK);
    }

    private void updateAllNeighbors(World world, BlockPos pos) {
        // 1. Aggiorna i vicini diretti (6 posizioni: N, S, E, W, UP, DOWN)
        world.updateNeighborsAlways(pos, this, null);

        // 2. Aggiorna i vicini diagonali (8 posizioni: sopra e sotto i 4 lati orizzontali)
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos sidePos = pos.offset(dir);

            // Notifica il blocco accanto (già fatto sopra, ma raddoppiare non guasta)
            world.updateNeighborsAlways(sidePos, this, null);
            // Notifica i blocchi diagonali (fondamentali per lo scalino)
            world.updateNeighborsAlways(sidePos.up(), this, null);
            world.updateNeighborsAlways(sidePos.down(), this, null);
        }
    }


    // This method is called when this block has been placed
    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock()) && !world.isClient()) {
            // Forza l'aggiornamento grafico dei vicini
            this.updateDiagonalShapes(world, pos);
            // Aggiorna l'energia
            CobaltWireNetwork.schedule(world, pos);
        }
    }

    // ATTENZIONE: Uso la firma esatta che hai nel tuo file caricato!
    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (moved) return;
        super.onStateReplaced(state, world, pos, moved);
        if (world.isClient()) return;

        // Quando rompi la polvere, forza l'aggiornamento grafico di chi le stava attorno
        this.updateDiagonalShapes(world, pos);
        // Aggiorna l'energia
        CobaltWireNetwork.schedule(world, pos);
    }

    // --- NUOVI METODI PER FORZARE LA GRAFICA ---

    private void updateDiagonalShapes(World world, BlockPos pos) {
        // 1. Forza l'aggiornamento dei 6 vicini diretti
        for (Direction dir : Direction.values()) {
            forceShapeUpdate(world, pos.offset(dir));
        }

        // 2. Forza l'aggiornamento dei vicini diagonali (Lo scalino!)
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos sidePos = pos.offset(dir);
            forceShapeUpdate(world, sidePos.up());
            forceShapeUpdate(world, sidePos.down());
        }
    }

    private void forceShapeUpdate(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        // Controlliamo se il blocco in questa posizione è una tua polvere
        if (state.getBlock() instanceof CobaltWireBlock) {
            // Calcoliamo la nuova forma (Punto, Linea, Rampa, ecc.)
            BlockState newState = CobaltWireShape.getUpdatedState(world, pos, state);

            // Se la forma calcolata è diversa da quella attuale, applichiamola immediatamente!
            if (state != newState) {
                world.setBlockState(pos, newState, Block.NOTIFY_ALL);
            }
        }
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        // Notifichiamo che l'energia è sparita
        this.updateAllNeighbors(world, pos);
        CobaltWireNetwork.schedule(world, pos);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!player.getAbilities().allowModifyWorld) return ActionResult.PASS;

        // Se è un cross completo o un puntino, permettiamo il toggle
        if (isFullyConnected(state) || isNotConnected(state)) {
            // Se è cross -> diventa puntino. Se è puntino -> torna cross.
            BlockState newState = isFullyConnected(state) ? getDotState(state) : getCrossState(state);

            newState = newState.with(POWER, state.get(POWER));
            world.setBlockState(pos, newState, Block.NOTIFY_ALL);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    // Helper per creare lo stato a puntino (tutti NONE)
    private BlockState getDotState(BlockState state) {
        return state.with(NORTH, WireConnection.NONE)
                .with(SOUTH, WireConnection.NONE)
                .with(EAST, WireConnection.NONE)
                .with(WEST, WireConnection.NONE);
    }


    // Helper to check if it's currently a dot
    private boolean isNotConnected(BlockState state) {
        return state.get(NORTH) == WireConnection.NONE &&
                state.get(SOUTH) == WireConnection.NONE &&
                state.get(EAST) == WireConnection.NONE &&
                state.get(WEST) == WireConnection.NONE;
    }

    // Helper to check if it's currently a cross
    private boolean isFullyConnected(BlockState state) {
        return state.get(NORTH) == WireConnection.SIDE &&
                state.get(SOUTH) == WireConnection.SIDE &&
                state.get(EAST) == WireConnection.SIDE &&
                state.get(WEST) == WireConnection.SIDE;
    }

    // Forces all 4 sides to 'SIDE' to create the cross look
    private BlockState getCrossState(BlockState state) {
        return state.with(NORTH, WireConnection.SIDE)
                .with(SOUTH, WireConnection.SIDE)
                .with(EAST, WireConnection.SIDE)
                .with(WEST, WireConnection.SIDE);
    }




}