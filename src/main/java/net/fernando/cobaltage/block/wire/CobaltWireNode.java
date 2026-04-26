package net.fernando.cobaltage.block.wire;

import net.fernando.cobaltage.block.CobaltWireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class CobaltWireNode extends CobaltNode {

    public int currentPower;
    public int virtualPower;
    public int externalPower;

    public boolean discovered;
    public boolean searched;

    // ⚡ LAG FIX: Traccia la massima energia teorica per delimitare l'esplorazione
    public int potentialPower;
    // ⚡ LAG FIX: Evita di calcolare due volte le fonti esterne
    public boolean externalCalculated;

    // Cache delle connessioni: evita di rileggere il World nella Fase 2
    public final List<CobaltWireNode> connectedWires = new ArrayList<>();



    public CobaltWireNode(BlockPos pos, BlockState state) {
        super(pos, state);
        this.currentPower = state.get(CobaltWireBlock.POWER);
        this.virtualPower = this.currentPower;
        this.externalPower = 0;
        this.potentialPower = 0;
        this.externalCalculated = false;
    }

    @Override
    public boolean isWire() {
        return true;
    }

    @Override
    public CobaltWireNode asWire() {
        return this;
    }
}