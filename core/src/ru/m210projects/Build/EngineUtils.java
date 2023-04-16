package ru.m210projects.Build;

public class EngineUtils {

    protected static Tables tables;

    public static Tables init(Engine engine) throws Exception {
        tables = engine.loadtables();
        return tables;
    }

    public static Tables getTables() {
        return tables;
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
