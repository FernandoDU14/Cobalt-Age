package net.fernando.cobaltrails;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fernando.cobaltrails.block.ModBlocks;
import net.minecraft.client.render.RenderLayer;

public class CobaltRailsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.COBALT_RAIL, RenderLayer.getCutout());
    }
}
