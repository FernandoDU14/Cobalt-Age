package net.fernando.cobaltage.block.signal;

import net.fernando.cobaltage.block.CobaltConverterBlock;
import net.fernando.cobaltage.util.ModTags;
import net.fernando.cobaltage.block.signal.cobalt.CobaltSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SignalUtils {

    // By "canListenRedstone" is meant every block state that should be part of Redstone Signal Channel
    // Currently is true when the state not a cobalt emitter (exception for the converter which is the bridge)
    public static boolean canListenRedstone(BlockState state) {
        if (state.getBlock() instanceof CobaltConverterBlock) {
            return true;
        }
        return !(state.getBlock() instanceof CobaltSource);
    }

    // By "canListenCobalt" is meant every block state that should be part of Cobalt Signal Channel
    // Currently is true when the state is classified as ""
    public static boolean canListenCobalt(BlockState state) {
        return !state.is(ModTags.Blocks.SHOULD_IGNORE_COBALT_SIGNALS);
    }

    // By "isRedstoneEmitterToCobaltSignalNode" is meant a block state that always emit Redstone Signals,
    // but should always emit also a Cobalt Signal, regardless of the facing the state does have.
    public static boolean isRedstoneEmitterToCobaltSignalNode(BlockState state) {
        return state.is(ModTags.Blocks.SHOULD_REDSTONE_SIGNAL_EMITTER_EMIT_ALSO_COBALT_SIGNAL);
    }

    // By "isRedstoneDirectionalEmitterToCobaltSignalNode" is meant a block state that always emit Redstone Signals,
    // but should also emit a Cobalt Signal when shouldCobaltWireConnectToRedstoneDirectionalEmitter is true.
    public static boolean isRedstoneDirectionalEmitterToCobaltSignalNode(BlockState state) {
        return( state.is(Blocks.OBSERVER) ||
                state.is(Blocks.CALIBRATED_SCULK_SENSOR)
        );
    }

    // By "shouldCobaltWireConnectToRedstoneDirectionalEmitter" is meant a condition over a Directional Emitters (Observers, Sculk Sensors...)
    // which when it's true, a generic wire can be visually linked to it.
    public static boolean shouldCobaltWireConnectToRedstoneDirectionalEmitter(BlockState state, Direction askingForLinkDirection) {
        // The Observer emits only in its opposite facing
        // The Calibrated Sculk Sensor does emit in its opposite facing, but it is always connected to it
        return( (state.is(Blocks.OBSERVER) && state.getValue(ObserverBlock.FACING) == askingForLinkDirection) ||
                (state.is(Blocks.CALIBRATED_SCULK_SENSOR))
        );
    }
}