package ru.m210projects.Build.Types.collections;

import java.util.List;

public abstract class LinkedMap<T> {

    protected final MapList<T>[] basket;
    protected final int poolIndex;
    protected MapNode<T>[] nodeMap;
    protected final List<T> list;
    protected final ValueSetter<T> valueSetter;

    @SuppressWarnings("unchecked")
    public LinkedMap(int maxValue, List<T> list, int initialCount, ValueSetter<T> valueSetter) {
        this.list = list;
        this.poolIndex = maxValue;
        this.basket = new MapList[poolIndex + 1];
        this.valueSetter = valueSetter;

        this.nodeMap = buildNodeArray(initialCount);
        for(int i = 0; i < basket.length; i++) {
            basket[i] = new MapList<T>();
        }

        fill(0);
    }

    /**
     * used in fill method for each new line
     */
    protected abstract T getInstance();

    public int insert(int value) {
        if (value < 0 || value >= basket.length) {
            value = poolIndex;
        }
        return insert(obtain(), value);
    }

    public boolean set(int element, int value) {
        if (value < 0 || value >= basket.length) {
            value = poolIndex;
        }

        if (element < 0 || element >= nodeMap.length) {
            // if spriteList was increased by another sprite map
            if (element < list.size()) {
                increase();
            } else {
                return false;
            }
        }

        MapNode<T> node = nodeMap[element];
        MapList<T> list = node.getParent();
        list.unlink(node);
        insert(node, value);
        return true;
    }

    public boolean remove(int element) {
        if (element < 0) {
            return false;
        }

        if (element >= nodeMap.length) {
            if (element < list.size()) {
                increase();
            } else {
                return false;
            }
        }

        MapNode<T> node = nodeMap[element];
        MapList<T> list = node.getParent();
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

    public MapList<T> get(int value) {
        return basket[value];
    }

    public MapNode<T> getFirst(int value) {
        return basket[value].getFirst();
    }

    protected int insert(MapNode<T> node, int value) {
        final MapList<T> list = basket[value];
        list.addFirst(node);
        setValue(node, value);
        return node.index;
    }

    protected void setValue(MapNode<T> node, int value) {
        valueSetter.setValue(node.get(), value);
    }

    protected MapNode<T> obtain() {
        final MapList<T> list = basket[poolIndex];
        if(list.getSize() == 0) {
            increase();
        }
        return list.removeFirst();
    }

    protected void fill(int from) {
        final MapList<T> list = basket[poolIndex];
        for (int i = from; i < nodeMap.length; i++) {
            MapNode<T> newNode = new MapNode<T>(i) {
                @Override
                public T get() {
                    return LinkedMap.this.list.get(index);
                }
            };
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
        MapNode<T>[] newNodeMap = buildNodeArray(newSize);
        System.arraycopy(nodeMap, 0, newNodeMap, 0, size);

        this.nodeMap = newNodeMap;
        fill(size);
    }

    @SuppressWarnings("unchecked")
    protected MapNode<T>[] buildNodeArray(int size) {
        return new MapNode[Math.max(1, size)];
    }

}
