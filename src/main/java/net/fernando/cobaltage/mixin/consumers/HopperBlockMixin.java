package net.fernando.cobaltage.mixin.consumers;

import net.fernando.cobaltage.util.CobaltPowerHelper;
import net.minecraft.block.HopperBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HopperBlock.class)
public abstract class HopperBlockMixin {
    @Redirect(
            method = "updateEnabled",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z")
    )
    private boolean cobalt$combinePowerSources(World world, BlockPos pos) {
        return world.isReceivingRedstonePower(pos) || CobaltPowerHelper.isPoweredByCobalt(world, pos);
    }
}