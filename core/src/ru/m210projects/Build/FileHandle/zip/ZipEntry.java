package ru.m210projects.Build.filehandle.zip;

import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.EntryInputStream;
import ru.m210projects.Build.filehandle.InputStreamProvider;

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

    public ZipEntry(InputStreamProvider provider, String name, java.util.zip.ZipEntry entry) {
        this.provider = provider;
        this.size = entry.getSize();
        this.entryName = entry.getName();
        this.name = name;
        this.directory = entry.isDirectory();
        if (name.contains(".")) {
            this.extension = name.substring(name.lastIndexOf(".") + 1);
        } else {
            this.extension = "";
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(provider.newInputStream()));

        java.util.zip.ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if(entry.getName().equals(entryName)) {
                return new EntryInputStream(zis, (int) size);
            }
            zis.closeEntry();
        }

        // shouldn't get here
        return new ByteArrayInputStream(new byte[0]);
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
    public boolean isDirectory() {
        return directory;
    }
}
