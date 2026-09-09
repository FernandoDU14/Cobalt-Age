package net.cobaltmc.cobaltage.mixin.world.entity.minecart;

import net.cobaltmc.cobaltage.CobaltAgeConfig;
import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.gamerules.CobaltAgeGameRules;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.*;

@Mixin(MinecartBehavior.class)
public abstract class MinecartBehaviorMixin {

    @Mutable
    @Final
    @Shadow
    protected final AbstractMinecart minecart;

    @Shadow public abstract void setPos(double d, double e, double f);

    @Shadow
    public abstract Level level();

    @Unique
    public int getCobaltOrGoldMaxSpeedByRailType(Block block, GameRules gamerules) {
        return switch (block) {
            case Block b when b == ModBlocks.COBALT_RAIL ->
                    gamerules.get(CobaltAgeGameRules.MAX_MINECART_SPEED_COBALT);
            case Block b when b == Blocks.POWERED_RAIL ->
                    gamerules.get(CobaltAgeGameRules.MAX_MINECART_SPEED_GOLD);
            default -> CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS;
        };
    }

    public MinecartBehaviorMixin(AbstractMinecart minecart) {
        this.minecart = minecart;
    }
}