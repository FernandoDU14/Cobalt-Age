package net.fernando.cobaltage.trim;

import net.fernando.cobaltage.CobaltAge;
import net.fernando.cobaltage.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public class ModTrimPatterns {
    public static final ResourceKey<TrimPattern> DUST = ResourceKey.create(Registries.TRIM_PATTERN,
            Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "dust"));

    public static void bootstrap(BootstrapContext<TrimPattern> context) {
        register(context, ModItems.DUST_SMITHING_TEMPLATE, DUST);
    }

    private static void register(BootstrapContext<TrimPattern> context, Item item, ResourceKey<TrimPattern> key) {
        TrimPattern trimPattern = new TrimPattern(key.identifier(),
                Component.translatable(Util.makeDescriptionId("trim_pattern", key.identifier())), false);

        context.register(key, trimPattern);
    }
}