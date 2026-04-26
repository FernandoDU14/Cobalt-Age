package net.fernando.cobaltage.gamerules;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fernando.cobaltage.CobaltAge;
import net.fernando.cobaltage.CobaltAgeConfig;
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
                .buildAndRegister(Identifier.of(CobaltAge.MOD_ID, name));
    }

    static {
        RAILS_CATEGORY = GameRuleCategory.register(Identifier.of(CobaltAge.MOD_ID, "rail_speeds"));
        MAX_MINECART_SPEED_GOLD = createRailGamerule("max_minecart_speed_gold", CobaltAgeConfig.GOLD_SPEED_BPS);
        MAX_MINECART_SPEED_COBALT = createRailGamerule("max_minecart_speed_cobalt", CobaltAgeConfig.COBALT_SPEED_BPS);
    }

    public static void registerGameRules() {
        CobaltAge.LOGGER.info("Registering GameRules for " + CobaltAge.MOD_ID);
    }
}