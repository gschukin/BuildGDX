package ru.m210projects.Build.Types.collections;

import java.util.List;

public abstract class LinkedMap<T> {

    protected final MapList[] basket;
    protected final int poolIndex;
    protected MapNode[] nodeMap;
    protected final List<T> list;
    protected final ValueSetter<T> valueSetter;

    public LinkedMap(int maxValue, List<T> list, int initialCount, ValueSetter<T> valueSetter) {
        this.list = list;
        this.poolIndex = maxValue;
        this.basket = new MapList[poolIndex + 1];
        this.valueSetter = valueSetter;

        this.nodeMap = new MapNode[initialCount];
        for(int i = 0; i < basket.length; i++) {
            basket[i] = new MapList();
        }

        fill(0);
    }

    protected abstract T getInstance();

    public int insert(int value) {
        if (value < 0 || value >= basket.length - 1) {
            return -1;
        }
        return insert(obtain(), value);
    }

    public boolean set(int element, int value) {
        if (value < 0 || value >= basket.length) {
            return false;
        }

        if (element < 0 || element >= nodeMap.length) {
            // if spriteList was increased by another sprite map
            if (element < list.size()) {
                increase();
            } else {
                return false;
            }
        }

        MapNode node = nodeMap[element];
        MapList list = node.getParent();
        list.unlink(node);
        insert(node, value);
        return true;
    }

    public boolean remove(int element) {
        if (element < 0 || element >= nodeMap.length) {
            if (element < list.size()) {
                increase();
            } else {
                return false;
            }
        }

        MapNode node = nodeMap[element];
        MapList list = node.getParent();
        if(list == basket[poolIndex]) {
            // already deleted
            return false;
        }

        list.unlink(node);
        list = basket[poolIndex];
        list.addFirst(node);
        setValue(node, -1);
        return true;
    }

    public MapList get(int value) {
        return basket[value];
    }

    public MapNode getFirst(int value) {
        return basket[value].getFirst();
    }

    protected int insert(MapNode node, int value) {
        final MapList list = basket[value];
        list.addFirst(node);
        setValue(node, value);
        return node.getIndex();
    }

    protected void setValue(MapNode node, int value) {
        valueSetter.setValue(list.get(node.getIndex()), value);
    }

    protected MapNode obtain() {
        final MapList list = basket[poolIndex];
        if(list.getSize() == 0) {
            increase();
        }
        return list.removeFirst();
    }

    protected void fill(int from) {
        final MapList list = basket[poolIndex];
        for (int i = from; i < nodeMap.length; i++) {
            MapNode newNode = new MapNode(i);
            list.addLast(newNode);
            if (i >= this.list.size()) {
                this.list.add(getInstance());
                setValue(newNode, -1);
            }
            nodeMap[i] = newNode;
        }
    }

    protected void increase() {
        final int size = nodeMap.length;
        final int newSize = (int) (size * 1.5f);
        MapNode[] newNodeMap = new MapNode[newSize];
        System.arraycopy(nodeMap, 0, newNodeMap, 0, size);

        this.nodeMap = newNodeMap;
        fill(size);
    }
}
