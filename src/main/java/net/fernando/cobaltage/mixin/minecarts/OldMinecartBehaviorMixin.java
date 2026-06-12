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

@Mixin(value = OldMinecartBehavior.class,  priority = 1500)
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
    public int cobaltage$getMaxRailSpeed(BlockState blockState) {
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
     * @reason DIPPI DIH
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

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 9
            )
    )
    private void wrapVelocityClamp(
            OldMinecartBehavior minecart,
            Vec3 velocity,
            Operation<Void> original
    ) {
        double maxSpeed =
                cobaltage$getMaxRailSpeed(
                        this.minecart.level().getBlockState(this.minecart.blockPosition())
                ) / 20.0F;

        original.call(
                minecart,
                new Vec3(
                        convergeAbs(velocity.x, maxSpeed),
                        velocity.y,
                        convergeAbs(velocity.z, maxSpeed)
                )
        );
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 0
            )
    )
    private void wrapAscendingEast( OldMinecartBehavior minecart, Vec3 velocityAdder, Operation<Void> original) {
        Vec3 velocity = minecart.getDeltaMovement();
        double vx = velocityAdder.x;
        if (velocity.x > CobaltAgeConfig.MAX_ASCENDING_SPEED) vx = CobaltAgeConfig.MAX_ASCENDING_SPEED;
        original.call(minecart, new Vec3(vx, velocityAdder.y, velocityAdder.z));
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 1
            )
    )
    private void wrapAscendingWest(
            OldMinecartBehavior minecart,
            Vec3 velocityAdder,
            Operation<Void> original
    ) {
        Vec3 velocity = minecart.getDeltaMovement();

        double vx = velocityAdder.x;

        if (velocity.x < -CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            vx = -CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }

        original.call(
                minecart,
                new Vec3(
                        vx,
                        velocityAdder.y,
                        velocityAdder.z
                )
        );
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 2
            )
    )
    private void wrapAscendingNorth(
            OldMinecartBehavior minecart,
            Vec3 velocityAdder,
            Operation<Void> original
    ) {
        Vec3 velocity = minecart.getDeltaMovement();

        double vz = velocityAdder.z;

        if (velocity.z < -CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            vz = -CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }

        original.call(
                minecart,
                new Vec3(
                        velocityAdder.x,
                        velocityAdder.y,
                        vz
                )
        );
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 3
            )
    )
    private void wrapAscendingSouth(
            OldMinecartBehavior minecart,
            Vec3 velocityAdder,
            Operation<Void> original
    ) {
        Vec3 velocity = minecart.getDeltaMovement();

        double vz = velocityAdder.z;

        if (velocity.z > CobaltAgeConfig.MAX_ASCENDING_SPEED) {
            vz = CobaltAgeConfig.MAX_ASCENDING_SPEED;
        }

        original.call(
                minecart,
                new Vec3(
                        velocityAdder.x,
                        velocityAdder.y,
                        vz
                )
        );
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    private void accurateCollisionCheckOnMove(
            AbstractMinecart minecart,
            MoverType movementType,
            Vec3 movement,
            Operation<Void> original,
            @Local(ordinal = 0) RailShape railShape
    ) {
        if (movement.horizontalDistance() < 0.6) {
            original.call(minecart, movementType, movement);
            return;
        }

        Vec3 destination = this.minecart.position().add(movement);

        int i = Mth.floor(destination.x);
        int j = Mth.floor(destination.y);
        int k = Mth.floor(destination.z);

        BlockState destinationBlockState =
                this.minecart.level().getBlockState(new BlockPos(i, j, k));

        if (destinationBlockState.is(BlockTags.RAILS)) {

            RailShape destinationShape =
                    destinationBlockState.getValue(
                            ((BaseRailBlock) destinationBlockState.getBlock())
                                    .getShapeProperty()
                    );

            if (
                    destinationShape == RailShape.ASCENDING_EAST && movement.x > 0.6 ||
                            destinationShape == RailShape.ASCENDING_WEST && movement.x < -0.6 ||
                            destinationShape == RailShape.ASCENDING_SOUTH && movement.z > 0.6 ||
                            destinationShape == RailShape.ASCENDING_NORTH && movement.z < -0.6
            ) {
                this.setPos(
                        this.minecart.getX(),
                        this.minecart.getY() + 1,
                        this.minecart.getZ()
                );
            }
        }

        original.call(minecart, movementType, movement);
    }
}