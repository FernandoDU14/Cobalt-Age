package net.fernando.cobaltrails.mixin.consumers;
import net.fernando.cobaltrails.block.CobaltWireBlock;
import net.fernando.cobaltrails.block.wire.CobaltWireNetwork;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public class PistonBlockMixin {

    @Inject(method = "shouldExtend", at = @At("RETURN"), cancellable = true)
    private void cobalt$checkCobaltPower(RedstoneView world, BlockPos pos, Direction pistonFace, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;

        if (!(world instanceof World actualWorld)) return;

        for (Direction dir : Direction.values()) {
            if (dir == pistonFace) continue;

            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            BlockPos neighborPosUp = pos.offset(dir).up();
            BlockState neighborStateUp = world.getBlockState(neighborPosUp);

            // 1. CONTROLLO DIRETTO (Il vicino del vicino è polvere?)
            if(dir == Direction.EAST || dir == Direction.WEST || dir == Direction.NORTH || dir == Direction.SOUTH){
                if (world.getBlockState(neighborPos.up()).getBlock() instanceof CobaltWireBlock) {
                    if (world.getBlockState(neighborPos.up()).get(CobaltWireBlock.POWER) > 0) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
                else if(world.getBlockState(neighborPos.up().up()).getBlock() instanceof CobaltWireBlock){
                    if (world.getBlockState(neighborPos.up().up()).get(CobaltWireBlock.POWER) > 0) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
            // 2. CONTROLLO ATTRAVERSO BLOCCHI (Strong Power)
            // Se il vicino è PIETRA, chiediamo se c'è Cobalto che la alimenta
            if (neighborState.isSolidBlock(world, neighborPos)) {
                // o se DI FIANCO a neighborPos c'è un repeater, ecc.
                // A. Controlliamo se sopra la pietra c'è polvere (Energia Forte verticale)
                BlockState stateAboveBlock = world.getBlockState(neighborPos.up());
                if (stateAboveBlock.getBlock() instanceof CobaltWireBlock) {
                    if (stateAboveBlock.get(CobaltWireBlock.POWER) > 0) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
                if (CobaltWireNetwork.getStrongPowerFromNeighbors(actualWorld, neighborPos) > 0) {
                    cir.setReturnValue(true);
                    return;
                }
                else{
                    for (Direction dirOfNeighborOfNeighbor : Direction.values()) {
                        if(dirOfNeighborOfNeighbor == Direction.EAST || dirOfNeighborOfNeighbor == Direction.WEST || dirOfNeighborOfNeighbor == Direction.NORTH || dirOfNeighborOfNeighbor == Direction.SOUTH) {
                            BlockPos neighborOfNeighborPos = neighborPos.offset(dirOfNeighborOfNeighbor);
                            BlockState neighborOfNeighborState = world.getBlockState(neighborOfNeighborPos);
                            if (neighborOfNeighborState.getBlock() instanceof CobaltWireBlock){
                                    if (neighborOfNeighborState.get(CobaltWireBlock.POWER)> 0) {
                                        // 2. NUOVO CONTROLLO: Il cavo sta "puntando" verso il blocco di pietra?
                                        // La direzione dalla polvere al blocco è l'opposto di quella dal blocco alla polvere
                                        Direction dirFromDustToBlock = dirOfNeighborOfNeighbor.getOpposite();
                                        boolean isConnected = false;

                                        switch (dirFromDustToBlock) {
                                            case NORTH -> isConnected = neighborOfNeighborState.get(net.minecraft.state.property.Properties.NORTH_WIRE_CONNECTION) != net.minecraft.block.enums.WireConnection.NONE;
                                            case SOUTH -> isConnected = neighborOfNeighborState.get(net.minecraft.state.property.Properties.SOUTH_WIRE_CONNECTION) != net.minecraft.block.enums.WireConnection.NONE;
                                            case EAST  -> isConnected = neighborOfNeighborState.get(net.minecraft.state.property.Properties.EAST_WIRE_CONNECTION) != net.minecraft.block.enums.WireConnection.NONE;
                                            case WEST  -> isConnected = neighborOfNeighborState.get(net.minecraft.state.property.Properties.WEST_WIRE_CONNECTION) != net.minecraft.block.enums.WireConnection.NONE;
                                        }

                                        // Se non è NONE, significa che è un SIDE (croce o linea) e quindi passa l'energia forte
                                        if (isConnected) {
                                            cir.setReturnValue(true);
                                            return;
                                        }
                                    }
                                }
                        }
                    }
                }
            }
            else if(neighborStateUp.isSolidBlock(world, neighborPosUp)){
                BlockState stateAboveBlock = world.getBlockState(neighborPosUp.up());
                if (stateAboveBlock.getBlock() instanceof CobaltWireBlock) {
                    if (stateAboveBlock.get(CobaltWireBlock.POWER) > 0) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }
    }
}