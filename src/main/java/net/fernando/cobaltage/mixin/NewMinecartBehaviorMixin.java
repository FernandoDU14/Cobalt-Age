package net.fernando.cobaltage.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fernando.cobaltage.CobaltAgeConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.rule.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ExperimentalMinecartController.class)
public abstract class NewMinecartBehaviorMixin extends MinecartBehaviorMixin {

    protected NewMinecartBehaviorMixin(AbstractMinecartEntity minecart) {
        super(minecart);
    }

    /**
     * Redirects the check to see if a block should act as a powered rail.
     * The original call is: blockState.is(Blocks.POWERED_RAIL)
     */
    @WrapOperation(
            method = {"decelerateFromPoweredRail", "accelerateFromPoweredRail"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"
            )
    )
    private boolean redirectIsPoweredRail(BlockState instance, Block block, Operation<Boolean> original) {

        if (block == Blocks.POWERED_RAIL) {
            return instance.getBlock() instanceof PoweredRailBlock;
        }

        return original.call(instance, block);
    }

    @Unique
    public int getMaxRailSpeed(BlockState blockState) {

        MinecraftServer server = this.minecart.getEntityWorld().getServer();

        if (server == null) {
            return CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS;
        }

        GameRules gamerules = server.getOverworld().getGameRules();
        int maxSpeed = getMaxSpeedByRailType(blockState.getBlock(), gamerules);
        return Math.min(maxSpeed, gamerules.getValue(GameRules.MAX_MINECART_SPEED));
    }

    @ModifyVariable(
            method = "accelerateFromPoweredRail",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/Vec3d;length()D",
                    ordinal = 0),
            argsOnly = true)
    private Vec3d limitSpeed(Vec3d velocity, @Local(argsOnly = true) BlockState railState) {
        double maxSpeedPerTick = getMaxRailSpeed(railState) / 20.0;
        double currentSpeed = velocity.length();

        if (currentSpeed > maxSpeedPerTick) {
            double newSpeed = Math.max(currentSpeed * 0.95 - 0.06, maxSpeedPerTick);
            return velocity.normalize().multiply(newSpeed);
        }
        return velocity;
    }
}