package net.fernando.cobaltage.block;

import com.mojang.serialization.MapCodec;
import net.fernando.cobaltage.block.wire.CobaltSignalSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public class CobaltDustBlock extends Block implements CobaltSignalSource {
    public static final MapCodec<CobaltDustBlock> CODEC = simpleCodec(CobaltDustBlock::new);

    @Override
    public @NonNull MapCodec<CobaltDustBlock> codec() {
        return CODEC;
    }

    public CobaltDustBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    // --- COBALT_INGOT POWER SYSTEM ---

    @Override
    public int getCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return 15;
    }

    // Non abbiamo bisogno di sovrascrivere getStrongCobaltPower() qui.
    // In Vanilla, il blocco di redstone dà solo Weak Power, mai Strong Power
    // attraverso altri blocchi. Il default dell'interfaccia a 0 va benissimo.

    // --- VANILLA ISOLATION E MECCANISMI ---

    @Override
    protected boolean isSignalSource(@NonNull BlockState state) {
        return false;
    }

    @Override
    protected int getSignal(@NonNull BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        // Direction in getWeakRedstonePower indica la direzione DA CUI si sta chiedendo energia.
        // Usiamo l'opposto per trovare il blocco che sta cercando di prelevare energia.
        BlockPos neighborPos = pos.relative(direction.getOpposite());
        BlockState neighborState = world.getBlockState(neighborPos);

        // Se il blocco vicino è redstone vanilla, rifiutiamo di alimentarlo.
        if (isVanillaRedstone(neighborState)) {
            return 0; // Niente energia per te, redstone rossa!
        }

        // Se è un pistone, una lampada, o una porta di ferro, diamo energia 15.
        return 15;
    }

    private boolean isVanillaRedstone(BlockState state) {
        // Here we need REDSTONE BLOCK
        return state.is(Blocks.REDSTONE_WIRE) ||
                state.is(Blocks.REPEATER) ||
                state.is(Blocks.COMPARATOR) ||
                state.is(Blocks.POWERED_RAIL) ||
                state.is(Blocks.ACTIVATOR_RAIL) ||
                state.is(Blocks.REDSTONE_TORCH) ||
                state.is(Blocks.REDSTONE_WALL_TORCH) ||
                state.is(Blocks.REDSTONE_BLOCK);
    }
}