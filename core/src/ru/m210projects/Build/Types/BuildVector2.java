package ru.m210projects.Build.Types;

import ru.m210projects.Build.EngineUtils;

import static ru.m210projects.Build.Pragmas.dmulscale;

public class BuildVector2 {

    private int x, y;

    public BuildVector2(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public BuildVector2 rotate(int xpivot, int ypivot, int daang) {
        int dacos = EngineUtils.cos(daang + 2048);
        int dasin = EngineUtils.sin(daang + 2048);

        x -= xpivot;
        y -= ypivot;
        x = dmulscale(x, dacos, -y, dasin, 14) + xpivot;
        y = dmulscale(y, dacos, x, dasin, 14) + ypivot;

        return this;
    }
}
