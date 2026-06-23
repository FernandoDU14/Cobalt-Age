package net.fernando.cobaltage.util.signal;

import net.fernando.cobaltage.block.CobaltConverterBlock;
import net.fernando.cobaltage.util.ModTags;
import net.fernando.cobaltage.util.interfaces.cobalt.CobaltEmitter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SignalHelper {

    public static boolean isPartOfRedstoneSignalChannel(BlockState state) {
        if (state.getBlock() instanceof CobaltConverterBlock) {
            return true;
        }
        return !(state.getBlock() instanceof CobaltEmitter);
    }

    public static boolean isPartOfCobaltSignalChannel(BlockState state) {
        return !state.is(ModTags.Blocks.CANT_RECIVE_FROM_COBALT_SIGNAL_CHANNEL);
    }

    public static boolean restrictedCobaltPowerSource(BlockState state) {
        return( state.is(Blocks.OBSERVER) ||
                state.is(Blocks.CALIBRATED_SCULK_SENSOR)
        );
    }

    public static boolean compatibleCobaltPowerSource(BlockState state) {
        return state.is(ModTags.Blocks.CAN_EMIT_IN_COBALT_SIGNAL_CHANNEL);
    }

    public static boolean canRestrictedCobaltPowerSourceConnectTo(BlockState state, Direction askingForLinkDirection) {
        // The Observer emits only in its opposite facing
        // The Calibrated Sculk Sensor does emit in its opposite facing, but it is always connected to it
        return( (state.is(Blocks.OBSERVER) && state.getValue(ObserverBlock.FACING) == askingForLinkDirection) ||
                (state.is(Blocks.CALIBRATED_SCULK_SENSOR))
        );
    }
}