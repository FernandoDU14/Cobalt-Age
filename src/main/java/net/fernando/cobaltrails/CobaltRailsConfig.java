package net.fernando.cobaltrails;

public class CobaltRailsConfig {

    public static final int COBALT_SPEED_BPS = 24;
    public static final int MAX_ASCENDING_SPEED_BPS = 10;
    public static final int GOLD_SPEED_BPS = 8;
    public static final int MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS = 24;

    // Do not touch (conversion below)
    private static float blockPerSecondToTick(int blockPerSecondSpeed) {
        return blockPerSecondSpeed / 20.0F;
    }

    public static final float COBALT_SPEED = blockPerSecondToTick(COBALT_SPEED_BPS);
    public static final float MAX_ASCENDING_SPEED = blockPerSecondToTick(MAX_ASCENDING_SPEED_BPS);
    public static final float GOLD_SPEED = blockPerSecondToTick(GOLD_SPEED_BPS);
    public static final float MAX_RAIL_SPEED_NOT_EXPERIMENTAL = blockPerSecondToTick(MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS);
}
