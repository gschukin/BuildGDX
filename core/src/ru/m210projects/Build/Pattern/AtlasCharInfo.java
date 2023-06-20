package ru.m210projects.Build.Pattern;

import ru.m210projects.Build.Types.Tile;

import static ru.m210projects.Build.Gameutils.*;
import static ru.m210projects.Build.Pragmas.scale;

public class AtlasCharInfo extends BuildChar {

    protected final int cols;
    protected final int rows;
    protected final int atlasx;
    protected final int atlasy;

    public AtlasCharInfo(BuildFont parent, int atlas, int atlasCols, int atlasRows, char ch) {
        super(parent);

        this.nTile = atlas;
        this.cols = atlasCols;
        this.rows = atlasRows;

        Tile tile = parent.draw.getTile(atlas);
        this.width = (short) (tile.getWidth() / cols);
        int height = tile.getHeight() / rows;
        this.atlasx = (ch % cols) * width;
        this.atlasy = (ch / cols) * height;
    }

    /**
     * Doesn't work without bit 8, and with bit 1024
     */
    protected void drawChar(int x, int y, int scale, int shade, int pal, int bits) {
        final int aspect = ((bits & 10) == 0 ? ((12 << 16) / 10) : 65536);

        int cx1 = x;
        int cy1 = y;
        int cx2 = cx1 + (scale(width, scale, aspect));
        int cy2 = cy1 + (scale(parent.nHeight, scale, 65536));
        if ((bits & 2) != 0) {
            bits |= 8;

            cx1 = coordsConvertXScaled(cx1, getType(bits));
            cx2 = coordsConvertXScaled(cx2, getType(bits));
            cy1 = coordsConvertYScaled(cy1);
            cy2 = coordsConvertYScaled(cy2);
        }

        int sx = x - scale(atlasx, scale, aspect);
        int sy = y - scale(atlasy, scale, 65536);

        parent.draw.rotatesprite(sx << 16, sy << 16, scale, 0, nTile, shade, pal, 16 | bits, cx1, cy1, cx2, cy2);
    }
}
