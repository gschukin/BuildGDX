package ru.m210projects.Build.Types;

import java.util.Objects;

public class HitInfo extends ScanInfo {

    public int x, y, z;

    public HitInfo() {
        x = y = z = -1;
    }

    public void init() {
        this.sector = -1;
        this.wall = -1;
        this.sprite = -1;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public void set(int x, int y, int z, int sector, int wall, int sprite) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.sector = sector;
        this.wall = wall;
        this.sprite = sprite;
    }

    public boolean eq(Hitscan hitInfo) {
        return x == hitInfo.hitx && y == hitInfo.hity && z == hitInfo.hitz && sector == hitInfo.hitsect && wall == hitInfo.hitwall && sprite == hitInfo.hitsprite;
    }
}
