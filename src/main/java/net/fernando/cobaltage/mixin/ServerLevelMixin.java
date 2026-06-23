package net.fernando.cobaltage.mixin;

import java.util.List;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fernando.cobaltage.util.interfaces.mixin.IServerLevel;
import net.fernando.cobaltage.wire.modern.WireHandler;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements IServerLevel {

    @Unique
    private WireHandler wireHandler;

    @Inject(
            method = "<init>",
            at = @At(
                    value = "TAIL"
            )
    )
    private void cobaltage$parseConfig(MinecraftServer server, Executor executor, LevelStorageSource.LevelStorageAccess storage, ServerLevelData data, ResourceKey<Level> key, LevelStem stem, boolean clientSide, long seed, List<CustomSpawner> customSpawners, boolean tickTime, RandomSequences randomSequences, CallbackInfo ci) {
        this.wireHandler = new WireHandler((ServerLevel)(Object)this, storage);
    }

    @Override
    public WireHandler cobaltage$getWireHandler() {
        return wireHandler;
    }
}
