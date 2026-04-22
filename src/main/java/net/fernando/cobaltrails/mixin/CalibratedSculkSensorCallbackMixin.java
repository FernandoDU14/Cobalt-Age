package net.fernando.cobaltrails.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.CalibratedSculkSensorBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.fernando.cobaltrails.block.CobaltWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// NOTA: Il target deve essere la classe interna Callback
@Mixin(targets = "net.minecraft.block.entity.CalibratedSculkSensorBlockEntity$Callback")
public class CalibratedSculkSensorCallbackMixin {

    @Inject(
            method = "getCalibrationFrequency(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)I",
            at = @At("RETURN"),
            cancellable = true,
            remap = true
    )
    private void cobalt_injectCobaltFrequency(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        // 1. Recuperiamo la direzione (lato ametista) come fa il codice originale
        // direction è il lato opposto al FACING del sensore
        Direction direction = state.get(CalibratedSculkSensorBlock.FACING).getOpposite();
        BlockPos inputPos = pos.offset(direction);
        BlockState inputState = world.getBlockState(inputPos);

        // 2. Se lì c'è il nostro cavo di cobalto, leggiamo il suo potere
        if (inputState.getBlock() instanceof CobaltWireBlock) {
            int cobaltPower = inputState.get(CobaltWireBlock.POWER);

            // 3. Se il cobalto ha energia, sovrascriviamo il valore di calibrazione
            // Anche se la Redstone vanilla darebbe 0, il cobalto "vince"
            if (cobaltPower > 0) {
                cir.setReturnValue(cobaltPower);
            }
        }
    }
}