package ru.m210projects.Build.osd;

import java.util.ArrayList;
import java.util.List;

public class ConsoleHistory {
    private final List<String> history;
    private final int depth;
    private int pos;

    public ConsoleHistory(int depth) {
        this.depth = depth;
        this.history = new ArrayList<>(depth);
    }

    public boolean add(String text) {
        pos = -1;
        if (history.size() != 0 && text.equals(history.get(0))) {
            return false;
        }

        if (history.size() >= depth) {
            history.remove(depth - 1);
        }
        history.add(0, text);
        return true;
    }

    public boolean hasPrev() {
        return pos < history.size() - 1;
    }

    public String prev() {
        if (!hasPrev()) {
            return history.get(pos);
        }

        pos++;
        return history.get(pos);
    }

    public boolean hasNext() {
        return pos > 0;
    }

    public String next() {
        if (!hasNext()) {
            pos = -1;
            return "";
        }

        pos--;
        return history.get(pos);
    }
}
