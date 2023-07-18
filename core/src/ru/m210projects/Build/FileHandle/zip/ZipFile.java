package ru.m210projects.Build.filehandle.zip;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.Group;
import ru.m210projects.Build.filehandle.InputStreamProvider;
import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.InflaterInputStream;
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

    protected Entry newEntry(InputStreamProvider provider, String name, java.util.zip.ZipEntry entry, ZipInputStream zis) throws IOException {
//        byte[] data = StreamUtils.readBytes(zis, (int) entry.getSize());
//        return new ZipEntry(provider, name, entry) {
//            @Override
//            public InputStream getInputStream() {
//                return new ByteArrayInputStream(data);
//            }
//        };
        return new ZipEntry(provider, name, entry);
    }

    public ZipFile(String name, InputStreamProvider provider) throws IOException {
        this.name = name;

        try (FastZipInputStream zis = new FastZipInputStream(new BufferedInputStream(provider.newInputStream()))) {
            this.entries = new LinkedHashMap<>();
            this.directories = new LinkedHashMap<>();

            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path path = Paths.get(entry.getName());
                Path file = path.getFileName();

                if (path.getParent() == null) {
                    Entry zip = entries.put(file.toString().toUpperCase(), newEntry(provider, file.toString(), entry, zis));
                    if (zip != null) {
                        zip.setParent(this);
                    }
                } else {
                    Entry zipEntry = newEntry(provider, file.toString(), entry, zis);

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
                    zipEntry.setParent(dir);
                }

                zis.skipEntry();
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

    @NotNull
    @Override
    public Entry getEntry(Path path) {
        ZipFile dir = this;
        Entry result = DUMMY_ENTRY;
        for (Path p : path) {
            Entry entry = dir.entries.getOrDefault(p.toString().toUpperCase(), DUMMY_ENTRY);
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
        return getEntry(Paths.get(name));
    }

    private void fillList(ZipFile dir, List<Entry> list) {
        list.addAll(dir.entries.values());
        for (ZipFile zip : dir.directories.values()) {
            fillList(zip, list);
        }
    }

    @Override
    public synchronized List<Entry> getEntries() {
        List<Entry> list = new ArrayList<>();
        fillList(this, list);
        return list;
    }
}
