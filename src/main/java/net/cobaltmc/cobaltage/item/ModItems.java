package net.cobaltmc.cobaltage.item;

import net.cobaltmc.cobaltage.CobaltAge;
import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.trim.ModTrimMaterials;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;

import java.util.function.Function;

public class ModItems {
    public static final Item COBALT_INGOT = registerItem("cobalt_ingot", (settings) -> new Item(settings.trimMaterial(ModTrimMaterials.COBALT_INGOT)));
    public static final Item COBALT_NUGGET = registerItem("cobalt_nugget", Item::new);
    public static final Item RAW_COBALT = registerItem("raw_cobalt", Item::new);
    public static final Item COBALT_DUST = registerItem("cobalt_dust", (settings) -> new BlockItem(ModBlocks.COBALT_DUST, settings.trimMaterial(ModTrimMaterials.COBALT_DUST)));
    public static final Item COBALT_TORCH = registerItem("cobalt_torch", (settings) -> new StandingAndWallBlockItem(ModBlocks.COBALT_TORCH, ModBlocks.COBALT_WALL_TORCH, Direction.DOWN, settings));
    public static final Item DUST_SMITHING_TEMPLATE = registerItem("dust_armor_trim_smithing_template", (settings) -> SmithingTemplateItem.createArmorTrimTemplate(settings.rarity(Rarity.RARE)));

    private static Item registerItem(String name, Function<Item.Properties, Item> function) {
        return Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath("cobaltage", name),
                function.apply((new Item.Properties()).setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("cobaltage", name)))));
    }

    public static void registerModItems() {
        CobaltAge.LOGGER.info("Regiestering mod items for cobaltage");

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register((entries) -> {
            entries.insertAfter(Items.IRON_INGOT, COBALT_INGOT);
            entries.insertAfter(Items.IRON_NUGGET, COBALT_NUGGET);
            entries.insertAfter(Items.RAW_IRON, RAW_COBALT);
            entries.insertAfter(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, DUST_SMITHING_TEMPLATE);
            entries.insertAfter(Items.REDSTONE, COBALT_DUST);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register((entries) -> {
            entries.insertAfter(Items.REDSTONE, COBALT_DUST);
            entries.insertAfter(Items.REDSTONE_TORCH, COBALT_TORCH);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register((entries) -> entries.insertAfter(Items.REDSTONE_TORCH,COBALT_TORCH));
    }
}