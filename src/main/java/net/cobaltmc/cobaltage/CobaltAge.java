package net.cobaltmc.cobaltage;

import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.block.entity.ModBlockEntities;
import net.cobaltmc.cobaltage.gamerules.CobaltAgeGameRules;
import net.cobaltmc.cobaltage.item.ModItems;
import net.cobaltmc.cobaltage.util.signal.profiler.Profiler;
import net.cobaltmc.cobaltage.world.gen.ModWorldGeneration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

public class CobaltAge implements ModInitializer {
    public static final String MOD_VERSION = getModVersion();

    private static String getModVersion() {
        return FabricLoader.getInstance().getModContainer(CobaltAgeConstants.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    public void onInitialize() {
        CobaltAgeConstants.LOGGER.info("Welcome to {} on Fabric! You are currently in a {} environment!",
                CobaltAgeConstants.MOD_NAME,
                FabricLoader.getInstance().getEnvironmentType().name()
        );

        CobaltAgeConstants.LOGGER.info("Initialization of {} version {}...", CobaltAgeConstants.MOD_NAME, MOD_VERSION);

        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();
        ModItems.registerModItems();
        CobaltAgeGameRules.registerGameRules();
        ModWorldGeneration.generateWorldGen();

        TradeOfferHelper.registerWanderingTraderOffers(factories ->
                factories.addAll(Identifier.fromNamespaceAndPath(CobaltAgeConstants.MOD_ID,
                        "emerald_for_dust_smithing_template"), (world, entity, random) -> new MerchantOffer(
                        new ItemCost(Items.EMERALD, 10),
                        new ItemStack(ModItems.DUST_SMITHING_TEMPLATE, 1), 4, 7, 0.04f)));

        CobaltAgeConstants.LOGGER.info("{} version {} initialized successfully!", CobaltAgeConstants.MOD_NAME, MOD_VERSION);
    }

    public static Profiler createProfiler() {
        return Profiler.DUMMY;
    }
}