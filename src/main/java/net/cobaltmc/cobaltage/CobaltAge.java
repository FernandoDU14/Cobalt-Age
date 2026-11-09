package net.cobaltmc.cobaltage;

import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.block.entity.ModBlockEntities;
import net.cobaltmc.cobaltage.gamerules.CobaltAgeGameRules;
import net.cobaltmc.cobaltage.item.ModItems;
import net.cobaltmc.cobaltage.world.gen.ModWorldGeneration;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobaltAge implements ModInitializer {
	public static final String MOD_ID = "cobaltage";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static boolean MODERN_SIGNAL_ENGINE = true;

	@Override
	public void onInitialize() {
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();
        ModItems.registerModItems();
        CobaltAgeGameRules.registerGameRules();
        ModWorldGeneration.generateWorldGen();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
