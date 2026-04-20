package net.fernando.cobaltrails;

import net.fabricmc.api.ModInitializer;

import net.fernando.cobaltrails.block.ModBlocks;
import net.fernando.cobaltrails.block.entity.ModBlockEntities;
import net.fernando.cobaltrails.gamerules.CobaltRailsGameRules;
import net.fernando.cobaltrails.item.ModItems;
import net.fernando.cobaltrails.world.gen.ModWorldGeneration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobaltRails implements ModInitializer {
	public static final String MOD_ID = "cobaltrails";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Inizializzazione Cobalt Rails...");

		// 1. Blocks and Block Entities
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerBlockEntities();
		// 2. Items
		ModItems.registerModItems();

		// 3. All the rest
		CobaltRailsGameRules.registerGameRules();
		ModWorldGeneration.generateWorldGen();

		LOGGER.info("Cobalt Rails inizializzato con successo!");
	}
}