package net.fernando.cobaltage;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.block.entity.ModBlockEntities;
import net.fernando.cobaltage.gamerules.CobaltRailsGameRules;
import net.fernando.cobaltage.item.ModItems;
import net.fernando.cobaltage.world.gen.ModWorldGeneration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobaltAge implements ModInitializer {
	public static final String MOD_ID = "cobaltage";
	public static final String MOD_NAME = "Cobalt Age";
	public static final String MOD_VERSION = "1.2.1";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final boolean DEBUG = false;
	public static boolean ModernSignalEngine = false;

	@Override
	public void onInitialize() {
		if (DEBUG) {
			LOGGER.warn("You are running a DEBUG version of {}!", MOD_NAME);
		}

		LOGGER.info("Initialization of {} version {}...", MOD_NAME, MOD_VERSION);

		// 1. Blocks and Block Entities
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerBlockEntities();
		// 2. Items
		ModItems.registerModItems();

		// 3. All the rest
		CobaltRailsGameRules.registerGameRules();
		ModWorldGeneration.generateWorldGen();

        TradeOfferHelper.registerWanderingTraderOffers(factories -> {
            factories.addAll(Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "emerald_for_dust_smithing_template"), (world, entity, random) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 10),
                    new ItemStack(ModItems.DUST_SMITHING_TEMPLATE, 1), 4, 7, 0.04f));
        });

		LOGGER.info("{} version {} initialized successfully!", MOD_NAME, MOD_VERSION);
	}
}