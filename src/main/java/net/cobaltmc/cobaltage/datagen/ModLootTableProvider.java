package net.cobaltmc.cobaltage.datagen;

import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootSubProvider {

    public ModLootTableProvider(FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        dropSelf(ModBlocks.COBALT_BLOCK);
        dropSelf(ModBlocks.RAW_COBALT_BLOCK);
        dropSelf(ModBlocks.COBALT_DUST_BLOCK);
        dropSelf(ModBlocks.COBALT_LAMP);

        add(ModBlocks.DEEPSLATE_COBALT_ORE, createOreDrop(ModBlocks.DEEPSLATE_COBALT_ORE, ModItems.RAW_COBALT));
        add(ModBlocks.COBALT_ORE, createOreDrop(ModBlocks.COBALT_ORE, ModItems.RAW_COBALT));

    }
}