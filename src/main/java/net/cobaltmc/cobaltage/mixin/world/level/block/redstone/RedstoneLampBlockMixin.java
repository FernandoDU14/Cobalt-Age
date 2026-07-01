package net.cobaltmc.cobaltage.mixin.world.level.block.redstone;

import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneLampBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RedstoneLampBlock.class)
public class RedstoneLampBlockMixin {

    @Redirect(method="getStateForPlacement", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z") )
    private boolean cobaltage$hasNeighborSignal(Level instance, BlockPos blockPos) {
        return ((SignalGetterByType)instance).cobaltage$hasNeighborSignalByType(SignalType.REDSTONE, blockPos);
    }

    @Redirect(method="neighborChanged", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z") )
    private boolean cobaltage$hasNeighborSignal2(Level instance, BlockPos blockPos) {
        return ((SignalGetterByType)instance).cobaltage$hasNeighborSignalByType(SignalType.REDSTONE, blockPos);
    }

    @Redirect(method="tick", at=@At(value="INVOKE", target="Lnet/minecraft/server/level/ServerLevel;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z") )
    private boolean cobaltage$hasNeighborSignal3(ServerLevel instance, BlockPos blockPos) {
        return ((SignalGetterByType)instance).cobaltage$hasNeighborSignalByType(SignalType.REDSTONE, blockPos);
    }

}
