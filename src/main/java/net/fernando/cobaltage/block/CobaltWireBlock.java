package net.fernando.cobaltage.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.fernando.cobaltage.block.wire.CobaltWireShape;
import net.fernando.cobaltage.block.wire.CobaltWireSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Map;
public class CobaltWireBlock extends Block  implements SimpleWaterloggedBlock, CobaltWireSource {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final BooleanProperty ISOLATED = BooleanProperty.create("isolated");
    private static final net.fernando.cobaltage.block.wire.CobaltWireNetwork NETWORK_HANDLER = new net.fernando.cobaltage.block.wire.CobaltWireNetwork();

    public CobaltWireBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any()
                .setValue(POWER, 0)
                .setValue(NORTH, RedstoneSide.SIDE)
                .setValue(SOUTH, RedstoneSide.SIDE)
                .setValue(EAST, RedstoneSide.SIDE)
                .setValue(WEST, RedstoneSide.SIDE)
                .setValue(ISOLATED, false)
                .setValue(WATERLOGGED, false));
    }
    
    private static final VoxelShape DOT_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 13.0);
    private static final Map<Direction, VoxelShape> SHAPES_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, Block.box(3.0, 0.0, 0.0, 13.0, 1.0, 13.0),
            Direction.SOUTH, Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 16.0),
            Direction.EAST,  Block.box(3.0, 0.0, 3.0, 16.0, 1.0, 13.0),
            Direction.WEST,  Block.box(0.0, 0.0, 3.0, 13.0, 1.0, 13.0),
            Direction.UP,    Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0)
    ));

    // Stubs verticali estratti in costanti per pulizia
    private static final VoxelShape UP_NORTH_STUB = Block.box(3.0, 0.0, 0.0, 13.0, 16.0, 1.0);
    private static final VoxelShape UP_SOUTH_STUB = Block.box(3.0, 0.0, 15.0, 13.0, 16.0, 16.0);
    private static final VoxelShape UP_EAST_STUB = Block.box(15.0, 0.0, 3.0, 16.0, 16.0, 13.0);
    private static final VoxelShape UP_WEST_STUB = Block.box(0.0, 0.0, 3.0, 1.0, 16.0, 13.0);

    // 2. L'array che conterrà le 81 combinazioni
    private static final VoxelShape[] SHAPE_CACHE = new VoxelShape[81];

    // 3. Generazione automatica all'avvio del gioco
    static {
        for (RedstoneSide north : RedstoneSide.values()) {
            for (RedstoneSide south : RedstoneSide.values()) {
                for (RedstoneSide east : RedstoneSide.values()) {
                    for (RedstoneSide west : RedstoneSide.values()) {
                        VoxelShape shape = DOT_SHAPE;

                        // Composizione NORTH
                        if (north == RedstoneSide.SIDE) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.NORTH));
                        } else if (north == RedstoneSide.UP) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.NORTH));
                            shape = Shapes.or(shape, UP_NORTH_STUB);
                        }

                        // Composizione SOUTH
                        if (south == RedstoneSide.SIDE) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.SOUTH));
                        } else if (south == RedstoneSide.UP) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.SOUTH));
                            shape = Shapes.or(shape, UP_SOUTH_STUB);
                        }

                        // Composizione EAST
                        if (east == RedstoneSide.SIDE) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.EAST));
                        } else if (east == RedstoneSide.UP) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.EAST));
                            shape = Shapes.or(shape, UP_EAST_STUB);
                        }

                        // Composizione WEST
                        if (west == RedstoneSide.SIDE) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.WEST));
                        } else if (west == RedstoneSide.UP) {
                            shape = Shapes.or(shape, SHAPES_BY_DIRECTION.get(Direction.WEST));
                            shape = Shapes.or(shape, UP_WEST_STUB);
                        }

                        // Calcolo matematico dell'indice (0 - 80)
                        int index = north.ordinal() +
                                south.ordinal() * 3 +
                                east.ordinal() * 9 +
                                west.ordinal() * 27;

                        // .optimize() unisce le intersezioni interne riducendo i poligoni da renderizzare
                        SHAPE_CACHE[index] = shape.optimize();
                    }
                }
            }
        }
    }

    private static int getShapeIndex(BlockState state) {
        return state.getValue(NORTH).ordinal() +
                state.getValue(SOUTH).ordinal() * 3 +
                state.getValue(EAST).ordinal() * 9 +
                state.getValue(WEST).ordinal() * 27;
    }

    @Override
    protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return SHAPE_CACHE[getShapeIndex(state)]; // Costo O(1), zero allocazioni, performance fulminee
    }

    public static EnumProperty<RedstoneSide> getProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("Invalid direction: %s".formatted(direction));
        };
    }

    public BlockState getWireShapeState(BlockGetter world, BlockPos pos, BlockState state) {
        return CobaltWireShape.getUpdatedState(world, pos, state);
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

        int red = Mth.clamp((int)(r * 255.0F), 0, 255);
        int green = Mth.clamp((int)(g * 255.0F), 0, 255);
        int blue = Mth.clamp((int)(b * 255.0F), 0, 255);

        return red << 16 | green << 8 | blue;
    }

    // Questo è il metodo "segreto" di Vanilla che posiziona le particelle lungo i fili
    private void addPoweredParticles(Level world, RandomSource random, BlockPos pos, int colorInt, Direction direction, Direction direction2, float f, float g) {
        float h = g - f;
        if (!(random.nextFloat() > 0.2F * h)) { // Non evocare troppe particelle
            float j = f + h * random.nextFloat();
            double d = (double)pos.getX() + 0.5 + (double)(0.4375F * (float)direction.getStepX() + j * (float)direction2.getStepX());
            double e = (double)pos.getY() + 0.5 + (double)(0.4375F * (float)direction.getStepY() + j * (float)direction2.getStepY());
            double k = (double)pos.getZ() + 0.5 + (double)(0.4375F * (float)direction.getStepZ() + j * (float)direction2.getStepZ());

            // Creiamo l'effetto polvere col colore dinamico
            DustParticleOptions particleEffect = new DustParticleOptions(colorInt, 1.0F);
            world.addParticle(particleEffect, d, e, k, 0.0, 0.0, 0.0);
        }
    }



    @Override
    public void animateTick(BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        int power = state.getValue(POWER);
        if (power == 0) return;

        // 1. Otteniamo il colore dinamico usando la tua funzione
        int colorInt = getCobaltColor(power);

        // 2. Ciclo sulle direzioni orizzontali (esattamente come Vanilla)
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            // Recuperiamo la connessione per quella direzione
            EnumProperty<RedstoneSide> property = getProperty(direction);

            switch (state.getValue(property)) {
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
    public boolean canSurvive(@NonNull BlockState state, LevelReader world, @NonNull BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        mutable.setWithOffset(pos, Direction.DOWN);
        BlockState floorState = world.getBlockState(mutable);
        return floorState.isFaceSturdy(world, mutable, Direction.UP) || floorState.is(Blocks.HOPPER);
    }

    @Override
    public @NonNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER, NORTH, SOUTH, EAST, WEST, ISOLATED, WATERLOGGED);
    }

    @Override
    public @NonNull RenderShape getRenderShape(@NonNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void neighborChanged(@NonNull BlockState state, Level world, @NonNull BlockPos pos, @NonNull Block sourceBlock, @Nullable Orientation orientation, boolean notify) {
        if (!world.isClientSide()) {
            NETWORK_HANDLER.updateNetwork(world, pos);
        }
    }

    @Override
    public @NonNull BlockState updateShape(
            BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView,
            @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos,
            @NonNull BlockState neighborState, @NonNull RandomSource random
    ) {
        // 💧 waterlogged handling
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        // ❌ se non può stare lì → aria
        if (!state.canSurvive(world, pos)) {
            return Blocks.AIR.defaultBlockState();
        }

        // 🔌 aggiorna connessioni (la TUA logica, non vanilla)
        BlockState newState = this.getWireShapeState(world, pos, state);

        // 💧 preserva waterlogged
        return newState.setValue(WATERLOGGED, state.getValue(WATERLOGGED));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();

        // 1. stato base con shape corretta (connessioni)
        BlockState state = this.getWireShapeState(world, pos, this.defaultBlockState());

        // 2. gestione waterlogged
        FluidState fluidState = world.getFluidState(pos);
        boolean waterlogged = fluidState.getType() == Fluids.WATER;

        return state.setValue(WATERLOGGED, waterlogged);
    }

    @Override
    public boolean isSignalSource(@NonNull BlockState state) {
        return true;
    }

    // 🔥 ENERGIA DEBOLE (Attiva pistoni, porte, ecc. quando non c'è altra redstone vicino, ovvero tutti gli emettitore che ascoltano getSignal)
    @Override
    protected int getSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        // In Minecraft, 'direction' è la faccia del cavo da cui il vicino sta chiedendo energia.
        // Se il vicino è SOPRA il cavo, chiederà energia alla faccia superiore (Direction.DOWN).

        // 1. Il cavo NON attiva mai i blocchi posti esattamente sopra di esso.
        if (direction == Direction.DOWN) return 0;

        int power = state.getValue(POWER);
        if (power == 0) return 0;

        // Se è un puntino (tutti i lati su NONE), non alimenta i vicini orizzontali.
        if (isNotConnected(state)) {
            return 0;
        }

        BlockPos neighborPos = pos.relative(direction.getOpposite());
        if (isVanillaRedstone(world.getBlockState(neighborPos))) return 0; // Isolamento Vanilla

        // 2. Alimenta sempre verso il basso (il blocco di supporto)
        if (direction == Direction.UP) return power;

        // 3. Controllo Direzionale (il modello tocca il blocco?)
        Direction outDir = direction.getOpposite(); // La direzione verso il blocco adiacente
        EnumProperty<RedstoneSide> property = getProperty(outDir);

        // Se c'è una connessione grafica (SIDE o UP) verso quel lato, passiamo l'energia
        if (state.getValue(property).isConnected()) {
            return power;
        }

        // Se è una linea dritta che non punta verso il pistone, non si attiva!
        return 0;
    }

    @Override
    public int getCobaltSignalIfLinked(BlockState state, Level world, BlockPos pos, Direction direction) {
        int power = state.getValue(POWER);
        if (power == 0) return 0;
        if (isNotConnected(state)) return 0;
        if (direction == Direction.UP) return power;
        Direction outDir = direction.getOpposite();
        EnumProperty<RedstoneSide> property = getProperty(outDir);
        if (state.getValue(property).isConnected()) return power;
        return 0;
    }

    @Override
    public int getDirectCobaltSignalIfLinked(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (direction == Direction.DOWN) return 0;
        int power = state.getValue(POWER);
        if (power <= 0) return 0;
        Direction side = direction.getOpposite();
        if (direction == Direction.UP) return power;
        if (isNotConnected(state)) return 0;
        EnumProperty<RedstoneSide> property = getProperty(side);
        if (state.getValue(property).isConnected()) return power;
        return 0;
    }

    // 🔥 ENERGIA FORTE (Ora alimenta anche i lati, come in Vanilla)
    @Override
    protected int getDirectSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        // La redstone non dà mai energia forte verso l'alto
        if (direction == Direction.DOWN) return 0;

        int power = state.getValue(POWER);
        if (power <= 0) return 0;

        Direction side = direction.getOpposite();

        // 1. ALIMENTARE IL BLOCCO SOTTOSTANTE
        if (direction == Direction.UP) {
            // Il puntino in Vanilla alimenta sempre il blocco su cui poggia.
            BlockPos blockBelowPos = pos.below();
            BlockState blockBelowState = world.getBlockState(blockBelowPos);

            // VEGGENZA: Isoliamo il blocco sottostante dalla vanilla
            if (blockBelowState.isRedstoneConductor(world, blockBelowPos)) {
                for (Direction dir : Direction.values()) {
                    if (dir == Direction.UP) continue; // Ignoriamo il cavo stesso
                    if (isVanillaRedstone(world.getBlockState(blockBelowPos.relative(dir)))) {
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
        EnumProperty<RedstoneSide> property = getProperty(side);
        if (state.getValue(property).isConnected()) {

            BlockPos sideBlockPos = pos.relative(side);
            BlockState sideBlockState = world.getBlockState(sideBlockPos);

            // VEGGENZA: Isoliamo il blocco laterale!
            // Se c'è della redstone vanilla che tocca questo blocco solido,
            // la Cobalt Dust si rifiuta di caricarlo, proteggendo i circuiti.
            // Se invece c'è solo un pistone, lo carica e lo fa scattare!
            if (sideBlockState.isRedstoneConductor(world, sideBlockPos)) {
                for (Direction dir : Direction.values()) {
                    if (dir == side.getOpposite()) continue; // Ignoriamo il lato da cui arriva il cavo
                    if (isVanillaRedstone(world.getBlockState(sideBlockPos.relative(dir)))) {
                        return 0;
                    }
                }
            }
            return power;
        }

        return 0;
    }

    private boolean isVanillaRedstone(BlockState state) {
        return state.is(Blocks.REDSTONE_WIRE) ||
                state.is(Blocks.REPEATER) ||
                state.is(Blocks.COMPARATOR) ||
                state.is(Blocks.REDSTONE_TORCH) ||
                state.is(Blocks.REDSTONE_WALL_TORCH) ||
                state.is(Blocks.REDSTONE_BLOCK);
    }

    private void updateAllNeighbors(Level world, BlockPos pos) {

        // Update of the next 6 neighbors
        world.updateNeighborsAt(pos, this, null);

        // Update of the next 8 diagonals neighbors
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            mutable.setWithOffset(pos, dir);

            // Per updateNeighborsAlways, Minecraft Vanilla preferisce gli Immutable per evitare side-effect asincroni.
            BlockPos immutableSide = mutable.immutable();

            world.updateNeighborsAt(immutableSide, this, null);
            world.updateNeighborsAt(immutableSide.above(), this, null);
            world.updateNeighborsAt(immutableSide.below(), this, null);
        }
    }


    // This method is called when this block has been placed
    @Override
    public void onPlace(BlockState state, @NonNull Level world, @NonNull BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock()) && !world.isClientSide()) {
            this.updateDiagonalShapes(world, pos);
            this.updateAllNeighbors(world, pos);
            NETWORK_HANDLER.updateNetwork(world, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(@NonNull BlockState state, @NonNull ServerLevel world, @NonNull BlockPos pos, boolean moved) {
        if (moved) return;
        super.affectNeighborsAfterRemoval(state, world, pos, false);
        if (!world.isClientSide()) {
            this.updateDiagonalShapes(world, pos);
            this.updateAllNeighbors(world, pos);
            NETWORK_HANDLER.updateNetwork(world, pos);
        }
    }

    // --- NUOVI METODI PER FORZARE LA GRAFICA ---

    private void updateDiagonalShapes(Level world, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        // Update of the next 6 neighbors
        for (Direction dir : Direction.values()) {
            forceShapeUpdate(world, mutable.setWithOffset(pos, dir));
        }

        // Update of the next 8 diagonals neighbors
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            mutable.setWithOffset(pos, dir);
            forceShapeUpdate(world, mutable.move(Direction.UP));

            mutable.setWithOffset(pos, dir);
            forceShapeUpdate(world, mutable.move(Direction.DOWN));
        }
    }

    private void forceShapeUpdate(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        // Controlliamo se il blocco in questa posizione è una tua polvere
        if (state.getBlock() instanceof CobaltWireBlock wireBlock) {
            // Calcoliamo la nuova forma (Punto, Linea, Rampa, ecc.)
            BlockState newState = wireBlock.getWireShapeState(world, pos, state);

            // Se la forma calcolata è diversa da quella attuale, applichiamola immediatamente!
            if (state != newState) {
                world.setBlock(pos, newState, Block.UPDATE_ALL);
                for (Direction dir : Direction.values()) {
                    world.updateNeighborsAt(pos.relative(dir), wireBlock);
                }
            }
        }
    }

    @Override
    public void setPlacedBy(@NonNull Level world, @NonNull BlockPos pos, @NonNull BlockState state, @Nullable LivingEntity placer, @NonNull ItemStack stack) {
        // Notifichiamo che l'energia è sparita
        this.updateAllNeighbors(world, pos);
        NETWORK_HANDLER.updateNetwork(world, pos);
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level world, @NonNull BlockPos pos, Player player, @NonNull BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;

        // Se è un cross completo o un puntino, permettiamo il toggle
        if (isFullyConnected(state) || isNotConnected(state)) {
            if(hasNeighborSignals(world, pos)){
                return InteractionResult.PASS;
            }
            // Se è cross -> diventa puntino. Se è puntino -> torna cross.
            BlockState newState = isFullyConnected(state) ? getDotState(state) : getCrossState(state);

            // 1. Manteniamo il livello di potenza attuale durante la transizione
            newState = newState.setValue(POWER, state.getValue(POWER));
            world.setBlock(pos, newState, Block.UPDATE_ALL);

            // --- FIX DEL BUG DELLA TORCIA ---
            // 2. Avvisiamo i vicini dei vicini! Così il blocco in diagonale
            // scopre che il cavo non lo sta più puntando e fa riaccendere la torcia.
            this.updateAllNeighbors(world, pos);

            // 3. Ricalcoliamo la rete. Cambiando forma, il cavo potrebbe
            // essersi disconnesso (o connesso) a una fonte di energia.
            NETWORK_HANDLER.updateNetwork(world, pos);

            return InteractionResult.SUCCESS;
        }
        // (world.getBlockState(pos.below()).is(ModBlocks.COBALT_RELAY)
        if (hasOneFreeConnectionInALineShape(world, pos)) {
            // Invertiamo lo stato di isolamento (true <-> false)
            BlockState newState = state.cycle(ISOLATED);

            // Aggiorna il blocco nel mondo ricalcolando la forma con il nuovo stato
            world.setBlock(pos, CobaltWireShape.getUpdatedState(world, pos, newState), Block.UPDATE_ALL);
            this.updateAllNeighbors(world, pos);
            NETWORK_HANDLER.updateNetwork(world, pos);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // Helper per capire se lo stato è 4 libero o 4 forzato da connessioni esterne
    private boolean hasNeighborSignals(Level world, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            // Se getRenderConnection (quello che usi per la forma) restituisce qualcosa
            // di diverso da NONE per una direzione, allora c'è un vicino.
            if (CobaltWireShape.getRenderConnection(world, pos, direction) != RedstoneSide.NONE) {
                return true;
            }
        }
        return false;
    }

    // Helper per capire se lo stato è di tipo side
    private boolean hasOneFreeConnectionInALineShape(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        int i = 0;
        int j = 0;
        if(state.getValue(NORTH) != (RedstoneSide.NONE)) i++;
        if(state.getValue(SOUTH) != (RedstoneSide.NONE)) i++;
        if(state.getValue(EAST) != (RedstoneSide.NONE)) j++;
        if(state.getValue(WEST) != (RedstoneSide.NONE)) j++;
        if(i==0 || j==0){ // One axis Connection (Line or Ramp)
            if(i>0){ // North-South Axis
                i = 0;
                if(CobaltWireShape.getRenderConnection(world, pos, Direction.NORTH) != RedstoneSide.NONE) i++;
                if(CobaltWireShape.getRenderConnection(world, pos, Direction.SOUTH) != RedstoneSide.NONE) i++;
                return i==1; // Requires 1 axis to be forced
            }else{  // East-West Axis
                j = 0;
                if(CobaltWireShape.getRenderConnection(world, pos, Direction.EAST) != RedstoneSide.NONE) j++;
                if(CobaltWireShape.getRenderConnection(world, pos, Direction.WEST) != RedstoneSide.NONE) j++;
                return j==1; // Requires 1 axis to be forced
            }
        }
        return false;
    }
    // Helper per creare lo stato a puntino (tutti NONE)
    private BlockState getDotState(BlockState state) {
        return state.setValue(NORTH, RedstoneSide.NONE)
                .setValue(SOUTH, RedstoneSide.NONE)
                .setValue(EAST, RedstoneSide.NONE)
                .setValue(WEST, RedstoneSide.NONE);
    }


    // Helper to check if it's currently a dot
    private boolean isNotConnected(BlockState state) {
        return state.getValue(NORTH) == RedstoneSide.NONE &&
                state.getValue(SOUTH) == RedstoneSide.NONE &&
                state.getValue(EAST) == RedstoneSide.NONE &&
                state.getValue(WEST) == RedstoneSide.NONE;
    }

    // Helper to check if it's currently a cross
    private boolean isFullyConnected(BlockState state) {
        return state.getValue(NORTH) == RedstoneSide.SIDE &&
                state.getValue(SOUTH) == RedstoneSide.SIDE &&
                state.getValue(EAST) == RedstoneSide.SIDE &&
                state.getValue(WEST) == RedstoneSide.SIDE;
    }

    // Forces all 4 sides to 'SIDE' to create the cross look
    private BlockState getCrossState(BlockState state) {
        return state.setValue(NORTH, RedstoneSide.SIDE)
                .setValue(SOUTH, RedstoneSide.SIDE)
                .setValue(EAST, RedstoneSide.SIDE)
                .setValue(WEST, RedstoneSide.SIDE);
    }




}