package net.fernando.cobaltage.mixin.consumers;

import net.fernando.cobaltage.util.signal.SignalTypeLevelExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.fernando.cobaltage.util.signal.SignalType.COBALT;

@Mixin(TntBlock.class)
public abstract class TntBlockMixin {

    @Redirect(
            method = {"onPlace", "neighborChanged"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean cobalt$combinePowerSources(Level world, BlockPos pos) {
        return world.hasNeighborSignal(pos) || ((SignalTypeLevelExtensions) world).hasNeighbourSignalByType(COBALT, pos);
    }
}