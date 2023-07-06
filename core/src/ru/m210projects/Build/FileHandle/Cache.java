package ru.m210projects.Build.filehandle;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.filehandle.fs.Directory;
import ru.m210projects.Build.filehandle.fs.FileEntry;
import ru.m210projects.Build.filehandle.grp.GrpFile;
import ru.m210projects.Build.filehandle.rff.RffFile;
import ru.m210projects.Build.filehandle.zip.ZipFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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

    public enum GroupType {
        NONE, DIRECTORY, RFF, GRP, ZIP
    }

    private final Directory gameDirectory;
    private final List<Group> groups = new ArrayList<>();
    private final Map<String, Integer> groupMap = new HashMap<>();

    public Cache(@NotNull Directory gameDirectory) {
        this.gameDirectory = gameDirectory;
    }

    public boolean add(Group group) {
        if (Objects.nonNull(group) && group.getSize() > 0) {
            groups.add(group);
            groupMap.put(group.getName().toUpperCase(), groups.size() - 1);
            return true;
        }
        return false;
    }

    @NotNull
    public Directory getGameDirectory() {
        return gameDirectory;
    }

    @NotNull
    public Directory getUserDirectory() {
        return null;
    }

    public boolean add(Entry entry) {
        Group group = newGroup(entry);
        return add(group);
    }

    @NotNull
    public Entry getEntry(String name, boolean searchFirst) {
        Entry entry = DUMMY_ENTRY;
        if (searchFirst) {
            // search in dynamic groups and external files

            // Search in dynamic groups first
//            for (int k = groups.size() - 1; k >= 0; k--) {
//                Group<? extends Entry> group = groups.get(k);
//                if (/*(group.flags & DYNAMIC) != 0 && */group.contains(name)) {
//                    return true;
//                }
//            }

            entry = gameDirectory.getEntry(name);
            if (entry.exists()) {
                return entry;
            }
        }

        // search in groups
        for (int k = groups.size() - 1; k >= 0; k--) {
            entry = groups.get(k).getEntry(name);
            if (entry.exists()) {
                return entry;
            }
        }

        return entry;
    }

    public boolean contains(String groupName) {
        return groupMap.containsKey(groupName.toUpperCase());
    }

    @NotNull
    public Group newGroup(Entry entry) {
        Group group = DUMMY_DIRECTORY;

        if (Objects.nonNull(entry)) {
            GroupType type = getGroupType(entry);
            try {
                switch (type) {
                    case DIRECTORY:
                        group = gameDirectory.getDirectory((FileEntry) entry);
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
        }
        return group;
    }

    @NotNull
    public Group getGroup(String groupName) {
        if (groupName.equals(gameDirectory.getName())) {
            return gameDirectory;
        }

        int index = groupMap.getOrDefault(groupName.toUpperCase(), -1);
        if (index != -1) {
            return groups.get(index);
        }

        FileEntry directoryEntry = gameDirectory.getEntry(groupName);
        if (directoryEntry.isDirectory()) {
            return gameDirectory.getDirectory(directoryEntry);
        }

        return DUMMY_DIRECTORY;
    }

    @NotNull
    public GroupType getGroupType(Entry entry) {
        if (entry instanceof FileEntry && ((FileEntry) entry).isDirectory()) {
            return GroupType.DIRECTORY;
        }

        if (entry.getSize() < 4) {
            return GroupType.NONE;
        }

        try (InputStream is = entry.getInputStream()) {
            switch (is.read()) {
                case 'K':
                    String value = StreamUtils.readString(is, 11);
                    if (value.equals("enSilverman")) {
                        return GroupType.GRP;
                    }
                    break;
                case 'R':
                    if (is.read() == 'F' && is.read() == 'F' && is.read() == 0x1A) {
                        return GroupType.RFF;
                    }
                    break;
                case 'P':
                    if (is.read() == 'K' && is.read() == 0x03 && is.read() == 0x04) {
                        return GroupType.ZIP;
                    }
                    break;
            }
        } catch (IOException ignored) {
        }
        return GroupType.NONE;
    }

    public List<Group> getGroups() {
        return groups;
    }
}
