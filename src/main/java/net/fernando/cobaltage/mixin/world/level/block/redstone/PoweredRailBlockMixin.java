package net.fernando.cobaltage.mixin.world.level.block.redstone;

import net.fernando.cobaltage.block.signal.SignalType;
import net.fernando.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
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
        return ((SignalGetterByType) level).cobaltage$hasNeighbourSignalByType(SignalType.REDSTONE, blockPos);
    }

    @Redirect(method = "updateState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"))

    public boolean hasNeighbourSignalByType_updateState(Level level, BlockPos blockPos) {
        return ((SignalGetterByType) level).cobaltage$hasNeighbourSignalByType(SignalType.REDSTONE, blockPos);
    }

}
