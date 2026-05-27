package net.fernando.cobaltage.mixin.consumers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.fernando.cobaltage.util.CobaltPowerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CopperBulbBlock;

@Mixin(CopperBulbBlock.class)
public abstract class CopperBulbMixin {

    @Redirect(method = "checkAndFlip", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean cobalt$combinePowerSources(ServerLevel instance, BlockPos blockPos) {
        return instance.hasNeighborSignal(blockPos) || CobaltPowerHelper.isPoweredByCobalt(instance, blockPos);
    }

}