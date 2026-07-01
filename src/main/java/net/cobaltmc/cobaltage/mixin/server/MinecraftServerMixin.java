package net.cobaltmc.cobaltage.mixin.server;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cobaltmc.cobaltage.util.interfaces.mixin.IServerLevel;
import net.cobaltmc.cobaltage.block.signal.engine.modern.WireHandler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(
            method = "saveAllChunks",
            at = @At(
                    value = "HEAD"
            )
    )
    private void cobaltage$save(boolean silent, boolean bl2, boolean bl3, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel overworld = ((MinecraftServer) (Object) this).overworld();
        WireHandler wireHandler = ((IServerLevel) overworld).cobaltage$getWireHandler();

        wireHandler.getConfig().save(silent);
    }
}
