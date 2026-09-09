package net.cobaltmc.cobaltage.mixin.world.entity.minecart;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.cobaltmc.cobaltage.CobaltAgeConfig;
import net.cobaltmc.cobaltage.CobaltAgeConstants;
import net.cobaltmc.cobaltage.block.cobalt.CobaltRailBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = OldMinecartBehavior.class)
public abstract class OldMinecartBehaviorMixin extends MinecartBehaviorMixin {

    public OldMinecartBehaviorMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            )
    )
    public boolean isPoweringRail(BlockState state, Block block, Operation<Boolean> original) {
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

    @Unique
    public int getMaxRailSpeed(BlockState blockState) {
        Block block = blockState.getBlock();
        MinecraftServer server = this.minecart.level().getServer();
        if (server == null) {
            CobaltAgeConstants.LOGGER.error("Could not access to server gamerules ! Please report this bug");
            return CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS;
        }
        GameRules gamerules = server.overworld().getGameRules();
        return getCobaltOrGoldMaxSpeedByRailType(block, gamerules);
    }

    @ModifyReturnValue(method = "getMaxSpeed", at = @At("RETURN"))
    private double modifyGetMaxSpeed(double originalSpeed, ServerLevel serverLevel) {
        double speed = getMaxRailSpeed(this.minecart.getInBlockState()) / 20.0D;
        return (this.minecart.isInWater() ? speed / 2.0 : speed);
    }

    @Unique
    private double convergeAbs(double speed, double targetSpeed) {
        if (Math.abs(speed) > targetSpeed) {
            return Math.signum(speed) * Math.max(Math.abs(speed) * 0.7, targetSpeed);
        } else {
            return speed;
        }
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 9
            )
    )
    public void wrapVelocityClamp(OldMinecartBehavior minecartController, Vec3 velocity, Operation<Void> original, @Local(ordinal = 0) RailShape railShape) {
        boolean isSlope = railShape == RailShape.ASCENDING_EAST ||
                railShape == RailShape.ASCENDING_WEST ||
                railShape == RailShape.ASCENDING_NORTH ||
                railShape == RailShape.ASCENDING_SOUTH;

        boolean isCurve = railShape == RailShape.NORTH_EAST ||
                railShape == RailShape.NORTH_WEST ||
                railShape == RailShape.SOUTH_EAST ||
                railShape == RailShape.SOUTH_WEST;

        double maxSpeed = isSlope ? CobaltAgeConfig.MAX_ASCENDING_SPEED : (getMaxRailSpeed(this.minecart.getInBlockState()) / 20.0F);

        double clampedX = velocity.x;
        double clampedZ = velocity.z;

        if (isCurve) {
            Vec3 currentVelocity = this.minecart.getDeltaMovement();
            if (Math.abs(currentVelocity.x) > 0.05) {
                clampedX = Math.signum(currentVelocity.x) * Math.min(Math.abs(velocity.x), maxSpeed);
            }
            if (Math.abs(currentVelocity.z) > 0.05) {
                clampedZ = Math.signum(currentVelocity.z) * Math.min(Math.abs(velocity.z), maxSpeed);
            }
        } else {
            clampedX = convergeAbs(velocity.x, maxSpeed);
            clampedZ = convergeAbs(velocity.z, maxSpeed);
        }

        double clampedY = velocity.y;
        if (clampedY < -maxSpeed) {
            clampedY = -maxSpeed;
        }

        original.call(minecartController, new Vec3(clampedX, clampedY, clampedZ));
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 0
            )
    )
    private void wrapAscendingEast(OldMinecartBehavior minecart, Vec3 velocityAdder, Operation<Void> original) {
        double maxSpeed = getMaxRailSpeed(this.minecart.getInBlockState()) / 20.0D;
        original.call(minecart, new Vec3(Math.min(velocityAdder.x, maxSpeed), velocityAdder.y, velocityAdder.z));
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 1
            )
    )
    private void wrapAscendingWest(OldMinecartBehavior minecart, Vec3 velocityAdder, Operation<Void> original) {
        double maxSpeed = getMaxRailSpeed(this.minecart.getInBlockState()) / 20.0D;
        original.call(minecart, new Vec3(Math.max(velocityAdder.x, -maxSpeed), velocityAdder.y, velocityAdder.z));
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 2
            )
    )
    private void wrapAscendingNorth(OldMinecartBehavior minecart, Vec3 velocityAdder, Operation<Void> original) {
        double maxSpeed = getMaxRailSpeed(this.minecart.getInBlockState()) / 20.0D;
        original.call(minecart, new Vec3(velocityAdder.x, velocityAdder.y, Math.max(velocityAdder.z, -maxSpeed)));
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 3
            )
    )
    private void wrapAscendingSouth(OldMinecartBehavior minecart, Vec3 velocityAdder, Operation<Void> original) {
        double maxSpeed = getMaxRailSpeed(this.minecart.getInBlockState()) / 20.0D;
        original.call(minecart, new Vec3(velocityAdder.x, velocityAdder.y, Math.min(velocityAdder.z, maxSpeed)));
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    public void accurateCollisionCheckOnMove(AbstractMinecart entity, MoverType moverType, Vec3 vec32, Operation<Void> original, @Local(ordinal = 0) RailShape railShape) {
        double threshold = 0.3;
        if (vec32.horizontalDistance() < threshold) {
            original.call(entity, moverType, vec32);
            return;
        }

        Vec3 destination = this.minecart.position().add(vec32);
        int i = Mth.floor(destination.x);
        int j = Mth.floor(destination.y);
        int k = Mth.floor(destination.z);
        BlockState destinationBlockState = this.minecart.level().getBlockState(new BlockPos(i, j, k));

        if (destinationBlockState.is(BlockTags.RAILS)) {
            RailShape destinationShape = destinationBlockState.getValue(((BaseRailBlock) destinationBlockState.getBlock()).getShapeProperty());

            boolean isCurve = destinationShape == RailShape.NORTH_EAST ||
                    destinationShape == RailShape.NORTH_WEST ||
                    destinationShape == RailShape.SOUTH_EAST ||
                    destinationShape == RailShape.SOUTH_WEST;

            if (isCurve) {
                vec32 = new Vec3(
                        Math.abs(vec32.x) > 0.01 ? Math.signum(vec32.x) * Math.max(Math.abs(vec32.x), 0.1) : vec32.x,
                        Math.min(vec32.y, 0.0),
                        Math.abs(vec32.z) > 0.01 ? Math.signum(vec32.z) * Math.max(Math.abs(vec32.z), 0.1) : vec32.z
                );
            } else {
                boolean matchesSlope = (destinationShape == RailShape.ASCENDING_EAST && vec32.x > threshold) ||
                        (destinationShape == RailShape.ASCENDING_WEST && vec32.x < -threshold) ||
                        (destinationShape == RailShape.ASCENDING_SOUTH && vec32.z > threshold) ||
                        (destinationShape == RailShape.ASCENDING_NORTH && vec32.z < -threshold);

                if (matchesSlope) {
                    double smoothY = Math.max(vec32.y, Math.abs(vec32.horizontalDistance() * 0.5));
                    vec32 = new Vec3(vec32.x, smoothY, vec32.z);
                }
            }
        }
        original.call(entity, moverType, vec32);
    }
}