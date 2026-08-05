package net.cobaltmc.cobaltage.datagen;

import net.cobaltmc.cobaltage.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
    public ModItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.@NonNull Provider lookup) {
        valueLookupBuilder(ItemTags.BEACON_PAYMENT_ITEMS)
                .add(ModItems.COBALT_INGOT);

        valueLookupBuilder(ItemTags.TRIM_MATERIALS)
                .add(ModItems.COBALT_INGOT)
                .add(ModItems.COBALT_DUST);
    }
}