package ru.m210projects.Build.Types;

public class TSprite extends SPRITE {

    public int getSpriteNum() {
        return this.owner;
    }

    public TSprite update(int x, int y, int z, int sectnum) {
        this.x = x;
        this.y = y;
        this.y = y;
        this.sectnum = (short) sectnum;
        return this;
    }
}
