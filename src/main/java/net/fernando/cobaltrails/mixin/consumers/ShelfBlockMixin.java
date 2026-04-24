package net.fernando.cobaltrails.mixin.consumers;
import net.fernando.cobaltrails.util.CobaltPowerHelperForMixin;
import net.minecraft.block.ShelfBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShelfBlock.class)
public abstract class ShelfBlockMixin {

    /**
     * Intercettiamo la lettura dell'energia sia quando la Shelf viene piazzata (getPlacementState)
     * sia quando subisce un aggiornamento (neighborUpdate).
     */
    @Redirect(
            method = {"neighborUpdate", "getPlacementState"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z")
    )
    private boolean cobalt$combinePowerSources(World world, BlockPos pos) {
        return world.isReceivingRedstonePower(pos) || CobaltPowerHelperForMixin.isPoweredByCobalt(world, pos);
    }
}