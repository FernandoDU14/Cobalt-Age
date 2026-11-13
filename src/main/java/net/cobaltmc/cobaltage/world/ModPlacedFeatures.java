package net.cobaltmc.cobaltage.world;

import net.cobaltmc.cobaltage.CobaltAge;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> COBALT_ORE_PLACED_KEY = registerKey("cobalt_ore_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.FEATURE);

        register(context, COBALT_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.COBALT_ORE),
                commonOrePlacement(6,
                        HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(32))));
    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                                 Holder<Feature> configuration, List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }

    private static List<PlacementModifier> orePlacement(final PlacementModifier frequencyModifier, final PlacementModifier heightRange) {
        return List.of(frequencyModifier, InSquarePlacement.spread(), heightRange, BiomeFilter.biome());
    }

    private static List<PlacementModifier> commonOrePlacement(final int count, final PlacementModifier heightRange) {
        return orePlacement(CountPlacement.of(count), heightRange);
    }
}