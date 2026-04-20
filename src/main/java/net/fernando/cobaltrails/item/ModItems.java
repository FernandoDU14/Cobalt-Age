package net.fernando.cobaltrails.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fernando.cobaltrails.CobaltRails;
import net.fernando.cobaltrails.block.ModBlocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import java.util.function.Function;

import static net.fernando.cobaltrails.block.ModBlocks.COBALT_DUST;

public class ModItems {

    public static final Item COBALT_INGOT = registerItem("cobalt_ingot", Item::new);
    public static final Item COBALT_NUGGET = registerItem("cobalt_nugget", Item::new);
    public static final Item RAW_COBALT = registerItem("raw_cobalt", Item::new);

    private static Item registerItem(String name, Function<Item.Settings, Item> function) {
        Identifier id = Identifier.of(CobaltRails.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        // Creiamo i settings con la chiave GIÀ INCLUSA
        Item.Settings settings = new Item.Settings().registryKey(key);

        // Creiamo l'item e lo registriamo con la stessa chiave
        return Registry.register(Registries.ITEM, key, function.apply(settings));
    }

    // Nella registrazione della polvere
    public static final Item COBALT_DUST = registerItem("cobalt_dust",
            settings -> new BlockItem(ModBlocks.COBALT_DUST, settings));

    public static void registerModItems(){
        CobaltRails.LOGGER.info("Regiestering mod items for " + CobaltRails.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.addBefore(Items.COPPER_INGOT, COBALT_INGOT);
            entries.addAfter(Items.IRON_NUGGET, COBALT_NUGGET);
            entries.addBefore(Items.RAW_COPPER, RAW_COBALT);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.addAfter(Items.REDSTONE, ModItems.COBALT_DUST);
        });
    }
}
