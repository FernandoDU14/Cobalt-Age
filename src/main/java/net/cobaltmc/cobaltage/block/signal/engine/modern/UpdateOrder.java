package net.cobaltmc.cobaltage.block.signal.engine.modern;

import java.util.Locale;
import java.util.function.Consumer;

public enum UpdateOrder {
    HORIZONTAL_FIRST_OUTWARD(new int[][]{{0, 2, 1, 3, 4, 5}, {1, 3, 2, 0, 4, 5}, {2, 0, 3, 1, 4, 5}, {3, 1, 0, 2, 4, 5}, {4, 5, 0, 2, 1, 3}, {5, 4, 0, 2, 1, 3}}, new int[][]{{0, 2, 1, 3, 4, 5}, {1, 3, 2, 0, 4, 5}, {2, 0, 3, 1, 4, 5}, {3, 1, 0, 2, 4, 5}, {4, 5, 0, 2, 1, 3}, {5, 4, 0, 2, 1, 3}}) {
        public void forEachNeighbor(WireHandler.NodeProvider nodes, Node source, int forward, Consumer<Node> action) {
            int rightward;
            int backward;
            int leftward;
            int downward;
            int upward;
            if (forward < 4) {
                rightward = forward + 1 & 3;
                backward = forward + 2 & 3;
                leftward = forward + 3 & 3;
                downward = 4;
                upward = 5;
            } else if (forward == 4) {
                rightward = 0;
                backward = 5;
                leftward = 2;
                downward = 3;
                upward = 1;
            } else {
                rightward = 2;
                backward = 4;
                leftward = 0;
                downward = 1;
                upward = 3;
            }

            Node front = nodes.getNeighbor(source, forward);
            Node right = nodes.getNeighbor(source, rightward);
            Node back = nodes.getNeighbor(source, backward);
            Node left = nodes.getNeighbor(source, leftward);
            Node below = nodes.getNeighbor(source, downward);
            Node above = nodes.getNeighbor(source, upward);
            action.accept(front);
            action.accept(back);
            action.accept(right);
            action.accept(left);
            action.accept(below);
            action.accept(above);
            action.accept(nodes.getNeighbor(front, rightward));
            action.accept(nodes.getNeighbor(back, leftward));
            action.accept(nodes.getNeighbor(front, leftward));
            action.accept(nodes.getNeighbor(back, rightward));
            action.accept(nodes.getNeighbor(front, downward));
            action.accept(nodes.getNeighbor(back, upward));
            action.accept(nodes.getNeighbor(front, upward));
            action.accept(nodes.getNeighbor(back, downward));
            action.accept(nodes.getNeighbor(right, downward));
            action.accept(nodes.getNeighbor(left, upward));
            action.accept(nodes.getNeighbor(right, upward));
            action.accept(nodes.getNeighbor(left, downward));
            action.accept(nodes.getNeighbor(front, forward));
            action.accept(nodes.getNeighbor(back, backward));
            action.accept(nodes.getNeighbor(right, rightward));
            action.accept(nodes.getNeighbor(left, leftward));
            action.accept(nodes.getNeighbor(below, downward));
            action.accept(nodes.getNeighbor(above, upward));
        }
    },
    HORIZONTAL_FIRST_INWARD(new int[][]{{0, 2, 1, 3, 4, 5}, {1, 3, 2, 0, 4, 5}, {2, 0, 3, 1, 4, 5}, {3, 1, 0, 2, 4, 5}, {4, 5, 0, 2, 1, 3}, {5, 4, 0, 2, 1, 3}}, new int[][]{{0, 2, 1, 3, 4, 5}, {1, 3, 2, 0, 4, 5}, {2, 0, 3, 1, 4, 5}, {3, 1, 0, 2, 4, 5}, {4, 5, 0, 2, 1, 3}, {5, 4, 0, 2, 1, 3}}) {
        public void forEachNeighbor(WireHandler.NodeProvider nodes, Node source, int forward, Consumer<Node> action) {
            int rightward;
            int backward;
            int leftward;
            int downward;
            int upward;
            if (forward < 4) {
                rightward = forward + 1 & 3;
                backward = forward + 2 & 3;
                leftward = forward + 3 & 3;
                downward = 4;
                upward = 5;
            } else if (forward == 4) {
                rightward = 0;
                backward = 5;
                leftward = 2;
                downward = 3;
                upward = 1;
            } else {
                rightward = 2;
                backward = 4;
                leftward = 0;
                downward = 1;
                upward = 3;
            }

            Node front = nodes.getNeighbor(source, forward);
            Node right = nodes.getNeighbor(source, rightward);
            Node back = nodes.getNeighbor(source, backward);
            Node left = nodes.getNeighbor(source, leftward);
            Node below = nodes.getNeighbor(source, downward);
            Node above = nodes.getNeighbor(source, upward);
            action.accept(nodes.getNeighbor(front, forward));
            action.accept(nodes.getNeighbor(back, backward));
            action.accept(nodes.getNeighbor(right, rightward));
            action.accept(nodes.getNeighbor(left, leftward));
            action.accept(nodes.getNeighbor(below, downward));
            action.accept(nodes.getNeighbor(above, upward));
            action.accept(nodes.getNeighbor(front, rightward));
            action.accept(nodes.getNeighbor(back, leftward));
            action.accept(nodes.getNeighbor(front, leftward));
            action.accept(nodes.getNeighbor(back, rightward));
            action.accept(nodes.getNeighbor(front, downward));
            action.accept(nodes.getNeighbor(back, upward));
            action.accept(nodes.getNeighbor(front, upward));
            action.accept(nodes.getNeighbor(back, downward));
            action.accept(nodes.getNeighbor(right, downward));
            action.accept(nodes.getNeighbor(left, upward));
            action.accept(nodes.getNeighbor(right, upward));
            action.accept(nodes.getNeighbor(left, downward));
            action.accept(front);
            action.accept(back);
            action.accept(right);
            action.accept(left);
            action.accept(below);
            action.accept(above);
        }
    },
    VERTICAL_FIRST_OUTWARD(new int[][]{{4, 5, 0, 2, 1, 3}, {4, 5, 1, 3, 2, 0}, {4, 5, 2, 0, 3, 1}, {4, 5, 3, 1, 0, 2}, {4, 5, 0, 2, 1, 3}, {4, 5, 0, 2, 1, 3}}, new int[][]{{4, 5, 0, 2, 1, 3}, {4, 5, 1, 3, 2, 0}, {4, 5, 2, 0, 3, 1}, {4, 5, 3, 1, 0, 2}, {4, 5, 0, 2, 1, 3}, {4, 5, 0, 2, 1, 3}}) {
        public void forEachNeighbor(WireHandler.NodeProvider nodes, Node source, int forward, Consumer<Node> action) {
            int rightward;
            int backward;
            int leftward;
            int downward;
            int upward;
            if (forward < 4) {
                rightward = forward + 1 & 3;
                backward = forward + 2 & 3;
                leftward = forward + 3 & 3;
                downward = 4;
                upward = 5;
            } else if (forward == 4) {
                rightward = 0;
                backward = 5;
                leftward = 2;
                downward = 3;
                upward = 1;
            } else {
                rightward = 2;
                backward = 4;
                leftward = 0;
                downward = 1;
                upward = 3;
            }

            Node front = nodes.getNeighbor(source, forward);
            Node right = nodes.getNeighbor(source, rightward);
            Node back = nodes.getNeighbor(source, backward);
            Node left = nodes.getNeighbor(source, leftward);
            Node below = nodes.getNeighbor(source, downward);
            Node above = nodes.getNeighbor(source, upward);
            action.accept(below);
            action.accept(above);
            action.accept(front);
            action.accept(back);
            action.accept(right);
            action.accept(left);
            action.accept(nodes.getNeighbor(below, forward));
            action.accept(nodes.getNeighbor(above, backward));
            action.accept(nodes.getNeighbor(below, backward));
            action.accept(nodes.getNeighbor(above, forward));
            action.accept(nodes.getNeighbor(below, rightward));
            action.accept(nodes.getNeighbor(above, leftward));
            action.accept(nodes.getNeighbor(below, leftward));
            action.accept(nodes.getNeighbor(above, rightward));
            action.accept(nodes.getNeighbor(front, rightward));
            action.accept(nodes.getNeighbor(back, leftward));
            action.accept(nodes.getNeighbor(front, leftward));
            action.accept(nodes.getNeighbor(back, rightward));
            action.accept(nodes.getNeighbor(below, downward));
            action.accept(nodes.getNeighbor(above, upward));
            action.accept(nodes.getNeighbor(front, forward));
            action.accept(nodes.getNeighbor(back, backward));
            action.accept(nodes.getNeighbor(right, rightward));
            action.accept(nodes.getNeighbor(left, leftward));
        }
    },
    VERTICAL_FIRST_INWARD(new int[][]{{4, 5, 0, 2, 1, 3}, {4, 5, 1, 3, 2, 0}, {4, 5, 2, 0, 3, 1}, {4, 5, 3, 1, 0, 2}, {4, 5, 0, 2, 1, 3}, {4, 5, 0, 2, 1, 3}}, new int[][]{{4, 5, 0, 2, 1, 3}, {4, 5, 1, 3, 2, 0}, {4, 5, 2, 0, 3, 1}, {4, 5, 3, 1, 0, 2}, {4, 5, 0, 2, 1, 3}, {4, 5, 0, 2, 1, 3}}) {
        public void forEachNeighbor(WireHandler.NodeProvider nodes, Node source, int forward, Consumer<Node> action) {
            int rightward;
            int backward;
            int leftward;
            int downward;
            int upward;
            if (forward < 4) {
                rightward = forward + 1 & 3;
                backward = forward + 2 & 3;
                leftward = forward + 3 & 3;
                downward = 4;
                upward = 5;
            } else if (forward == 4) {
                rightward = 0;
                backward = 5;
                leftward = 2;
                downward = 3;
                upward = 1;
            } else {
                rightward = 2;
                backward = 4;
                leftward = 0;
                downward = 1;
                upward = 3;
            }

            Node front = nodes.getNeighbor(source, forward);
            Node right = nodes.getNeighbor(source, rightward);
            Node back = nodes.getNeighbor(source, backward);
            Node left = nodes.getNeighbor(source, leftward);
            Node below = nodes.getNeighbor(source, downward);
            Node above = nodes.getNeighbor(source, upward);
            action.accept(nodes.getNeighbor(below, downward));
            action.accept(nodes.getNeighbor(above, upward));
            action.accept(nodes.getNeighbor(front, forward));
            action.accept(nodes.getNeighbor(back, backward));
            action.accept(nodes.getNeighbor(right, rightward));
            action.accept(nodes.getNeighbor(left, leftward));
            action.accept(nodes.getNeighbor(below, forward));
            action.accept(nodes.getNeighbor(above, backward));
            action.accept(nodes.getNeighbor(below, backward));
            action.accept(nodes.getNeighbor(above, forward));
            action.accept(nodes.getNeighbor(below, rightward));
            action.accept(nodes.getNeighbor(above, leftward));
            action.accept(nodes.getNeighbor(below, leftward));
            action.accept(nodes.getNeighbor(above, rightward));
            action.accept(nodes.getNeighbor(front, rightward));
            action.accept(nodes.getNeighbor(back, leftward));
            action.accept(nodes.getNeighbor(front, leftward));
            action.accept(nodes.getNeighbor(back, rightward));
            action.accept(below);
            action.accept(above);
            action.accept(front);
            action.accept(back);
            action.accept(right);
            action.accept(left);
        }
    };

    private final int[][] directNeighbors;
    private final int[][] cardinalNeighbors;

    private UpdateOrder(int[][] directNeighbors, int[][] cardinalNeighbors) {
        this.directNeighbors = directNeighbors;
        this.cardinalNeighbors = cardinalNeighbors;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ENGLISH);
    }

    public static UpdateOrder byId(String id) {
        return valueOf(id.toUpperCase(Locale.ENGLISH));
    }

    public int[] directNeighbors(int forward) {
        return this.directNeighbors[forward];
    }

    public int[] cardinalNeighbors(int forward) {
        return this.cardinalNeighbors[forward];
    }

    public abstract void forEachNeighbor(WireHandler.NodeProvider var1, Node var2, int var3, Consumer<Node> var4);
}