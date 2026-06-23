package net.fernando.cobaltage.mixin;

import net.fernando.cobaltage.block.CobaltWireBlock;
import net.fernando.cobaltage.block.ModBlocks;
import net.fernando.cobaltage.block.abstracts.CobaltDiodeBlock;
import net.fernando.cobaltage.util.signal.SignalType;
import net.fernando.cobaltage.util.interfaces.signalgetters.BlockStateBaseSignalGetterByType;
import net.fernando.cobaltage.util.interfaces.signalgetters.SignalGetterByType;
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

    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    @Unique
    private static final ThreadLocal<BlockPos.MutableBlockPos> STATIC_MUTABLE = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    @Unique
    Direction[] DIRECTIONS = Direction.values();

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
    public boolean cobaltage$hasNeighbourSignalByType(SignalType type, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = STATIC_MUTABLE.get();
        if (this.cobaltage$getSignalByType(type, mutable.setWithOffset(pos, Direction.DOWN), Direction.DOWN) > 0) {
            return true;
        } else if (this.cobaltage$getSignalByType(type, mutable.setWithOffset(pos, Direction.UP), Direction.UP) > 0) {
            return true;
        } else if (this.cobaltage$getSignalByType(type, mutable.setWithOffset(pos, Direction.NORTH), Direction.NORTH) > 0) {
            return true;
        } else if (this.cobaltage$getSignalByType(type, mutable.setWithOffset(pos, Direction.SOUTH), Direction.SOUTH) > 0) {
            return true;
        } else if (this.cobaltage$getSignalByType(type, mutable.setWithOffset(pos, Direction.WEST), Direction.WEST) > 0) {
            return true;
        } else {
            return this.cobaltage$getSignalByType(type, mutable.setWithOffset(pos, Direction.EAST), Direction.EAST) > 0;
        }
    }

    @Override
    public int cobaltage$getBestNeighborSignalByType(SignalType type, BlockPos blockPos) {
        int i = 0;
        for(Direction direction : DIRECTIONS) {
            int j = this.cobaltage$getSignalByType(type, blockPos.relative(direction), direction);
            if (j >= 15) {
                return 15;
            }
            if (j > i) {
                i = j;
            }
        }
        return i;
    }

    @Override
    public int cobaltage$getControlInputSignalByType(SignalType type, BlockPos blockPos, Direction direction, boolean bl) {
        BlockState blockState = this.getBlockState(blockPos);
        if(type == SignalType.REDSTONE){ // is REDSTONE
            if (bl) {
                return DiodeBlock.isDiode(blockState) ? this.cobaltage$getDirectSignalByType(type, blockPos, direction) : 0;
            } else if (blockState.is(Blocks.REDSTONE_BLOCK)) {
                return 15;
            } else if (blockState.is(Blocks.REDSTONE_WIRE)) {
                return blockState.getValue(RedStoneWireBlock.POWER);
            } else {
                return blockState.isSignalSource() ? this.cobaltage$getDirectSignalByType(type, blockPos, direction) : 0;
            }
        }else{ // is COBALT
            if (bl) {
                return CobaltDiodeBlock.isDiode(blockState) ? this.cobaltage$getDirectSignalByType(type, blockPos, direction) : 0;
            } else if (blockState.is(ModBlocks.COBALT_DUST_BLOCK)) {
                return 15;
            } else if (blockState.is(ModBlocks.COBALT_DUST)) {
                return blockState.getValue(CobaltWireBlock.POWER);
            } else {
                return this.cobaltage$getDirectSignalByType(type, blockPos, direction);
            }
        }
    }

    @Override
    public int cobaltage$getDirectSignalToByType(SignalType type, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = STATIC_MUTABLE.get();
        int i = 0;
        i = Math.max(i, this.cobaltage$getDirectSignalByType(type, mutable.setWithOffset(pos, Direction.DOWN), Direction.DOWN));
        if (i >= 15) {
            return i;
        } else {
            i = Math.max(i, this.cobaltage$getDirectSignalByType(type, mutable.setWithOffset(pos, Direction.UP), Direction.UP));
            if (i >= 15) {
                return i;
            } else {
                i = Math.max(i, this.cobaltage$getDirectSignalByType(type, mutable.setWithOffset(pos, Direction.NORTH), Direction.NORTH));
                if (i >= 15) {
                    return i;
                } else {
                    i = Math.max(i, this.cobaltage$getDirectSignalByType(type, mutable.setWithOffset(pos, Direction.SOUTH), Direction.SOUTH));
                    if (i >= 15) {
                        return i;
                    } else {
                        i = Math.max(i, this.cobaltage$getDirectSignalByType(type, mutable.setWithOffset(pos, Direction.WEST), Direction.WEST));
                        if (i >= 15) {
                            return i;
                        } else {
                            i = Math.max(i, this.cobaltage$getDirectSignalByType(type, mutable.setWithOffset(pos, Direction.EAST), Direction.EAST));
                            return i >= 15 ? i : i;
                        }
                    }
                }
            }
        }
    }
}