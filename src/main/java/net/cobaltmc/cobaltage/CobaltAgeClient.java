package net.cobaltmc.cobaltage;

import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.block.cobalt.CobaltWireBlock;
import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltColorUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CobaltAgeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockColorRegistry.register(List.of(new BlockTintSource() {
            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                int color = state.hasProperty(CobaltWireBlock.POWER) ?
                        CobaltColorUtil.getCobaltColor(state.getValue(CobaltWireBlock.POWER)) :
                        CobaltColorUtil.getCobaltColor(0);
                return ARGB.opaque(color); // Ensures alpha is fully opaque (0xFF000000)
            }

            @Override
            public int color(BlockState state) {
                return ARGB.opaque(CobaltColorUtil.getCobaltColor(0));
            }
        }), ModBlocks.COBALT_DUST);

        BlockColorRegistry.register(List.of(new BlockTintSource() {
            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                int color = state.hasProperty(CobaltWireBlock.POWER) ?
                        CobaltColorUtil.getCobaltColor(state.getValue(CobaltWireBlock.POWER)) :
                        CobaltColorUtil.getCobaltColor(0);
                return ARGB.opaque(color);
            }

            @Override
            public int color(BlockState state) {
                return ARGB.opaque(CobaltColorUtil.getCobaltColor(0));
            }
        }), ModBlocks.COBALT_RELAY);

        initialize3dCobaltRailsResourcePack();
        initializeCobaltWirePowerLevelResourcePack();
        initializeCobaltAgeDDMResourcePack();
    }

    private static void initialize3dCobaltRailsResourcePack() {
        Identifier id = Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobaltrails3d");
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CobaltAge.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(id, modContainer, Component.literal("CobaltAge 3D Cobalt Rails"), PackActivationType.NORMAL);
    }

    private static void initializeCobaltWirePowerLevelResourcePack() {
        Identifier id = Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_power_level");
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CobaltAge.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(id, modContainer, Component.literal("CobaltAge Cobalt Wire Power Level"), PackActivationType.NORMAL);
    }

    private static void initializeCobaltAgeDDMResourcePack() {
        Identifier id = Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_age_ddm");
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(CobaltAge.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(id, modContainer, Component.literal("CobaltAge Dark Mode GUI"), PackActivationType.NORMAL);
    }
}