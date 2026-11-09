package net.cobaltmc.cobaltage.gamerules;

import net.cobaltmc.cobaltage.CobaltAge;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder.IntegerRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

public class CobaltAgeGameRules {
    public static GameRuleCategory RAILS_CATEGORY = GameRuleCategory.register(Identifier.fromNamespaceAndPath("cobaltage", "rail_speeds"));
    public static GameRule<Integer> MAX_MINECART_SPEED_GOLD = createRailGamerule("max_minecart_speed_gold", 8);
    public static GameRule<Integer> MAX_MINECART_SPEED_COBALT = createRailGamerule("max_minecart_speed_cobalt", 24);

    private static GameRule<Integer> createRailGamerule(String name, int defaultValue) {
        return IntegerRuleBuilder.forInteger(defaultValue).range(1, 1000).category(RAILS_CATEGORY)
                .buildAndRegister(Identifier.fromNamespaceAndPath("cobaltage", name));
    }

    public static void registerGameRules() {
        CobaltAge.LOGGER.info("Registering GameRules for cobaltage");
    }
}