package net.fernando.cobaltrails.block.entity;

import net.fernando.cobaltrails.CobaltRails;
import net.fernando.cobaltrails.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    // 1. Dichiariamo la variabile (verrà inizializzata nel metodo register)
    public static BlockEntityType<CobaltComparatorBlockEntity> COBALT_COMPARATOR_ENTITY;

    // 2. Creiamo il metodo che mancava o che dava errore
    public static void registerModBlockEntities() {
        COBALT_COMPARATOR_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(CobaltRails.MOD_ID, "cobalt_comparator"),
                BlockEntityType.Builder.create(CobaltComparatorBlockEntity::new, ModBlocks.COBALT_COMPARATOR).build(null)
        );

        CobaltRails.LOGGER.info("Registering Block Entities for " + CobaltRails.MOD_ID);
    }
}