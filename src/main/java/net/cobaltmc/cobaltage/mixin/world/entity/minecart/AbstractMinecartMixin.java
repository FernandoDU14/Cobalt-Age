package net.cobaltmc.cobaltage.mixin.world.entity.minecart;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin {

    @Redirect(
            method = "getRedstoneDirection",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
    public boolean isPoweringRail(BlockState state, Object block) {
        if (block == Blocks.POWERED_RAIL) {
            Block unknownRail = state.getBlock();
            return (unknownRail instanceof PoweredRailBlock);
        } else {
            return state.is((Block) block);
        }
    }
}