package ru.m210projects.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BitMap {

    private byte[] array;

    public BitMap(int capacity) {
        this.array = new byte[capacity >> 3];
    }

    public BitMap() {
        this.array = new byte[128];
    }

    public void clear() {
        Arrays.fill(array, (byte) 0);
    }

    public boolean getBit(int index) {
        checkIndex(index);
        return (array[index >> 3] & (1 << (index & 7))) != 0;
    }

    public void setBit(int index) {
        checkIndex(index);
        array[index >> 3] |= (1 << (index & 7));
    }

    public void clearBit(int index) {
        checkIndex(index);
        array[index >> 3] &= ~(1 << (index & 7));
    }

    private void checkIndex(int index) {
        int i =  (index >> 3);
        if (i >= array.length) {
            int size = 1;
            while (size <= i) {
                size <<= 1;
            }

            byte[] newArray = new byte[size];
            System.arraycopy(array, 0, newArray, 0, array.length);
            this.array = newArray;
        }
    }

    @Override
    public String toString() {
        List<Integer> list = new ArrayList<>();
        final int size = (array.length << 3);
        for (int i = 0; i < size; i++) {
            if (getBit(i)) {
                list.add(i);
            }
        }
        return list.toString();
    }
}
