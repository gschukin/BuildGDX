package ru.m210projects.Build.filehandle.fs;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.Pattern.Tools.NaturalComparator;
import ru.m210projects.Build.filehandle.Entry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class FileEntry implements Entry, Comparable<FileEntry> {

    private final Path path;
    private final String name;
    private final String extension;
    private final long size;

    public FileEntry(Path path) throws IOException {
        this(path, path.getFileName().toString(), Files.size(path));
    }

    public FileEntry(Path path, String name, long size) {
        this.path = path;
        this.name = name;
        if (name.contains(".")) {
            this.extension = name.substring(name.lastIndexOf(".") + 1);
        } else {
            this.extension = "";
        }
        this.size = size;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public boolean isDirectory() {
        return Files.isDirectory(path);
    }

    public Path getPath() {
        return path;
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
        return Files.exists(path);
    }

    public boolean delete() {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String toString() {
        return "FileEntry{" +
                "name='" + name + '\'' +
                ", size=" + size +
                '}';
    }

    @Override
    public int compareTo(@NotNull FileEntry f) {
        String s1 = this.getName();
        String s2 = f.getName();

        return NaturalComparator.compare(s1, s2);
    }
}
