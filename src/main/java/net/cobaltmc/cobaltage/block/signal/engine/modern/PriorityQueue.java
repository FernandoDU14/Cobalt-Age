//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.cobaltmc.cobaltage.block.signal.engine.modern;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Iterator;
import org.jspecify.annotations.NonNull;

public class PriorityQueue extends AbstractQueue<Node> {
    private static final int OFFSET = 0;
    private final Node[] tails = new Node[16];
    private Node head;
    private Node tail;
    private int size;

    PriorityQueue() {
    }

    public boolean offer(Node node) {
        if (node == null) {
            throw new NullPointerException();
        } else {
            int priority = node.priority();
            if (this.contains(node)) {
                if (node.priority == priority) {
                    return false;
                }

                this.move(node, priority);
            } else {
                this.insert(node, priority);
            }

            return true;
        }
    }

    public Node poll() {
        if (this.head == null) {
            return null;
        } else {
            Node node = this.head;
            Node next = node.next_node;
            if (next == null) {
                this.clear();
            } else {
                if (node.priority != next.priority) {
                    this.tails[node.priority + 0] = null;
                }

                node.next_node = null;
                next.prev_node = null;
                this.head = next;
                --this.size;
            }

            return node;
        }
    }

    public Node peek() {
        return this.head;
    }

    public void clear() {
        Node n;
        for(Node node = this.head; node != null; n.next_node = null) {
            n = node;
            node = node.next_node;
            n.prev_node = null;
        }

        Arrays.fill(this.tails, (Object)null);
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    public @NonNull Iterator<Node> iterator() {
        throw new UnsupportedOperationException();
    }

    public int size() {
        return this.size;
    }

    public boolean contains(Node node) {
        return node == this.head || node.prev_node != null;
    }

    private void move(Node node, int priority) {
        this.remove(node);
        this.insert(node, priority);
    }

    private void remove(Node node) {
        Node prev = node.prev_node;
        Node next = node.next_node;
        if (node == this.tail || node.priority != next.priority) {
            if (node != this.head && node.priority == prev.priority) {
                this.tails[node.priority + 0] = prev;
            } else {
                this.tails[node.priority + 0] = null;
            }
        }

        if (node == this.head) {
            this.head = next;
        } else {
            prev.next_node = next;
        }

        if (node == this.tail) {
            this.tail = prev;
        } else {
            next.prev_node = prev;
        }

        node.prev_node = null;
        node.next_node = null;
        --this.size;
    }

    private void insert(Node node, int priority) {
        node.priority = priority;
        if (this.head == null) {
            this.head = this.tail = node;
        } else if (priority > this.head.priority) {
            this.linkHead(node);
        } else if (priority <= this.tail.priority) {
            this.linkTail(node);
        } else {
            this.linkAfter(this.findPrev(node), node);
        }

        this.tails[priority + 0] = node;
        ++this.size;
    }

    private void linkHead(Node node) {
        node.next_node = this.head;
        this.head.prev_node = node;
        this.head = node;
    }

    private void linkTail(Node node) {
        this.tail.next_node = node;
        node.prev_node = this.tail;
        this.tail = node;
    }

    private void linkAfter(Node prev, Node node) {
        this.linkBetween(prev, node, prev.next_node);
    }

    private void linkBetween(Node prev, Node node, Node next) {
        prev.next_node = node;
        node.prev_node = prev;
        node.next_node = next;
        next.prev_node = node;
    }

    private Node findPrev(Node node) {
        Node prev = null;

        for(int i = node.priority + 0; i < this.tails.length; ++i) {
            prev = this.tails[i];
            if (prev != null) {
                break;
            }
        }

        return prev;
    }
}
