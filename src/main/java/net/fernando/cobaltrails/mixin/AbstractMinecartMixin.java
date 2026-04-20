package net.fernando.cobaltrails.mixin;

import net.fernando.cobaltrails.CobaltRails;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartMixin {

    @Redirect(
            method = "getLaunchDirection",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
    public boolean isPoweringRail(BlockState state, Block block) {
        if (block == Blocks.POWERED_RAIL) {
            Block unknownRail = state.getBlock();
            return (unknownRail instanceof PoweredRailBlock);
        } else {
            CobaltRails.LOGGER.warn("isOf() Mixin called with something else than Blocks.POWERED_RAIL");
            return state.isOf(block);
        }
    }

}