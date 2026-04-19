package net.fernando.cobaltrails.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fernando.cobaltrails.CobaltRails;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {

    public static final Item COBALT_INGOT = registerItem("cobalt_ingot", Item::new);
    public static final Item COBALT_NUGGET = registerItem("cobalt_nugget", Item::new);
    public static final Item RAW_COBALT = registerItem("raw_cobalt", Item::new);


    private static Item registerItem(String name, Function<Item.Settings, Item> function) {
        return Registry.register(Registries.ITEM, Identifier.of(CobaltRails.MOD_ID, name),
                function.apply(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(CobaltRails.MOD_ID, name)))));
    }

    public static void registerModItems(){
        CobaltRails.LOGGER.info("Regiestering mod items for " + CobaltRails.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.addBefore(Items.COPPER_INGOT, COBALT_INGOT);
            entries.addAfter(Items.IRON_NUGGET, COBALT_NUGGET);
            entries.addBefore(Items.RAW_COPPER, RAW_COBALT);
        });
    }
}
