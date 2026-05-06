package net.fernando.cobaltage.mixin.consumers;
import net.minecraft.block.BulbBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.fernando.cobaltage.util.CobaltPowerHelper;

@Mixin(BulbBlock.class)
public abstract class CopperBulbMixin {

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean cobalt$combinePowerSources(ServerWorld world, BlockPos pos) {
        return world.isReceivingRedstonePower(pos) || CobaltPowerHelper.isPoweredByCobalt(world, pos);
    }

}