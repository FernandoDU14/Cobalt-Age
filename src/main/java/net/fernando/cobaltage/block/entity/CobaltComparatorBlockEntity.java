package net.fernando.cobaltage.block.entity;

import net.fernando.cobaltage.block.CobaltComparatorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.storage.ReadView;   // Nuovi import 1.21.11
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CobaltComparatorBlockEntity extends BlockEntity {
    private int outputSignal;

    public CobaltComparatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COBALT_COMPARATOR_ENTITY, pos, state);
    }

    public int getOutputSignal() {
        return this.outputSignal;
    }

    public void setOutputSignal(int outputSignal) {
        this.outputSignal = outputSignal;
        this.markDirty(); // Ricordati sempre di segnare il blocco come "sporco" per salvarlo
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("OutputSignal", this.outputSignal);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.outputSignal = view.getInt("OutputSignal", 0);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CobaltComparatorBlockEntity blockEntity) {
        if (world.isClient()) return;

        // Eseguiamo il controllo ogni 2 tick per simulare il comportamento vanilla
        if (world.getTime() % 2 != 0) return;

        if (state.getBlock() instanceof CobaltComparatorBlock block) {
            int currentInput = block.getPower(world, pos, state);

            // Se il segnale è cambiato, forziamo un aggiornamento del blocco
            if (blockEntity.getOutputSignal() != currentInput) {
                // Nota: usiamo getOutputSignal() o un campo d'appoggio per il confronto
                world.scheduleBlockTick(pos, state.getBlock(), 1);
            }
        }
    }


}