package ru.m210projects.Build.filehandle.art;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class CachedArtEntry extends ArtEntry {

    private final byte[] data;

    public CachedArtEntry(int num, byte[] data, int width, int height) {
        super(() -> new ByteArrayInputStream(data), num, 0, width, height, 0);
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void copyData(byte[] data) {
        if (data.length != getSize()) {
            throw new RuntimeException("Wrong tile data length");
        }
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    public void clearData() {
        Arrays.fill(data, (byte) 0);
    }

}
