package net.fernando.cobaltage.util;

import net.fernando.cobaltage.CobaltAge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        //public static final TagKey<Block> NEEDS_PINK_GARNET_TOOL = createTag("needs_pink_garnet_tool");
        //public static final TagKey<Block> INCORRECT_FOR_PINK_GARNET_TOOL = createTag("incorrect_for_pink_garnet_tool");

        public static final TagKey<Block> CAN_EMIT_IN_COBALT_SIGNAL_CHANNEL = createTag("can_emit_in_cobalt_signal_channel");
        public static final TagKey<Block> CANT_RECIVE_FROM_COBALT_SIGNAL_CHANNEL = createTag("cant_recive_from_cobalt_signal_channel");

        private static TagKey<Block> createTag(String name) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, name));
        }
    }

    public static class Items {
        // public static final TagKey<Item> TRANSFORMABLE_ITEMS = createTag("transformable_items");

        private static TagKey<Item> createTag(String name) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, name));
        }
    }
}