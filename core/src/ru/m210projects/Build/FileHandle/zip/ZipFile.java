package ru.m210projects.Build.filehandle.zip;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.Group;
import ru.m210projects.Build.filehandle.InputStreamProvider;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipInputStream;

import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_ENTRY;

public class ZipFile implements Group {

    private final Map<String, Entry> entries;
    private final Map<String, ZipFile> directories;
    private final String name;

    private ZipFile(String name) {
        this.name = name;
        this.entries = new LinkedHashMap<>();
        this.directories = new LinkedHashMap<>();
    }

    public ZipFile(String name, InputStreamProvider provider) throws IOException {
        this.name = name;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(provider.newInputStream()))) {
            this.entries = new LinkedHashMap<>();
            this.directories = new LinkedHashMap<>();

            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path path = Paths.get(entry.getName());
                Path file = path.getFileName();

                if (path.getParent() == null) {
                    entries.put(file.toString().toUpperCase(), new ZipEntry(provider, file.toString(), entry));
                } else {
                    ZipEntry zipEntry = new ZipEntry(provider, file.toString(), entry);

                    ZipFile dir = this;
                    for (Path p : path) {
                        if(p.equals(file)) {
                            break;
                        }

                        String dirName = p.toString();
                        Map<String, ZipFile> directories = dir.directories;
                        String key = dirName.toUpperCase();

                        dir = directories.getOrDefault(key, new ZipFile(dirName));
                        directories.putIfAbsent(key, dir);
                    }

                    dir.entries.put(zipEntry.getName().toUpperCase(), zipEntry);
                }
                zis.closeEntry();
            }
        }
    }

    @Override
    public synchronized int getSize() {
        return entries.size();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public synchronized List<Entry> getEntries() {
        return new ArrayList<>(entries.values());
    }

    @NotNull
    @Override
    public Entry getEntry(Path path) {
        ZipFile dir = this;
        Entry result = DUMMY_ENTRY;
        for (Path p : path) {
            Entry entry = dir.getEntry(p.toString());
            if (!entry.exists()) {
                return DUMMY_ENTRY;
            }

            if(entry.isDirectory()) {
                dir = dir.directories.getOrDefault(entry.getName().toUpperCase(), this);
            }
            result = entry;
        }

        return result;
    }

    @NotNull
    @Override
    public Entry getEntry(String name) {
        Objects.requireNonNull(name, "name");
        Entry entry;
        synchronized (this) {
            entry = entries.getOrDefault(name.toUpperCase(), DUMMY_ENTRY);
        }
        return entry;
    }
}
