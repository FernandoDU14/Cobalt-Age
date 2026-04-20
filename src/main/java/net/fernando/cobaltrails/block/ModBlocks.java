package net.fernando.cobaltrails.block;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fernando.cobaltrails.CobaltRails;
import net.minecraft.block.*;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.ConstantIntProvider;

import java.util.function.Function;

public class ModBlocks {

    public static final Block DEEPSLATE_COBALT_ORE = registerBlock("deepslate_cobalt_ore",
            settings -> new ExperienceDroppingBlock(ConstantIntProvider.create(0), settings
                    .mapColor(MapColor.GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresTool()
                    .strength(4.5F, 3.0F).sounds(BlockSoundGroup.DEEPSLATE)));

    public static final Block COBALT_ORE = registerBlock("cobalt_ore",
            settings -> new ExperienceDroppingBlock(ConstantIntProvider.create(0), settings
                    .mapColor(MapColor.GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresTool()
                    .strength(3.0F, 3.0F)));

    public static final Block RAW_COBALT_BLOCK = registerBlock("raw_cobalt_block",
            settings -> new Block(settings.mapColor(MapColor.DARK_AQUA)
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresTool().strength(4.0F, 6.0F)));

    public static final Block COBALT_BLOCK = registerBlock("cobalt_block",
            settings -> new Block(settings.mapColor(MapColor.LAPIS_BLUE)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresTool()
                    .strength(4.0F, 6.0F).sounds(BlockSoundGroup.METAL)));

    public static final Block COBALT_RAIL = registerBlock("cobalt_rail",
            settings -> new PoweredRailBlock(settings.noCollision().strength(0.7F).sounds(BlockSoundGroup.METAL)));

    // Method to register blocks which "don't have an item"
    private static Block registerBlockWithoutItem(String name, Block block) {
        return Registry.register(Registries.BLOCK, Identifier.of(CobaltRails.MOD_ID, name), block);
    }

    private static Block registerBlock(String name, Function<AbstractBlock.Settings, Block> function) {
        // 1 We create the ID and the Key
        Identifier id = Identifier.of(CobaltRails.MOD_ID, name);
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);

        // 2. We inject directly the Key
        AbstractBlock.Settings settings = AbstractBlock.Settings.create().registryKey(blockKey);

        // 3. We create the instance of the Block with his settings
        Block block = function.apply(settings);

        // 4. We register the Block
        Block registeredBlock = Registry.register(Registries.BLOCK, blockKey, block);

        // 5. We register the Item related to the Block
        registerBlockItem(name, registeredBlock);

        return registeredBlock;
    }


    private static void registerBlockItem(String name, Block block) {
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(CobaltRails.MOD_ID, name));

        Registry.register(Registries.ITEM, itemKey,
                new BlockItem(block, new Item.Settings().registryKey(itemKey)));
    }
    // Repeater e Comparator
    public static final Block COBALT_REPEATER = registerBlock("cobalt_repeater",
            settings -> new CobaltRepeaterBlock(AbstractBlock.Settings.copy(Blocks.REPEATER)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(CobaltRails.MOD_ID, "cobalt_repeater")))));

    public static final Block COBALT_COMPARATOR = registerBlock("cobalt_comparator",
            settings -> new CobaltComparatorBlock(AbstractBlock.Settings.copy(Blocks.COMPARATOR)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(CobaltRails.MOD_ID, "cobalt_comparator")))));


    public static final Block COBALT_DUST = registerBlockWithoutItem("cobalt_dust",
            new CobaltDustBlock(AbstractBlock.Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(CobaltRails.MOD_ID, "cobalt_dust")))
                    .noCollision().breakInstantly().nonOpaque().pistonBehavior(PistonBehavior.DESTROY).sounds(BlockSoundGroup.STONE)));


    // La torcia verticale può restare così (ma usa registerBlockWithoutItem se vuoi gestire l'item in ModItems)
    public static final Block COBALT_TORCH = registerBlockWithoutItem("cobalt_torch",
            new CobaltTorchBlock(AbstractBlock.Settings.copy(Blocks.REDSTONE_TORCH)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(CobaltRails.MOD_ID, "cobalt_torch")))));

    // La Wall Torch deve SEMPRE essere senza item
    public static final Block COBALT_WALL_TORCH = registerBlockWithoutItem("cobalt_wall_torch",
            new CobaltWallTorchBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(CobaltRails.MOD_ID, "cobalt_wall_torch")))
                    .noCollision()
                    .breakInstantly()
                    .luminance(state -> state.contains(Properties.LIT) && state.get(Properties.LIT) ? 7 : 0)
                    .sounds(BlockSoundGroup.WOOD)
                    .pistonBehavior(PistonBehavior.DESTROY)));

    public static void registerModBlocks() {
        CobaltRails.LOGGER.info("Registering mod blocks for " + CobaltRails.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> {
            entries.addBefore(Blocks.COPPER_ORE, COBALT_ORE);
            entries.addBefore(COBALT_ORE, DEEPSLATE_COBALT_ORE);
            entries.addBefore(Blocks.RAW_COPPER_BLOCK, RAW_COBALT_BLOCK);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.addAfter(Blocks.POWERED_RAIL, COBALT_RAIL);
            entries.addAfter(Items.REPEATER, COBALT_REPEATER);
            entries.addAfter(Items.COMPARATOR, COBALT_COMPARATOR);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.addBefore(Blocks.GOLD_BLOCK, COBALT_BLOCK);
        });

    }
}