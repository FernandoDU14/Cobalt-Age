package net.fernando.cobaltage.mixin;

import net.fernando.cobaltage.util.signal.SignalType;
import net.fernando.cobaltage.util.signal.SignalTypeBlockStateExtensions;
import net.fernando.cobaltage.util.signal.SignalTypeLevelExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Level.class)
public abstract class LevelMixin implements SignalTypeLevelExtensions {

    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    @Unique
    private static final ThreadLocal<BlockPos.MutableBlockPos> STATIC_MUTABLE = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    @Override
    public int getSignalByType(SignalType type, BlockPos pos, Direction dir) {
        BlockState state = this.getBlockState(pos);
        return ((SignalTypeBlockStateExtensions) state).getSignalByType(type, (Level) (Object) this, pos, dir);
    }

    @Override
    public int getDirectSignalByType(SignalType type, BlockPos pos, Direction dir) {
        BlockState state = this.getBlockState(pos);
        return ((SignalTypeBlockStateExtensions) state).getDirectSignalByType(type, (Level) (Object) this, pos, dir);
    }

    @Override
    public boolean hasSignalByType(SignalType type, BlockPos pos, Direction dir) {
        return this.getSignalByType(type, pos, dir) > 0;
    }

    @Override
    public boolean hasNeighbourSignalByType(SignalType type, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = STATIC_MUTABLE.get();
        if (this.getSignalByType(type, mutable.setWithOffset(pos, Direction.DOWN), Direction.DOWN) > 0) {
            return true;
        } else if (this.getSignalByType(type, mutable.setWithOffset(pos, Direction.UP), Direction.UP) > 0) {
            return true;
        } else if (this.getSignalByType(type, mutable.setWithOffset(pos, Direction.NORTH), Direction.NORTH) > 0) {
            return true;
        } else if (this.getSignalByType(type, mutable.setWithOffset(pos, Direction.SOUTH), Direction.SOUTH) > 0) {
            return true;
        } else if (this.getSignalByType(type, mutable.setWithOffset(pos, Direction.WEST), Direction.WEST) > 0) {
            return true;
        } else {
            return this.getSignalByType(type, mutable.setWithOffset(pos, Direction.EAST), Direction.EAST) > 0;
        }
    }
}