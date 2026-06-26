package net.fernando.cobaltage.util.interfaces.mixin.signalgetters;

import net.fernando.cobaltage.block.signal.SignalType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

public interface BlockStateBaseSignalGetterByType {
    int cobaltage$getSignalByType(SignalType type, BlockGetter level, BlockPos pos, Direction dir);
    int cobaltage$getDirectSignalByType(SignalType type, BlockGetter level, BlockPos pos, Direction dir);
}