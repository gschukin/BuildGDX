package ru.m210projects.Build.Types;

public class ClipInfo {

    private int x, y, z;
    private int sectnum;

    public ClipInfo() {
        x = y = z = -1;
        sectnum = -1;
    }

    public void set(int x, int y, int z, int sectnum) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.sectnum = sectnum;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getSectnum() {
        return sectnum;
    }
}
