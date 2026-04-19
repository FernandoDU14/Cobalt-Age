package net.fernando.cobaltrails.mixin;

import net.fernando.cobaltrails.CobaltRails;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.robofox.copperrails.CopperRails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartMixin {

    @Redirect(
            method = "getRedstoneDirection",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    public boolean isPoweringRail(BlockState state, Block block) {
        // This code is injected into the start of AbstractMinecartEntity.moveAlongTrack()V
        if (block == Blocks.POWERED_RAIL) {
            Block unknownRail = state.getBlock();
            // We want to check if this is a powering rail
            return (unknownRail instanceof PoweredRailBlock || unknownRail == Blocks.POWERED_RAIL);
        } else {
            CobaltRails.LOGGER.warn("isOf() Mixin called with something else than Blocks.POWERED_RAIL");
            return state.is(block);
        }
    }

}
