package net.fernando.cobaltage.trim;

import net.fernando.cobaltage.CobaltAge;
import net.fernando.cobaltage.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.equipment.trim.ArmorTrimAssets;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class ModTrimMaterials {

    public static final RegistryKey<ArmorTrimMaterial> COBALT_INGOT = RegistryKey.of(RegistryKeys.TRIM_MATERIAL,
            Identifier.of(CobaltAge.MOD_ID, "cobalt_ingot"));

    public static final RegistryKey<ArmorTrimMaterial> COBALT_DUST = RegistryKey.of(RegistryKeys.TRIM_MATERIAL,
            Identifier.of(CobaltAge.MOD_ID, "cobalt_dust"));

    public static void bootstrap(Registerable<ArmorTrimMaterial> registerable) {
        register(registerable, COBALT_INGOT, Registries.ITEM.getEntry(ModItems.COBALT_INGOT),
                Style.EMPTY.withColor(TextColor.parse("#33a3e6").getOrThrow()), "cobalt_ingot");

        register(registerable, COBALT_DUST, Registries.ITEM.getEntry(ModItems.COBALT_DUST),
                Style.EMPTY.withColor(TextColor.parse("#33c2e6").getOrThrow()), "cobalt_dust");

    }

    private static void register(Registerable<ArmorTrimMaterial> registerable, RegistryKey<ArmorTrimMaterial> armorTrimKey,
                                 RegistryEntry<Item> item, Style style, String assetName) {
        ArmorTrimMaterial trimMaterial = new ArmorTrimMaterial(
                ArmorTrimAssets.of(assetName),
                Text.translatable(Util.createTranslationKey("trim_material", armorTrimKey.getValue())).fillStyle(style));

        registerable.register(armorTrimKey, trimMaterial);
    }
}