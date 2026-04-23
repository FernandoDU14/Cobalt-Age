package net.fernando.cobaltrails.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fernando.cobaltrails.block.ModBlocks;
import net.fernando.cobaltrails.item.ModItems;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootTableProvider {

    public ModLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        addDrop(ModBlocks.COBALT_BLOCK);
        addDrop(ModBlocks.RAW_COBALT_BLOCK);
        addDrop(ModBlocks.COBALT_DUST_BLOCK);

        addDrop(ModBlocks.DEEPSLATE_COBALT_ORE, oreDrops(ModBlocks.DEEPSLATE_COBALT_ORE, ModItems.RAW_COBALT));
        addDrop(ModBlocks.COBALT_ORE, oreDrops(ModBlocks.COBALT_ORE, ModItems.RAW_COBALT));

    }
}