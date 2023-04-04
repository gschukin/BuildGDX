package ru.m210projects.Build.Types;

import java.util.Objects;

public class ScanInfo {

    public int sector = -1, wall = -1, sprite = -1;

    public int getSector() {
        return sector;
    }

    public void setSector(int sector) {
        this.sector = sector;
    }

    public int getWall() {
        return wall;
    }

    public void setWall(int wall) {
        this.wall = wall;
    }

    public int getSprite() {
        return sprite;
    }

    public void setSprite(int sprite) {
        this.sprite = sprite;
    }

}
