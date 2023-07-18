package ru.m210projects.Build.filehandle.rff;

import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.EntryInputStream;
import ru.m210projects.Build.filehandle.Group;
import ru.m210projects.Build.filehandle.InputStreamProvider;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

public class RffEntry implements Entry {

    protected enum DictFlags {
        ID(1),
        EXTERNAL(1 << 1),
        PRELOAD(1 << 2),
        PRELOCK(1 << 3),
        ENCRYPTED(1 << 4);

        private final int bit;

        DictFlags(int bit) {
            this.bit = bit;
        }

        public int getBit() {
            return bit;
        }

        public boolean checkBit(int flags) {
            return (flags & bit) != 0;
        }
    }

    private final InputStreamProvider provider;
    private final int id;
    private final int offset;
    private final int size;
    private final int packedSize;
    private final LocalDateTime date;
    private final int flags;
    private final String name;
    private final String fmt;
    Group parent;

    public RffEntry(InputStreamProvider provider, int id, int offset, int size, int packedSize, LocalDateTime date, int flags, String name, String fmt) {
        this.provider = provider;
        this.id = id;
        this.offset = offset;
        this.size = size;
        this.packedSize = packedSize;
        this.date = date;
        if (id != 0 && !DictFlags.ID.checkBit(flags)) {
            flags |= DictFlags.ID.getBit();
        }
        this.flags = flags;
        this.name = String.format("%s.%s",name.trim(), fmt).toUpperCase();
        this.fmt = fmt.toUpperCase();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream in;
        synchronized (this) {
            in = new EntryInputStream(RffInputStream.getInputStream(provider, isEncrypted(), offset, 0, 256), size);
        }
        return in;
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
    public long getSize() {
        return size;
    }

    /**
     * @return name without path and extension
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getExtension() {
        return fmt;
    }

    public int getId() {
        return id;
    }

    public boolean isEncrypted() {
        return (flags & DictFlags.ENCRYPTED.getBit()) != 0;
    }

    public boolean isIDUsed() {
        return (flags & DictFlags.ID.getBit()) != 0;
    }

    public int getPackedSize() {
        return packedSize;
    }

    public LocalDateTime getDate() {
        return date;
    }

    int getFlags() {
        return flags;
    }

    @Override
    public String toString() {
        return String.format("%s id=%d, size=%d", name, id, size);
    }
}
