package net.fernando.cobaltrails.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fernando.cobaltrails.CobaltRails;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item COBALT_INGOT = registerItem("cobalt_ingot", new Item(new Item.Settings()));
    public static final Item COBALT_NUGGET = registerItem("cobalt_nugget", new Item(new Item.Settings()));
    public static final Item RAW_COBALT = registerItem("raw_cobalt", new Item(new Item.Settings()));


    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(CobaltRails.MOD_ID, name), item);
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
