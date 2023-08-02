package ru.m210projects.Build.osd;

import java.util.ArrayList;
import java.util.List;

public class OsdString {

    private final StringBuilder text = new StringBuilder();
    private final List<Short> fmt = new ArrayList<>();

    public void clear() {
        this.text.setLength(0);
        this.fmt.clear();
    }

    public void insert(int pos, char ch, int pal, int shade) {
        if(pos < text.length()) {
            text.setCharAt(pos, ch);
            fmt.set(pos, (short) (pal + (shade << 8)));
        } else {
            text.insert(pos, ch);
            fmt.add((short) (pal + (shade << 8)));
        }
    }

    public int getLength() {
        return text.length();
    }

    public int getPal(int pos) {
        if (pos >= fmt.size()) {
            return 0;
        }

        return fmt.get(pos) & 0xFF;
    }

    public int getShade(int pos) {
        if (pos >= fmt.size()) {
            return 0;
        }

        return (fmt.get(pos) >> 8);
    }

    public char getCharAt(int pos) {
        return text.charAt(pos);
    }

    public boolean isEmpty() {
        return fmt.isEmpty();
    }

    @Override
    public String toString() {
        return text.toString();
    }
}