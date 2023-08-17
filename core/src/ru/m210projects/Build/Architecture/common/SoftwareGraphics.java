package ru.m210projects.Build.Architecture.common;

public interface SoftwareGraphics {

    byte[] getRasterBuffer();

    void changePalette(byte[] palette);

}
