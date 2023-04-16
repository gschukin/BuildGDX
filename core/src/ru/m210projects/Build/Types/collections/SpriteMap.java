package ru.m210projects.Build.Types.collections;

import ru.m210projects.Build.Types.Sprite;

import java.net.ProtocolException;
import java.util.List;

public class SpriteMap {

    protected final SpriteList[] basket;
    protected final int poolIndex;
    protected SpriteNode[] nodeMap;
    protected final ValueSetter valueSetter;
    protected final List<Sprite> spriteList;

    public SpriteMap(int listCount, List<Sprite> spriteList, int spriteCount, MapType type) {
        this.spriteList = spriteList;
        this.poolIndex = listCount;
        this.basket = new SpriteList[poolIndex + 1];

        this.nodeMap = new SpriteNode[spriteCount];
        for(int i = 0; i < basket.length; i++) {
            basket[i] = new SpriteList();
        }

        switch (type) {
            case SECTOR_SETTER:
                this.valueSetter = (index, val) -> spriteList.get(index).setSectnum(val);
                break;
            case STATUS_SETTER:
                this.valueSetter = (index, val) -> spriteList.get(index).setStatnum(val);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        fill(0);
    }

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
            if (element < spriteList.size()) {
                increase();
            } else {
                return false;
            }
        }

        SpriteNode node = nodeMap[element];
        SpriteList list = node.getParent();
        list.unlink(node);
        insert(node, value);
        return true;
    }

    public boolean remove(int element) {
        if (element < 0 || element >= nodeMap.length) {
            if (element < spriteList.size()) {
                increase();
            } else {
                return false;
            }
        }

        SpriteNode node = nodeMap[element];
        SpriteList list = node.getParent();
        if(list == basket[poolIndex]) {
            // already deleted
            return false;
        }

        list.unlink(node);
        list = basket[poolIndex];
        list.addFirst(node);
        setValue(node, poolIndex);
        return true;
    }

    public SpriteList get(int value) {
        return basket[value];
    }

    public SpriteNode getFirst(int value) {
        return basket[value].getFirst();
    }

    protected int insert(SpriteNode node, int value) {
        final SpriteList list = basket[value];
        list.addFirst(node);
        setValue(node, value);
        return node.getIndex();
    }

    protected void setValue(SpriteNode node, int value) {
        node.setValue(value);
    }

    protected SpriteNode obtain() {
        final SpriteList list = basket[poolIndex];
        if(list.getSize() == 0) {
            increase();
        }
        return list.removeFirst();
    }

    protected void fill(int from) {
        final SpriteList list = basket[poolIndex];
        for (int i = from; i < nodeMap.length; i++) {
            SpriteNode newNode = new SpriteNode(i, valueSetter);
            list.addLast(newNode);
            if (i >= spriteList.size()) {
                spriteList.add(newInstance());
                setValue(newNode, poolIndex);
            }
            nodeMap[i] = newNode;
        }
    }

    protected void increase() {
        final int size = nodeMap.length;
        final int newSize = (int) (size * 1.5f);
        SpriteNode[] newNodeMap = new SpriteNode[newSize];
        System.arraycopy(nodeMap, 0, newNodeMap, 0, size);

        this.nodeMap = newNodeMap;
        fill(size);
    }

    protected Sprite newInstance() {
        return new Sprite();
    }
}
