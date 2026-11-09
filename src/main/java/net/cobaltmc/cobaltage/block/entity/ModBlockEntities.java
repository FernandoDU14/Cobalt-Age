package net.cobaltmc.cobaltage.block.entity;

import net.cobaltmc.cobaltage.CobaltAge;
import net.cobaltmc.cobaltage.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static final BlockEntityType<CobaltComparatorBlockEntity> COBALT_COMPARATOR = register(
            "cobalt_comparator_be",
            FabricBlockEntityTypeBuilder.create(
                    CobaltComparatorBlockEntity::new,
                    ModBlocks.COBALT_COMPARATOR // Adjust to .get() if ModBlocks still uses a Supplier
            ).build()
    );

    private static <T extends BlockEntityType<?>> T register(String name, T blockEntityType) {
        return Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, name), // Use new Identifier(CobaltAge.MOD_ID, name) for versions below 1.21
                blockEntityType
        );
    }

    public static void registerBlockEntities() {
        CobaltAge.LOGGER.info("Registering Block Entities for " + CobaltAge.MOD_ID);
    }
}