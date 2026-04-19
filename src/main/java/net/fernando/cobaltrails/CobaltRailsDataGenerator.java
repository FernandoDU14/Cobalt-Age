package net.fernando.cobaltrails;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fernando.cobaltrails.datagen.ModBlockTagProvider;
import net.fernando.cobaltrails.datagen.ModLootTableProvider;
import net.fernando.cobaltrails.datagen.ModModelProvider;
import net.fernando.cobaltrails.datagen.ModRecipeProvider;
import net.minecraft.data.client.ModelProvider;

public class CobaltRailsDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        pack.addProvider(ModBlockTagProvider::new);
        pack.addProvider(ModLootTableProvider::new);
        pack.addProvider(ModModelProvider::new);
        pack.addProvider(ModRecipeProvider::new);
	}
}
