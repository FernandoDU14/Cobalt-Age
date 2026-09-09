package net.cobaltmc.cobaltage.item;

import java.util.function.Function;
import net.cobaltmc.cobaltage.CobaltAgeConstants;
import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.trim.ModTrimMaterials;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ItemLike;

public class ModItems {
    public static final Item COBALT_INGOT = registerItem("cobalt_ingot", (settings) -> new Item(settings.trimMaterial(ModTrimMaterials.COBALT_INGOT)));
    public static final Item COBALT_NUGGET = registerItem("cobalt_nugget", Item::new);
    public static final Item RAW_COBALT = registerItem("raw_cobalt", Item::new);
    public static final Item COBALT_DUST = registerItem("cobalt_dust", (settings) -> new BlockItem(ModBlocks.COBALT_DUST, settings.trimMaterial(ModTrimMaterials.COBALT_DUST)));
    public static final Item COBALT_TORCH = registerItem("cobalt_torch", (settings) -> new StandingAndWallBlockItem(ModBlocks.COBALT_TORCH, ModBlocks.COBALT_WALL_TORCH, Direction.DOWN, settings));
    public static final Item DUST_SMITHING_TEMPLATE = registerItem("dust_armor_trim_smithing_template", (settings) -> SmithingTemplateItem.createArmorTrimTemplate(settings.rarity(net.minecraft.world.item.Rarity.RARE)));

    private static Item registerItem(String name, Function<Item.Properties, Item> function) {
        return Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath("cobaltage", name),
                function.apply((new Item.Properties()).setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("cobaltage", name)))));
    }

    public static void registerModItems() {
        CobaltAgeConstants.LOGGER.info("Regiestering mod items for cobaltage");

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register((entries) -> {
            entries.addAfter(Items.IRON_INGOT, COBALT_INGOT);
            entries.addAfter(Items.IRON_NUGGET, COBALT_NUGGET);
            entries.addAfter(Items.RAW_IRON, RAW_COBALT);
            entries.addAfter(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, DUST_SMITHING_TEMPLATE);
            entries.addAfter(Items.REDSTONE, COBALT_DUST);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register((entries) -> {
            entries.addAfter(Items.REDSTONE, COBALT_DUST);
            entries.addAfter(Items.REDSTONE_TORCH, COBALT_TORCH);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register((entries) -> entries.addAfter(Items.REDSTONE_TORCH,COBALT_TORCH));
    }
}