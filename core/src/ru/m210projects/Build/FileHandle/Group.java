package ru.m210projects.Build.filehandle;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public interface Group extends Iterable<Entry> {

    List<Entry> getEntries();
    int getSize();
    String getName();
    Entry getEntry(String name);
    default Entry getEntry(Path path) {
        return getEntry(path.toString());
    }
    
    default Iterator<Entry> iterator() {
        return getEntries().iterator();
    }
}
