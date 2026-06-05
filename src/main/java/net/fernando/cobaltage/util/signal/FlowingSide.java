package net.fernando.cobaltage.util.signal;

import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public enum FlowingSide implements StringRepresentable {
    TOWARDS_COBALT("towards_cobalt"),
    TOWARDS_REDSTONE("towards_redstone"),
    NONE("none");

    private final String name;

    FlowingSide(String name) {
        this.name = name;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.name;
    }
}