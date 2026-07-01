package net.fernando.cobaltage.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fernando.cobaltage.CobaltAge;
import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.trim.ModTrimMaterials;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
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
            settings -> new StandingAndWallBlockItem(
                    ModBlocks.COBALT_TORCH,
                    ModBlocks.COBALT_WALL_TORCH,
                    Direction.DOWN,
                    settings) // Standard placement settings
    );

    public static final Item DUST_SMITHING_TEMPLATE = registerItem("dust_armor_trim_smithing_template",
            settings -> SmithingTemplateItem.createArmorTrimTemplate(settings.rarity(Rarity.UNCOMMON)));

    private static Item registerItem(String name, Function<Item.Properties, Item> function) {
        Identifier id = Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);

        Item.Properties settings = new Item.Properties().setId(key);

        return Registry.register(BuiltInRegistries.ITEM, key, function.apply(settings));
    }

    public static void registerModItems(){
        CobaltAge.LOGGER.info("Regiestering mod items for " + CobaltAge.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.addAfter(Items.IRON_INGOT, COBALT_INGOT);
            entries.addAfter(Items.IRON_NUGGET, COBALT_NUGGET);
            entries.addAfter(Items.RAW_IRON, RAW_COBALT);
            entries.addAfter(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, DUST_SMITHING_TEMPLATE);
            entries.addAfter(Items.REDSTONE, COBALT_DUST);
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
            entries.addAfter(Items.REDSTONE, ModItems.COBALT_DUST);
            entries.addAfter(Items.REDSTONE_TORCH, ModItems.COBALT_TORCH);
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.addAfter(Items.REDSTONE_TORCH, ModItems.COBALT_TORCH);
            entries.addAfter(Items.REDSTONE_LAMP, ModBlocks.COBALT_LAMP);
        });
    }
}
