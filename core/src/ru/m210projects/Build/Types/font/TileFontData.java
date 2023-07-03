package ru.m210projects.Build.Types.font;

import ru.m210projects.Build.Render.TextureHandle.DummyTileData;

import java.nio.ByteBuffer;

public abstract class TileFontData extends DummyTileData {

    public TileFontData(int width, int height) {
        super(PixelFormat.Rgba, width, height);
        buildAtlas(data);
    }

    public abstract ByteBuffer buildAtlas(ByteBuffer data);
}

