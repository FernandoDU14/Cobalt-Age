package net.fernando.cobaltage.util;

import net.fernando.cobaltage.block.CobaltConverterBlock;
import net.fernando.cobaltage.block.CobaltWireBlock;
import net.fernando.cobaltage.block.wire.CobaltPowerSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SignalHelper {

    public static boolean isPartOfRedstoneSignalChannel(BlockState state) {
        if (state.getBlock() instanceof CobaltConverterBlock) {
            return true;
        }
        return !(state.getBlock() instanceof CobaltPowerSource ||
                state.getBlock() instanceof CobaltWireBlock);
    }

    public static boolean isPartOfCobaltSignalChannel(BlockState state) {
        return !state.is(ModTags.Blocks.INCOMPATIBLE_COBALT_SOURCES);
    }

    public static boolean restrictedCobaltPowerSource(BlockState state) {
        // The Observer emits only in its opposite facing
        // The Calibrated Sculk Sensor does emit in its opposite facing
        return( state.is(Blocks.OBSERVER) ||
                state.is(Blocks.CALIBRATED_SCULK_SENSOR)
        );
    }

    public static boolean compatibleCobaltPowerSource(BlockState state) {
        return state.is(ModTags.Blocks.COMPATIBLE_COBALT_SOURCES);
    }

    public static boolean isWirePointingTo(BlockState state, Direction dirToTarget) {
        return switch (dirToTarget) {
            case NORTH -> state.getValue(BlockStateProperties.NORTH_REDSTONE).isConnected();
            case SOUTH -> state.getValue(BlockStateProperties.SOUTH_REDSTONE).isConnected();
            case EAST -> state.getValue(BlockStateProperties.EAST_REDSTONE).isConnected();
            case WEST -> state.getValue(BlockStateProperties.WEST_REDSTONE).isConnected();
            case UP -> state.getValue(BlockStateProperties.UP);
            case DOWN -> state.getValue(BlockStateProperties.DOWN);
        };
    }
}