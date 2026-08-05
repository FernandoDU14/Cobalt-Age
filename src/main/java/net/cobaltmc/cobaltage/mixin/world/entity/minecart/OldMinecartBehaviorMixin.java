package net.cobaltmc.cobaltage.mixin.world.entity.minecart;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.cobaltmc.cobaltage.CobaltAgeConfig;
import net.cobaltmc.cobaltage.CobaltAge;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = OldMinecartBehavior.class, priority = 1500)
public abstract class OldMinecartBehaviorMixin extends MinecartBehaviorMixin {

    public OldMinecartBehaviorMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
    public boolean isPoweringRail(BlockState state, Object block, Operation<Boolean> original) {
        if (block == Blocks.POWERED_RAIL) {
            Block unknownRail = state.getBlock();
            return (unknownRail instanceof CobaltRailBlock || unknownRail == Blocks.POWERED_RAIL);
        } else {
            CobaltAge.LOGGER.warn("isOf() Mixin called with something else than Blocks.POWERED_RAIL");
            return original.call(state, block);
        }
    }

    @Unique
    public int getMaxRailSpeed(BlockState blockState) {
        Block block = blockState.getBlock();
        MinecraftServer server = this.level().getServer();
        if (server == null) {
            CobaltAge.LOGGER.error("Could not access to server gamerules ! Please report this bug");
            return CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS;
        }
        GameRules gamerules = server.getGameRules();
        int maxSpeed = getCobaltOrGoldMaxSpeedByRailType(block, gamerules);
        return Integer.min(maxSpeed, CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS);
    }

    @Overwrite
    public double getMaxSpeed(ServerLevel serverLevel) {
        return (this.minecart.isInWater() ? CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL / 2.0 : CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL);
    }

    @Unique
    public double convergeAbs(double speed, double targetSpeed) {
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
                    ordinal = 9))
    public void setVelocityClamp(OldMinecartBehavior minecart, Vec3 velocity, Operation<Void> original) {
        double maxSpeed = getMaxRailSpeed(this.minecart.getInBlockState()) / 20.0F;
        original.call(minecart, new Vec3(convergeAbs(velocity.x, maxSpeed), velocity.y, convergeAbs(velocity.z, maxSpeed)));
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 0))
    public void setVelocityAscendingEast(OldMinecartBehavior minecart, Vec3 velocity_adder, Operation<Void> original) {
        Vec3 velocity = minecart.getDeltaMovement();
        double v_x = velocity_adder.x;
        double v_y = velocity_adder.y;
        double v_z = velocity_adder.z;
        if (velocity.x > CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            v_x = CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }
        original.call(minecart, new Vec3(v_x, v_y, v_z));
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 1))
    public void setVelocityAscendingWest(OldMinecartBehavior minecart, Vec3 velocity_adder, Operation<Void> original) {
        Vec3 velocity = minecart.getDeltaMovement();
        double v_x = velocity_adder.x;
        double v_y = velocity_adder.y;
        double v_z = velocity_adder.z;
        if (velocity.x < - CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            v_x = - CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }
        original.call(minecart, new Vec3(v_x, v_y, v_z));
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 2))
    public void setVelocityAscendingNorth(OldMinecartBehavior minecart, Vec3 velocity_adder, Operation<Void> original) {
        Vec3 velocity = minecart.getDeltaMovement();
        double v_x = velocity_adder.x;
        double v_y = velocity_adder.y;
        double v_z = velocity_adder.z;
        if (velocity.z < - CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            v_z = - CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }
        original.call(minecart, new Vec3(v_x, v_y, v_z));
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 3))
    public void setVelocityAscendingSouth(OldMinecartBehavior minecart, Vec3 velocity_adder, Operation<Void> original) {
        Vec3 velocity = minecart.getDeltaMovement();
        double v_x = velocity_adder.x;
        double v_y = velocity_adder.y;
        double v_z = velocity_adder.z;
        if (velocity.z > CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            v_z = CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }
        original.call(minecart, new Vec3(v_x, v_y, v_z));
    }

    @Unique
    public RailShape getRailShape(BlockState blockState, Property<RailShape> property, Operation<Comparable<?>> original) {
        return (RailShape) original.call(blockState, property);
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;",
                    ordinal = 1))
    public <T extends Comparable<T>> T getMoveAlongTrackMixin(BlockState blockState, Property<RailShape> property, Operation<Comparable<?>> original) {
        return (T) getRailShape(blockState, property, original);
    }

    @WrapOperation(
            method = "getPosOffs",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;",
                    ordinal = 0))
    public <T extends Comparable<T>> T getSnapPositionToRailWithOffsetMixin(BlockState blockState, Property<RailShape> property, Operation<Comparable<?>> original) {
        return (T) getRailShape(blockState, property, original);
    }

    @WrapOperation(
            method = "getPos",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;",
                    ordinal = 0))
    public <T extends Comparable<T>> T getSnapPositionToRailMixin(BlockState blockState, Property<RailShape> property, Operation<Comparable<?>> original) {
        return (T) getRailShape(blockState, property, original);
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    public void accurateCollisionCheckOnMove(AbstractMinecart minecart, MoverType moverType, Vec3 vec32, Operation<Void> original, @Local(ordinal = 0) RailShape railShape) {
        if (vec32.horizontalDistance() < 0.6) {
            original.call(minecart, moverType, vec32);
            return;
        }
        Vec3 destination = this.minecart.position().add(vec32);
        int i = Mth.floor(destination.x);
        int j = Mth.floor(destination.y);
        int k = Mth.floor(destination.z);
        BlockState destinationBlockState = this.level().getBlockState(new BlockPos(i, j, k));
        if (destinationBlockState.is(BlockTags.RAILS)) {
            RailShape destinationShape = destinationBlockState.getValue(((BaseRailBlock) destinationBlockState.getBlock()).getShapeProperty());
            if (destinationShape == RailShape.ASCENDING_EAST && vec32.x > 0.6 ||
                    destinationShape == RailShape.ASCENDING_WEST && vec32.x < -0.6 ||
                    destinationShape == RailShape.ASCENDING_SOUTH && vec32.z > 0.6 ||
                    destinationShape == RailShape.ASCENDING_NORTH && vec32.z < -0.6
            ) {
                this.setPos(this.minecart.getX(), this.minecart.getY() + 1, this.minecart.getZ());
            }
        }
        original.call(minecart, moverType, vec32);
    }
}