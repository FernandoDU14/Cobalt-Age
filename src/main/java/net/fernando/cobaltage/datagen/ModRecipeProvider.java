package net.fernando.cobaltage.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fernando.cobaltage.CobaltAge;
import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.item.ModItems;
import net.fernando.cobaltage.trim.ModTrimPatterns;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {

    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }



    @Override
    protected @NonNull RecipeProvider createRecipeProvider(HolderLookup.@NonNull Provider registries, @NonNull RecipeOutput exporter) {
        return new RecipeProvider(registries, exporter) {
            @Override
            public void buildRecipes() {
                List<ItemLike> COBALT_ORSE_SMELTABLES = List.of(ModItems.RAW_COBALT,
                        ModBlocks.COBALT_ORE, ModBlocks.DEEPSLATE_COBALT_ORE);

                oreSmelting(COBALT_ORSE_SMELTABLES, RecipeCategory.MISC, ModItems.COBALT_INGOT,
                        1.0f, 200, "cobalt_ingot.json");

                oreBlasting(COBALT_ORSE_SMELTABLES, RecipeCategory.MISC, ModItems.COBALT_INGOT,
                        1.0f, 100, "cobalt_ingot.json");
                oreBlasting(List.of(ModBlocks.RAW_COBALT_BLOCK), RecipeCategory.MISC, ModBlocks.COBALT_BLOCK,
                        1.0f, 100, "cobalt_block");

                nineBlockStorageRecipes(RecipeCategory.MISC, ModItems.COBALT_INGOT,
                        RecipeCategory.BUILDING_BLOCKS, ModBlocks.COBALT_BLOCK);

                nineBlockStorageRecipes(RecipeCategory.MISC, ModItems.RAW_COBALT,
                        RecipeCategory.BUILDING_BLOCKS, ModBlocks.RAW_COBALT_BLOCK);

                nineBlockStorageRecipes(RecipeCategory.REDSTONE, ModItems.COBALT_DUST,
                        RecipeCategory.REDSTONE, ModBlocks.COBALT_DUST_BLOCK);

                shaped(RecipeCategory.MISC, ModBlocks.COBALT_RAIL, 6)
                        .pattern("C C")
                        .pattern("CBC")
                        .pattern("CDC")
                        .define('C', ModItems.COBALT_INGOT)
                        .define('B', Items.BREEZE_ROD)
                        .define('D', ModItems.COBALT_DUST)
                        .unlockedBy(getHasName(ModItems.COBALT_INGOT), has(ModItems.COBALT_INGOT))
                        .save(output);

                shaped(RecipeCategory.MISC, ModItems.COBALT_INGOT)
                        .pattern("CCC")
                        .pattern("CCC")
                        .pattern("CCC")
                        .define('C', ModItems.COBALT_NUGGET)
                        .unlockedBy(getHasName(ModItems.COBALT_INGOT), has(ModItems.COBALT_INGOT))
                        .save(output, ResourceKey.create(Registries.RECIPE,Identifier.parse("cobalt_ingot_from_cobalt_nugget")));

                shaped(RecipeCategory.REDSTONE, ModBlocks.COBALT_LAMP, 4)
                        .pattern(" Q ")
                        .pattern("BGB")
                        .pattern(" C ")
                        .define('G', Blocks.GLOWSTONE)
                        .define('C', ModBlocks.COBALT_DUST)
                        .define('Q', Items.QUARTZ)
                        .define('B', ModBlocks.COBALT_BLOCK)
                        .unlockedBy(getHasName(ModBlocks.COBALT_DUST), has(ModBlocks.COBALT_DUST))
                        .save(output);

                shapeless(RecipeCategory.REDSTONE, ModBlocks.COBALT_DUST, 1)
                        .requires(ModItems.COBALT_NUGGET)
                        .requires(Items.REDSTONE)
                        .unlockedBy(getHasName(ModItems.COBALT_INGOT), has(ModItems.COBALT_INGOT))
                        .save(output,
                                ResourceKey.create(Registries.RECIPE,Identifier.parse("cobalt_dust_from_redstone_and_cobalt_nugget")));

                shapeless(RecipeCategory.MISC, ModItems.COBALT_NUGGET, 9)
                        .requires(ModItems.COBALT_INGOT)
                        .unlockedBy(getHasName(ModItems.COBALT_INGOT), has(ModItems.COBALT_INGOT))
                        .save(output);


                shaped(RecipeCategory.REDSTONE, ModBlocks.CONVERTER)
                        .pattern("CQT")
                        .pattern("SSS")
                        .define('C', ModItems.COBALT_TORCH)
                        .define('Q', Items.QUARTZ)
                        .define('T', Items.REDSTONE_TORCH)
                        .define('S', Items.STONE)
                        .unlockedBy(getHasName(ModItems.COBALT_TORCH), has(ModItems.COBALT_TORCH))
                        .save(output);

                shaped(RecipeCategory.REDSTONE, ModItems.COBALT_TORCH)
                        .pattern("C")
                        .pattern("S")
                        .define('C', ModBlocks.COBALT_DUST)
                        .define('S', Items.STICK)
                        .unlockedBy(getHasName(ModItems.COBALT_DUST), has(ModItems.COBALT_DUST))
                        .save(output);

                shaped(RecipeCategory.REDSTONE, ModBlocks.COBALT_COMPARATOR)
                        .pattern(" C ")
                        .pattern("CQC")
                        .pattern("SSS")
                        .define('C', ModItems.COBALT_TORCH)
                        .define('S', Items.STONE)
                        .define('Q', Items.QUARTZ)
                        .unlockedBy(getHasName(ModItems.COBALT_TORCH), has(ModItems.COBALT_TORCH))
                        .save(output);

                shaped(RecipeCategory.REDSTONE, ModBlocks.COBALT_REPEATER)
                        .pattern("CDC")
                        .pattern("SSS")
                        .define('C', ModItems.COBALT_TORCH)
                        .define('S', Items.STONE)
                        .define('D', ModItems.COBALT_DUST)
                        .unlockedBy(getHasName(ModItems.COBALT_TORCH), has(ModItems.COBALT_TORCH))
                        .save(output);

                shaped(RecipeCategory.REDSTONE, ModBlocks.COBALT_RELAY)
                        .pattern("GCG")
                        .pattern("GDG")
                        .pattern("GCG")
                        .define('C', ModItems.COBALT_INGOT)
                        .define('G', Items.GLASS_PANE)
                        .define('D', ModItems.COBALT_DUST)
                        .unlockedBy(getHasName(ModItems.COBALT_INGOT), has(ModItems.COBALT_INGOT))
                        .save(output);

                trimSmithing(ModItems.DUST_SMITHING_TEMPLATE, ModTrimPatterns.DUST,
                        ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "dust_trim")));

                shaped(RecipeCategory.MISC, ModItems.DUST_SMITHING_TEMPLATE, 2)
                        .pattern("DTD")
                        .pattern("DCD")
                        .pattern("DDD")
                        .define('C', ModBlocks.COBALT_DUST_BLOCK)
                        .define('T', ModItems.DUST_SMITHING_TEMPLATE)
                        .define('D', Items.DIAMOND)
                        .unlockedBy(getHasName(ModItems.DUST_SMITHING_TEMPLATE), has(ModItems.DUST_SMITHING_TEMPLATE))
                        .save(output);

                }
        };
    }

    @Override
    public @NonNull String getName() {
        return "Cobalt Age recipes";
    }
}