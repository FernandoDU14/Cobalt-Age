package net.fernando.cobaltage.mixin.minecarts;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fernando.cobaltage.block.ModBlocks;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin {

    @WrapOperation(
            method = "getRedstoneDirection",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")
    )
    public boolean isPoweringRail(BlockState state, Block block, Operation<Boolean> original) {
        // Not touching the call of other rails that aren't Powered and Cobalt
        if (original.call(state, block)) {
            return true;
        }
        // Cobalt Support only and only if original call has said no
        return block == Blocks.POWERED_RAIL && state.is(ModBlocks.COBALT_RAIL);
    }
}