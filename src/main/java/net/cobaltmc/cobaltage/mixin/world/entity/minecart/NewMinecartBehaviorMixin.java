package net.cobaltmc.cobaltage.mixin.world.entity.minecart;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.cobaltmc.cobaltage.CobaltAgeConfig;
import net.cobaltmc.cobaltage.block.ModBlocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = NewMinecartBehavior.class, priority = 1500)
public abstract class NewMinecartBehaviorMixin extends MinecartBehaviorMixin {

    protected NewMinecartBehaviorMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    /**
     * Redirects the check to see if a block should act as a powered rail.
     * The original call is: blockState.is(Blocks.POWERED_RAIL)
     */
    @WrapOperation(
            method = {"calculateHaltTrackSpeed", "calculateBoostTrackSpeed"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            )
    )
    private boolean redirectIsPoweredRail(BlockState instance, Block block, Operation<Boolean> original) {
        if (instance.is(ModBlocks.COBALT_RAIL) || instance.is(Blocks.POWERED_RAIL)) {
            return true;
        }
        return original.call(instance, block);
    }

    @Unique
    public int getCobaltOrGoldMaxRailSpeed(BlockState blockState) {

        MinecraftServer server = this.minecart.level().getServer();

        if (server == null) {
            return CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS;
        }

        GameRules gamerules = server.overworld().getGameRules();
        int maxSpeed = getCobaltOrGoldMaxSpeedByRailType(blockState.getBlock(), gamerules);
        return Math.min(maxSpeed, gamerules.get(GameRules.MAX_MINECART_SPEED));
    }

    @ModifyVariable(
            method = "calculateBoostTrackSpeed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;length()D",
                    ordinal = 0))
    private Vec3 limitSpeed(Vec3 velocity, @Local BlockState railState) {

        Block block = railState.getBlock();
        if (block != Blocks.POWERED_RAIL && block != ModBlocks.COBALT_RAIL) {
            // Not touching the velocity of other rails that aren't Powered and Cobalt
            return velocity;
        }

        double maxSpeedPerTick = getCobaltOrGoldMaxRailSpeed(railState) / 20.0;
        double currentSpeed = velocity.length();

        if (currentSpeed > maxSpeedPerTick) {
            double newSpeed = Math.max(currentSpeed * 0.95 - 0.06, maxSpeedPerTick);
            return velocity.normalize().scale(newSpeed);
        }
        return velocity;
    }
}