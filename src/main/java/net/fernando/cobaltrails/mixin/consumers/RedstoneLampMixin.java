package net.fernando.cobaltrails.mixin.consumers;
import net.fernando.cobaltrails.util.CobaltPowerHelperForMixin;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RedstoneLampBlock.class)
public abstract class RedstoneLampMixin {

    // Controlla l'accensione e lo spegnimento immediato
    @Redirect(
            method = {"neighborUpdate", "scheduledTick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z")
    )
    private boolean cobalt$combinePowerSources(World world, BlockPos pos) {
        return world.isReceivingRedstonePower(pos) || CobaltPowerHelperForMixin.isPoweredByCobalt(world, pos);
    }
}