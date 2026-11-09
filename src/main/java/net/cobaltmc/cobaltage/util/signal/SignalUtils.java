package net.cobaltmc.cobaltage.util.signal;

import net.cobaltmc.cobaltage.block.cobalt.CobaltConverterBlock;
import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltSignalSource;
import net.cobaltmc.cobaltage.util.ModTags;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SignalUtils {

    // By "isRedstoneListener" is meant every block state that should be part of Redstone Signal Channel
    // Currently is true when the state not a cobalt emitter (exception for the converter which is the bridge)
    public static boolean isRedstoneListener(BlockState state) {
        if (state.getBlock() instanceof CobaltConverterBlock) {
            return true;
        }
        return !(state.getBlock() instanceof CobaltSignalSource);
    }

    // By "isCobaltListener" is meant every block state that should be part of Cobalt Signal Channel
    // Currently is true when the state is classified as ""
    public static boolean isCobaltListener(BlockState state) {
        return !state.is(ModTags.Blocks.SHOULD_IGNORE_COBALT_SIGNALS);
    }

    // By "shouldRedstoneSourceEmitCobalt" is meant a block state that always emit Redstone Signals,
    // but should always emit also a Cobalt Signal, regardless of the facing the state does have.
    public static boolean shouldRedstoneSourceEmitCobalt(BlockState state) {
        return state.is(ModTags.Blocks.SHOULD_REDSTONE_SOURCE_EMIT_COBALT);
    }

    // By "shouldDirectionalRedstoneSourceEmitCobalt" is meant a block state that always emit Redstone Signals,
    // but should also emit a Cobalt Signal when shouldCobaltWireLinkToDirectionalRedstoneSource is true.
    public static boolean shouldDirectionalRedstoneSourceEmitCobalt(BlockState state) {
        return( state.is(Blocks.OBSERVER) ||
                state.is(Blocks.CALIBRATED_SCULK_SENSOR)
        );
    }

    // By "shouldCobaltWireLinkToDirectionalRedstoneSource" is meant a condition over a Directional Emitters (Observers, Sculk Sensors...)
    // which when it's true, a generic wire can be visually linked to it.
    public static boolean shouldCobaltWireLinkToDirectionalRedstoneSource(BlockState state, Direction askingForLinkDirection) {
        // The Observer emits only in its opposite facing
        // The Calibrated Sculk Sensor does emit in its opposite facing, but it is always connected to it
        return( (state.is(Blocks.OBSERVER) && state.getValue(ObserverBlock.FACING) == askingForLinkDirection) ||
                (state.is(Blocks.CALIBRATED_SCULK_SENSOR))
        );
    }
}