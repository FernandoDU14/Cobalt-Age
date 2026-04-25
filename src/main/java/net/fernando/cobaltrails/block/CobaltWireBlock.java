package net.fernando.cobaltrails.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.fernando.cobaltrails.block.wire.CobaltWireShape;
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
import net.minecraft.util.math.MathHelper;
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
public class CobaltWireBlock extends Block  implements Waterloggable {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final IntProperty POWER = Properties.POWER;
    public static final EnumProperty<WireConnection> NORTH = Properties.NORTH_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> SOUTH = Properties.SOUTH_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> EAST = Properties.EAST_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> WEST = Properties.WEST_WIRE_CONNECTION;
    private static final net.fernando.cobaltrails.block.wire.CobaltWireNetwork NETWORK_HANDLER = new net.fernando.cobaltrails.block.wire.CobaltWireNetwork();

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
            default -> throw new IllegalArgumentException("Invalid direction: %s".formatted(direction));
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

    // Function to compute the color gradient of the Cobalt Dust
    private static int getCobaltColor(int power) {
        float f = (float)power / 15.0F;
        // Calcoliamo le componenti R, G, B basandoci sul livello di carica
        // Per il cobalto: poco rosso, medio verde, molto blu
        float r = f * 0.1f + 0.1f;
        float g = f * 0.5f + 0.3f;
        float b = f * 1.1f + 0.4f;

        if(f!=0){
            b = b + 0.1f;
            g = g + 0.1f;
        }

        int red = MathHelper.clamp((int)(r * 255.0F), 0, 255);
        int green = MathHelper.clamp((int)(g * 255.0F), 0, 255);
        int blue = MathHelper.clamp((int)(b * 255.0F), 0, 255);

        return red << 16 | green << 8 | blue;
    }

    // Questo è il metodo "segreto" di Vanilla che posiziona le particelle lungo i fili
    private void addPoweredParticles(World world, Random random, BlockPos pos, int colorInt, Direction direction, Direction direction2, float f, float g) {
        float h = g - f;
        if (!(random.nextFloat() > 0.2F * h)) { // Non evocare troppe particelle
            float j = f + h * random.nextFloat();
            double d = (double)pos.getX() + 0.5 + (double)(0.4375F * (float)direction.getOffsetX() + j * (float)direction2.getOffsetX());
            double e = (double)pos.getY() + 0.5 + (double)(0.4375F * (float)direction.getOffsetY() + j * (float)direction2.getOffsetY());
            double k = (double)pos.getZ() + 0.5 + (double)(0.4375F * (float)direction.getOffsetZ() + j * (float)direction2.getOffsetZ());

            // Creiamo l'effetto polvere col colore dinamico
            DustParticleEffect particleEffect = new DustParticleEffect(colorInt, 1.0F);
            world.addParticleClient(particleEffect, d, e, k, 0.0, 0.0, 0.0);
        }
    }



    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int power = state.get(POWER);
        if (power == 0) return;

        // 1. Otteniamo il colore dinamico usando la tua funzione
        int colorInt = getCobaltColor(power);

        // 2. Ciclo sulle direzioni orizzontali (esattamente come Vanilla)
        for (Direction direction : Direction.Type.HORIZONTAL) {
            // Recuperiamo la connessione per quella direzione
            EnumProperty<WireConnection> property = getProperty(direction);

            switch (state.get(property)) {
                case UP:
                    addPoweredParticles(world, random, pos, colorInt, direction, Direction.UP, -0.5F, 0.5F);
                    break; // Aggiunto break per sicurezza
                case SIDE:
                    addPoweredParticles(world, random, pos, colorInt, Direction.DOWN, direction, 0.0F, 0.5F);
                    break;
                case NONE:
                default:
                    addPoweredParticles(world, random, pos, colorInt, Direction.DOWN, direction, 0.0F, 0.3F);
                    break;
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
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation orientation, boolean notify) {
        if (!world.isClient()) {
            NETWORK_HANDLER.updateNetwork(world, pos);
        }
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

    // 🔥 ENERGIA FORTE (Ora alimenta anche i lati, come in Vanilla)
    @Override
    protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        // La redstone non dà mai energia forte verso l'alto
        if (direction == Direction.DOWN) return 0;

        int power = state.get(POWER);
        if (power <= 0) return 0;

        Direction side = direction.getOpposite();

        // 1. ALIMENTARE IL BLOCCO SOTTOSTANTE
        if (direction == Direction.UP) {
            // Il puntino in Vanilla alimenta sempre il blocco su cui poggia.
            BlockPos blockBelowPos = pos.down();
            BlockState blockBelowState = world.getBlockState(blockBelowPos);

            // VEGGENZA: Isoliamo il blocco sottostante dalla vanilla
            if (blockBelowState.isSolidBlock(world, blockBelowPos)) {
                for (Direction dir : Direction.values()) {
                    if (dir == Direction.UP) continue; // Ignoriamo il cavo stesso
                    if (isVanillaRedstone(world.getBlockState(blockBelowPos.offset(dir)))) {
                        return 0;
                    }
                }
            }
            return power;
        }

        // 2. ALIMENTARE I BLOCCHI LATERALI (La tua rifinitura!)
        // Un puntino non spara energia forte ai lati
        if (isNotConnected(state)) return 0;

        // Se il modello del cavo è "connesso" (SIDE o UP) verso quella direzione
        EnumProperty<WireConnection> property = getProperty(side);
        if (state.get(property).isConnected()) {

            BlockPos sideBlockPos = pos.offset(side);
            BlockState sideBlockState = world.getBlockState(sideBlockPos);

            // VEGGENZA: Isoliamo il blocco laterale!
            // Se c'è della redstone vanilla che tocca questo blocco solido,
            // la Cobalt Dust si rifiuta di caricarlo, proteggendo i circuiti.
            // Se invece c'è solo un pistone, lo carica e lo fa scattare!
            if (sideBlockState.isSolidBlock(world, sideBlockPos)) {
                for (Direction dir : Direction.values()) {
                    if (dir == side.getOpposite()) continue; // Ignoriamo il lato da cui arriva il cavo
                    if (isVanillaRedstone(world.getBlockState(sideBlockPos.offset(dir)))) {
                        return 0;
                    }
                }
            }
            return power;
        }

        return 0;
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
            this.updateDiagonalShapes(world, pos);
            this.updateAllNeighbors(world, pos);
            NETWORK_HANDLER.updateNetwork(world, pos);
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (moved) return;
        super.onStateReplaced(state, world, pos, false);
        if (!world.isClient()) {
            this.updateDiagonalShapes(world, pos);
            this.updateAllNeighbors(world, pos);
            NETWORK_HANDLER.updateNetwork(world, pos);
        }
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
        NETWORK_HANDLER.updateNetwork(world, pos);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!player.getAbilities().allowModifyWorld) return ActionResult.PASS;

        // Se è un cross completo o un puntino, permettiamo il toggle
        if (isFullyConnected(state) || isNotConnected(state)) {
            // Se è cross -> diventa puntino. Se è puntino -> torna cross.
            BlockState newState = isFullyConnected(state) ? getDotState(state) : getCrossState(state);

            // 1. Manteniamo il livello di potenza attuale durante la transizione
            newState = newState.with(POWER, state.get(POWER));
            world.setBlockState(pos, newState, Block.NOTIFY_ALL);

            // --- FIX DEL BUG DELLA TORCIA ---
            // 2. Avvisiamo i vicini dei vicini! Così il blocco in diagonale
            // scopre che il cavo non lo sta più puntando e fa riaccendere la torcia.
            this.updateAllNeighbors(world, pos);

            // 3. Ricalcoliamo la rete. Cambiando forma, il cavo potrebbe
            // essersi disconnesso (o connesso) a una fonte di energia.
            NETWORK_HANDLER.updateNetwork(world, pos);

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