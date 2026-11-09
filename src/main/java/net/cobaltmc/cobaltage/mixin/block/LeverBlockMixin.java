package net.cobaltmc.cobaltage.mixin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LeverBlock.class})
public class LeverBlockMixin {
    @Inject(
            method = {"animateTick"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void cobalt$replaceLeverParticles(BlockState state, Level world, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if ((Boolean)state.getValue(LeverBlock.POWERED) && random.nextFloat() < 0.25F) {
            Direction facing = (Direction)state.getValue(LeverBlock.FACING);
            AttachFace face = (AttachFace)state.getValue(LeverBlock.FACE);
            Direction oppositeFacing = facing.getOpposite();
            Direction baseDir = face == AttachFace.CEILING ? Direction.UP : (face == AttachFace.FLOOR ? Direction.DOWN : facing);
            Direction horizontalOffsetDir = baseDir.getOpposite();
            double x = (double)pos.getX() + (double)0.5F + 0.1 * (double)oppositeFacing.getStepX() + 0.2 * (double)horizontalOffsetDir.getStepX();
            double y = (double)pos.getY() + (double)0.5F + 0.1 * (double)oppositeFacing.getStepY() + 0.2 * (double)horizontalOffsetDir.getStepY();
            double z = (double)pos.getZ() + (double)0.5F + 0.1 * (double)oppositeFacing.getStepZ() + 0.2 * (double)horizontalOffsetDir.getStepZ();
            int cobaltBlue = 39423;
            ParticleOptions particle;
            if (random.nextBoolean()) {
                particle = DustParticleOptions.REDSTONE;
            } else {
                particle = new DustParticleOptions(cobaltBlue, 1.0F);
            }

            world.addParticle(particle, x, y, z, (double)0.0F, (double)0.0F, (double)0.0F);
            ci.cancel();
        }

    }
}