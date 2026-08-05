package net.cobaltmc.cobaltage.block.signal.engine.modern;

public class WireConnection {
    final WireNode wire;
    final int iDir;
    final boolean offer;
    final boolean accept;
    WireConnection next;

    WireConnection(WireNode wire, int iDir, boolean offer, boolean accept) {
        this.wire = wire;
        this.iDir = iDir;
        this.offer = offer;
        this.accept = accept;
    }
}
