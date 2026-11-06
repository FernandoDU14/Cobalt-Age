package net.cobaltmc.cobaltage.datagen;

import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.item.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        blockStateModelGenerator.createActiveRail(ModBlocks.COBALT_RAIL);

        blockStateModelGenerator.createTrivialCube(ModBlocks.COBALT_ORE);
        blockStateModelGenerator.createTrivialCube(ModBlocks.DEEPSLATE_COBALT_ORE);
        blockStateModelGenerator.createTrivialCube(ModBlocks.COBALT_BLOCK);
        blockStateModelGenerator.createTrivialCube(ModBlocks.RAW_COBALT_BLOCK);
        blockStateModelGenerator.createTrivialCube(ModBlocks.COBALT_DUST_BLOCK);
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        itemModelGenerator.generateFlatItem(ModItems.COBALT_INGOT, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.RAW_COBALT, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.COBALT_NUGGET, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.DUST_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
    }
}