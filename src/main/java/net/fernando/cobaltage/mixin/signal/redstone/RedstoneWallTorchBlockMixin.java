package net.fernando.cobaltage.mixin.signal.redstone;


import net.fernando.cobaltage.util.signal.SignalType;
import net.fernando.cobaltage.util.signal.SignalTypeLevelExtensions;
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
        return ((SignalTypeLevelExtensions) level).hasSignalByType(SignalType.REDSTONE, blockPos, direction);
    }

}
