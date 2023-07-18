package ru.m210projects.Build.filehandle;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.Types.collections.MapNode;
import ru.m210projects.Build.filehandle.fs.Directory;
import ru.m210projects.Build.filehandle.fs.FileEntry;
import ru.m210projects.Build.filehandle.grp.GrpFile;
import ru.m210projects.Build.filehandle.rff.RffFile;
import ru.m210projects.Build.filehandle.zip.ZipFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ru.m210projects.Build.filehandle.CacheResourceMap.CachePriority.*;
import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_DIRECTORY;
import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_ENTRY;

public class Cache {

    /**
     * поиск сначала в файлах
     * поиск сначала в группах
     * поиск в конкретной группе (не надо?)
     * добавить по пути (проверить что группа)
     * добавить как группу
     * добавить как ресурс (проверить что группа и поставить "динамик")
     * проверить загруженную группу по имени
     * удалить группу по имени
     * удалить группу по ссылке
     */

    protected final CacheResourceMap cacheResourceMap = new CacheResourceMap();

    public Cache(@NotNull Directory gameDirectory) {
        cacheResourceMap.addGroup(gameDirectory, HIGH);
    }

    public boolean addGroup(Group group, CacheResourceMap.CachePriority priority) {
        if (Objects.nonNull(group) && group.getSize() > 0) {
            cacheResourceMap.addGroup(group, priority);
            return true;
        }
        return false;
    }

    public boolean isGameDirectory(Group group) {
        if (!(group instanceof Directory) || group.equals(DUMMY_DIRECTORY)) {
            return false;
        }

        return group.equals(getGameDirectory());
    }

    public boolean removeGroup(Group group) {
        return cacheResourceMap.removeGroup(group);
    }

    @NotNull
    public Directory getGameDirectory() {
        Group group = cacheResourceMap.getGroup(0);
        if (group instanceof Directory) {
            return (Directory) group;
        }
        return DUMMY_DIRECTORY;
    }

    public CacheResourceMap.CachePriority getPriority(Group group) {
        return cacheResourceMap.getPriority(group);
    }

    public boolean addGroup(Entry groupEntry, CacheResourceMap.CachePriority priority) {
        Group group = newGroup(groupEntry);
        return addGroup(group, priority);
    }

    @NotNull
    public Entry getEntry(Path path, boolean searchFirst) {
        Entry entry = DUMMY_ENTRY;
        int i = searchFirst ? HIGHEST.getLevel() : NORMAL.getLevel();
        for (; i >= NORMAL.getLevel(); i--) {
            for (MapNode<Group> node = cacheResourceMap.getFirst(i); node != null; node = node.getNext()) {
                entry = node.get().getEntry(path);
                if (entry.exists()) {
                    return entry;
                }
            }
        }
        return entry;
    }

    @NotNull
    public Entry getEntry(String name, boolean searchFirst) {
        return getEntry(Paths.get(name), searchFirst);
    }

    public boolean containsGroup(String groupName) {
        return cacheResourceMap.containsGroup(groupName);
    }

    @NotNull
    public Group newGroup(Entry entry) {
        Group group = DUMMY_DIRECTORY;

        GroupType type = getGroupType(entry);
        try {
            switch (type) {
                case DIRECTORY:
                    group = getGameDirectory().getDirectory((FileEntry) entry);
                    break;
                case GRP:
                    group = new GrpFile(entry.getName(), entry::getInputStream);
                    break;
                case RFF:
                    group = new RffFile(entry.getName(), entry::getInputStream);
                    break;
                case ZIP:
                    group = new ZipFile(entry.getName(), entry::getInputStream);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return group;
    }

    @NotNull
    public Group getGroup(String groupName) {
        Group group = cacheResourceMap.getGroup(groupName);
        if (group.getSize() > 0) {
            return group;
        }
        return DUMMY_DIRECTORY;
    }

    @NotNull
    public GroupType getGroupType(Entry entry) {
        if (entry instanceof FileEntry && entry.isDirectory()) {
            return GroupType.DIRECTORY;
        }

        if (entry.getSize() < 4) {
            return GroupType.NONE;
        }

        try (InputStream is = entry.getInputStream()) {
            switch (StreamUtils.readSignedByte(is)) {
                case 'K':
                    String value = StreamUtils.readString(is, 11);
                    if (value.equals("enSilverman")) {
                        return GroupType.GRP;
                    }
                    break;
                case 'R':
                    if (StreamUtils.readSignedByte(is) == 'F' && StreamUtils.readSignedByte(is) == 'F' && StreamUtils.readSignedByte(is) == 0x1A) {
                        return GroupType.RFF;
                    }
                    break;
                case 'P':
                    if (StreamUtils.readSignedByte(is) == 'K' && StreamUtils.readSignedByte(is) == 0x03 && StreamUtils.readSignedByte(is) == 0x04) {
                        return GroupType.ZIP;
                    }
                    break;
            }
        } catch (IOException ignored) {
        }
        return GroupType.NONE;
    }

    public List<Group> getGroups() {
        List<Group> list = new ArrayList<>();
        for (int i = NORMAL.getLevel(); i <= HIGHEST.getLevel(); i++) {
            for (MapNode<Group> node = cacheResourceMap.getFirst(i); node != null; node = node.getNext()) {
                list.add(node.get());
            }
        }
        return list;
    }

    public enum GroupType {
        NONE, DIRECTORY, RFF, GRP, ZIP
    }
}
