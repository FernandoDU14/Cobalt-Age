package net.fernando.cobaltage;

import net.fabricmc.api.ModInitializer;

import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.block.blockentities.ModBlockEntities;
import net.fernando.cobaltage.gamerules.CobaltRailsGameRules;
import net.fernando.cobaltage.item.ModItems;
import net.fernando.cobaltage.world.gen.ModWorldGeneration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobaltAge implements ModInitializer {
	public static final String MOD_ID = "cobaltage";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initialization of Cobalt Age...");

		// 1. Blocks and Block Entities
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerBlockEntities();
		// 2. Items
		ModItems.registerModItems();

		// 3. All the rest
		CobaltRailsGameRules.registerGameRules();
		ModWorldGeneration.generateWorldGen();

		LOGGER.info("Cobalt Age initialized successfully!");
	}
}