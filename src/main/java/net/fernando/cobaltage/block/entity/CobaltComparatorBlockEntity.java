package net.fernando.cobaltage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

public class CobaltComparatorBlockEntity extends BlockEntity {
    private static final int DEFAULT_OUTPUT = 0;
    private int output = 0;

    public CobaltComparatorBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.COBALT_COMPARATOR_ENTITY, blockPos, blockState);
    }

    protected void saveAdditional(@NonNull ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putInt("OutputSignal", this.output);
    }

    protected void loadAdditional(@NonNull ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.output = valueInput.getIntOr("OutputSignal", 0);
    }

    public int getOutputSignal() {
        return this.output;
    }

    public void setOutputSignal(int i) {
        this.output = i;
    }
}