package net.cobaltmc.cobaltage.mixin.world.entity.minecart;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.cobaltmc.cobaltage.CobaltAgeConstants;
import net.cobaltmc.cobaltage.block.cobalt.CobaltRailBlock;
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
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            )
    )
    private boolean isPoweringRail(BlockState state, Block block, Operation<Boolean> original) {
        if (block == Blocks.POWERED_RAIL) {
            return switch (state.getBlock()) {
                case CobaltRailBlock c -> true;
                case Block b when b == Blocks.POWERED_RAIL -> true;
                default -> false;
            };
        } else {
            CobaltAgeConstants.LOGGER.warn("is() Mixin called with something else than Blocks.POWERED_RAIL");
            return original.call(state, block);
        }
    }
}