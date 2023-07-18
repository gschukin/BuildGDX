package ru.m210projects.Build.filehandle;

import ru.m210projects.Build.Types.collections.LinkedMap;
import ru.m210projects.Build.Types.collections.MapList;
import ru.m210projects.Build.Types.collections.MapNode;

import java.util.HashMap;
import java.util.Map;

import static ru.m210projects.Build.filehandle.fs.Directory.DUMMY_DIRECTORY;

public class CacheResourceMap extends LinkedMap<Group> {

    /**
     * key: group name
     * value: index in groups list
     */
    protected final Map<String, Integer> groupMap = new HashMap<>();

    public CacheResourceMap() {
        super(3, null, 16, null);
    }

    public void addGroup(Group group, CachePriority priority) {
        if (group.getSize() > 0) {
            GroupNode node = (GroupNode) obtain();
            node.group = group;
            node.priority = priority;
            groupMap.put(group.getName().toUpperCase(), node.getIndex());
            insert(node, priority.getLevel());
        }
    }

    public boolean removeGroup(Group group) {
        final String key = group.getName().toUpperCase();
        int index = groupMap.getOrDefault(key, -1);
        if (index != -1) {
            groupMap.remove(key);
            return remove(index);
        }
        return false;
    }

    public boolean containsGroup(String groupName) {
        return groupMap.containsKey(groupName.toUpperCase());
    }

    public CachePriority getPriority(Group group) {
        final String key = group.getName().toUpperCase();
        int index = groupMap.getOrDefault(key, -1);
        if (index != -1) {
            GroupNode node = (GroupNode) nodeMap[index];
            return node.priority;
        }
        return CachePriority.NULL;
    }

    public Group getGroup(String groupName) {
        int index = groupMap.getOrDefault(groupName.toUpperCase(), -1);
        if (index != -1) {
            return nodeMap[index].get();
        }
        return DUMMY_DIRECTORY;
    }

    Group getGroup(int index) {
        return nodeMap[index].get();
    }

    @Override
    protected Group getInstance() {
        return DUMMY_DIRECTORY;
    }

    @Override
    protected void fill(int from) {
        final MapList<Group> list = basket[poolIndex];
        for (int i = from; i < nodeMap.length; i++) {
            MapNode<Group> newNode = new GroupNode(i);
            list.addLast(newNode);
            nodeMap[i] = newNode;
        }
    }

    @Override
    protected void setValue(MapNode<Group> node, int value) {
        if (value == -1) {
            ((GroupNode) node).group = DUMMY_DIRECTORY;
            ((GroupNode) node).priority = CachePriority.NULL;
        }
    }

    private final static class GroupNode extends MapNode<Group> {
        private Group group = DUMMY_DIRECTORY;
        private CachePriority priority = CachePriority.NULL;

        private GroupNode(int index) {
            super(index);
        }

        @Override
        public Group get() {
            return group;
        }
    }

    public enum CachePriority {
        NULL(-1), NORMAL(0), HIGH(1), HIGHEST(2);

        private final int level;

        CachePriority(int level) {
            this.level = level;
        }

        public final int getLevel() {
            return level;
        }

        public static CachePriority getPriority(int level) {
            switch (level) {
                case 1:
                    return HIGH;
                case 2:
                    return HIGHEST;
            }
            return NORMAL;
        }
    }
}