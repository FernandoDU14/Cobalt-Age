package net.fernando.cobaltrails;

import net.fabricmc.api.ModInitializer;

import net.fernando.cobaltrails.block.ModBlocks;
import net.fernando.cobaltrails.item.ModItems;
import net.fernando.cobaltrails.world.gen.ModWorldGeneration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobaltRails implements ModInitializer {
	public static final String MOD_ID = "cobaltrails";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();

        ModWorldGeneration.generateWorldGen();
	}
}