package net.fernando.cobaltage.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.item.ModItems;
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.data.recipe.RecipeGenerator;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {

    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }



    @Override
    protected @NonNull RecipeGenerator getRecipeGenerator(RegistryWrapper.@NonNull WrapperLookup registries, @NonNull RecipeExporter exporter) {
        return new RecipeGenerator(registries, exporter) {
            @Override
            public void generate() {
                List<ItemConvertible> COBALT_ORSE_SMELTABLES = List.of(ModItems.RAW_COBALT,
                        ModBlocks.COBALT_ORE, ModBlocks.DEEPSLATE_COBALT_ORE);

                offerSmelting(COBALT_ORSE_SMELTABLES, RecipeCategory.MISC, ModItems.COBALT_INGOT,
                        1.0f, 200, "cobalt_ingot");

                offerBlasting(COBALT_ORSE_SMELTABLES, RecipeCategory.MISC, ModItems.COBALT_INGOT,
                        1.0f, 100, "cobalt_ingot");
                offerBlasting(List.of(ModBlocks.RAW_COBALT_BLOCK), RecipeCategory.MISC, ModBlocks.COBALT_BLOCK,
                        1.0f, 100, "cobalt_block");

                offerReversibleCompactingRecipes(RecipeCategory.MISC, ModItems.COBALT_INGOT,
                        RecipeCategory.BUILDING_BLOCKS, ModBlocks.COBALT_BLOCK);

                offerReversibleCompactingRecipes(RecipeCategory.MISC, ModItems.RAW_COBALT,
                        RecipeCategory.BUILDING_BLOCKS, ModBlocks.RAW_COBALT_BLOCK);

                createShaped(RecipeCategory.MISC, ModBlocks.COBALT_RAIL, 6)
                        .pattern("C C")
                        .pattern("CBC")
                        .pattern("CRC")
                        .input('C', ModItems.COBALT_INGOT)
                        .input('B', Items.BREEZE_ROD)
                        .input('R', Items.REDSTONE)
                        .criterion(hasItem(ModItems.COBALT_INGOT), conditionsFromItem(ModItems.COBALT_INGOT))
                        .offerTo(exporter);

                createShaped(RecipeCategory.MISC, ModItems.COBALT_INGOT)
                        .pattern("CCC")
                        .pattern("CCC")
                        .pattern("CCC")
                        .input('C', ModItems.COBALT_NUGGET)
                        .criterion(hasItem(ModItems.COBALT_INGOT), conditionsFromItem(ModItems.COBALT_INGOT))
                        .offerTo(exporter, RegistryKey.of(RegistryKeys.RECIPE,Identifier.of("cobalt_ingot_from_cobalt_nugget")));

                createShapeless(RecipeCategory.MISC, ModItems.COBALT_NUGGET, 9)
                        .input(ModItems.COBALT_INGOT)
                        .criterion(hasItem(ModItems.COBALT_INGOT), conditionsFromItem(ModItems.COBALT_INGOT))
                        .offerTo(exporter);

                offerReversibleCompactingRecipes(RecipeCategory.REDSTONE, ModItems.COBALT_DUST,
                        RecipeCategory.REDSTONE, ModBlocks.COBALT_DUST_BLOCK);


                createShaped(RecipeCategory.REDSTONE, ModBlocks.CONVERTER)
                        .pattern("CQT")
                        .pattern("SSS")
                        .input('C', ModItems.COBALT_TORCH)
                        .input('Q', Items.QUARTZ)
                        .input('T', Items.REDSTONE_TORCH)
                        .input('S', Items.STONE)
                        .criterion(hasItem(ModItems.COBALT_TORCH), conditionsFromItem(ModItems.COBALT_TORCH))
                        .offerTo(exporter);

                createShaped(RecipeCategory.REDSTONE, ModBlocks.COBALT_COMPARATOR)
                        .pattern(" C ")
                        .pattern("CQC")
                        .pattern("SSS")
                        .input('C', ModItems.COBALT_TORCH)
                        .input('S', Items.STONE)
                        .input('Q', Items.QUARTZ)
                        .criterion(hasItem(ModItems.COBALT_TORCH), conditionsFromItem(ModItems.COBALT_TORCH))
                        .offerTo(exporter);

                createShaped(RecipeCategory.REDSTONE, ModBlocks.COBALT_REPEATER)
                        .pattern("CDC")
                        .pattern("SSS")
                        .input('C', ModItems.COBALT_TORCH)
                        .input('S', Items.STONE)
                        .input('D', ModItems.COBALT_DUST)
                        .criterion(hasItem(ModItems.COBALT_TORCH), conditionsFromItem(ModItems.COBALT_TORCH))
                        .offerTo(exporter);
            }
        };
    }

    @Override
    public String getName() {
        return "Cobalt rails recipes";
    }
}