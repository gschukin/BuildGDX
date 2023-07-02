package ru.m210projects.Build;

import ru.m210projects.Build.Types.font.BitmapFont;

public class EngineUtils {

    protected static Tables tables;
    protected static BitmapFont largeFont, smallFont;

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

}
