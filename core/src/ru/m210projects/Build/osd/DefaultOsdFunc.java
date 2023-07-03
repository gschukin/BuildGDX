package ru.m210projects.Build.osd;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.EngineUtils;
import ru.m210projects.Build.Types.Tile;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;

import static ru.m210projects.Build.Pragmas.divscale;
import static ru.m210projects.Build.Pragmas.mulscale;
import static ru.m210projects.Build.RenderService.xdim;
import static ru.m210projects.Build.RenderService.ydim;
import static ru.m210projects.Build.Strhandler.toCharArray;

public class DefaultOsdFunc implements OsdFunc {

    protected final Engine engine;
    protected final char[] charbuf = new char[1];
    protected int BGTILE = -1;
    protected int BGCTILE = -1;
    protected int PALETTE;
    protected int BORDTILE = -1;
    protected int BITSTH = 1 + 32 + 8 + 16;    // high translucency
    protected int BITSTL = 1 + 8 + 16;    // low translucency
    protected int BITS = 8 + 16 + 64;        // solid
    protected int BORDERANG = 0;
    protected int SHADE = 50;
    protected Font font;

    public DefaultOsdFunc(Engine engine) {
        this.engine = engine;
        this.font = EngineUtils.getLargeFont();
    }

    protected int calcStartX(int x, int scale) {
        return mulscale(((long) x << 3) + 4, scale, 16);
    }

    protected int calcStartY(int y) {
        return y * font.getSize();
    }

    @Override
    public void drawchar(int x, int y, char ch, int shade, OsdColor color, int scale) {
        x = calcStartX(x, scale);
        y = mulscale(calcStartY(y), scale, 16);
        charbuf[0] = ch;
        font.drawText(x, y, charbuf, scale / 65536.0f, shade, color.getPal(), TextAlign.Left, Transparent.None, false);
    }

    @Override
    public int drawosdstr(int x, int y, OsdString text, int len, int shade, OsdColor color, int scale) {
        if (text == null || text.getLength() == 0) {
            return 1;
        }

        int chpos = 0, xpos = x;
        int totalRows = (text.getLength() / len) + 1;
        int row = totalRows - 1;
        int textX = calcStartX(x, scale);
        int textY = calcStartY(y - row);

        while (chpos < text.getLength()) {
            char symb = text.getCharAt(chpos);
            if (symb == 0) {
                break;
            }

            if (xpos == len) {
                xpos = x;
                row--;
                textX = calcStartX(x, scale);
                textY = calcStartY(y - row);
            }

            charbuf[0] = symb;
            int symbWidth = font.drawText(textX, mulscale(textY, scale, 16), charbuf, scale / 65536.0f, text.getShade(chpos), text.getPal(chpos), TextAlign.Left, Transparent.None, false);
            textX += mulscale(symbWidth, scale, 16);
            chpos++;
            xpos++;
        }

        return totalRows;
    }

    @Override
    public void drawstr(int x, int y, String text, int shade, OsdColor color, int scale) {
        font.drawText(calcStartX(x, scale), mulscale(calcStartY(y), scale, 16), toCharArray(text), scale / 65536.0f, 0, color.getPal(), TextAlign.Left, Transparent.None, false);
    }

    @Override
    public void drawcursor(int x, int y, boolean overType, int scale) {
        if ((System.currentTimeMillis() & 0x400) == 0) {
            char ch = '_';
            if (overType) {
                ch = '#';
            }
            charbuf[0] = ch;

            font.drawText(calcStartX(x, scale), mulscale(calcStartY(y), scale, 16), charbuf, scale / 65536.0f, 0, OsdColor.DEFAULT.getPal(), TextAlign.Left, Transparent.None, false);
        }
    }

    @Override
    public void drawlogo(int daydim) {
        if (BGCTILE != -1) {
            Tile pic = engine.getTile(BGCTILE);

            int xsiz = pic.getWidth();
            int ysiz = pic.getHeight();

            if (pic.hasSize()) {
                engine.rotatesprite((xdim - xsiz) << 15, (daydim - ysiz) << 16, 65536, 0, BGCTILE, SHADE - 32, PALETTE, BITSTL, 0, 0, xdim, daydim);
            }
        }
    }

    @Override
    public void clearbg(int col, int row) {
        int bits = BITSTH;
        int daydim = (row * font.getSize()) + 5;

        Tile pic = engine.getTile(BGTILE);

        int xsiz = pic.getWidth();
        int ysiz = pic.getHeight();

        if (!pic.hasSize()) {
            return;
        }

        int tx2 = xdim / xsiz;
        int ty2 = daydim / ysiz;

        for (int x = tx2; x >= 0; x--) {
            for (int y = ty2; y >= 0; y--) {
                engine.rotatesprite(x * xsiz << 16, y * ysiz << 16, 65536, 0, BGTILE, SHADE, PALETTE, bits, 0, 0, xdim, daydim);
            }
        }

        drawlogo(daydim);

        if (BORDTILE != -1) {
            pic = engine.getTile(BORDTILE);
            if (pic.hasSize()) {
                xsiz = pic.getHeight();
                if (xsiz > 0) {
                    tx2 = xdim / xsiz;
                    for (int x = tx2; x >= 0; x--) {
                        engine.rotatesprite(x * xsiz << 16, (daydim - 1) << 16, 65536, BORDERANG, BORDTILE, SHADE + 12, PALETTE, BITS, 0, 0, xdim, daydim + 1);
                    }
                }
            }
        }
    }

    @Override
    public int getPulseShade(int speed) {
        return EngineUtils.sin(engine.getTotalClock() << 4) >> 11;
    }

    @Override
    public void showOsd(boolean isFullscreen) {
        // fix for TCs like Layre which don't have the BGTILE for
        // some reason
        // most of this is copied from my dummytile stuff in defs.c
        Tile pic = engine.getTile(BGTILE);
        if (pic.getWidth() == 0 || pic.getHeight() == 0) {
            engine.allocatepermanenttile(BGTILE, 1, 1);
        }
    }

    @Override
    public int getcolumnwidth(int osdtextscale) {
        return divscale(xdim, osdtextscale, 16) / (font.getCharInfo(' ').getCellSize()) - 3;
    }

    @Override
    public int getrowheight(int osdtextscale) {
        return divscale(ydim, osdtextscale, 16) / font.getSize();
    }

    @Override
    public boolean textHandler(String text) {
        return false;
    }

}
