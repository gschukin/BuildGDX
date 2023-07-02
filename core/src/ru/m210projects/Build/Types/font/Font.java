package ru.m210projects.Build.Types.font;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.TileFont;
import ru.m210projects.Build.Types.Transparent;

import static ru.m210projects.Build.Gameutils.coordsConvertXScaled;
import static ru.m210projects.Build.Gameutils.coordsConvertYScaled;
import static ru.m210projects.Build.RenderService.ydim;
import static ru.m210projects.Build.Strhandler.toCharArray;

public class Font {

    protected final Engine engine;
    protected CharInfo[] charInfo;
    protected int height;
    protected boolean verticalScaled = true;

    public Font(Engine engine) {
        this.engine = engine;
        this.charInfo = new CharInfo[256];
        this.charInfo[0] = new CharInfo(this, -1, 0);
    }

    protected void addCharInfo(char ch, CharInfo charInfo) {
        this.charInfo[ch] = charInfo;
    }

    public TileFont.FontType getFontType() {
        return TileFont.FontType.TILE_FONT;
    }

    public CharInfo getCharInfo(char ch) {
        if (ch >= charInfo.length || charInfo[ch] == null) {
            return charInfo[0];
        }

        return this.charInfo[ch];
    }

    public boolean isVerticalScaled() {
        return verticalScaled;
    }

    public void setVerticalScaled(boolean verticalScaled) {
        this.verticalScaled = verticalScaled;
    }

    public int getWidth(char[] text, float scale) {
        int width = 0;
        for (int pos = 0; text != null && pos < text.length && text[pos] != 0; pos++) {
            width += getCharInfo(text[pos]).getCellSize() * scale;
        }
        return width;
    }

    public int getWidth(String text, float scale) {
        return getWidth(toCharArray(text), scale);
    }

    public int getHeight() {
        return height;
    }

    public int drawText(int x, int y, char[] text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, boolean shadow) {
        if (shadow) {
            engine.getrender().printext(this, (int) (x + scale), (int) (y + scale), text, scale, 127, 0, align, transparent);
        }
        return engine.getrender().printext(this, x, y, text, scale, shade, palnum, align, transparent);
    }

    public int drawText(int x, int y, String text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, boolean shadow) {
        return drawText(x, y, toCharArray(text), scale, shade, palnum, align, transparent, shadow);
    }

    public int drawTextScaled(int x, int y, char[] text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, ConvertType type, boolean shadow) {
        x = coordsConvertXScaled(x, type);
        y = coordsConvertYScaled(y);

        int xdim = (4 * ydim) / 3;
        return drawText(x, y, text, (scale * xdim) / 320.0f, shade, palnum, align, transparent, shadow);
    }

    public int drawTextScaled(int x, int y, String text, float scale, int shade, int palnum, TextAlign align, Transparent transparent, ConvertType type, boolean shadow) {
        return drawTextScaled(x, y, toCharArray(text), scale, shade, palnum, align, transparent, type, shadow);
    }

    public int drawCharScaled(int x, int y, char symb, float scale, int shade, int palnum, Transparent transparent, ConvertType type, boolean shadow) {
        return drawTextScaled(x, y, new char[] { symb }, scale, shade, palnum, TextAlign.Left, transparent, type, shadow);
    }
}
