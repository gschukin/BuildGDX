package ru.m210projects.Build.filehandle.fs;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Pattern.Tools.NaturalComparator;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.Group;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_DIRECTORY;

public class FileEntry implements Entry, Comparable<FileEntry> {

    private final Path path;
    private final String name;
    private final String extension;
    private final boolean isDirectory;
    private final long size;
    private Group parent;
    protected Directory physicalDirectory;

    public FileEntry(Path path) throws IOException {
        this(path, path.getFileName().toString(), Files.size(path));
    }

    public FileEntry(Path path, String name, long size) {
        this.path = path;
        this.name = name;
        if (name.contains(".")) {
            this.extension = name.substring(name.lastIndexOf(".") + 1).toUpperCase();
        } else {
            this.extension = "";
        }
        this.isDirectory = Files.isDirectory(path);
        this.size = size;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    public Directory getDirectory() {
        return physicalDirectory.getDirectory(this);
    }

    @SuppressWarnings("IOStreamConstructor")
    @Override
    public InputStream getInputStream() throws IOException {
        // Files.newInputStream(path) is shitty slow!!!
        return new FileInputStream(path.toFile());
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
        try {
            if (isDirectory) {
                return true;
            }
            return Files.exists(path);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Directory getParent() {
        if (parent instanceof Directory) {
            return (Directory) parent;
        }

        return physicalDirectory;
    }

    @Override
    public void setParent(Group parent) {
        if (parent == null || parent.equals(DUMMY_DIRECTORY)) {
            parent = physicalDirectory;
        }

        // the parent might be GRP file
        this.parent = parent;
        if (physicalDirectory == null
                && parent instanceof Directory
                && ((Directory) parent).getPath().equals(path.getParent())) {
            physicalDirectory = (Directory) parent;
        }
    }

    public boolean delete() {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Path getRelativePath() {
        return BuildGdx.cache.getGameDirectory().getPath().relativize(path);
    }

    @Override
    public int compareTo(@NotNull FileEntry f) {
        String s1 = this.getName();
        String s2 = f.getName();

        return NaturalComparator.compare(s1, s2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileEntry)) return false;
        FileEntry fileEntry = (FileEntry) o;
        return isDirectory == fileEntry.isDirectory && size == fileEntry.size && Objects.equals(path, fileEntry.path) && Objects.equals(name, fileEntry.name) && Objects.equals(extension, fileEntry.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name, extension, size, isDirectory);
    }

    @Override
    public String toString() {
        if (isDirectory()) {
            return name;
        }
        return String.format("%s size=%d", name, size);
    }
}
