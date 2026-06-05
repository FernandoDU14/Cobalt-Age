package net.fernando.cobaltage.mixin.signal.both;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.world.level.block.LeverBlock.FACE;
import static net.minecraft.world.level.block.LeverBlock.FACING;
import static net.minecraft.world.level.block.LeverBlock.POWERED;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

@Mixin(LeverBlock.class)
public class LeverBlockMixin {

    @Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
    private void cobalt$replaceLeverParticles(BlockState state, Level world, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // If Lever is Active
        if (state.getValue(POWERED) && random.nextFloat() < 0.25F) {

            // 1. Position compute
            Direction facing = state.getValue(FACING);
            AttachFace face = state.getValue(FACE);
            Direction oppositeFacing = facing.getOpposite();
            Direction baseDir = (face == AttachFace.FLOOR) ? Direction.UP : (face == AttachFace.CEILING ? Direction.DOWN : facing);
            Direction horizontalOffsetDir = baseDir.getOpposite();

            double x = (double)pos.getX() + 0.5 + 0.1 * (double)oppositeFacing.getStepX() + 0.2 * (double)horizontalOffsetDir.getStepX();
            double y = (double)pos.getY() + 0.5 + 0.1 * (double)oppositeFacing.getStepY() + 0.2 * (double)horizontalOffsetDir.getStepY();
            double z = (double)pos.getZ() + 0.5 + 0.1 * (double)oppositeFacing.getStepZ() + 0.2 * (double)horizontalOffsetDir.getStepZ();

            // 2. Color selection: 50% red, 50% cobalt
            DustParticleOptions particle;
            int cobaltBlue = (0) | (153 << 8) | 255;
            if (random.nextBoolean()) {
                particle = DustParticleOptions.REDSTONE; // 3. vanilla red
            } else {
                // 4. cobalt particle
                particle = new DustParticleOptions(cobaltBlue, 1.0f);
            }

            world.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);

            // 5. Blocking the original particle spawn
            ci.cancel();
        }
    }
}