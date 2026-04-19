package net.fernando.cobaltrails.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class CobaltWallTorchBlock extends RedstoneTorchBlock {
    public CobaltWallTorchBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(LIT)) {
            double d = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double e = (double)pos.getY() + 0.7;
            double f = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            // Usiamo Dust blu invece di quella rossa
            int cobaltBlue = (0 << 16) | (153 << 8) | 255;
            DustParticleEffect cobaltDust = new DustParticleEffect(cobaltBlue, 1.0f);

            world.addParticleClient(cobaltDust, d, e, f, 0.0, 0.0, 0.0);
        }
    }
}