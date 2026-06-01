package net.fernando.cobaltage.util.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

public interface SignalTypeBlockStateExtensions {
    // Example usage:
    // rearPos = myPos.offset(direction)
    // ((SignalTypeLevelExtensions) world).getSignalByType(COBALT, rearPos, direction.getOpposite());
    // Takes the energy from the rear pos. Do not forget getOpposite()!
    int getSignalByType(SignalType type, BlockGetter level, BlockPos pos, Direction dir);
    int getDirectSignalByType(SignalType type, BlockGetter level, BlockPos pos, Direction dir);
}