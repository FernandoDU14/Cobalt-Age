package net.fernando.cobaltage.mixin.consumers;

import net.fernando.cobaltage.util.CobaltPowerHelperForMixin;
import net.minecraft.block.CrafterBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {

    @Redirect(method = "neighborUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean cobalt$combinePowerSources(World world, BlockPos pos) {
        return world.isReceivingRedstonePower(pos) || CobaltPowerHelperForMixin.isPoweredByCobalt(world, pos);
    }
}