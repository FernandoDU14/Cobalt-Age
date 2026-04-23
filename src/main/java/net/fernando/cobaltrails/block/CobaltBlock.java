package net.fernando.cobaltrails.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class CobaltBlock extends Block implements CobaltPowerSource {
    public static final MapCodec<CobaltBlock> CODEC = createCodec(CobaltBlock::new);

    @Override
    public MapCodec<CobaltBlock> getCodec() {
        return CODEC;
    }

    public CobaltBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    // --- COBALT POWER SYSTEM ---

    @Override
    public int getCobaltPower(BlockState state, World world, BlockPos pos) {
        // Alimenta i Cobalt Wires e gli altri componenti della tua mod con potenza massima.
        return 15;
    }

    // Non abbiamo bisogno di sovrascrivere getStrongCobaltPower() qui.
    // In Vanilla, il blocco di redstone dà solo Weak Power, mai Strong Power
    // attraverso altri blocchi. Il default dell'interfaccia a 0 va benissimo.

    // --- VANILLA ISOLATION E MECCANISMI ---

    @Override
    protected boolean emitsRedstonePower(BlockState state) {
        return false;
    }

    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        // Direction in getWeakRedstonePower indica la direzione DA CUI si sta chiedendo energia.
        // Usiamo l'opposto per trovare il blocco che sta cercando di prelevare energia.
        BlockPos neighborPos = pos.offset(direction.getOpposite());
        BlockState neighborState = world.getBlockState(neighborPos);

        // Se il blocco vicino è redstone vanilla, rifiutiamo di alimentarlo.
        if (isVanillaRedstone(neighborState)) {
            return 0; // Niente energia per te, redstone rossa!
        }

        // Se è un pistone, una lampada, o una porta di ferro, diamo energia 15.
        return 15;
    }

    // Il fidato metodo di isolamento usato in tutti gli altri tuoi componenti
    private boolean isVanillaRedstone(BlockState state) {
        return state.isOf(Blocks.REDSTONE_WIRE) ||
                state.isOf(Blocks.REPEATER) ||
                state.isOf(Blocks.COMPARATOR) ||
                state.isOf(Blocks.REDSTONE_TORCH) ||
                state.isOf(Blocks.REDSTONE_WALL_TORCH) ||
                state.isOf(Blocks.REDSTONE_BLOCK);
    }
}