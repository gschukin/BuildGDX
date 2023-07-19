package ru.m210projects.Build.filehandle.fs;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.Group;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

public class Directory implements Group {

    public static final FileEntry DUMMY_ENTRY = new FileEntry(Paths.get("DUMMY"), "dummy", -1) {
        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public Path getRelativePath() {
            return getPath();
        }

        @Override
        public Directory getParent() {
            return DUMMY_DIRECTORY;
        }

        @Override
        public Path getPath() {
            return Paths.get("");
        }
    };

    public static final Directory DUMMY_DIRECTORY = new Directory() {
        @Override
        public List<Entry> getEntries() {
            return new ArrayList<>();
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public String getName() {
            return "dummy";
        }

        @Override
        public @NotNull FileEntry getEntry(String name) {
            return DUMMY_ENTRY;
        }
    };

    private final Map<String, FileEntry> entries = new HashMap<>();
    private final Map<String, Directory> directories = new HashMap<>();
    private final Path path;
    FileEntry directoryEntry = DUMMY_ENTRY;

    protected Directory() {
        this.path = DUMMY_ENTRY.getPath();
        this.directoryEntry = DUMMY_ENTRY;
    }

    public Directory(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            this.path = dir;
            stream.forEach(this::addEntry);
        }
    }

    /**
     * Directory files quantity
     */
    @Override
    public synchronized int getSize() {
        return entries.size();
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    public Path getPath() {
        return path;
    }

    @Override
    public synchronized List<Entry> getEntries() {
        return new ArrayList<>(entries.values());
    }

    public FileEntry getDirectoryEntry() {
        return directoryEntry;
    }

    @NotNull
    public FileEntry getEntry(String name) {
        Objects.requireNonNull(name, "name");
        FileEntry entry;
        synchronized (this) {
            entry = entries.getOrDefault(name.toUpperCase(), DUMMY_ENTRY);
            if (entry.isDirectory()) {
                addDirectory(entry);
            }
        }
        return entry;
    }

    @Override
    public synchronized FileEntry getEntry(Path relPath) {
        Directory dir = this;
        FileEntry result = DUMMY_ENTRY;
        for (Path p : relPath) {
            FileEntry entry = dir.getEntry(p.toString());
            if (!entry.exists()) {
                return DUMMY_ENTRY;
            }

            if (entry.isDirectory()) {
                dir = dir.addDirectory(entry);
            }
            result = entry;
        }
        return result;
    }
    @NotNull
    public Directory getDirectory(FileEntry dirEntry) {
        Directory directory = DUMMY_DIRECTORY;
        if (dirEntry != null && dirEntry.isDirectory()) {
            if (this.path.equals(dirEntry.getPath())) {
                return this;
            }

            try {
                Path relPath = this.path.relativize(dirEntry.getPath());
                Directory dir = this;
                for (Path p : relPath) {
                    Map<String, Directory> directories = dir.directories;
                    String key = p.toString().toUpperCase();

                    // if key is not exists and directory contains this entry, add entry to map
                    if (!directories.containsKey(key) && dir.entries.containsValue(dirEntry)) {
                        Directory subDir = new Directory(dirEntry.getPath());
                        subDir.directoryEntry = dirEntry;
                        directories.put(key, subDir);
                    }
                    dir = directories.getOrDefault(key, DUMMY_DIRECTORY);
                }
                directory = dir;
            } catch (Exception ignored) {
                return DUMMY_DIRECTORY;
            }
        }
        return directory;
    }

    @NotNull
    private Directory addDirectory(FileEntry entry) {
        String key = entry.getName().toUpperCase();
        return directories.computeIfAbsent(key, e -> {
            try {
                Directory subDir = new Directory(entry.getPath());
                subDir.directoryEntry = entry;
                return subDir;
            } catch (IOException ignored) {
            }
            return DUMMY_DIRECTORY;
        });
    }

    public void addEntry(Path path) {
        FileEntry entry = newEntry(path);
        if (entry.exists()) {
            entries.put(entry.getName().toUpperCase(), entry);
            entry.setParent(this);
        }
    }

    public boolean revalidate() {
        int files = entries.size();

        String[] list = path.toFile().list();
        if (list != null && list.length != files) {
            directories.clear();
            entries.clear();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                stream.forEach(this::addEntry);
            } catch (IOException e) {
                Console.out.println(String.format("Directory %s invalidate failed!", path), OsdColor.RED);
            }
        }
        return files != entries.size();
    }

    @NotNull
    private FileEntry newEntry(Path path) {
        try {
            return new FileEntry(path);
        } catch (NoSuchFileException e) {
            Console.out.println(String.format("Path \"%s\" is not found.", path), OsdColor.RED);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DUMMY_ENTRY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Directory)) return false;
        Directory directory = (Directory) o;
        return Objects.equals(path, directory.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
