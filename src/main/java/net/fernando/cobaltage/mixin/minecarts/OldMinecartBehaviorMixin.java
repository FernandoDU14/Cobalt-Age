package net.fernando.cobaltage.mixin.minecarts;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fernando.cobaltage.CobaltAge;
import net.fernando.cobaltage.CobaltAgeConfig;
import net.fernando.cobaltage.block.ModBlocks;
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
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(OldMinecartBehavior.class)
public abstract class OldMinecartBehaviorMixin extends MinecartBehaviorMixin {

    protected OldMinecartBehaviorMixin(AbstractMinecart minecart) {
        super(minecart);
    }
    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    public boolean isPoweringRail(BlockState state, Block block, Operation<Boolean> original) {
        if (block == Blocks.POWERED_RAIL) {
            Block unknownRail = state.getBlock();
            return (unknownRail == ModBlocks.COBALT_RAIL || unknownRail == Blocks.POWERED_RAIL);
        }
        return original.call(state, block);
    }

    @Unique
    public int getMaxRailSpeed(BlockState blockState) {
        MinecraftServer server = this.minecart.level().getServer();
        if (server == null) {
            CobaltAge.LOGGER.error("Could not access server gamerules! Please report this bug");
            return CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS;
        }
        GameRules gamerules = server.overworld().getGameRules();
        int maxSpeed = getCobaltOrGoldMaxSpeedByRailType(blockState.getBlock(), gamerules);
        return Integer.min(maxSpeed, CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS);
    }

    /**
     * @author Un JaviDP
     * @reason DIPPI DIH (try find me!)
     */
    @Overwrite
    public double getMaxSpeed(ServerLevel serverWorld) {
        return (this.minecart.isInWater() ? CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL / 2.0 : CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL);
    }

    @Unique
    private double convergeAbs(double speed, double targetSpeed) {
        if (Math.abs(speed) > targetSpeed) {
            return Math.signum(speed) * Math.max(Math.abs(speed) * 0.7, targetSpeed);
        } else {
            return speed;
        }
    }

    @Redirect(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 9))
    public void setVelocityClamp(OldMinecartBehavior minecart, Vec3 velocity) {
        double maxSpeed = getMaxRailSpeed(this.minecart.level().getBlockState(this.minecart.blockPosition())) / 20.0F;
        minecart.setDeltaMovement(convergeAbs(velocity.x, maxSpeed), velocity.y, convergeAbs(velocity.z, maxSpeed));
    }

    @Redirect(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 0))
    public void setVelocityAscendingEast(OldMinecartBehavior minecart, Vec3 velocity_adder) {
        Vec3 velocity = minecart.getDeltaMovement();
        double v_x = velocity_adder.x;
        double v_y = velocity_adder.y;
        double v_z = velocity_adder.z;
        if (velocity.x > CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            v_x = CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }
        minecart.setDeltaMovement(v_x, v_y, v_z);
    }

    @Redirect(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 1))
    public void setVelocityAscendingWest(OldMinecartBehavior minecart, Vec3 velocity_adder) {
        Vec3 velocity = minecart.getDeltaMovement();
        double v_x = velocity_adder.x;
        double v_y = velocity_adder.y;
        double v_z = velocity_adder.z;
        if (velocity.x < - CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            v_x = - CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }
        minecart.setDeltaMovement(v_x, v_y, v_z);
    }

    @Redirect(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 2))
    public void setVelocityAscendingNorth(OldMinecartBehavior minecart, Vec3 velocity_adder) {
        Vec3 velocity = minecart.getDeltaMovement();
        double v_x = velocity_adder.x;
        double v_y = velocity_adder.y;
        double v_z = velocity_adder.z;
        if (velocity.z < - CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            v_z = - CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }
        minecart.setDeltaMovement(v_x, v_y, v_z);
    }

    @Redirect(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 3))
    public void setVelocityAscendingSouth(OldMinecartBehavior minecart, Vec3 velocity_adder) {
        Vec3 velocity = minecart.getDeltaMovement();
        double v_x = velocity_adder.x;
        double v_y = velocity_adder.y;
        double v_z = velocity_adder.z;
        if (velocity.z > CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            v_z = CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }
        minecart.setDeltaMovement(v_x, v_y, v_z);
    }

    @Redirect(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    public void accurateCollisionCheckOnMove(AbstractMinecart minecart, MoverType movementType, Vec3 vec3d, @Local(ordinal = 0) RailShape railShape) {
        if (vec3d.horizontalDistance() < 0.6) {
            minecart.move(movementType, vec3d);
            return;
        }
        Vec3 destination = this.minecart.position().add(vec3d);
        int i = Mth.floor(destination.x);
        int j = Mth.floor(destination.y);
        int k = Mth.floor(destination.z);
        BlockState destinationBlockState = this.minecart.level().getBlockState(new BlockPos(i, j, k));
        if (destinationBlockState.is(BlockTags.RAILS)) {
            RailShape destinationShape = destinationBlockState.getValue(((BaseRailBlock) destinationBlockState.getBlock()).getShapeProperty());
            if (destinationShape == RailShape.ASCENDING_EAST && vec3d.x > 0.6 ||
                    destinationShape == RailShape.ASCENDING_WEST && vec3d.x < -0.6 ||
                    destinationShape == RailShape.ASCENDING_SOUTH && vec3d.z > 0.6 ||
                    destinationShape == RailShape.ASCENDING_NORTH && vec3d.z > -0.6
            ) {
                this.setPos(this.minecart.getX(), this.minecart.getY() + 1, this.minecart.getZ());
            }
        }
        minecart.move(movementType, vec3d);
    }
}