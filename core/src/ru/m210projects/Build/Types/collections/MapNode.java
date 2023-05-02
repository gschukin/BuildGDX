package ru.m210projects.Build.Types.collections;

public abstract class MapNode<T> {

    protected final int index;
    protected MapList<T> parent;
    protected MapNode<T> next;
    protected MapNode<T> prev;

    protected MapNode(int index) {
        this.index = index;
    }

    public abstract T get();

    public MapNode<T> getNext() {
        return next;
    }

    public MapNode<T> getPrev() {
        return prev;
    }

    protected MapNode<T> link(MapList<T> parent, MapNode<T> prev, MapNode<T> next) {
        this.parent = parent;
        this.next = next;
        this.prev = prev;
        return this;
    }

    protected MapList<T> getParent() {
        return parent;
    }

    public int getIndex() {
        return index;
    }

}
