package net.cobaltmc.cobaltage.util;

import net.cobaltmc.cobaltage.CobaltAge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> SHOULD_REDSTONE_SOURCE_EMIT_COBALT = createTag("should_redstone_source_emit_cobalt");
        public static final TagKey<Block> SHOULD_IGNORE_COBALT_SIGNALS = createTag("should_ignore_cobalt_signals");

        private static TagKey<Block> createTag(String name) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, name));
        }
    }

    public static class Items {
        private static TagKey<Item> createTag(String name) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, name));
        }
    }
}