package ru.m210projects.Build.Types.collections;

public class MapNode {

    private final int index;

    protected MapList parent;
    protected MapNode next;
    protected MapNode prev;

    protected MapNode(int index) {
        this.index = index;
    }

    public MapNode getNext() {
        return next;
    }

    public MapNode getPrev() {
        return prev;
    }

    protected MapNode link(MapList parent, MapNode prev, MapNode next) {
        this.parent = parent;
        this.next = next;
        this.prev = prev;
        return this;
    }

    protected MapList getParent() {
        return parent;
    }

    public int getIndex() {
        return index;
    }
}
