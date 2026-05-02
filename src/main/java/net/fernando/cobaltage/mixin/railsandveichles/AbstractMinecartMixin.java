package net.fernando.cobaltage.mixin.railsandveichles;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fernando.cobaltage.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartMixin {

    @WrapOperation(
            method = "getLaunchDirection",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z")
    )
    public boolean isPoweringRail(BlockState state, Block block, Operation<Boolean> original) {
        // Se l'originale (o Copper Rails) dice già true, non interferire.
        if (original.call(state, block)) {
            return true;
        }
        // Aggiungiamo il supporto al cobalto solo se l'originale ha detto false.
        return block == Blocks.POWERED_RAIL && state.isOf(ModBlocks.COBALT_RAIL);
    }
}