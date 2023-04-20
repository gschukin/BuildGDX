package ru.m210projects.Build.Types.collections;

import java.util.NoSuchElementException;

public class MapList {

    private MapNode first;
    private MapNode last;
    private int size;

    public void addFirst(MapNode newNode) {
        final MapNode f = first;
        first = newNode.link(this, null, f);
        if (f == null) {
            last = newNode;
        } else {
            f.prev = newNode;
        }
        size++;
    }

    public void addLast(MapNode newNode) {
        final MapNode l = last;
        last = newNode.link(this, l, null);
        if (l == null) {
            first = newNode;
        } else {
            l.next = newNode;
        }
        size++;
    }

    public MapNode removeFirst() {
        final MapNode f = first;
        if (f == null) {
            throw new NoSuchElementException();
        }
        unlink(f);
        return f;
    }

    public MapNode removeLast() {
        final MapNode l = last;
        if (l == null) {
            throw new NoSuchElementException();
        }
        unlink(l);
        return l;
    }

    public int getSize() {
        return size;
    }

    protected void unlink(MapNode x) {
        x.parent = null;
        final MapNode next = x.next;
        final MapNode prev = x.prev;

        if (prev == null) {
            first = next;
        } else {
            prev.next = next;
            x.prev = null;
        }

        if (next == null) {
            last = prev;
        } else {
            next.prev = prev;
            x.next = null;
        }
        size--;
    }

    public MapNode getFirst() {
        return first;
    }

    public MapNode getLast() {
        return last;
    }

    @Override
    public String toString() {
        if(size == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (MapNode x = first; x != null; x = x.next) {
            sb.append(x.getIndex());
            if (x.next != null) {
                sb.append(',').append(' ');
            }
        }
        sb.append(']');
        return sb.toString();
    }
}

