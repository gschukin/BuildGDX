package ru.m210projects.Build.Types;

import java.util.concurrent.atomic.AtomicInteger;

public class Variable extends AtomicInteger {

    public Variable(int initialValue) {
        super(initialValue);
    }

    public Variable() {
    }

    public boolean isBigger(int value) {
        return get() > value;
    }

    public boolean isSmaller(int value) {
        return get() < value;
    }

}
