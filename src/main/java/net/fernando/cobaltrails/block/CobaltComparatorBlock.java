package net.fernando.cobaltrails.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class CobaltComparatorBlock extends ComparatorBlock {
    public CobaltComparatorBlock(Settings settings) {
        super(settings);
    }

    /*
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CobaltComparatorBlockEntity(pos, state);
    }

    @Override
    protected int getOutputLevel(BlockView world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CobaltComparatorBlockEntity cobaltEntity) {
            return cobaltEntity.getOutputSignal();
        }
        return 0;
    }

    // In 1.21.1 il metodo di aggiornamento principale è spesso neighborUpdate
    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, net.minecraft.block.Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            this.refreshOutput(world, pos, state);
        }
    }

    private void refreshOutput(World world, BlockPos pos, BlockState state) {
        int i = this.calculateOutputLevel(world, pos, state);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        int j = blockEntity instanceof CobaltComparatorBlockEntity cobaltEntity ? cobaltEntity.getOutputSignal() : 0;

        if (i != j || state.get(POWERED) != this.hasPower(world, pos, state)) {
            world.scheduleBlockTick(pos, this, 2, TickPriority.HIGH);
        }
    }

    private int calculateOutputLevel(World world, BlockPos pos, BlockState state) {
        int i = this.getPower(world, pos, state);
        int j = this.getMaxInputLevelSides(world, pos, state);
        return state.get(MODE) == net.minecraft.block.enums.ComparatorMode.SUBTRACT ? Math.max(i - j, 0) : i;
    }


    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int i = this.calculateOutputLevel(world, pos, state);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CobaltComparatorBlockEntity cobaltEntity) {
            cobaltEntity.setOutputSignal(i);
            cobaltEntity.markDirty();
        }
        // Chiama la logica vanilla per aggiornare lo stato visivo (acceso/spento)
        super.scheduledTick(state, world, pos, random);
    }

     */

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(POWERED)) {
            // Logica particelle blu simile alla polvere
            Direction direction = state.get(FACING);
            double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double e = (double)pos.getY() + 0.4 + (random.nextDouble() - 0.5) * 0.2;
            double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;

            int cobaltBlue = (0 << 16) | (153 << 8) | 255;
            DustParticleEffect cobaltDust = new DustParticleEffect(cobaltBlue, 1.0f);

            world.addParticleClient(cobaltDust, d, e, f, 0.0, 0.0, 0.0);
        }
    }

}