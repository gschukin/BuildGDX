package ru.m210projects.Build.filehandle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DataEntry implements Entry {

    private final InputStreamProvider provider;
    private final String name;
    private final int size;
    private final String extension;

    public DataEntry(String name, byte[] data) {
        provider = () -> new ByteArrayInputStream(data);
        this.size = data.length;
        this.name = name;
        if (name.contains(".")) {
            this.extension = name.substring(name.lastIndexOf(".") + 1);
        } else {
            this.extension = "";
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return provider.newInputStream();
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

}
