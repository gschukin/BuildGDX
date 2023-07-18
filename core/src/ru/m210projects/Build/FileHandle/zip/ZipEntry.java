package ru.m210projects.Build.filehandle.zip;

import ru.m210projects.Build.filehandle.*;
import ru.m210projects.Build.osd.Console;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

public class ZipEntry implements Entry {
    private final InputStreamProvider provider;
    private final long size;
    private final String name;
    private final String entryName;
    private final String extension;
    private final boolean directory;
    Group parent;
    private byte[] cache;

    public ZipEntry(InputStreamProvider provider, String name, java.util.zip.ZipEntry entry) {
        this.provider = provider;
        this.size = entry.getSize();
        this.entryName = entry.getName();
        this.name = name;
        this.directory = entry.isDirectory();
        if (name.contains(".")) {
            this.extension = name.substring(name.lastIndexOf(".") + 1).toUpperCase();
        } else {
            this.extension = "";
        }
    }

    private EntryInputStream getZipInputStream() throws IOException {
        FastZipInputStream zis = new FastZipInputStream(new BufferedInputStream(provider.newInputStream()));
        java.util.zip.ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().equals(entryName)) {
                return new EntryInputStream(zis, (int) size);
            }
            zis.skipEntry();
        }

        // shouldn't get here
        return new EntryInputStream(new ByteArrayInputStream(new byte[0]), 0);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (cache == null) {
            load();
        }
        return new ByteArrayInputStream(cache);
    }

    public void load() {
        try (InputStream is = getZipInputStream()) {
            cache = StreamUtils.readBytes(is, (int) size);
        } catch (Exception e) {
            Console.out.println(String.format("File %s can't be loaded: %s", name, e));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public Group getParent() {
        return parent;
    }

    @Override
    public void setParent(Group parent) {
        this.parent = parent;
    }

    @Override
    public boolean isDirectory() {
        return directory;
    }

    @Override
    public String toString() {
        if (directory) {
            return name;
        }
        return String.format("%s size=%d", name, size);
    }
}
