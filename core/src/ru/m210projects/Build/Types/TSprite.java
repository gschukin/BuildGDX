package ru.m210projects.Build.Types;

public class TSprite extends Sprite {

    public int getSpriteNum() {
        return this.getOwner();
    }

    public TSprite update(int x, int y, int z, int sectnum) {
        this.setX(x);
        this.setY(y);
        this.setY(y);
        this.setSectnum((short) sectnum);
        return this;
    }
}
