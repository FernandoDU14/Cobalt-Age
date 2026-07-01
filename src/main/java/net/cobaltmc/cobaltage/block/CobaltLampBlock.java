package net.cobaltmc.cobaltage.block;

import com.mojang.serialization.MapCodec;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static net.cobaltmc.cobaltage.block.signal.SignalType.COBALT;

public class CobaltLampBlock extends Block {
    public static final MapCodec<CobaltLampBlock> CODEC = simpleCodec(CobaltLampBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;


    @Override
    public @NonNull MapCodec<CobaltLampBlock> codec() {
        return CODEC;
    }

    public CobaltLampBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(LIT, false)
                .setValue(POWER, 0));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        int i = ((SignalGetterByType)blockPlaceContext.getLevel()).cobaltage$getBestNeighborSignalByType(COBALT, blockPlaceContext.getClickedPos());
        return this.defaultBlockState()
                .setValue(LIT,i > 0)
                .setValue(POWER, i);
    }

    @Override
    protected void neighborChanged(@NonNull BlockState blockState, Level level, @NonNull BlockPos blockPos, @NonNull Block block, @Nullable Orientation orientation, boolean bl) {
        if (!level.isClientSide()) {
            int i = ((SignalGetterByType)level).cobaltage$getBestNeighborSignalByType(COBALT, blockPos);
            level.setBlock(blockPos, blockState.setValue(LIT, i > 0).setValue(POWER, i), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, POWER);
    }
}
