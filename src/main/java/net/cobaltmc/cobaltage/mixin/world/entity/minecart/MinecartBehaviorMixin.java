package net.cobaltmc.cobaltage.mixin.world.entity.minecart;

import net.cobaltmc.cobaltage.CobaltAgeConfig;
import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.gamerules.CobaltRailsGameRules;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
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

    @Unique
    public int getCobaltOrGoldMaxSpeedByRailType(Block block, GameRules gamerules){
        int answer = CobaltAgeConfig.MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS;
        if (block == ModBlocks.COBALT_RAIL) {
            answer = gamerules.get(CobaltRailsGameRules.MAX_MINECART_SPEED_COBALT);
        } else if (block == Blocks.POWERED_RAIL) {
            answer = gamerules.get(CobaltRailsGameRules.MAX_MINECART_SPEED_GOLD);
        }
        return answer;
    }


    public MinecartBehaviorMixin(AbstractMinecart minecart) {
        this.minecart = minecart;
    }
}