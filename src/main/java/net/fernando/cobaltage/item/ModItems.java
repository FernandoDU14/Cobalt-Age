package net.fernando.cobaltage.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fernando.cobaltage.CobaltAge;
import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.trim.ModTrimMaterials;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.Direction;

import java.util.function.Function;

public class ModItems {

    public static final Item COBALT_INGOT = registerItem("cobalt_ingot",
            settings -> new Item(settings.trimMaterial(ModTrimMaterials.COBALT_INGOT))
    );
    public static final Item COBALT_NUGGET = registerItem("cobalt_nugget", Item::new);
    public static final Item RAW_COBALT = registerItem("raw_cobalt", Item::new);

    public static final Item COBALT_DUST = registerItem("cobalt_dust",
            settings -> new BlockItem(ModBlocks.COBALT_DUST, settings.trimMaterial(ModTrimMaterials.COBALT_DUST)));

    public static final Item COBALT_TORCH = registerItem("cobalt_torch",
            settings -> new VerticallyAttachableBlockItem(
                    ModBlocks.COBALT_TORCH,
                    ModBlocks.COBALT_WALL_TORCH,
                    Direction.DOWN,
                    settings) // Standard placement settings
    );

    public static final Item DUST_SMITHING_TEMPLATE = registerItem("dust_armor_trim_smithing_template",
            settings -> SmithingTemplateItem.of(settings.rarity(Rarity.UNCOMMON)));

    private static Item registerItem(String name, Function<Item.Settings, Item> function) {
        Identifier id = Identifier.of(CobaltAge.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        Item.Settings settings = new Item.Settings().registryKey(key);

        return Registry.register(Registries.ITEM, key, function.apply(settings));
    }

    public static void registerModItems(){
        CobaltAge.LOGGER.info("Regiestering mod items for " + CobaltAge.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.addAfter(Items.IRON_INGOT, COBALT_INGOT);
            entries.addAfter(Items.IRON_NUGGET, COBALT_NUGGET);
            entries.addAfter(Items.RAW_IRON, RAW_COBALT);
            entries.addAfter(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, DUST_SMITHING_TEMPLATE);
            entries.addAfter(Items.REDSTONE, COBALT_DUST);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.addAfter(Items.REDSTONE, ModItems.COBALT_DUST);
            entries.addAfter(Items.REDSTONE_TORCH, ModItems.COBALT_TORCH);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.addAfter(Items.REDSTONE_TORCH, ModItems.COBALT_TORCH);
        });
    }
}
