package net.cobaltmc.cobaltage.mixin.world.level.block.redstone;


import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RedstoneWallTorchBlock.class)
public class RedstoneWallTorchBlockMixin {

    @Redirect(method = "hasNeighborSignal", at = @At(value="INVOKE", target="Lnet/minecraft/world/level/Level;hasSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    private boolean hasSignalByType(Level level, BlockPos blockPos, Direction direction) {
        return ((SignalGetterByType) level).cobaltage$hasSignalByType(SignalType.REDSTONE, blockPos, direction);
    }

}
