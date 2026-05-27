package net.fernando.cobaltage.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.item.ModItems;
import net.minecraft.core.HolderLookup;
import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootTableProvider {

    public ModLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        dropSelf(ModBlocks.COBALT_BLOCK);
        dropSelf(ModBlocks.RAW_COBALT_BLOCK);
        dropSelf(ModBlocks.COBALT_DUST_BLOCK);

        add(ModBlocks.DEEPSLATE_COBALT_ORE, createOreDrop(ModBlocks.DEEPSLATE_COBALT_ORE, ModItems.RAW_COBALT));
        add(ModBlocks.COBALT_ORE, createOreDrop(ModBlocks.COBALT_ORE, ModItems.RAW_COBALT));

    }
}