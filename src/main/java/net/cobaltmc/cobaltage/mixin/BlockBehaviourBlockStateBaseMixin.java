package net.cobaltmc.cobaltage.mixin;

import net.cobaltmc.cobaltage.block.cobalt.CobaltDustBlock;
import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltSignalSource;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.BlockStateBaseSignalGetterByType;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.cobaltmc.cobaltage.util.signal.SignalUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockBehaviourBlockStateBaseMixin implements BlockStateBaseSignalGetterByType {
    @Shadow
    public abstract Block getBlock();

    @Shadow
    public abstract int getSignal(BlockGetter level, BlockPos pos, Direction direction);

    @Shadow
    public abstract int getDirectSignal(BlockGetter level, BlockPos pos, Direction direction);

    @Override
    public int cobaltage$getSignalByType(SignalType type, BlockGetter level, BlockPos pos, Direction dir) {
        BlockState state = (BlockState) (Object) this;
        Block block = this.getBlock();

        return switch (type) {
            case REDSTONE -> {
                boolean canListenRedstoneSignals = SignalUtils.isRedstoneListener(state);
                int signal = 0;

                if (level instanceof Level world) {
                    if (state.isRedstoneConductor(world, pos) || block instanceof CobaltDustBlock) {
                        signal = ((SignalGetterByType) world).cobaltage$getDirectSignalToByType(type, pos, dir.getOpposite());
                    }
                }

                yield (!canListenRedstoneSignals && signal == 0) ? 0 : Math.max(signal, this.getSignal(level, pos, dir));
            }
            case COBALT -> {
                boolean canListenCobaltSignals = SignalUtils.isCobaltListener(state);
                if (!canListenCobaltSignals) {
                    yield 0;
                }

                int cobaltSignal = 0;
                if (level instanceof Level world) {
                    if (state.isRedstoneConductor(world, pos) || block instanceof RedStoneWireBlock) {
                        cobaltSignal = Math.max(cobaltSignal, ((SignalGetterByType) world).cobaltage$getDirectSignalToByType(type, pos, dir.getOpposite()));
                    }

                    if (SignalUtils.shouldRedstoneSourceEmitCobalt(state) || SignalUtils.shouldDirectionalRedstoneSourceEmitCobalt(state)) {
                        cobaltSignal = Math.max(cobaltSignal, this.getSignal(level, pos, dir));
                    }

                    if (block instanceof CobaltSignalSource cobaltSignalSource) {
                        cobaltSignal = Math.max(cobaltSignal, cobaltSignalSource.getCobaltSignal(state, world, pos, dir));
                    }
                }

                yield cobaltSignal;
            }
        };
    }

    @Override
    public int cobaltage$getDirectSignalByType(SignalType type, BlockGetter level, BlockPos pos, Direction dir) {
        BlockState state = (BlockState) (Object) this;
        Block block = this.getBlock();

        return switch (type) {
            case REDSTONE -> SignalUtils.isRedstoneListener(state) ? this.getDirectSignal(level, pos, dir) : 0;
            case COBALT -> {
                if (!SignalUtils.isCobaltListener(state)) {
                    yield 0;
                }

                if (block instanceof CobaltSignalSource cobaltSignalSource && level instanceof Level world) {
                    yield cobaltSignalSource.getDirectCobaltSignal(state, world, pos, dir);
                }

                yield this.getDirectSignal(level, pos, dir);
            }
        };
    }
}