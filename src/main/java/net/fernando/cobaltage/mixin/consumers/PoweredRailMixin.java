package net.fernando.cobaltage.mixin.consumers;
import net.fernando.cobaltage.util.signal.SignalTypeLevelExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredRailBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.fernando.cobaltage.util.signal.SignalType.COBALT;

@Mixin(PoweredRailBlock.class)
public abstract class PoweredRailMixin {

    @Redirect(
            method = {
                    "updateState",
                    "isSameRailWithPower(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ZILnet/minecraft/world/level/block/state/properties/RailShape;)Z"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean cobalt$combinePowerSources(Level world, BlockPos pos) {
        return world.hasNeighborSignal(pos) || ((SignalTypeLevelExtensions) world).hasNeighbourSignalByType(COBALT, pos);
    }
}