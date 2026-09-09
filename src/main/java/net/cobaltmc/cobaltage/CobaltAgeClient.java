package net.cobaltmc.cobaltage;

import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.block.cobalt.CobaltWireBlock;
import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltColorUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class CobaltAgeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.putBlock(ModBlocks.COBALT_RAIL, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.COBALT_DUST, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, ModBlocks.COBALT_TORCH, ModBlocks.COBALT_WALL_TORCH, ModBlocks.COBALT_REPEATER, ModBlocks.COBALT_COMPARATOR, ModBlocks.COBALT_RELAY);

        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            int power = state.getValue(CobaltWireBlock.POWER);
            return CobaltColorUtil.getCobaltColor(power);
        }, ModBlocks.COBALT_DUST);

        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (tintIndex == 0) {
                int power = state.getValue(CobaltWireBlock.POWER);
                return CobaltColorUtil.getCobaltColor(power);
            } else {
                return -1;
            }
        }, ModBlocks.COBALT_RELAY);

        initialize3dCobaltRailsResourcePack();
        initializeCobaltWirePowerLevelResourcePack();
        initializeCobaltAgeDDMResourcePack();
    }

    private static void initialize3dCobaltRailsResourcePack() {
        Identifier id = Identifier.fromNamespaceAndPath(CobaltAgeConstants.MOD_ID, "cobaltrails3d");
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CobaltAgeConstants.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(id, modContainer, Component.literal("CobaltAge 3D Cobalt Rails"), PackActivationType.NORMAL);
    }

    private static void initializeCobaltWirePowerLevelResourcePack() {
        Identifier id = Identifier.fromNamespaceAndPath(CobaltAgeConstants.MOD_ID, "cobalt_power_level");
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CobaltAgeConstants.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(id, modContainer, Component.literal("CobaltAge Cobalt Wire Power Level"), PackActivationType.NORMAL);
    }

    private static void initializeCobaltAgeDDMResourcePack() {
        Identifier id = Identifier.fromNamespaceAndPath(CobaltAgeConstants.MOD_ID, "cobalt_age_ddm");
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CobaltAgeConstants.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(id, modContainer, Component.literal("CobaltAge Dark Mode GUI"), PackActivationType.NORMAL);
    }
}