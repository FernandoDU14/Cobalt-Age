package net.fernando.cobaltrails.mixin.consumers;

import net.fernando.cobaltrails.util.CobaltPowerHelperForMixin;
import net.minecraft.block.TntBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TntBlock.class)
public abstract class TntBlockMixin {

    /**
     * Redirigiamo il controllo dell'energia redstone sia durante il piazzamento
     * che durante l'aggiornamento dei blocchi adiacenti.
     */
    @Redirect(
            method = {"onBlockAdded", "neighborUpdate"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z")
    )
    private boolean cobalt$combinePowerSources(World world, BlockPos pos) {
        // La TNT esplode se riceve Redstone o Cobalto
        return world.isReceivingRedstonePower(pos) || CobaltPowerHelperForMixin.isPoweredByCobalt(world, pos);
    }
}