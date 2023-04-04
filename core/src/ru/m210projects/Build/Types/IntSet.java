package ru.m210projects.Build.Types;

import ru.m210projects.Build.BitMap;

import java.util.ArrayList;
import java.util.List;

public class IntSet {

    private final BitMap bitmap;
    private final List<Integer> array;

    public IntSet() {
        this.bitmap = new BitMap();
        this.array = new ArrayList<>();
    }

    public IntSet(int capacity) {
        this.bitmap = new BitMap(capacity);
        this.array = new ArrayList<>(capacity);
    }

    public int size() {
        return array.size();
    }

    public boolean isEmpty() {
        return array.isEmpty();
    }

    public boolean contains(int value) {
        return bitmap.getBit(value);
    }

    public boolean addValue(int value) {
        if (!bitmap.getBit(value)) {
            bitmap.setBit(value);
            array.add(value);
            return true;
        }
        return false;
    }

    public int getValue(int index) {
        return array.get(index);
    }

    public boolean removeValue(int value) {
        if (bitmap.getBit(value)) {
            array.remove((Integer) value);
            bitmap.clearBit(value);
            return true;
        }
        return false;
    }

    public void clear() {
        array.clear();
        bitmap.clear();
    }

    @Override
    public String toString() {
        return array.toString();
    }
}
