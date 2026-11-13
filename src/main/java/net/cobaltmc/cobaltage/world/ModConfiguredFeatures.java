package net.cobaltmc.cobaltage.world;

import net.cobaltmc.cobaltage.CobaltAge;
import net.cobaltmc.cobaltage.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures {
    public static final ResourceKey<Feature> COBALT_ORE = registerKey("cobalt_ore");

    public static void bootstrap(BootstrapContext<Feature> context) {
        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

        context.register(COBALT_ORE, new OreFeature(
                List.of(
                        BlockReplacement.replace(stoneReplaceables, ModBlocks.COBALT_ORE.defaultBlockState()),
                        BlockReplacement.replace(deepslateReplaceables, ModBlocks.DEEPSLATE_COBALT_ORE.defaultBlockState())
                ),
                8
        ));
    }

    public static ResourceKey<Feature> registerKey(String name) {
        return ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, name));
    }
}