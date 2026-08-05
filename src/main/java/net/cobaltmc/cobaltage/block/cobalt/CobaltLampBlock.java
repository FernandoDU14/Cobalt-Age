package net.cobaltmc.cobaltage.block.cobalt;

import com.mojang.serialization.MapCodec;
import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class CobaltLampBlock extends Block {
    public static final MapCodec<CobaltLampBlock> CODEC = simpleCodec(CobaltLampBlock::new);
    public static final BooleanProperty LIT;
    public static final IntegerProperty POWER;

    public @NonNull MapCodec<CobaltLampBlock> codec() {
        return CODEC;
    }

    public CobaltLampBlock(Properties properties) {
        super(properties);
        this.registerDefaultState((this.defaultBlockState().setValue(LIT, false)).setValue(POWER, 0));
    }

    public @Nullable BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        int i = ((SignalGetterByType)blockPlaceContext.getLevel()).cobaltage$getBestNeighborSignalByType(SignalType.COBALT, blockPlaceContext.getClickedPos());
        return (this.defaultBlockState().setValue(LIT, i > 0)).setValue(POWER, i);
    }

    protected void neighborChanged(@NonNull BlockState blockState, Level level, @NonNull BlockPos blockPos, @NonNull Block block, @Nullable Orientation orientation, boolean bl) {
        if (!level.isClientSide()) {
            int i = ((SignalGetterByType)level).cobaltage$getBestNeighborSignalByType(SignalType.COBALT, blockPos);
            level.setBlock(blockPos, (blockState.setValue(LIT, i > 0)).setValue(POWER, i), 2);
        }

    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{LIT, POWER});
    }

    static {
        LIT = BlockStateProperties.LIT;
        POWER = BlockStateProperties.POWER;
    }
}