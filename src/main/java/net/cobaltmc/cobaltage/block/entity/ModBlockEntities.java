package net.cobaltmc.cobaltage.block.entity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.cobaltmc.cobaltage.CobaltAge;
import net.cobaltmc.cobaltage.block.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static BlockEntityType<CobaltComparatorBlockEntity> COBALT_COMPARATOR_ENTITY;

    public static void registerBlockEntities() {
        COBALT_COMPARATOR_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(CobaltAge.MOD_ID, "cobalt_comparator_be"),
                FabricBlockEntityTypeBuilder.create(
                        CobaltComparatorBlockEntity::new,
                        ModBlocks.COBALT_COMPARATOR
                ).build()
        );
    }
}