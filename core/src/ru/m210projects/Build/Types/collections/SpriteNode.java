package ru.m210projects.Build.Types.collections;

public class SpriteNode {

    private final int index;
    private final ValueSetter valueSetter;

    protected SpriteList parent;
    protected SpriteNode next;
    protected SpriteNode prev;

    protected SpriteNode(int index, ValueSetter valueSetter) {
        this.index = index;
        this.valueSetter = valueSetter;
    }

    public SpriteNode getNext() {
        return next;
    }

    public SpriteNode getPrev() {
        return prev;
    }

    protected SpriteNode link(SpriteList parent, SpriteNode prev, SpriteNode next) {
        this.parent = parent;
        this.next = next;
        this.prev = prev;
        return this;
    }

    protected SpriteList getParent() {
        return parent;
    }

    protected void setValue(int value) {
        this.valueSetter.setValue(index, value);
    }

    public int getIndex() {
        return index;
    }
}
