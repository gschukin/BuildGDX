package ru.m210projects.Build.Pattern;

import ru.m210projects.Build.Types.Tile;

import static ru.m210projects.Build.Pattern.BuildFont.nSpace;
import static ru.m210projects.Build.Pragmas.scale;

public class BuildChar {

    public int nTile = -1;
    public short width;
    public short xOffset, yOffset;
    protected final BuildFont parent;

    public BuildChar(BuildFont parent) {
        this.parent = parent;
    }

    public int getWidth() {
        return width;
    }

    public int getWidth(int scale) {
        return scale(getWidth(), scale, 0x10000);
    }

    public int draw(int x, int y, int scale, int shade, int pal, int nBits, boolean shadow) {
        int width = getWidth(scale);
        if (width == 0 || nTile == -1) {
            return 0;
        }

        Tile pic;
        if (nTile != nSpace && (pic = parent.draw.getTile(nTile)) != null && pic.hasSize()) {
            if (shadow) {
                drawChar((x + xOffset + 1), (y + yOffset + 1), scale, 127, 0, nBits);
            }
            drawChar(x + xOffset, y + yOffset, scale, shade, pal, nBits);
        }
        return width;
    }

    protected void drawChar(int x, int y, int scale, int shade, int pal, int nBits) {
        parent.draw.rotatesprite(x << 16, y << 16, scale, 0, nTile, shade, pal, parent.nFlags | nBits);
    }
}
