package ru.m210projects.Build.Types.font;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Types.TileFont;

public class BitmapFont extends Font {

    private final byte[] data;
    private final int atlasWidth, atlasHeight;

    public BitmapFont(Engine engine, byte[] data, int atlasWidth, int atlasHeight, int cols, int rows) {
        super(engine);

        this.data = data;
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.height = atlasHeight / rows;

        for (int i = 0; i < cols * rows; i++) {
            addCharInfo((char) i, new AtlasCharInfo(this, (char) i, 0, atlasWidth, atlasHeight, cols, rows));
        }

        setVerticalScaled(false);
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public TileFont.FontType getFontType() {
        return TileFont.FontType.BITMAP_FONT;
    }

    public int getAtlasWidth() {
        return atlasWidth;
    }

    public int getAtlasHeight() {
        return atlasHeight;
    }
}
