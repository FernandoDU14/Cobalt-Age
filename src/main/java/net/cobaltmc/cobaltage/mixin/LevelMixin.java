package net.cobaltmc.cobaltage.mixin;

import net.cobaltmc.cobaltage.block.ModBlocks;
import net.cobaltmc.cobaltage.block.abstracts.CobaltDiodeBlock;
import net.cobaltmc.cobaltage.block.cobalt.CobaltWireBlock;
import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.BlockStateBaseSignalGetterByType;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Level.class)
public abstract class LevelMixin implements SignalGetterByType {
    @Unique
    private static final ThreadLocal<BlockPos.MutableBlockPos> STATIC_MUTABLE = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    @Unique
    private final Direction[] cobaltage$DIRECTIONS = Direction.values();

    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Override
    public int cobaltage$getSignalByType(SignalType type, BlockPos pos, Direction dir) {
        BlockState state = this.getBlockState(pos);
        return ((BlockStateBaseSignalGetterByType) state).cobaltage$getSignalByType(type, (Level) (Object) this, pos, dir);
    }

    @Override
    public int cobaltage$getDirectSignalByType(SignalType type, BlockPos pos, Direction dir) {
        BlockState state = this.getBlockState(pos);
        return ((BlockStateBaseSignalGetterByType) state).cobaltage$getDirectSignalByType(type, (Level) (Object) this, pos, dir);
    }

    @Override
    public boolean cobaltage$hasSignalByType(SignalType type, BlockPos pos, Direction dir) {
        return this.cobaltage$getSignalByType(type, pos, dir) > 0;
    }

    @Override
    public boolean cobaltage$hasNeighborSignalByType(SignalType type, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = STATIC_MUTABLE.get();
        for (Direction direction : cobaltage$DIRECTIONS) {
            if (this.cobaltage$getSignalByType(type, mutable.setWithOffset(pos, direction), direction) > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int cobaltage$getBestNeighborSignalByType(SignalType type, BlockPos blockPos) {
        int bestSignal = 0;
        for (Direction direction : this.cobaltage$DIRECTIONS) {
            int signal = this.cobaltage$getSignalByType(type, blockPos.relative(direction), direction);
            if (signal >= 15) {
                return 15;
            }
            if (signal > bestSignal) {
                bestSignal = signal;
            }
        }
        return bestSignal;
    }

    @Override
    public int cobaltage$getControlInputSignalByType(SignalType type, BlockPos blockPos, Direction direction, boolean checkDiode) {
        BlockState blockState = this.getBlockState(blockPos);
        if (type == SignalType.REDSTONE) {
            if (checkDiode) {
                return DiodeBlock.isDiode(blockState) ? this.cobaltage$getDirectSignalByType(type, blockPos, direction) : 0;
            } else if (blockState.is(Blocks.REDSTONE_BLOCK)) {
                return 15;
            } else if (blockState.is(Blocks.REDSTONE_WIRE)) {
                return blockState.getValue(RedStoneWireBlock.POWER);
            } else {
                return blockState.isSignalSource() ? this.cobaltage$getDirectSignalByType(type, blockPos, direction) : 0;
            }
        } else if (checkDiode) {
            return CobaltDiodeBlock.isDiode(blockState) ? this.cobaltage$getDirectSignalByType(type, blockPos, direction) : 0;
        } else if (blockState.is(ModBlocks.COBALT_DUST_BLOCK)) {
            return 15;
        } else {
            return blockState.is(ModBlocks.COBALT_DUST) ? blockState.getValue(CobaltWireBlock.POWER) :
                    this.cobaltage$getDirectSignalByType(type, blockPos, direction);
        }
    }

    @Override
    public int cobaltage$getDirectSignalToByType(SignalType type, BlockPos pos, Direction ignoreDirection) {
        BlockPos.MutableBlockPos mutable = STATIC_MUTABLE.get();
        int maxSignal = 0;

        for (Direction direction : cobaltage$DIRECTIONS) {
            if (direction == ignoreDirection) continue;

            int signal = this.cobaltage$getDirectSignalByType(type, mutable.setWithOffset(pos, direction), direction);
            if (signal >= 15) {
                return 15;
            }
            maxSignal = Math.max(maxSignal, signal);
        }

        return maxSignal;
    }
}