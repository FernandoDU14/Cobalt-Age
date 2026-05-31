package net.fernando.cobaltage.mixin.consumers;
import net.fernando.cobaltage.util.SignalTypeLevelExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.fernando.cobaltage.util.SignalType.COBALT;

@Mixin(TrapDoorBlock.class)
public abstract class TrapdoorBlockMixin {

    @Redirect(
            method = "neighborChanged",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean cobalt$combinePowerSources(Level world, BlockPos pos) {
        return world.hasNeighborSignal(pos) || ((SignalTypeLevelExtensions) world).hasNeighbourSignalByType(COBALT, pos);
    }
}