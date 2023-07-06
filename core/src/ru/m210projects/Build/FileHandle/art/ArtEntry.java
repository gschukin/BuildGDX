package ru.m210projects.Build.filehandle.art;

import ru.m210projects.Build.Types.AnimType;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.EntryInputStream;
import ru.m210projects.Build.filehandle.InputStreamProvider;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static ru.m210projects.Build.Engine.pow2long;

public class ArtEntry implements Entry {

    private final int num;
    private final int offset;
    private final int width;
    private final int height;
    private final int sizex;
    private final int sizey;
    private int flags;
    private final InputStreamProvider provider;
    private final int size;

    private byte[] data; //get rid of this in future

    // allocated tile
    public ArtEntry(int num, byte[] data, int width, int height) {
        this(() -> new ByteArrayInputStream(data), num, 0, width, height, 0);
        this.data = data;
    }

    public ArtEntry(InputStreamProvider provider, int num, int offset, int width, int height, int flags) {
        this.provider = provider;
        this.num = num;
        this.offset = offset;
        this.width = width;
        int sizex = 15;
        while ((sizex > 1) && (pow2long[sizex] > width)) {
            sizex--;
        }
        this.sizex = sizex;
        this.height = height;
        int sizey = 15;
        while ((sizey > 1) && (pow2long[sizey] > height)) {
            sizey--;
        }
        this.sizey = sizey;
        this.flags = flags;
        this.size = width * height;
    }

    public InputStream getInputStream() throws IOException {
        InputStream is = provider.newInputStream();
        if (is.skip(offset) != offset) {
            throw new EOFException();
        }
        return new EntryInputStream(is, size);
    }

    @Override
    public byte[] getBytes() {
        if (!isLoaded()) {
            data = Entry.super.getBytes();
        }

        return data;
    }

    public boolean isLoaded() {
        return data != null;
    }

    public boolean hasSize() {
        return size != 0;
    }


    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getExtension() {
        return "";
    }

    @Override
    public boolean exists() {
        return true;
    }

    public int getNum() {
        return num;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSizex() {
        return sizex;
    }

    public int getSizey() {
        return sizey;
    }

    public long getSize() {
        return size;
    }

    public byte getOffsetX() {
        return (byte) ((flags >> 8) & 0xFF);
    }

    public byte getOffsetY() {
        return (byte) ((flags >> 16) & 0xFF);
    }

    public int getAnimFrames() {
        return flags & 0x3F;
    }

    public int getAnimSpeed() {
        return (flags >> 24) & 15;
    }

    public AnimType getType() {
        return AnimType.findAnimType(flags);
    }

    public void setAnimType(AnimType type) {
        flags |= type.getBit();
    }

    public void setAnimFrames(int frames) {
        flags |= frames & 0x3F;
    }

    public void setAnimSpeed(int speed) {
        flags |= (speed & 15) << 24;
    }

    public void disableAnimation() {
        flags &= ~0xFF0000FF;
    }

    public boolean hasXOffset() {
        return (flags & 0x0000FF00) != 0;
    }

    public boolean hasYOffset() {
        return (flags & 0x00FF0000) != 0;
    }

    public void setOffset(int x, int y) {
        flags &= ~0x00FFFF00;
        flags |= (x & 0xFF) << 8;
        flags |= (y & 0xFF) << 16;
    }

    @Override
    public String toString() {
        return "ArtEntry{" +
                "num=" + num +
                ", offset=" + offset +
                ", width=" + width +
                ", height=" + height +
                ", flags=" + flags +
                ", size=" + size +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtEntry)) return false;
        ArtEntry artEntry = (ArtEntry) o;
        return num == artEntry.num && offset == artEntry.offset && width == artEntry.width && height == artEntry.height && flags == artEntry.flags && size == artEntry.size && Objects.equals(provider, artEntry.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(num, offset, width, height, flags, provider, size);
    }
}
