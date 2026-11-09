package net.cobaltmc.cobaltage.block.cobalt;

import com.mojang.serialization.MapCodec;
import net.cobaltmc.cobaltage.block.signal.cobalt.CobaltSignalSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public class CobaltDustBlock extends Block implements CobaltSignalSource {
    public static final MapCodec<CobaltDustBlock> CODEC = simpleCodec(CobaltDustBlock::new);

    public @NonNull MapCodec<CobaltDustBlock> codec() {
        return CODEC;
    }

    public CobaltDustBlock(Properties settings) {
        super(settings);
    }

    public int getCobaltSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return 15;
    }

    protected int getDirectSignal(@NonNull BlockState blockState, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos, @NonNull Direction direction) {
        return 15;
    }

    protected boolean isSignalSource(@NonNull BlockState state) {
        return true;
    }
}