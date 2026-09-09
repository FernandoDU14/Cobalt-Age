package net.cobaltmc.cobaltage;

public class CobaltAgeConfig {
    public static final int COBALT_SPEED_BPS = 24;
    public static final int MAX_ASCENDING_SPEED_BPS = 10;
    public static final int GOLD_SPEED_BPS = 8;
    public static final int MAX_RAIL_SPEED_NOT_EXPERIMENTAL_BPS = 500;
    public static final float COBALT_SPEED = blockPerSecondToTick(24);
    public static final float MAX_ASCENDING_SPEED = blockPerSecondToTick(10);
    public static final float GOLD_SPEED = blockPerSecondToTick(8);
    public static final float MAX_RAIL_SPEED_NOT_EXPERIMENTAL = blockPerSecondToTick(24);

    private static float blockPerSecondToTick(int blockPerSecondSpeed) {
        return (float)blockPerSecondSpeed / 20.0F;
    }
}
