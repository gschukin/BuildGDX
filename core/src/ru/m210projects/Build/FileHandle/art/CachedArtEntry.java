package ru.m210projects.Build.filehandle.art;

import ru.m210projects.Build.Engine;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class CachedArtEntry extends ArtEntry {

    private final byte[] data;
    private final Engine engine;

    public CachedArtEntry(Engine engine, int num, byte[] data, int width, int height) {
        super(() -> new ByteArrayInputStream(data), num, 0, width, height, 0);
        this.data = data;
        this.engine = engine;
    }

    public byte[] getData() {
        return data;
    }

    public void copyData(byte[] data) {
        if (data.length < size) {
            throw new RuntimeException("Wrong tile data length");
        }
        System.arraycopy(data, 0, this.data, 0, size);
        engine.getrender().invalidatetile(num, -1, -1);
    }

    public void clearData() {
        Arrays.fill(data, (byte) 0);
        engine.getrender().invalidatetile(num, -1, -1);
    }

}
