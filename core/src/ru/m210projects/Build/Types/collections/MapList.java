package ru.m210projects.Build.Types.collections;

import java.util.NoSuchElementException;

public class MapList<T> {

    private MapNode<T> first;
    private MapNode<T> last;
    private int size;

    public void addFirst(MapNode<T> newNode) {
        final MapNode<T> f = first;
        first = newNode.link(this, null, f);
        if (f == null) {
            last = newNode;
        } else {
            f.prev = newNode;
        }
        size++;
    }

    public void addLast(MapNode<T> newNode) {
        final MapNode<T> l = last;
        last = newNode.link(this, l, null);
        if (l == null) {
            first = newNode;
        } else {
            l.next = newNode;
        }
        size++;
    }

    public MapNode<T> removeFirst() {
        final MapNode<T> f = first;
        if (f == null) {
            throw new NoSuchElementException();
        }
        unlink(f);
        return f;
    }

    public MapNode<T> removeLast() {
        final MapNode<T> l = last;
        if (l == null) {
            throw new NoSuchElementException();
        }
        unlink(l);
        return l;
    }

    public int getSize() {
        return size;
    }

    protected void unlink(MapNode<T> x) {
        x.parent = null;
        final MapNode<T> next = x.next;
        final MapNode<T> prev = x.prev;

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

    public MapNode<T> getFirst() {
        return first;
    }

    public MapNode<T> getLast() {
        return last;
    }

    @Override
    public String toString() {
        if(size == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (MapNode<T> x = first; x != null; x = x.next) {
            sb.append(x.index);
            if (x.next != null) {
                sb.append(',').append(' ');
            }
        }
        sb.append(']');
        return sb.toString();
    }
}

