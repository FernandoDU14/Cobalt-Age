package net.cobaltmc.cobaltage.mixin.world.entity.minecart;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MinecartBoatPushMixin {

    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void allowBoatMinecartStacking(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if ((self instanceof AbstractMinecart && entity instanceof Boat) ||
                (self instanceof Boat && entity instanceof AbstractMinecart)) {
            cir.setReturnValue(false);
        }
    }
}