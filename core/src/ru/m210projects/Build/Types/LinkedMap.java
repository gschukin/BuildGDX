package ru.m210projects.Build.Types;

import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.StringJoiner;

public abstract class LinkedMap {

    protected final int[] first;
    protected final int[] size;
    protected int[] prev;
    protected int[] next;
    protected final int poolIndex;
    protected final ListItr itr;

    public LinkedMap(int listCount, int listCapacity) {
        poolIndex = listCount;
        first = new int[poolIndex + 1];
        size = new int[poolIndex + 1];
        prev = new int[listCapacity];
        next = new int[listCapacity];
        Arrays.fill(first, -1);
        fill(0);

        itr = new ListItr();
    }

    protected abstract void put(int element, int value);

    protected abstract int get(int element);

    protected int link(int element, int value) {
        int f = first[value];
        next[element] = f;
        first[value] = element;
        if (f >= 0) {
            prev[f] = element;
        }
        put(element, value);
        size[value]++;
        return element;
    }

    protected void unlink(int element) {
        final int next = this.next[element];
        final int prev = this.prev[element];
        final int value = get(element);

        if (prev == -1) {
            this.first[value] = next;
        } else {
            this.next[prev] = next;
            this.prev[element] = -1;
        }

        if (next >= 0) {
            this.prev[next] = prev;
            this.next[element] = -1;
        }
        size[value]--;
    }

    public int insert(int element, int value) {
        if (value < 0 || value >= first.length) {
            return -1;
        }

        if (element < 0 || element >= next.length) {
            return -1;
        }

        int f = first[value];
        next[element] = -1;
		if (f >= 0) {
            prev[element] = prev[f];
            next[prev[f]] = element;
            prev[f] = element;
		} else {
			prev[element] = element;
			first[value] = element;
		}

        size[value]++;
        return element;
    }

    public int insert(int value) {
        if (value < 0 || value >= first.length - 1) {
            return -1;
        }
        return link(obtain(), value);
    }

    public boolean remove(int element) {
        if (element < 0 || element >= next.length || get(element) == poolIndex) {
            return false;
        }
        unlink(element);
        link(element, poolIndex);
        return true;
    }

    public boolean set(int element, int value) {
        if (value < 0 || (value > first.length)) {
            return false;
        }

        if (get(element) != value) {
            unlink(element);
            link(element, value);
        }

        return true;
    }

    public int getFree() {
        int element = first[poolIndex];
        if (element == -1) {
            element = increase();
        }
        return element;
    }

    protected void fill(int from) {
        int size = next.length;
        for (int i = from; i < size; i++) {
            prev[i] = (i - 1);
            next[i] = (i + 1);
        }
        prev[from] = -1;
        next[size - 1] = -1;
        first[poolIndex] = from;
        this.size[poolIndex] = size;
    }

    protected int increase() {
        int size = next.length;
        int newSize = size << 1;
        int[] newNext = new int[newSize];
        int[] newPrev = new int[newSize];
        System.arraycopy(next, 0, newNext, 0, size);
        System.arraycopy(prev, 0, newPrev, 0, size);

        this.next = newNext;
        this.prev = newPrev;
        fill(size);

        return size;
    }

    protected int obtain() {
        int element = getFree();

        final int next = this.next[element];
        first[poolIndex] = next;
        if (next >= 0) {
            prev[next] = -1;
        }
        prev[element] = -1;
        size[poolIndex]--;
        return element;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < first.length; i++) {
            if (first[i] == -1) {
                continue;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("map[");
            if(i == first.length - 1) {
                sb.append("free");
            } else {
                sb.append(i);
            }
            sb.append("].size(");
            sb.append(size[i]);
            sb.append("): ");

            sb.append('[');
            int index = first[i];
            for (; ; ) {
                sb.append(index);
                if (next[index] == -1) {
                    break;
                }

                if (next[index] == index) {
                    sb.append("List error at index ").append(index);
                    System.err.println("List error at index " + index);
                    break;
                }

                sb.append(',').append(' ');
                index = next[index];
            }
            sb.append(']');
            joiner.add(sb.toString());
        }

        return joiner.toString();
    }

    private class ListItr implements ListIterator<Integer>, Iterable<Integer> {

        private int index = -1, current = -1;

        @Override
        public Iterator<Integer> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return index >= 0;
        }

        @Override
        public Integer next() {
            current = index;
            index = next[current];
            return current;
        }

        @Override
        public boolean hasPrevious() {
            return current != -1 && prev[current] != -1;
        }

        @Override
        public Integer previous() {
            index = current;
            current = prev[current];
            return current;
        }

        @Override
        public int nextIndex() {
            return next[current];
        }

        @Override
        public int previousIndex() {
            return prev[current];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(Integer value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(Integer value) {
            throw new UnsupportedOperationException();
        }
    }

    public Iterable<Integer> getIndicesOf(int value) {
        itr.index = first[value];
        itr.current = -1;
        return itr;
    }

}
