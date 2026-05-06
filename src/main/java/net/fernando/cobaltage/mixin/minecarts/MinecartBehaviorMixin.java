package net.fernando.cobaltage.mixin.minecarts;

import net.fernando.cobaltage.CobaltAgeConfig;
import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.gamerules.CobaltRailsGameRules;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartController;

import net.minecraft.world.rule.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecartController.class)
public abstract class MinecartBehaviorMixin {

    @Shadow
    protected final AbstractMinecartEntity minecart;

    @Shadow public abstract void setPos(double d, double e, double f);

    @Unique
    public int getCobaltOrGoldMaxSpeedByRailType(Block block, GameRules gamerules){
        int answer = CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS;
        if (block == ModBlocks.COBALT_RAIL) {
            answer = gamerules.getValue(CobaltRailsGameRules.MAX_MINECART_SPEED_COBALT);
        } else if (block == Blocks.POWERED_RAIL) {
            answer = gamerules.getValue(CobaltRailsGameRules.MAX_MINECART_SPEED_GOLD);
        }
        return answer;
    }


    public MinecartBehaviorMixin(AbstractMinecartEntity minecart) {
        this.minecart = minecart;
    }
}