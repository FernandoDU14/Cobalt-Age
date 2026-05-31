package net.fernando.cobaltage.mixin;

import net.fernando.cobaltage.block.wire.CobaltPowerSource;
import net.fernando.cobaltage.block.CobaltWireBlock;
import net.fernando.cobaltage.util.SignalType;
import net.fernando.cobaltage.util.SignalTypeBlockStateExtensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static net.fernando.cobaltage.util.SignalHelper.*;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateMixin implements SignalTypeBlockStateExtensions {

    @Shadow public abstract Block getBlock();
    @Shadow public abstract int getSignal(BlockGetter level, BlockPos pos, Direction direction);
    @Shadow public abstract int getDirectSignal(BlockGetter level, BlockPos pos, Direction direction);

    @Unique
    private static final ThreadLocal<BlockPos.MutableBlockPos> STATIC_MUTABLE = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    @Unique
    private int collectDirectSignalFromNeighbors(SignalType type, Level world, BlockPos pos) {
        int maxStrong = 0;
        BlockPos.MutableBlockPos mutable = STATIC_MUTABLE.get();

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = mutable.setWithOffset(pos, direction);
            BlockState neighborState = world.getBlockState(neighborPos);

            int strong = ((SignalTypeBlockStateExtensions) neighborState).getDirectSignalByType(type, world, neighborPos, direction);
            if (strong > maxStrong) {
                maxStrong = strong;
                if (maxStrong >= 15) return 15; // Short-circuit
            }
        }
        return maxStrong;
    }

    @Override
    public int getSignalByType(SignalType type, BlockGetter level, BlockPos pos, Direction dir) {
        BlockState state = (BlockState) (Object) this;

        Block block = this.getBlock();
        boolean isCobaltish = isPartOfCobaltSignalChannel(state);
        boolean isRedstonish = isPartOfRedstoneSignalChannel(state);

        return switch (type) {
            case REDSTONE -> {
                // 0. Simple Redstone Sources
                if (!isRedstonish) yield 0;
                if (level instanceof Level world && state.isRedstoneConductor(world, pos)) {
                    yield collectDirectSignalFromNeighbors(type, world, pos);
                }
                yield this.getSignal(level, pos, dir);
            }
            case COBALT -> {
                if (!isCobaltish) yield 0;
                // A. Cobalt sources
                if (block instanceof CobaltPowerSource cobaltPowerSource && level instanceof Level world) {
                    yield cobaltPowerSource.getCobaltSignal(state, world, pos);
                }
                if(block instanceof CobaltWireBlock cobaltWireBlock && level instanceof Level world ) {
                    yield cobaltWireBlock.getCobaltSignalIfLinked(state, world, pos, dir);
                }
                // B. Compatible or restricted cobalt sources
                if (compatibleCobaltPowerSource(state) || restrictedCobaltPowerSource(state)) {
                    yield this.getSignal(level, pos, dir);
                }
                // C. Direct Signal from Neighbours
                if (level instanceof Level world && (state.isRedstoneConductor(world, pos) || block instanceof PoweredBlock)) {
                    yield collectDirectSignalFromNeighbors(type, world, pos);
                }
                yield 0;
            }
        };
    }

    @Override
    public int getDirectSignalByType(SignalType type, BlockGetter level, BlockPos pos, Direction dir) {
        BlockState state = (BlockState) (Object) this;
        Block block = this.getBlock();

        boolean isCobaltish = isPartOfCobaltSignalChannel(state);
        boolean isRedstonish = isPartOfRedstoneSignalChannel(state);

        return switch (type) {
            case REDSTONE -> isRedstonish ? this.getDirectSignal(level, pos, dir) : 0;
            case COBALT -> {
                if (!isCobaltish) yield 0;
                // Direct Cobalt Signal by Cobalt Power Sources
                if (block instanceof CobaltPowerSource cobaltPowerSource && level instanceof Level world) {
                    yield cobaltPowerSource.getDirectCobaltSignal(state, world, pos, dir.getOpposite());
                }
                // Direct Cobalt Signal by Cobalt Wires
                if (block instanceof CobaltWireBlock cobaltWireBlock && level instanceof Level world) {
                    yield cobaltWireBlock.getDirectCobaltSignalIfLinked(state, world, pos, dir);
                }
                // Direct Redstone Signal
                yield this.getDirectSignal(level, pos, dir);
            }
        };
    }
}