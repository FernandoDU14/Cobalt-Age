package net.fernando.cobaltage.block;

import net.fernando.cobaltage.block.wire.CobaltWireNetwork;
import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

public class CobaltConverterBlock extends Block implements Waterloggable, CobaltPowerSource {

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final IntProperty POWER = Properties.POWER;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    // Proprietà per il modello di Blockbench
    public static final BooleanProperty COBALT_LIT = BooleanProperty.of("cobalt_lit");
    public static final BooleanProperty REDSTONE_LIT = BooleanProperty.of("redstone_lit");
    public static final BooleanProperty COBALT_INPUT = BooleanProperty.of("cobalt_input");

    private static final CobaltWireNetwork NETWORK_HANDLER = new CobaltWireNetwork();

    public CobaltConverterBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWER, 0)
                .with(COBALT_LIT, false)
                .with(REDSTONE_LIT, false)
                .with(COBALT_INPUT, false)
                .with(WATERLOGGED, false));
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        Direction facing = state.get(FACING);

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

        if (state.get(COBALT_LIT)) {
            // La torcia Cobalt è spostata "IN AVANTI" rispetto al centro
            double x = centerX + (facing.getOffsetX() * offsetDistance) + randX;
            double y = centerY + randY;
            double z = centerZ + (facing.getOffsetZ() * offsetDistance) + randZ;

            int cobaltBlue = (0) | (153 << 8) | 255;
            DustParticleEffect cobaltDust = new DustParticleEffect(cobaltBlue, 1.0f);
            world.addParticleClient(cobaltDust, x, y, z, 0.0, 0.0, 0.0);
        }

        if (state.get(REDSTONE_LIT)) {
            // La torcia Redstone è spostata "ALL'INDIETRO" rispetto al centro (nota il segno meno)
            double x = centerX - (facing.getOffsetX() * offsetDistance) + randX;
            double y = centerY + randY;
            double z = centerZ - (facing.getOffsetZ() * offsetDistance) + randZ;

            world.addParticleClient(DustParticleEffect.DEFAULT, x, y, z, 0.0F, 0.0F, 0.0F);
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER, COBALT_LIT, REDSTONE_LIT, COBALT_INPUT, WATERLOGGED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos floorPos = pos.down();
        BlockState floorState = world.getBlockState(floorPos);

        // This will allow the placement only and only if the block under is valid and is not another Cobalt Dust
        return floorState.isSideSolidFullSquare(world, floorPos, Direction.UP) || floorState.isOf(Blocks.HOPPER);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        if (state.get(WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        // ❌ se non può stare lì → aria
        if (!state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }


    protected int getCobaltInputPower(World world, BlockPos pos, BlockState state, Boolean InvertFacing) {
        Direction direction = state.get(FACING);
        if(InvertFacing){
            direction = direction.getOpposite();
        }
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
        else if (rearState.isSolidBlock(world, rearPos)) {
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


    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation orientation, boolean notify) {
        if (world.isClient()) return;

        Direction cobaltSide = state.get(FACING);
        Direction redstoneSide = cobaltSide.getOpposite();
        BlockPos redstoneSideBlockPos = pos.offset(redstoneSide);

        // 1. Leggi segnali
        int cobaltIn = getCobaltInputPower(world, pos, state, false);
        int redstoneIn = world.getEmittedRedstonePower(redstoneSideBlockPos, redstoneSide);


        int currentPower = 0;
        boolean isCobaltInputMode = state.get(COBALT_INPUT);
        boolean actualRedstoneLit = state.get(REDSTONE_LIT);
        boolean actualCobaltLit = state.get(COBALT_LIT);

        int newPower = 0;
        boolean nextCobaltInput = isCobaltInputMode;

        // 🛑 LOGICA ANTI-FEEDBACK (State Lock) 🛑
        if (isCobaltInputMode) {
            // Stiamo traducendo da Cobalt a Redstone
            if (cobaltIn > 0) {
                newPower = cobaltIn - 1;
            }
            currentPower = state.get(POWER);
        } else {
            // Stiamo traducendo da Redstone a Cobalt
            if (redstoneIn > 0) {
                // ulteriore controllo che non sia sorgente cobalt:
                if(getCobaltInputPower(world, pos, state, true) == 0){
                    currentPower = state.get(POWER);
                    newPower = redstoneIn - 1;
                }
            } else {
                // Redstone spenta. Spegniamo tutto.
                currentPower = state.get(POWER);
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
                    world.playSound(null, pos, SoundEvents.BLOCK_COMPARATOR_CLICK, SoundCategory.BLOCKS, 0.1F, 0.55F);
                }
            }else{
                // se è acceso, e mi sto spegnendo
                if(!actualRedstoneLit){
                    world.playSound(null, pos, SoundEvents.BLOCK_CRAFTER_FAIL, SoundCategory.BLOCKS, 0.9F, 0.55F);
                }
            }


            BlockState newState = state
                    .with(POWER, newPower)
                    .with(COBALT_INPUT, nextCobaltInput)
                    .with(COBALT_LIT, nextCobaltLit)
                    .with(REDSTONE_LIT, nextRedstoneLit);

            world.setBlockState(pos, newState, Block.NOTIFY_ALL);
            updateNeighbors(world, pos, newState);
        }
    }

    private void updateNeighbors(World world, BlockPos pos, BlockState state) {
        Direction redstoneSide = state.get(FACING);
        Direction cobaltSide = redstoneSide.getOpposite();

        // Aggiorna Redstone
        world.updateNeighborsAlways(pos, this, null);
        world.updateNeighborsAlways(pos.offset(redstoneSide), this, null);

        // Aggiorna Cobalt Network
        NETWORK_HANDLER.updateNetwork(world, pos.offset(cobaltSide));
    }



    // --- VANILLA REDSTONE OUTPUT ---
    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    // --- COBALT OUTPUT ---
    @Override
    public int getCobaltPower(BlockState state, World world, BlockPos pos) {
        // Emette Cobalt solo se la modalità è Redstone -> Cobalt
        return !state.get(COBALT_INPUT) ? state.get(POWER) : 0;
    }

    @Override
    protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return getWeakRedstonePower(state, world, pos, direction);
    }


    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        // Emette solo se la modalità è Cobalt -> Redstone e la faccia che chiede è quella davanti
        if (state.get(COBALT_INPUT) && direction == state.get(FACING)) {
            return state.get(POWER);
        }
        return 0;
    }

    @Override
    public int getStrongCobaltPower(BlockState state, World world, BlockPos pos, Direction direction) {
        // Emette segnale forte verso il retro
        if (direction == state.get(FACING) && !state.get(COBALT_INPUT)) {
            return state.get(POWER);
        }
        return 0;
    }
}
