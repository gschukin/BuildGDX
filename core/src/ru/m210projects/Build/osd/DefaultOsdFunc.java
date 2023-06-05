package ru.m210projects.Build.osd;

public class DefaultOsdFunc implements OsdFunc {
    @Override
    public void drawchar(int x, int y, char ch, int shade, OsdColor color, int scale) {

    }

    @Override
    public int drawosdstr(int x, int y, OsdString text, int len, int shade, OsdColor color, int scale) {
        return 1;
    }

    @Override
    public void drawstr(int x, int y, String text, int shade, OsdColor color, int scale) {

    }

    @Override
    public void drawcursor(int x, int y, boolean overType, int scale) {

    }

    @Override
    public void drawlogo(int daydim) {

    }

    @Override
    public void clearbg(int col, int row) {

    }

    @Override
    public int getPulseShade(int speed) {
        return 0;
    }

    @Override
    public void showOsd(boolean isFullscreen) {

    }

    @Override
    public long getTicks() {
        return 0;
    }

    @Override
    public int getcolumnwidth(int osdtextscale) {
        return 0;
    }

    @Override
    public int getrowheight(int osdtextscale) {
        return 0;
    }

    @Override
    public boolean textHandler(String text) {
        return false;
    }
}
