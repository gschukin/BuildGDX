package ru.m210projects.Build.Types;


import com.badlogic.gdx.utils.Pool;

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
    private final Pool<ListItr> iterators;


    public LinkedMap(int listCount, int listCapacity) {
        poolIndex = listCount;
        first = new int[poolIndex + 1];
        size = new int[poolIndex + 1];
        prev = new int[listCapacity];
        next = new int[listCapacity];
        Arrays.fill(first, -1);
        iterators = new Pool<ListItr>() {
            @Override
            protected ListItr newObject() {
                return new ListItr();
            }
        };
        fill(0);
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

    public void toArrays(short[] head, short[] next, short[] prev) {
        for (int i = 0; i < Math.min(first.length, head.length); i++) {
            head[i] = (short) first[i];
        }
        for (int i = 0; i < Math.min(this.next.length, next.length); i++) {
            next[i] = (short) this.next[i];
        }
        for (int i = 0; i < Math.min(this.prev.length, prev.length); i++) {
            prev[i] = (short) this.prev[i];
        }
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
            if (i == first.length - 1) {
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

    private class ListItr implements ListIterator<Integer>, Iterable<Integer>, Pool.Poolable {

        private int current = -1;
        private Runnable func;
        private int listIndex = 0, value;

        private final Runnable nextFunc = () -> current = next[current];
        private final Runnable firstFunc = () -> {
            current = first[value];
            func = nextFunc;
        };

        @Override
        public Iterator<Integer> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            boolean hasNext = listIndex < size[value];
            if (!hasNext) {
                iterators.free(this);
            }
            return hasNext;
        }

        @Override
        public Integer next() {
            func.run();
            listIndex++;
            return current;
        }

        @Override
        public boolean hasPrevious() {
            return current != -1 && prev[current] != -1;
        }

        @Override
        public Integer previous() {
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

        @Override
        public void reset() {
            current = -1;
            func = null;
            listIndex = 0;
            value = -1;
        }
    }

    public Iterable<Integer> getIndicesOf(int value) {
        ListItr itr = iterators.obtain();
        itr.value = value;
        itr.listIndex = 0;
        itr.func = itr.firstFunc;
        return itr;
    }

}
