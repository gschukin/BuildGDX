package ru.m210projects.Build.Types;

public interface FastColorLookup {

    void invalidate();
    byte getClosestColorIndex(byte[] palette, int r, int g, int b);

}
