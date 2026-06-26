package net.fernando.cobaltage.mixin.world.level.block.state;

import net.fernando.cobaltage.block.CobaltDustBlock;
import net.fernando.cobaltage.block.signal.cobalt.CobaltSource;
import net.fernando.cobaltage.block.signal.SignalType;
import net.fernando.cobaltage.util.interfaces.mixin.signalgetters.BlockStateBaseSignalGetterByType;
import net.fernando.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
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

import static net.fernando.cobaltage.block.signal.SignalNode.*;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockBehaviourBlockStateBaseMixin implements BlockStateBaseSignalGetterByType {

    @Shadow public abstract Block getBlock();
    @Shadow public abstract int getSignal(BlockGetter level, BlockPos pos, Direction direction);
    @Shadow public abstract int getDirectSignal(BlockGetter level, BlockPos pos, Direction direction);

    @Override
    public int cobaltage$getSignalByType(SignalType type, BlockGetter level, BlockPos pos, Direction dir) {
        BlockState state = (BlockState) (Object) this;
        Block block = this.getBlock();

        boolean isCobaltish = isCobaltSignalNode(state);
        boolean isRedstonish = isRedstoneSignalNode(state);

        return switch (type) {
            case REDSTONE -> {
                int i = 0;
                // Reading the Direct Power from Conductors
                if (level instanceof Level world && (state.isRedstoneConductor(world, pos) || block instanceof CobaltDustBlock)) {
                      i = ((SignalGetterByType) level).cobaltage$getDirectSignalToByType(type, pos);
                }
                // The rest if it can be read
                if (!isRedstonish && i==0) yield 0;
                yield Math.max(i, this.getSignal(level, pos, dir));
            }
            case COBALT -> {
                int i = 0;
                // Reading the Direct Power from Conductors
                if (level instanceof Level world && (state.isRedstoneConductor(world, pos) || block instanceof PoweredBlock)) {
                    i = Math.max(i, ((SignalGetterByType) level).cobaltage$getDirectSignalToByType(type, pos));
                }
                // The rest if it can be read
                if (!isCobaltish && i==0) yield 0;
                if (isRedstoneEmitterToCobaltSignalNode(state) || isRedstoneDirectionalEmitterToCobaltSignalNode(state)) {
                    i = Math.max(i, this.getSignal(level, pos, dir));
                }
                if (block instanceof CobaltSource cobaltSource && level instanceof Level world) {
                    i = Math.max(i, cobaltSource.getCobaltSignal(state, world, pos, dir));
                }
                yield i;
            }
        };
    }

    @Override
    public int cobaltage$getDirectSignalByType(SignalType type, BlockGetter level, BlockPos pos, Direction dir) {
        BlockState state = (BlockState) (Object) this;
        Block block = this.getBlock();

        boolean isCobaltish = isCobaltSignalNode(state);
        boolean isRedstonish = isRedstoneSignalNode(state);

        return switch (type) {
            case REDSTONE -> isRedstonish ? this.getDirectSignal(level, pos, dir) : 0;
            case COBALT -> {
                if (!isCobaltish) yield 0;
                // Direct Cobalt Signal by Cobalt Power Sources
                if (block instanceof CobaltSource cobaltSource && level instanceof Level world) {
                    yield cobaltSource.getDirectCobaltSignal(state, world, pos, dir);
                }
                // Direct Redstone Signal
                yield this.getDirectSignal(level, pos, dir);
            }
        };
    }
}