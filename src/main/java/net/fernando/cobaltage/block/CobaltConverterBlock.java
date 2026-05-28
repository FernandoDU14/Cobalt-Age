package net.fernando.cobaltage.block;

import net.fernando.cobaltage.block.wire.CobaltWireNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class CobaltConverterBlock extends Block implements SimpleWaterloggedBlock, CobaltPowerSource {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    public static final BooleanProperty COBALT_LIT = BooleanProperty.create("cobalt_lit");
    public static final BooleanProperty REDSTONE_LIT = BooleanProperty.create("redstone_lit");
    public static final BooleanProperty COBALT_INPUT = BooleanProperty.create("cobalt_input");

    private static final CobaltWireNetwork NETWORK_HANDLER = new CobaltWireNetwork();

    public CobaltConverterBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWER, 0)
                .setValue(COBALT_LIT, false)
                .setValue(REDSTONE_LIT, false)
                .setValue(COBALT_INPUT, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public void animateTick(BlockState state, @NonNull Level world, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);

        // 1. Troviamo il centro esatto del blocco
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.4; // L'altezza delle torce
        double centerZ = pos.getZ() + 0.5;

        // 2. Creiamo l'oscillazione casuale (il tremolio del fumo)
        double randX = (random.nextDouble() - 0.5) * 0.2;
        double randY = (random.nextDouble() - 0.5) * 0.2;
        double randZ = (random.nextDouble() - 0.5) * 0.2;

        // 3. Offset di distanza dal centro: 4 pixel (0.25 blocchi)
        double offsetDistance = 0.25;

        if (state.getValue(COBALT_LIT)) {
            // La torcia Cobalt è spostata "IN AVANTI" rispetto al centro
            double x = centerX + (facing.getStepX() * offsetDistance) + randX;
            double y = centerY + randY;
            double z = centerZ + (facing.getStepZ() * offsetDistance) + randZ;

            int cobaltBlue = (0) | (153 << 8) | 255;
            DustParticleOptions cobaltDust = new DustParticleOptions(cobaltBlue, 1.0f);
            world.addParticle(cobaltDust, x, y, z, 0.0, 0.0, 0.0);
        }

        if (state.getValue(REDSTONE_LIT)) {
            // La torcia Redstone è spostata "ALL'INDIETRO" rispetto al centro (nota il segno meno)
            double x = centerX - (facing.getStepX() * offsetDistance) + randX;
            double y = centerY + randY;
            double z = centerZ - (facing.getStepZ() * offsetDistance) + randZ;

            world.addParticle(DustParticleOptions.REDSTONE, x, y, z, 0.0F, 0.0F, 0.0F);
        }
    }

    @Override
    public @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER, COBALT_LIT, REDSTONE_LIT, COBALT_INPUT, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public @NonNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canSurvive(@NonNull BlockState state, LevelReader world, BlockPos pos) {
        BlockPos floorPos = pos.below();
        BlockState floorState = world.getBlockState(floorPos);

        // This will allow the placement only and only if the block under is valid and is not another Cobalt Dust
        return floorState.isFaceSturdy(world, floorPos, Direction.UP) || floorState.is(Blocks.HOPPER);
    }

    @Override
    public void onPlace(BlockState state, @NonNull Level world, @NonNull BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock()) && !world.isClientSide()) {
            updateConverterState(state, world, pos);
            NETWORK_HANDLER.updateNetwork(world, pos.relative(state.getValue(FACING).getOpposite()));
        }
    }

    @Override
    public @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        // ❌ se non può stare lì → aria
        if (!state.canSurvive(world, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }


    protected int getCobaltInputPower(Level world, BlockPos pos, BlockState state, Boolean InvertFacing) {
        Direction direction = state.getValue(FACING);
        if(InvertFacing){
            direction = direction.getOpposite();
        }
        BlockPos rearPos = pos.relative(direction);
        BlockState rearState = world.getBlockState(rearPos);

        int power = 0;

        // 🟦 1. Sorgenti Cobalt
        if (rearState.getBlock() instanceof CobaltPowerSource source) {
            if (source.getSignalType() == CobaltPowerSource.CobaltSignalType.COBALT) {
                if (rearState.getBlock() instanceof CobaltRepeaterBlock ||
                        rearState.getBlock() instanceof CobaltComparatorBlock) {
                    Direction facing = rearState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    if (facing == direction) power = Math.max(power, source.getCobaltPower(rearState, world, rearPos));
                }
                else{
                    power = Math.max(power, source.getCobaltPower(rearState, world, rearPos));
                }
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
        // 🟦 4. Blocco Solido caricato da energia forte di tipo cobalt o compatibile
        else if (rearState.isRedstoneConductor(world, rearPos)) {
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

    private void updateConverterState(BlockState state, Level world, BlockPos pos){
        if (world.isClientSide()) return;

        Direction cobaltSide = state.getValue(FACING);
        Direction redstoneSide = cobaltSide.getOpposite();
        BlockPos redstoneSideBlockPos = pos.relative(redstoneSide);

        // 1. Leggi segnali
        int cobaltIn = getCobaltInputPower(world, pos, state, false);
        int redstoneIn = world.getSignal(redstoneSideBlockPos, redstoneSide);


        int currentPower = 0;
        boolean isCobaltInputMode = state.getValue(COBALT_INPUT);
        boolean actualRedstoneLit = state.getValue(REDSTONE_LIT);
        boolean actualCobaltLit = state.getValue(COBALT_LIT);

        int newPower = 0;
        boolean nextCobaltInput = isCobaltInputMode;

        // 🛑 LOGICA ANTI-FEEDBACK (State Lock) 🛑
        if (isCobaltInputMode) {
            // Stiamo traducendo da Cobalt a Redstone
            if (cobaltIn > 0) {
                newPower = cobaltIn - 1;
            }
            currentPower = state.getValue(POWER);
        } else {
            // Stiamo traducendo da Redstone a Cobalt
            if (redstoneIn > 0) {
                // ulteriore controllo che non sia sorgente cobalt:
                if(getCobaltInputPower(world, pos, state, true) == 0){
                    currentPower = state.getValue(POWER);
                    newPower = redstoneIn - 1;
                }
            } else {
                // Redstone spenta. Spegniamo tutto.
                currentPower = state.getValue(POWER);
            }
        }

        // 🔄 CAMBIO DI MODALITÀ 🔄
        // Se ci siamo completamente spenti, siamo liberi di ascoltare un nuovo
        // segnale da ENTRAMBE le parti. Chi arriva prima, vince.
        if (newPower == 0) {
            if (cobaltIn > 0) {
                newPower = cobaltIn - 1;
                nextCobaltInput = true;
            } else if (redstoneIn > 0) {
                if(getCobaltInputPower(world, pos, state, true) == 0){
                    newPower = redstoneIn - 1;
                    nextCobaltInput = false;
                }
            }
        }

        boolean nextCobaltLit = !nextCobaltInput && newPower > 0;
        boolean nextRedstoneLit = nextCobaltInput && newPower > 0;

        // Se qualcosa è cambiato, aggiorniamo il mondo
        if (currentPower != newPower || isCobaltInputMode != nextCobaltInput) {


            if (actualRedstoneLit == actualCobaltLit) {
                // se è tutto spento
                if(!actualRedstoneLit){
                    // però mi sto accendendo
                    world.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.1F, 0.55F);
                }
            }else{
                // se è acceso, e mi sto spegnendo
                if(!actualRedstoneLit){
                    world.playSound(null, pos, SoundEvents.CRAFTER_FAIL, SoundSource.BLOCKS, 0.9F, 0.55F);
                }
            }


            BlockState newState = state
                    .setValue(POWER, newPower)
                    .setValue(COBALT_INPUT, nextCobaltInput)
                    .setValue(COBALT_LIT, nextCobaltLit)
                    .setValue(REDSTONE_LIT, nextRedstoneLit);

            world.setBlock(pos, newState, Block.UPDATE_ALL);
            updateNeighbors(world, pos, newState);
        }
    }

    @Override
    public void neighborChanged(@NonNull BlockState state, Level world, @NonNull BlockPos pos, @NonNull Block sourceBlock, @Nullable Orientation orientation, boolean notify) {
        if (world.isClientSide()) return;
        updateConverterState(state, world, pos);
    }

    private void updateNeighbors(Level world, BlockPos pos, BlockState state) {
        Direction cobaltSide = state.getValue(FACING);
        Direction redstoneSide = cobaltSide.getOpposite();

        // Aggiorna Tutti i Primi Vicini e Primi Vicini Diagonali
        world.updateNeighborsAt(pos, this, null);
        world.updateNeighborsAt(pos.relative(redstoneSide), this, null);

        // Aggiorna Cobalt Network
        NETWORK_HANDLER.updateNetwork(world, pos.relative(cobaltSide));
    }

    // --- VANILLA REDSTONE OUTPUT ---
    @Override
    public boolean isSignalSource(@NonNull BlockState state) {
        return true;
    }

    // --- COBALT_INGOT OUTPUT ---
    @Override
    public int getCobaltPower(BlockState state, Level world, BlockPos pos) {
        // Emette Cobalt solo se la modalità è Redstone -> Cobalt
        return !state.getValue(COBALT_INPUT) ? state.getValue(POWER) : 0;
    }

    @Override
    protected int getDirectSignal(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        return getSignal(state, world, pos, direction);
    }


    @Override
    protected int getSignal(BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull Direction direction) {
        // Emette solo se la modalità è Cobalt -> Redstone e la faccia che chiede è quella davanti
        if (state.getValue(COBALT_INPUT) && direction == state.getValue(FACING)) {
            return state.getValue(POWER);
        }
        return 0;
    }

    @Override
    public int getStrongCobaltPower(BlockState state, Level world, BlockPos pos, Direction direction) {
        // Emette segnale forte verso il retro
        if (direction == state.getValue(FACING) && !state.getValue(COBALT_INPUT)) {
            return state.getValue(POWER);
        }
        return 0;
    }
}
