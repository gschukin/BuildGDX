package ru.m210projects.Build.Types;

import ru.m210projects.Build.Render.Types.Color;

import static java.lang.Math.max;
import static java.lang.Math.min;

public interface PaletteManager {

    Color getFogColor(int pal);

    FastColorLookup createFastColorLookup(int rscale, int gscale, int bscale);

    boolean isValidPalette(int paletteIndex);

    boolean changePalette(final byte[] palette);

    byte[] makePalookup(final int palnum, byte[] remapbuf, int r, int g, int b, int dastat);

    default int getPalookup(int davis, int dashade) { // jfBuild
        return (min(max(dashade + (davis >> 8), 0), getShadeCount() - 1));
    }

    byte[] getBasePalette();

    Palette getCurrentPalette();

    FastColorLookup getFastColorLookup();

    int getColorIndex(int pal, int colorIndex);

    byte[][] getPalookupBuffer();

    byte[] getTranslucBuffer();

    int getShadeCount();

    byte[][] getBritableBuffer();

}
