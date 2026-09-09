package net.cobaltmc.cobaltage.mixin.world.entity.minecart;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.cobaltmc.cobaltage.CobaltAgeConfig;
import net.cobaltmc.cobaltage.CobaltAgeConstants;
import net.cobaltmc.cobaltage.block.cobalt.CobaltRailBlock;
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

@Mixin(value = NewMinecartBehavior.class)
public abstract class NewMinecartBehaviorMixin extends MinecartBehaviorMixin {

    protected NewMinecartBehaviorMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @Unique
    private boolean isPoweringRail(BlockState state, Block block, Operation<Boolean> original) {
        if (block == Blocks.POWERED_RAIL) {
            return switch (state.getBlock()) {
                case CobaltRailBlock c -> true;
                case Block b when b == Blocks.POWERED_RAIL -> true;
                default -> false;
            };
        } else {
            CobaltAgeConstants.LOGGER.warn("is() Mixin called with something else than Blocks.POWERED_RAIL");
            return original.call(state, block);
        }
    }

    @WrapOperation(
            method = "calculateHaltTrackSpeed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            )
    )
    public boolean isPoweringHaltTrackSpeed(BlockState state, Block block, Operation<Boolean> original) {
        return isPoweringRail(state, block, original);
    }

    @WrapOperation(
            method = "calculateBoostTrackSpeed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            )
    )
    public boolean isPoweringBootTrackSpeed(BlockState state, Block block, Operation<Boolean> original) {
        return isPoweringRail(state, block, original);
    }

    @Unique
    public int getMaxRailSpeed(BlockState blockState) {
        Block block = blockState.getBlock();
        MinecraftServer server = this.minecart.level().getServer();
        if (server == null) {
            CobaltAgeConstants.LOGGER.error("Could not access to server gamerules ! Please report this bug");
            return CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS;
        }
        GameRules gamerules = server.overworld().getGameRules();
        int maxSpeed = getCobaltOrGoldMaxSpeedByRailType(block, gamerules);
        return Math.min(maxSpeed, gamerules.get(GameRules.MAX_MINECART_SPEED));
    }

    @ModifyVariable(
            method = "calculateBoostTrackSpeed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;length()D",
                    ordinal = 0
            ),
            argsOnly = true
    )
    private Vec3 slowedDownVec3(Vec3 vec3, @Local(ordinal = 0, argsOnly = true) BlockState blockState) {
        float maxSpeed = getMaxRailSpeed(blockState) / 20.0F;
        if (vec3.length() > maxSpeed) {
            double d = Math.max(vec3.length() * 0.95 - 0.06, maxSpeed);
            vec3 = vec3.normalize().scale(d);
        }
        return vec3;
    }
}