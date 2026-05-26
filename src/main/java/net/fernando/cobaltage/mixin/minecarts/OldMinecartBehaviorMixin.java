package net.fernando.cobaltage.mixin.minecarts;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fernando.cobaltage.CobaltAge;
import net.fernando.cobaltage.CobaltAgeConfig;
import net.fernando.cobaltage.block.ModBlocks;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.DefaultMinecartController;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.rule.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DefaultMinecartController.class)
public abstract class OldMinecartBehaviorMixin extends MinecartBehaviorMixin {

    protected OldMinecartBehaviorMixin(AbstractMinecartEntity minecart) {
        super(minecart);
    }
    @WrapOperation(
            method = "moveOnRail",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
    public boolean isPoweringRail(BlockState state, Block block, Operation<Boolean> original) {
        if (block == Blocks.POWERED_RAIL) {
            Block unknownRail = state.getBlock();
            return (unknownRail == ModBlocks.COBALT_RAIL || unknownRail == Blocks.POWERED_RAIL);
        }
        return original.call(state, block);
    }

    @Unique
    public int getMaxRailSpeed(BlockState blockState) {
        MinecraftServer server = this.minecart.getEntityWorld().getServer();
        if (server == null) {
            CobaltAge.LOGGER.error("Could not access server gamerules! Please report this bug");
            return CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS;
        }
        GameRules gamerules = server.getOverworld().getGameRules();
        int maxSpeed = getCobaltOrGoldMaxSpeedByRailType(blockState.getBlock(), gamerules);
        return Integer.min(maxSpeed, CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS);
    }

    /**
     */
    @Overwrite
    public double getMaxSpeed(ServerWorld serverWorld) {
        return (this.minecart.isTouchingWater() ? CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL / 2.0 : CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL);
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
            method = "moveOnRail",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/vehicle/DefaultMinecartController;setVelocity(Lnet/minecraft/util/math/Vec3d;)V",
                    ordinal = 9))
    public void setVelocityClamp(DefaultMinecartController minecart, Vec3d velocity) {
        double maxSpeed = getMaxRailSpeed(this.minecart.getEntityWorld().getBlockState(this.minecart.getBlockPos())) / 20.0F;
        minecart.setVelocity(convergeAbs(velocity.x, maxSpeed), velocity.y, convergeAbs(velocity.z, maxSpeed));
    }

    @Redirect(
            method = "moveOnRail",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/vehicle/DefaultMinecartController;setVelocity(Lnet/minecraft/util/math/Vec3d;)V",
                    ordinal = 0))
    public void setVelocityAscendingEast(DefaultMinecartController minecart, Vec3d velocity_adder) {
        Vec3d velocity = minecart.getVelocity();
        double v_x = velocity_adder.x;
        double v_y = velocity_adder.y;
        double v_z = velocity_adder.z;
        if (velocity.x > CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            v_x = CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }
        minecart.setVelocity(v_x, v_y, v_z);
    }

    @Redirect(
            method = "moveOnRail",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/vehicle/DefaultMinecartController;setVelocity(Lnet/minecraft/util/math/Vec3d;)V",
                    ordinal = 1))
    public void setVelocityAscendingWest(DefaultMinecartController minecart, Vec3d velocity_adder) {
        Vec3d velocity = minecart.getVelocity();
        double v_x = velocity_adder.x;
        double v_y = velocity_adder.y;
        double v_z = velocity_adder.z;
        if (velocity.x < - CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            v_x = - CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }
        minecart.setVelocity(v_x, v_y, v_z);
    }

    @Redirect(
            method = "moveOnRail",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/vehicle/DefaultMinecartController;setVelocity(Lnet/minecraft/util/math/Vec3d;)V",
                    ordinal = 2))
    public void setVelocityAscendingNorth(DefaultMinecartController minecart, Vec3d velocity_adder) {
        Vec3d velocity = minecart.getVelocity();
        double v_x = velocity_adder.x;
        double v_y = velocity_adder.y;
        double v_z = velocity_adder.z;
        if (velocity.z < - CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            v_z = - CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }
        minecart.setVelocity(v_x, v_y, v_z);
    }

    @Redirect(
            method = "moveOnRail",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/vehicle/DefaultMinecartController;setVelocity(Lnet/minecraft/util/math/Vec3d;)V",
                    ordinal = 3))
    public void setVelocityAscendingSouth(DefaultMinecartController minecart, Vec3d velocity_adder) {
        Vec3d velocity = minecart.getVelocity();
        double v_x = velocity_adder.x;
        double v_y = velocity_adder.y;
        double v_z = velocity_adder.z;
        if (velocity.z > CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            v_z = CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }
        minecart.setVelocity(v_x, v_y, v_z);
    }

    @Redirect(
            method = "moveOnRail",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
            )
    )
    public void accurateCollisionCheckOnMove(AbstractMinecartEntity minecart, MovementType movementType, Vec3d vec3d, @Local(ordinal = 0) RailShape railShape) {
        if (vec3d.horizontalLength() < 0.6) {
            minecart.move(movementType, vec3d);
            return;
        }
        Vec3d destination = this.minecart.getEntityPos().add(vec3d);
        int i = MathHelper.floor(destination.x);
        int j = MathHelper.floor(destination.y);
        int k = MathHelper.floor(destination.z);
        BlockState destinationBlockState = this.minecart.getEntityWorld().getBlockState(new BlockPos(i, j, k));
        if (destinationBlockState.isIn(BlockTags.RAILS)) {
            RailShape destinationShape = destinationBlockState.get(((AbstractRailBlock) destinationBlockState.getBlock()).getShapeProperty());
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