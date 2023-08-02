package ru.m210projects.Build;

import ru.m210projects.Build.Types.Point;
import ru.m210projects.Build.Types.font.BitmapFont;

import static java.lang.Math.abs;
import static ru.m210projects.Build.Pragmas.dmulscale;

public class EngineUtils {

    protected static Tables tables;
    protected static BitmapFont largeFont, smallFont;
    private static final Point TMP_POINT = new Point();

    public static Tables init(Engine engine) throws Exception {
        tables = engine.loadtables();

        largeFont = new BitmapFont(engine, tables.textfont, 128, 128, 16, 16);
        smallFont = new BitmapFont(engine, tables.smalltextfont, 128, 128, 16, 16);

        return tables;
    }

    public static Tables getTables() {
        return tables;
    }

    public static BitmapFont getLargeFont() {
        return largeFont;
    }

    public static BitmapFont getSmallFont() {
        return smallFont;
    }

    public static int qdist(long dx, long dy) {
        dx = abs(dx);
        dy = abs(dy);

        if (dx > dy) {
            dy = (3 * dy) >> 3;
        } else {
            dx = (3 * dx) >> 3;
        }

        return (int) (dx + dy);
    }

    public static int sin(int k) {
        return tables.sin(k);
    }

    public static int cos(int k) {
        return tables.cos(k);
    }

    public static int getAngle(int xvect, int yvect) {
        return tables.getAngle(xvect, yvect);
    }

    public static int sqrt(int a) {
        return tables.sqrt(a);
    }

    public static final int[] POW2LONG = { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768,
            65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728,
            268435456, 536870912, 1073741824, 2147483647 };

    public static int powToLong(int value) {
        return POW2LONG[value];
    }

    public static Point rotatepoint(int xpivot, int ypivot, int x, int y, int daang) { // jfBuild
        int dacos = EngineUtils.cos(daang + 2048);
        int dasin = EngineUtils.sin(daang + 2048);
        x -= xpivot;
        y -= ypivot;

        TMP_POINT.set(dmulscale(x, dacos, -y, dasin, 14) + xpivot,
                dmulscale(y, dacos, x, dasin, 14) + ypivot);
        return TMP_POINT;
    }

}
