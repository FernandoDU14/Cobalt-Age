package net.fernando.cobaltage.mixin.consumers;
import net.fernando.cobaltage.util.CobaltPowerHelper;
import net.minecraft.block.ShelfBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShelfBlock.class)
public abstract class ShelfBlockMixin {

    @Redirect(
            method = {"neighborUpdate", "getPlacementState"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z")
    )
    private boolean cobalt$combinePowerSources(World world, BlockPos pos) {
        return world.isReceivingRedstonePower(pos) || CobaltPowerHelper.isPoweredByCobalt(world, pos);
    }
}