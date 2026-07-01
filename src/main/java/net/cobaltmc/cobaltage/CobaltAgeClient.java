package net.cobaltmc.cobaltage;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.cobaltmc.cobaltage.block.CobaltWireBlock;
import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltColorUtil;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class CobaltAgeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.putBlock(ModBlocks.COBALT_RAIL, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.COBALT_DUST, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT,
                ModBlocks.COBALT_TORCH,
                ModBlocks.COBALT_WALL_TORCH,
                ModBlocks.COBALT_REPEATER,
                ModBlocks.COBALT_COMPARATOR,
                ModBlocks.COBALT_RELAY
        );

        // Dynamic Color for the Cobalt Dust
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            int power = state.getValue(CobaltWireBlock.POWER);
            return CobaltColorUtil.getCobaltColor(power);
        }, ModBlocks.COBALT_DUST);

        // Registrazione per il Cobalt Relay
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            // Se il tintIndex è 0 (quello che abbiamo messo nel JSON della dust), calcola il colore
            if (tintIndex == 0) {
                int power = state.getValue(CobaltWireBlock.POWER);
                return CobaltColorUtil.getCobaltColor(power);
            }
            // Altrimenti, restituisci -1 per non applicare alcuna tinta (mantiene i colori originali delle texture)
            return -1;
        }, ModBlocks.COBALT_RELAY);

        initialize3dCobaltRailsResourcePack();
        initializeCobaltWirePowerLevelResourcePack();
        initializeCobaltAgeDDMResourcePack();
    }

    private static void initialize3dCobaltRailsResourcePack() {
        Identifier id = Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobaltrails3d");
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CobaltAge.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(id, modContainer, Component.nullToEmpty("CobaltAge 3D Rails"), PackActivationType.NORMAL);
    }

    private static void initializeCobaltWirePowerLevelResourcePack() {
        Identifier id = Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_power_level");
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CobaltAge.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(id, modContainer, Component.nullToEmpty("Cobalt Wire Power Level"), PackActivationType.NORMAL);
    }

    private static void initializeCobaltAgeDDMResourcePack() {
        Identifier id = Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_age_ddm");
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CobaltAge.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(id, modContainer, Component.nullToEmpty("Cobalt Age DDM GUIs"), PackActivationType.NORMAL);
    }


}
