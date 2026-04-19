package net.fernando.cobaltrails.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fernando.cobaltrails.block.ModBlocks;
import net.fernando.cobaltrails.item.ModItems;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {

    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        List<ItemConvertible> COBALT_ORSE_SMELTABLES = List.of(ModItems.RAW_COBALT,
                ModBlocks.COBALT_ORE, ModBlocks.DEEPSLATE_COBALT_ORE);

        offerSmelting(exporter, COBALT_ORSE_SMELTABLES, RecipeCategory.MISC, ModItems.COBALT_INGOT,
                1.0f, 200, "cobalt_ingot");

        offerBlasting(exporter, COBALT_ORSE_SMELTABLES, RecipeCategory.MISC, ModItems.COBALT_INGOT,
                1.0f, 100, "cobalt_ingot");
        offerBlasting(exporter, List.of(ModBlocks.RAW_COBALT_BLOCK), RecipeCategory.MISC, ModBlocks.COBALT_BLOCK,
                1.0f, 100, "cobalt_block");

        offerReversibleCompactingRecipes(exporter, RecipeCategory.MISC, ModItems.COBALT_INGOT,
                RecipeCategory.BUILDING_BLOCKS, ModBlocks.COBALT_BLOCK);

        offerReversibleCompactingRecipes(exporter, RecipeCategory.MISC, ModItems.RAW_COBALT,
                RecipeCategory.BUILDING_BLOCKS, ModBlocks.RAW_COBALT_BLOCK);

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModBlocks.COBALT_RAIL)
                .pattern("C C")
                .pattern("CBC")
                .pattern("CRC")
                .input('C', ModItems.COBALT_INGOT)
                .input('B', Items.BREEZE_ROD)
                .input('R', Items.REDSTONE)
                .criterion(hasItem(ModItems.COBALT_INGOT), conditionsFromItem(ModItems.COBALT_INGOT))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.COBALT_INGOT)
                .pattern("CCC")
                .pattern("CCC")
                .pattern("CCC")
                .input('C', ModItems.COBALT_NUGGET)
                .criterion(hasItem(ModItems.COBALT_INGOT), conditionsFromItem(ModItems.COBALT_INGOT))
                .offerTo(exporter, Identifier.of("cobalt_ingot_from_cobalt_nugget"));

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.COBALT_NUGGET, 9)
                .input(ModItems.COBALT_INGOT)
                .criterion(hasItem(ModItems.COBALT_INGOT), conditionsFromItem(ModItems.COBALT_INGOT))
                .offerTo(exporter);
    }
}
