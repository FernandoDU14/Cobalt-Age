package net.fernando.cobaltage.block.signal.cobalt;

import net.minecraft.util.Mth;

public final class CobaltColorUtil {
    private CobaltColorUtil() {
    }

    public static int getCobaltColor(int power) {
        float f = (float) power / 15.0F;
        float r = f * 0.1F + 0.1F;
        float g = f * 0.5F + 0.3F;
        float b = f * 1.1F + 0.4F;

        if (f != 0) {
            b += 0.1F;
            g += 0.1F;
        }

        int red = Mth.clamp((int) (r * 255.0F), 0, 255);
        int green = Mth.clamp((int) (g * 255.0F), 0, 255);
        int blue = Mth.clamp((int) (b * 255.0F), 0, 255);

        return red << 16 | green << 8 | blue;
    }
}
