package net.fernando.cobaltage;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fernando.cobaltage.block.ModBlocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class CobaltAgeClient implements ClientModInitializer {

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

        // Dynamic Color for the Cobalt Dust
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            int power = state.get(RedstoneWireBlock.POWER);
            return getCobaltColor(power);
        }, ModBlocks.COBALT_DUST);

        initialize3dCobaltRailsResourcePack();
        initializeCobaltWirePowerLevelResourcePack();
    }

    // Function to compute the color gradient of the Cobalt Dust
    private static int getCobaltColor(int power) {
        float f = (float)power / 15.0F;
        // Calcoliamo le componenti R, G, B basandoci sul livello di carica
        // Per il cobalto: poco rosso, medio verde, molto blu
        float r = f * 0.1f + 0.1f;
        float g = f * 0.5f + 0.3f;
        float b = f * 1.1f + 0.4f;

        if(f!=0){
            b = b + 0.1f;
            g = g + 0.1f;
        }

        int red = MathHelper.clamp((int)(r * 255.0F), 0, 255);
        int green = MathHelper.clamp((int)(g * 255.0F), 0, 255);
        int blue = MathHelper.clamp((int)(b * 255.0F), 0, 255);

        return red << 16 | green << 8 | blue;
    }

    private static void initialize3dCobaltRailsResourcePack() {
        Identifier id = Identifier.of(CobaltAge.MOD_ID, "cobaltrails3d");
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CobaltAge.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(id, modContainer, Text.of("CobaltAge 3D Rails"), PackActivationType.NORMAL);
    }

    private static void initializeCobaltWirePowerLevelResourcePack() {
        Identifier id = Identifier.of(CobaltAge.MOD_ID, "cobalt_power_level");
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CobaltAge.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(id, modContainer, Text.of("Cobalt Wire Power Level"), PackActivationType.NORMAL);
    }


}