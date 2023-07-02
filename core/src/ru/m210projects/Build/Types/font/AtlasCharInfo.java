package ru.m210projects.Build.Types.font;

public class AtlasCharInfo extends CharInfo {

    protected float tx1;
    protected float ty1;
    protected float tx2;
    protected float ty2;

    public AtlasCharInfo(Font parent, char ch, int atlasTile, int atlasWidth, int atlasHeight, int cols, int rows) {
        super(parent, atlasTile, (atlasWidth / cols), 0, 0);

        this.width = (short) (atlasWidth / cols);
        int height = atlasHeight / rows;

        this.tx1 = (float) (ch % cols) / cols;
        this.ty1 = (float) (ch / cols) / rows;
        this.tx2 = tx1 + (width / (float) atlasWidth);
        this.ty2 = ty1 + (height / (float) atlasHeight);
    }

    @Override
    public int getHeight() {
        return super.getCellSize();
    }

    public float getTx1() {
        return tx1;
    }

    public float getTy1() {
        return ty1;
    }

    public float getTx2() {
        return tx2;
    }

    public float getTy2() {
        return ty2;
    }
}
