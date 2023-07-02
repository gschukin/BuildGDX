package ru.m210projects.Build.Types.font;

public class CharInfo {

    public int tile = -1;
    public short width; // symbol width
    public short cellSize; // width + gap
    public short xOffset, yOffset;
    protected final Font parent;

    public CharInfo(Font parent, int tile, int cellSize) {
        this(parent, tile, cellSize, 0, 0);
    }

    public CharInfo(Font parent, int tile, int cellSize, int xOffset, int yOffset) {
        this.parent = parent;

        this.tile = tile;
        this.xOffset = (short) xOffset;
        this.yOffset = (short) yOffset;
        this.cellSize = (short) cellSize;
        if (tile != -1) {
            this.width = (short) parent.engine.getTile(tile).getWidth();
        } else {
            this.width = (short) cellSize;
        }
    }

    public Font getParent() {
        return parent;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return parent.engine.getTile(tile).getHeight();
    }

    public short getCellSize() {
        return cellSize;
    }

    public int getTile() {
        return tile;
    }
}
