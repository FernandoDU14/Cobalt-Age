package net.fernando.cobaltage.block;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fernando.cobaltage.CobaltAge;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import java.util.function.Function;

public class ModBlocks {

    // Blocks
    public static final Block DEEPSLATE_COBALT_ORE = registerBlock("deepslate_cobalt_ore",
            settings -> new DropExperienceBlock(ConstantInt.of(0), settings
                    .mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                    .strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)));

    public static final Block COBALT_ORE = registerBlock("cobalt_ore",
            settings -> new DropExperienceBlock(ConstantInt.of(0), settings
                    .mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F)));

    public static final Block RAW_COBALT_BLOCK = registerBlock("raw_cobalt_block",
            settings -> new Block(settings.mapColor(MapColor.WARPED_STEM)
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(4.0F, 6.0F)));

    public static final Block COBALT_BLOCK = registerBlock("cobalt_block",
            settings -> new Block(settings.mapColor(MapColor.LAPIS)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops()
                    .strength(4.0F, 6.0F).sound(SoundType.METAL)));

    // Redstone & Functional Blocks
    public static final Block COBALT_RAIL = registerBlock("cobalt_rail",
            settings -> new CobaltRailBlock(BlockBehaviour.Properties.of()
                    .noCollision()
                    .strength(0.7F)
                    .sound(SoundType.METAL)
                    .setId(ResourceKey.create(
                            Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_rail")))));

    public static final Block COBALT_REPEATER = registerBlock("cobalt_repeater",
            settings -> new CobaltRepeaterBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.DESTROY)
                    .setId(ResourceKey.create(
                            Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_repeater")))));

    public static final Block COBALT_COMPARATOR = registerBlock("cobalt_comparator",
            settings -> new CobaltComparatorBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.DESTROY)
                    .setId(ResourceKey.create(
                            Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_comparator")))));

    public static final Block CONVERTER = registerBlock("converter",
            settings -> new CobaltConverterBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.DESTROY)
                    .setId(ResourceKey.create(
                            Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "converter")))));

    public static final Block COBALT_DUST = registerBlockWithoutItem("cobalt_dust",
            new CobaltWireBlock(BlockBehaviour.Properties.of()
                    .noCollision()
                    .instabreak()
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)
                    .sound(SoundType.STONE)
                    .setId(ResourceKey.create(
                            Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_dust")
                    ))
            ));

    public static final Block COBALT_RELAY = registerBlock("cobalt_relay",
            settings -> new CobaltRelayBlock(BlockBehaviour.Properties.of()
                    .instrument(NoteBlockInstrument.HAT)
                    .strength(0.3F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isValidSpawn(Blocks::never)
                    .isRedstoneConductor(Blocks::never)
                    .isSuffocating(Blocks::never)
                    .isViewBlocking(Blocks::never)
                    .mapColor(MapColor.WARPED_STEM)
                    .setId(ResourceKey.create(
                            Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_relay")))));

    public static final Block COBALT_DUST_BLOCK = registerBlock("cobalt_dust_block",
            settings -> new CobaltDustBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WARPED_STEM)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .setId(ResourceKey.create(
                            Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_dust_block")))));

    public static final Block COBALT_LAMP = registerBlock(
            "cobalt_lamp",
            settings -> new CobaltLampBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WARPED_STEM)
                    .lightLevel(state -> state.hasProperty(BlockStateProperties.LIT)
                            && state.hasProperty(BlockStateProperties.POWER)
                            && state.getValue(BlockStateProperties.LIT) ? state.getValue(BlockStateProperties.POWER) : 0)
                    .strength(0.3F)
                    .sound(SoundType.COPPER_BULB)
                    .isValidSpawn(Blocks::always)
                    .setId(ResourceKey.create(
                            Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_lamp")))));

    public static final Block COBALT_TORCH = registerBlockWithoutItem("cobalt_torch",
            new CobaltTorchBlock(BlockBehaviour.Properties.of()
                    .noCollision()
                    .instabreak()
                    .lightLevel(state -> state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) ? 7 : 0)
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.DESTROY)
                    .setId(ResourceKey.create(
                            Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_torch")))));

    public static final Block COBALT_WALL_TORCH = registerBlockWithoutItem("cobalt_wall_torch",
            new CobaltWallTorchBlock(BlockBehaviour.Properties.of()
                    .noCollision()
                    .instabreak()
                    .lightLevel(state -> state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) ? 7 : 0)
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.DESTROY)
                    .setId(ResourceKey.create(
                            Registries.BLOCK,
                            Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_wall_torch")))));

    public static void registerModBlocks() {
        CobaltAge.LOGGER.info("Registering mod blocks for " + CobaltAge.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
            entries.addBefore(Blocks.COPPER_ORE, COBALT_ORE);
            entries.addBefore(COBALT_ORE, DEEPSLATE_COBALT_ORE);
            entries.addBefore(Blocks.RAW_COPPER_BLOCK, RAW_COBALT_BLOCK);
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
            entries.addAfter(Blocks.POWERED_RAIL, COBALT_RAIL);
            entries.addAfter(Items.REPEATER, COBALT_REPEATER);
            entries.addAfter(COBALT_REPEATER, CONVERTER);
            entries.addAfter(Items.COMPARATOR, COBALT_COMPARATOR);
            entries.addAfter(Blocks.REDSTONE_BLOCK, COBALT_DUST_BLOCK);
            entries.addAfter(ModBlocks.COBALT_DUST_BLOCK, COBALT_RELAY);
            entries.addAfter(Blocks.REDSTONE_LAMP, COBALT_LAMP);
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.addBefore(Blocks.GOLD_BLOCK, COBALT_BLOCK);
            entries.addAfter(Blocks.REDSTONE_BLOCK, COBALT_DUST_BLOCK);
        });

        CobaltAge.LOGGER.info("Successfully registered mod blocks for " + CobaltAge.MOD_ID);
    }

    // Method to register blocks which "don't have an item"
    private static Block registerBlockWithoutItem(String name, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, name), block);
    }

    private static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> function) {
        Identifier id = Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties settings = BlockBehaviour.Properties.of().setId(blockKey);
        Block block = function.apply(settings);
        Block registeredBlock = Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        registerBlockItem(name, registeredBlock);
        return registeredBlock;
    }


    private static void registerBlockItem(String name, Block block) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, name));

        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey)));
    }
}