package ru.m210projects.Build.filehandle;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class EntryInputStream extends BufferedInputStream {
    private int remaining;
    private final int size;

    public EntryInputStream(@NotNull InputStream entryInputStream, int size) {
        super(entryInputStream, Math.min(size, 8192));
        this.remaining = size;
        this.size = size;
    }

    @Override
    public synchronized int read(byte @NotNull [] b, int off, int len) throws IOException {
        if (len > remaining) {
            len = remaining;
        }
        len = super.read(b, off, len);
        remaining -= len;
        return len;
    }

    @Override
    public synchronized int read() throws IOException {
        if (remaining > 0) {
            remaining--;
            return super.read();
        }
        return -1;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        if (n > remaining) {
            n = remaining;
        }
        long total = super.skip(n);
        remaining -= total;
        return total;
    }

    @Override
    public synchronized int available() throws IOException {
        InputStream input = in;
        if (input == null) {
            throw new IOException("Stream closed");
        }
        return remaining;
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
        remaining = size;
    }
}
