package net.fernando.cobaltage.mixin.particles;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.block.LeverBlock.FACE;
import static net.minecraft.block.LeverBlock.FACING;
import static net.minecraft.block.LeverBlock.POWERED;

@Mixin(LeverBlock.class)
public class LeverBlockMixin {

    @Inject(method = "randomDisplayTick", at = @At("HEAD"), cancellable = true)
    private void cobalt$replaceLeverParticles(BlockState state, World world, BlockPos pos, Random random, CallbackInfo ci) {
        // Se la leva è attiva
        if (state.get(POWERED) && random.nextFloat() < 0.25F) {

            // 1. Calcolo posizione (copiato dalla logica vanilla per precisione millimetrica)
            Direction facing = state.get(FACING);
            BlockFace face = state.get(FACE);
            Direction oppositeFacing = facing.getOpposite();
            Direction baseDir = (face == BlockFace.FLOOR) ? Direction.UP : (face == BlockFace.CEILING ? Direction.DOWN : facing);
            Direction horizontalOffsetDir = baseDir.getOpposite();

            double x = (double)pos.getX() + 0.5 + 0.1 * (double)oppositeFacing.getOffsetX() + 0.2 * (double)horizontalOffsetDir.getOffsetX();
            double y = (double)pos.getY() + 0.5 + 0.1 * (double)oppositeFacing.getOffsetY() + 0.2 * (double)horizontalOffsetDir.getOffsetY();
            double z = (double)pos.getZ() + 0.5 + 0.1 * (double)oppositeFacing.getOffsetZ() + 0.2 * (double)horizontalOffsetDir.getOffsetZ();

            // 2. Selezione Colore: 50% Rosso, 50% Blu Cobalto
            DustParticleEffect particle;
            int cobaltBlue = (0) | (153 << 8) | 255;
            if (random.nextBoolean()) {
                particle = DustParticleEffect.DEFAULT; // Rosso Vanilla
            } else {
                // 4. Creiamo la tua particella personalizzata
                particle = new DustParticleEffect(cobaltBlue, 1.0f);
            }

            world.addParticleClient(particle, x, y, z, 0.0, 0.0, 0.0);

            // 3. CANCELLIAMO l'esecuzione del metodo originale
            // In questo modo le particelle vanilla non vengono generate due volte
            ci.cancel();
        }
    }
}