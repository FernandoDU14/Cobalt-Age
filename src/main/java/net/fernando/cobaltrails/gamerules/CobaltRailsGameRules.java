package net.fernando.cobaltrails.gamerules;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fernando.cobaltrails.CobaltRails;
import net.fernando.cobaltrails.CobaltRailsConfig;
import net.minecraft.util.Identifier;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleCategory;

public class CobaltRailsGameRules {

    public static GameRuleCategory RAILS_CATEGORY;
    public static GameRule<Integer> MAX_MINECART_SPEED_GOLD;
    public static GameRule<Integer> MAX_MINECART_SPEED_COBALT;

    private static GameRule<Integer> createRailGamerule(String name, int defaultValue) {
        return GameRuleBuilder.IntegerRuleBuilder.forInteger(defaultValue)
                .range(1, 1000)
                .category(RAILS_CATEGORY)
                .buildAndRegister(Identifier.of(CobaltRails.MOD_ID, name));
    }

    static {
        RAILS_CATEGORY = GameRuleCategory.register(Identifier.of(CobaltRails.MOD_ID, "rail_speeds"));
        MAX_MINECART_SPEED_GOLD = createRailGamerule("max_minecart_speed_gold", CobaltRailsConfig.GOLD_SPEED_BPS);
        MAX_MINECART_SPEED_COBALT = createRailGamerule("max_minecart_speed_cobalt", CobaltRailsConfig.COBALT_SPEED_BPS);
    }
}
