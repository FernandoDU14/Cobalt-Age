package net.cobaltmc.cobaltage.datagen;

import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.references.BlockIds;
import net.minecraft.references.BlockItemIds;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {

    public ModBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.@NonNull Provider lookup) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.getRK(ModBlocks.COBALT_BLOCK))
                .add(ModBlocks.getRK(ModBlocks.RAW_COBALT_BLOCK))
                .add(ModBlocks.getRK(ModBlocks.COBALT_ORE))
                .add(ModBlocks.getRK(ModBlocks.DEEPSLATE_COBALT_ORE))
                .add(ModBlocks.getRK(ModBlocks.COBALT_DUST_BLOCK));

        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.getRK(ModBlocks.COBALT_BLOCK))
                .add(ModBlocks.getRK(ModBlocks.RAW_COBALT_BLOCK))
                .add(ModBlocks.getRK(ModBlocks.COBALT_ORE))
                .add(ModBlocks.getRK(ModBlocks.DEEPSLATE_COBALT_ORE));

        tag(BlockTags.RAILS)
                .add(ModBlocks.getRK(ModBlocks.COBALT_RAIL));

        tag(BlockTags.BEACON_BASE_BLOCKS)
                .add(ModBlocks.getRK(ModBlocks.COBALT_BLOCK));

        // Blocks that emit redstone signal but should emit also a cobalt signal
        tag(ModTags.Blocks.SHOULD_REDSTONE_SOURCE_EMIT_COBALT)
                .addOptionalTag(BlockTags.BUTTONS)
                .addOptionalTag(BlockTags.PRESSURE_PLATES)
                .addOptionalTag(BlockTags.LIGHTNING_RODS)
                .add(BlockItemIds.LEVER.block())
                .add(BlockItemIds.TARGET.block())
                .add(BlockItemIds.SCULK_SENSOR.block())
                .add(BlockItemIds.TRIPWIRE_HOOK.block())
                .add(BlockItemIds.DAYLIGHT_DETECTOR.block())
                .add(BlockItemIds.JUKEBOX.block())
                .add(BlockItemIds.TRAPPED_CHEST.block())
                .add(BlockItemIds.LECTERN.block());

        // Blocks that should ignore cobalt signals
        tag(ModTags.Blocks.SHOULD_IGNORE_COBALT_SIGNALS)
                .add(BlockItemIds.REDSTONE_DUST.block())
                .add(BlockItemIds.REDSTONE_TORCH.block())
                .add(BlockItemIds.ACTIVATOR_RAIL.block())
                .add(BlockItemIds.POWERED_RAIL.block())
                .add(BlockIds.REDSTONE_WALL_TORCH)
                .add(BlockItemIds.REPEATER.block())
                .add(BlockItemIds.COMPARATOR.block());
    }
}