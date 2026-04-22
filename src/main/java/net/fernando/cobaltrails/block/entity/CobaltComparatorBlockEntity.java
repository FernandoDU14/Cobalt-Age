package net.fernando.cobaltrails.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.storage.ReadView;   // Nuovi import 1.21.11
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

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


}