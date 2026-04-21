package net.fernando.cobaltrails.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fernando.cobaltrails.CobaltRails;
import net.fernando.cobaltrails.block.ModBlocks;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.function.Function;

public class ModItems {

    public static final Item COBALT_INGOT = registerItem("cobalt_ingot", Item::new);
    public static final Item COBALT_NUGGET = registerItem("cobalt_nugget", Item::new);
    public static final Item RAW_COBALT = registerItem("raw_cobalt", Item::new);

    public static final Item COBALT_DUST = registerItem("cobalt_dust",
            settings -> new BlockItem(ModBlocks.COBALT_DUST, settings));

    public static final Item COBALT_TORCH = registerItem("cobalt_torch",
            settings -> new VerticallyAttachableBlockItem(
                    ModBlocks.COBALT_TORCH,
                    ModBlocks.COBALT_WALL_TORCH,
                    Direction.DOWN,
                    settings) // Standard placement settings
    );

    private static Item registerItem(String name, Function<Item.Settings, Item> function) {
        Identifier id = Identifier.of(CobaltRails.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        Item.Settings settings = new Item.Settings().registryKey(key);

        return Registry.register(Registries.ITEM, key, function.apply(settings));
    }

    public static void registerModItems(){
        CobaltRails.LOGGER.info("Regiestering mod items for " + CobaltRails.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.addBefore(Items.COPPER_INGOT, COBALT_INGOT);
            entries.addAfter(Items.IRON_NUGGET, COBALT_NUGGET);
            entries.addBefore(Items.RAW_COPPER, RAW_COBALT);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.addAfter(Items.REDSTONE, ModItems.COBALT_DUST);
            entries.addAfter(Items.REDSTONE_TORCH, ModItems.COBALT_TORCH);
        });
    }
}
