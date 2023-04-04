package ru.m210projects.Build.Types;

public class NearInfo extends ScanInfo {

    private int distance = -1;

    public void init(int sector, int wall, int sprite, int distance) {
        this.sector = sector;
        this.wall = wall;
        this.sprite = sprite;
        this.distance = distance;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }
}
