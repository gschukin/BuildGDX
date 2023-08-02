package ru.m210projects.Build.Types;

public class Point { // BuildVector2 the same
    private int x, y, z;

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Point set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;

        return this;
    }

    public Point set(int x, int y) {
        this.x = x;
        this.y = y;
        this.z = 0;

        return this;
    }

    public boolean equals(int x, int y) {
        return this.x == x && this.y == y;
    }

    public boolean equals(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
    }
}
