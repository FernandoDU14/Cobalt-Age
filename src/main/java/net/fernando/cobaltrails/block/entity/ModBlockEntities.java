package net.fernando.cobaltrails.block.entity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fernando.cobaltrails.CobaltRails;
import net.fernando.cobaltrails.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static BlockEntityType<CobaltComparatorBlockEntity> COBALT_COMPARATOR_ENTITY;

    public static void registerBlockEntities() {
        // Qui ASSEGNIAMO il valore alla variabile
        COBALT_COMPARATOR_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(CobaltRails.MOD_ID, "cobalt_comparator_be"),
                FabricBlockEntityTypeBuilder.create(
                        CobaltComparatorBlockEntity::new,
                        ModBlocks.COBALT_COMPARATOR // Assicurati che questo blocco non sia null!
                ).build()
        );
    }
}