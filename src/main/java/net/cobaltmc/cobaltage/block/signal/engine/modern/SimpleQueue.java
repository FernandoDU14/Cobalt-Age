package net.cobaltmc.cobaltage.block.signal.engine.modern;

import org.jspecify.annotations.NonNull;

import java.util.AbstractQueue;
import java.util.Iterator;

public class SimpleQueue extends AbstractQueue<WireNode> {
    private WireNode head;
    private WireNode tail;
    private int size;

    SimpleQueue() {
    }

    public boolean offer(WireNode node) {
        if (node == null) {
            throw new NullPointerException();
        } else {
            if (this.tail == null) {
                this.head = this.tail = node;
            } else {
                this.tail.next_wire = node;
                this.tail = node;
            }

            ++this.size;
            return true;
        }
    }

    public WireNode poll() {
        if (this.head == null) {
            return null;
        } else {
            WireNode node = this.head;
            WireNode next = node.next_wire;
            if (next == null) {
                this.head = this.tail = null;
            } else {
                node.next_wire = null;
                this.head = next;
            }

            --this.size;
            return node;
        }
    }

    public WireNode peek() {
        return this.head;
    }

    public void clear() {
        WireNode n;
        for(WireNode node = this.head; node != null; n.next_wire = null) {
            n = node;
            node = node.next_wire;
        }

        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    public @NonNull Iterator<WireNode> iterator() {
        return new SimpleIterator();
    }

    public int size() {
        return this.size;
    }

    private class SimpleIterator implements Iterator<WireNode> {
        private WireNode curr;
        private WireNode next;

        private SimpleIterator() {
            this.next = SimpleQueue.this.head;
        }

        public boolean hasNext() {
            if (this.next == null && this.curr != null) {
                this.next = this.curr.next_wire;
            }

            return this.next != null;
        }

        public WireNode next() {
            this.curr = this.next;
            this.next = this.curr.next_wire;
            return this.curr;
        }
    }
}