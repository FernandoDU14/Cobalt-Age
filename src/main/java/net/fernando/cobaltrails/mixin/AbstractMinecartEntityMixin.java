package net.fernando.cobaltrails.mixin;

import net.fernando.cobaltrails.block.ModBlocks;
import net.fernando.cobaltrails.gamerules.CobaltRailsGameRules;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin {

    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    protected void onGetMaxSpeed(CallbackInfoReturnable<Double> cir) {
        AbstractMinecartEntity entity = (AbstractMinecartEntity) (Object) this;
        BlockState state = entity.getWorld().getBlockState(entity.getBlockPos());

        // Check if the minecart is on a Cobalt Rail
        if (state.isOf(ModBlocks.COBALT_RAIL)) {
            // Get value from GameRule and convert BPS to Tick velocity
            float speedBps = entity.getWorld().getGameRules().getInt(CobaltRailsGameRules.MAX_MINECART_SPEED_COBALT);
            cir.setReturnValue((double) (speedBps / 20.0F));
        }
        // Check if the minecart is on a Powered Rail (Gold)
        else if (state.isOf(Blocks.POWERED_RAIL)) {
            float speedBps = entity.getWorld().getGameRules().getInt(CobaltRailsGameRules.MAX_MINECART_SPEED_GOLD);
            cir.setReturnValue((double) (speedBps / 20.0F));
        }
    }
}