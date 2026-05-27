package net.fernando.cobaltage.block.blockentities;

import net.fernando.cobaltage.block.CobaltComparatorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

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
        this.setChanged(); // Ricordati sempre di segnare il blocco come "sporco" per salvarlo
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("OutputSignal", this.outputSignal);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput view) {
        super.loadAdditional(view);
        this.outputSignal = view.getIntOr("OutputSignal", 0);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, CobaltComparatorBlockEntity blockEntity) {
        if (world.isClientSide()) return;

        // Eseguiamo il controllo ogni 2 tick per simulare il comportamento vanilla
        if (world.getGameTime() % 2 != 0) return;

        if (state.getBlock() instanceof CobaltComparatorBlock block) {
            int currentInput = block.getInputSignal(world, pos, state);

            // Se il segnale è cambiato, forziamo un aggiornamento del blocco
            if (blockEntity.getOutputSignal() != currentInput) {
                // Nota: usiamo getOutputSignal() o un campo d'appoggio per il confronto
                world.scheduleTick(pos, state.getBlock(), 1);
            }
        }
    }


}