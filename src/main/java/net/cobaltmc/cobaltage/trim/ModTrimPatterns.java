package net.cobaltmc.cobaltage.trim;

import net.cobaltmc.cobaltage.CobaltAge;
import net.cobaltmc.cobaltage.CobaltAgeConstants;
import net.cobaltmc.cobaltage.item.ModItems;
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
            Identifier.fromNamespaceAndPath(CobaltAgeConstants.MOD_ID, "dust"));

    public static void bootstrap(BootstrapContext<TrimPattern> context) {
        register(context, ModItems.DUST_SMITHING_TEMPLATE, DUST);
    }

    private static void register(BootstrapContext<TrimPattern> context, Item item, ResourceKey<TrimPattern> key) {
        TrimPattern trimPattern = new TrimPattern(key.identifier(),
                Component.translatable(Util.makeDescriptionId("trim_pattern", key.identifier())), false);

        context.register(key, trimPattern);
    }
}