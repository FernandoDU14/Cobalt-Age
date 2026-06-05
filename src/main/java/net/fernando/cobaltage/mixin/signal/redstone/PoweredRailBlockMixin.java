package net.fernando.cobaltage.mixin.signal.redstone;

import net.fernando.cobaltage.util.signal.SignalType;
import net.fernando.cobaltage.util.signal.SignalTypeLevelExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredRailBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(PoweredRailBlock.class)
public class PoweredRailBlockMixin {

    @Redirect(method = "isSameRailWithPower", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"))

    public boolean hasNeighbourSignalByType_isSameRailWithPower(Level level, BlockPos blockPos) {
        return ((SignalTypeLevelExtensions) level).hasNeighbourSignalByType(SignalType.REDSTONE, blockPos);
    }

    @Redirect(method = "updateState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"))

    public boolean hasNeighbourSignalByType_updateState(Level level, BlockPos blockPos) {
        return ((SignalTypeLevelExtensions) level).hasNeighbourSignalByType(SignalType.REDSTONE, blockPos);
    }

}
