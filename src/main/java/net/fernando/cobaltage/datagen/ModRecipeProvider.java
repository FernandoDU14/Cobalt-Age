package net.fernando.cobaltage.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fernando.cobaltage.CobaltAge;
import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.item.ModItems;
import net.fernando.cobaltage.trim.ModTrimPatterns;
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
                        1.0f, 200, "cobalt_ingot.json");

                offerBlasting(COBALT_ORSE_SMELTABLES, RecipeCategory.MISC, ModItems.COBALT_INGOT,
                        1.0f, 100, "cobalt_ingot.json");
                offerBlasting(List.of(ModBlocks.RAW_COBALT_BLOCK), RecipeCategory.MISC, ModBlocks.COBALT_BLOCK,
                        1.0f, 100, "cobalt_block");

                offerReversibleCompactingRecipes(RecipeCategory.MISC, ModItems.COBALT_INGOT,
                        RecipeCategory.BUILDING_BLOCKS, ModBlocks.COBALT_BLOCK);

                offerReversibleCompactingRecipes(RecipeCategory.MISC, ModItems.RAW_COBALT,
                        RecipeCategory.BUILDING_BLOCKS, ModBlocks.RAW_COBALT_BLOCK);

                offerReversibleCompactingRecipes(RecipeCategory.REDSTONE, ModItems.COBALT_DUST,
                        RecipeCategory.REDSTONE, ModBlocks.COBALT_DUST_BLOCK);

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

                createShapeless(RecipeCategory.REDSTONE, ModBlocks.COBALT_DUST, 1)
                        .input(ModItems.COBALT_NUGGET)
                        .input(Items.REDSTONE)
                        .criterion(hasItem(ModItems.COBALT_INGOT), conditionsFromItem(ModItems.COBALT_INGOT))
                        .offerTo(exporter,
                                RegistryKey.of(RegistryKeys.RECIPE,Identifier.of("cobalt_dust_from_redstone_and_cobalt_nugget")));

                createShapeless(RecipeCategory.MISC, ModItems.COBALT_NUGGET, 9)
                        .input(ModItems.COBALT_INGOT)
                        .criterion(hasItem(ModItems.COBALT_INGOT), conditionsFromItem(ModItems.COBALT_INGOT))
                        .offerTo(exporter);


                createShaped(RecipeCategory.REDSTONE, ModBlocks.CONVERTER)
                        .pattern("CQT")
                        .pattern("SSS")
                        .input('C', ModItems.COBALT_TORCH)
                        .input('Q', Items.QUARTZ)
                        .input('T', Items.REDSTONE_TORCH)
                        .input('S', Items.STONE)
                        .criterion(hasItem(ModItems.COBALT_TORCH), conditionsFromItem(ModItems.COBALT_TORCH))
                        .offerTo(exporter);

                createShaped(RecipeCategory.REDSTONE, ModItems.COBALT_TORCH)
                        .pattern("C")
                        .pattern("S")
                        .input('C', ModBlocks.COBALT_DUST)
                        .input('S', Items.STICK)
                        .criterion(hasItem(ModItems.COBALT_DUST), conditionsFromItem(ModItems.COBALT_DUST))
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

                createShaped(RecipeCategory.REDSTONE, ModBlocks.COBALT_RELAY)
                        .pattern("GCG")
                        .pattern("GDG")
                        .pattern("GCG")
                        .input('C', ModItems.COBALT_INGOT)
                        .input('G', Items.GLASS_PANE)
                        .input('D', ModItems.COBALT_DUST)
                        .criterion(hasItem(ModItems.COBALT_INGOT), conditionsFromItem(ModItems.COBALT_INGOT))
                        .offerTo(exporter);

                offerSmithingTrimRecipe(ModItems.DUST_SMITHING_TEMPLATE, ModTrimPatterns.DUST,
                        RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(CobaltAge.MOD_ID, "dust_trim")));

                createShaped(RecipeCategory.MISC, ModItems.DUST_SMITHING_TEMPLATE, 2)
                        .pattern("DTD")
                        .pattern("DCD")
                        .pattern("DDD")
                        .input('C', ModBlocks.COBALT_DUST_BLOCK)
                        .input('T', ModItems.DUST_SMITHING_TEMPLATE)
                        .input('D', Items.DIAMOND)
                        .criterion(hasItem(ModItems.DUST_SMITHING_TEMPLATE), conditionsFromItem(ModItems.DUST_SMITHING_TEMPLATE))
                        .offerTo(exporter);

                }
        };
    }

    @Override
    public String getName() {
        return "Cobalt Age recipes";
    }
}