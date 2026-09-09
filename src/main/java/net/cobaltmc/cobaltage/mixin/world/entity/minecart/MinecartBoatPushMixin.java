package net.cobaltmc.cobaltage.mixin.world.entity.minecart;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecart.class)
public abstract class MinecartBoatPushMixin {

    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void cancelBoatPushing(Entity entity, CallbackInfo ci) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;

        if (entity instanceof Boat || cart.hasPassenger(entity) || cart.hasIndirectPassenger(entity)) {
            ci.cancel();
        }
    }
}