package ru.m210projects.Build.osd;

public interface OsdFunc {

    void drawchar(int x, int y, char ch, int shade, OsdColor color, int scale);

    int drawosdstr(int x, int y, OsdString text, int len, int shade, OsdColor color, int scale);

    void drawstr(int x, int y, String text, int shade, OsdColor color, int scale);

    void drawcursor(int x, int y, boolean overType, int scale);

    void drawlogo(int daydim);

    void clearbg(int col, int row);

    /**
     * sintable[(totalclock << 4) & 2047] >> 11;
     */
    int getPulseShade(int speed);

    /**
     * fix for TCs like Layre which don't have the BGTILE for
     * some reason
     * most of this is copied from my dummytile stuff in defs.c
     */
    void showOsd(boolean isFullscreen);

    long getTicks();

    int getcolumnwidth(int osdtextscale);

    int getrowheight(int osdtextscale);

    boolean textHandler(String text);

}
