package net.fernando.cobaltrails.mixin;

import net.fernando.cobaltrails.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin {

    // 1. BLOCCO ESTETICO (I "filetti" rossi/blu)
    @Inject(method = "getRenderConnectionType(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/block/enums/WireConnection;",
            at = @At("HEAD"), cancellable = true)
    private void stopVisualConnection(BlockView world, BlockPos pos, Direction direction, CallbackInfoReturnable<WireConnection> cir) {
        BlockState me = world.getBlockState(pos);
        BlockState target = world.getBlockState(pos.offset(direction));

        if (isMismatched(me, target)) {
            cir.setReturnValue(WireConnection.NONE);
        }
    }



    // 2. BLOCCO LOGICO (L'energia non passa)
    // Intercettiamo il momento in cui un filo legge il potere del vicino.
    // Se i colori sono diversi, facciamo finta che il potere sia 0.
    @Redirect(method = "getWeakRedstonePower",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;get(Lnet/minecraft/state/property/Property;)Ljava/lang/Comparable;"))
    private Comparable<?> hidePowerFromMismatchedWire(BlockState neighborState, Property<?> property, BlockState state, BlockView world, BlockPos pos, Direction direction) {
        // 'neighborState' è il blocco di cui la redstone sta leggendo il potere (l'istanza del Redirect)
        // 'state' è lo stato della polvere che sta eseguendo il controllo (il parametro del metodo originale)

        if (isMismatched(state, neighborState)) {
            return 0; // Se i colori non corrispondono, per il sistema il potere è 0
        }

        // Altrimenti procedi con la lettura normale del potere
        return neighborState.get(property);
    }


    // Funzione di utilità per capire se stiamo mischiando Rosso e Blu
    @Unique
    private boolean isMismatched(BlockState a, BlockState b) {
        boolean aIsBlue = a.isOf(ModBlocks.COBALT_DUST);
        boolean bIsBlue = b.isOf(ModBlocks.COBALT_DUST);
        boolean aIsRed = a.isOf(Blocks.REDSTONE_WIRE);
        boolean bIsRed = b.isOf(Blocks.REDSTONE_WIRE);

        return (aIsBlue && bIsRed) || (aIsRed && bIsBlue);
    }
}