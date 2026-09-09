package net.cobaltmc.cobaltage.trim;

import net.cobaltmc.cobaltage.CobaltAge;
import net.cobaltmc.cobaltage.CobaltAgeConstants;
import net.cobaltmc.cobaltage.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.trim.MaterialAssetGroup;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

public class ModTrimMaterials {

    public static final ResourceKey<TrimMaterial> COBALT_INGOT = ResourceKey.create(Registries.TRIM_MATERIAL,
            Identifier.fromNamespaceAndPath(CobaltAgeConstants.MOD_ID, "cobalt_ingot"));

    public static final ResourceKey<TrimMaterial> COBALT_DUST = ResourceKey.create(Registries.TRIM_MATERIAL,
            Identifier.fromNamespaceAndPath(CobaltAgeConstants.MOD_ID, "cobalt_dust"));

    public static void bootstrap(BootstrapContext<TrimMaterial> registerable) {
        register(registerable, COBALT_INGOT, BuiltInRegistries.ITEM.wrapAsHolder(ModItems.COBALT_INGOT),
                Style.EMPTY.withColor(TextColor.parseColor("#33a3e6").getOrThrow()), "cobalt_ingot");

        register(registerable, COBALT_DUST, BuiltInRegistries.ITEM.wrapAsHolder(ModItems.COBALT_DUST),
                Style.EMPTY.withColor(TextColor.parseColor("#33c2e6").getOrThrow()), "cobalt_dust");

    }

    private static void register(BootstrapContext<TrimMaterial> registerable, ResourceKey<TrimMaterial> armorTrimKey,
                                 Holder<Item> item, Style style, String assetName) {
        TrimMaterial trimMaterial = new TrimMaterial(
                MaterialAssetGroup.create(assetName),
                Component.translatable(Util.makeDescriptionId("trim_material", armorTrimKey.identifier())).withStyle(style));

        registerable.register(armorTrimKey, trimMaterial);
    }
}