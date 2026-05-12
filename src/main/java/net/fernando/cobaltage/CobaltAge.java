package net.fernando.cobaltage;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.block.blockentities.ModBlockEntities;
import net.fernando.cobaltage.gamerules.CobaltRailsGameRules;
import net.fernando.cobaltage.item.ModItems;
import net.fernando.cobaltage.world.gen.ModWorldGeneration;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
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

        TradeOfferHelper.registerWanderingTraderOffers(factories -> {
            factories.addAll(Identifier.of(CobaltAge.MOD_ID, "emerald_for_dust_smithing_template"), (world, entity, random) -> new TradeOffer(
                    new TradedItem(Items.EMERALD, 10),
                    new ItemStack(ModItems.DUST_SMITHING_TEMPLATE, 1), 4, 7, 0.04f));
        });

		LOGGER.info("Cobalt Age initialized successfully!");
	}
}