package net.fernando.cobaltrails;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fernando.cobaltrails.block.ModBlocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.MathHelper;

import static net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock;

public class CobaltRailsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.putBlock(ModBlocks.COBALT_RAIL, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.COBALT_DUST, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlocks(BlockRenderLayer.CUTOUT,
                ModBlocks.COBALT_TORCH,
                ModBlocks.COBALT_WALL_TORCH,
                ModBlocks.COBALT_REPEATER,
                ModBlocks.COBALT_COMPARATOR
        );

        // COLORE DINAMICO PER IL BLOCCO
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            int power = state.get(RedstoneWireBlock.POWER);
            return getCobaltColor(power);
        }, ModBlocks.COBALT_DUST);
    }

    // Funzione per calcolare il gradiente blu
    private static int getCobaltColor(int power) {
        float f = (float)power / 15.0F;
        // Calcoliamo le componenti R, G, B basandoci sul livello di carica
        // Per il cobalto: poco rosso, medio verde, molto blu
        float r = f * 0.1f + 0.1f;
        float g = f * 0.5f + 0.2f;
        float b = f * 0.9f + 0.4f;

        int red = MathHelper.clamp((int)(r * 255.0F), 0, 255);
        int green = MathHelper.clamp((int)(g * 255.0F), 0, 255);
        int blue = MathHelper.clamp((int)(b * 255.0F), 0, 255);

        return red << 16 | green << 8 | blue;
    }

}