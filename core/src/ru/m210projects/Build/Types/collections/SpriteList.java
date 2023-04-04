package ru.m210projects.Build.Types.collections;

import java.util.NoSuchElementException;

public class SpriteList {

    private SpriteNode first;
    private SpriteNode last;
    private int size;

    public void addFirst(SpriteNode newNode) {
        final SpriteNode f = first;
        first = newNode.link(this, null, f);
        if (f == null) {
            last = newNode;
        } else {
            f.prev = newNode;
        }
        size++;
    }

    public void addLast(SpriteNode newNode) {
        final SpriteNode l = last;
        last = newNode.link(this, l, null);
        if (l == null) {
            first = newNode;
        } else {
            l.next = newNode;
        }
        size++;
    }

    public SpriteNode removeFirst() {
        final SpriteNode f = first;
        if (f == null) {
            throw new NoSuchElementException();
        }
        unlink(f);
        return f;
    }

    public SpriteNode removeLast() {
        final SpriteNode l = last;
        if (l == null) {
            throw new NoSuchElementException();
        }
        unlink(l);
        return l;
    }

    public int getSize() {
        return size;
    }

    protected void unlink(SpriteNode x) {
        x.parent = null;
        final SpriteNode next = x.next;
        final SpriteNode prev = x.prev;

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

    public SpriteNode getFirst() {
        return first;
    }

    public SpriteNode getLast() {
        return last;
    }

    @Override
    public String toString() {
        if(size == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (SpriteNode x = first; x != null; x = x.next) {
            sb.append(x.getIndex());
            if (x.next != null) {
                sb.append(',').append(' ');
            }
        }
        sb.append(']');
        return sb.toString();
    }
}

