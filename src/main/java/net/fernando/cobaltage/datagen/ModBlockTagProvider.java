package net.fernando.cobaltage.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.@NonNull Provider lookup) {
        valueLookupBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.COBALT_BLOCK)
                .add(ModBlocks.RAW_COBALT_BLOCK)
                .add(ModBlocks.COBALT_ORE)
                .add(ModBlocks.DEEPSLATE_COBALT_ORE)
                .add(ModBlocks.COBALT_DUST_BLOCK);

        valueLookupBuilder(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.COBALT_BLOCK)
                .add(ModBlocks.RAW_COBALT_BLOCK)
                .add(ModBlocks.COBALT_ORE)
                .add(ModBlocks.DEEPSLATE_COBALT_ORE);

        valueLookupBuilder(BlockTags.RAILS)
                .add(ModBlocks.COBALT_RAIL);

        valueLookupBuilder(BlockTags.BEACON_BASE_BLOCKS)
                .add(ModBlocks.COBALT_BLOCK);

        // Blocks that emit redstone signal but should emit also a cobalt signal
        valueLookupBuilder(ModTags.Blocks.SHOULD_REDSTONE_SOURCE_EMIT_COBALT)
                .addOptionalTag(BlockTags.BUTTONS)
                .addOptionalTag(BlockTags.PRESSURE_PLATES)
                .addOptionalTag(BlockTags.LIGHTNING_RODS)
                .add(Blocks.LEVER)
                .add(Blocks.TARGET)
                .add(Blocks.SCULK_SENSOR)
                .add(Blocks.TRIPWIRE_HOOK)
                .add(Blocks.DAYLIGHT_DETECTOR)
                .add(Blocks.JUKEBOX)
                .add(Blocks.TRAPPED_CHEST)
                .add(Blocks.LECTERN);

        // Blocks that should ignore cobalt signals
        valueLookupBuilder(ModTags.Blocks.SHOULD_IGNORE_COBALT_SIGNALS)
                .add(Blocks.REDSTONE_WIRE)
                .add(Blocks.REDSTONE_TORCH)
                .add(Blocks.ACTIVATOR_RAIL)
                .add(Blocks.POWERED_RAIL)
                .add(Blocks.REDSTONE_WALL_TORCH)
                .add(Blocks.REPEATER)
                .add(Blocks.COMPARATOR);
    }
}