package ru.m210projects.Build;

import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.Types.Wall;
import ru.m210projects.Build.Types.collections.SpriteNode;

import java.util.concurrent.atomic.AtomicInteger;

import static ru.m210projects.Build.Engine.*;

public class BoardService {

    public final AtomicInteger floorz = new AtomicInteger();
    public final AtomicInteger ceilingz = new AtomicInteger();
    Engine engine;

    public BoardService(Engine engine) {
        this.engine = engine;
    }

    public boolean isValidSector(int sectnum) {
        return Gameutils.isValidSector(sectnum);
    }

    public Sector getSector(int dasector) {
        if (!isValidSector(dasector)) {
            return null;
        }
        return sector[dasector];
    }

    public Wall getWall(int wallptr) {
        return wall[wallptr];
    }

    public Wall getNextWall(Wall wall) {
        return getWall(wall.getPoint2());
    }

    public boolean inside(int x, int y, Sector sector) {
        if (sector == null) {
            return false;
        }

        int cnt = 0;
        short start = sector.getWallptr();
        int end = start + sector.getWallnum() - 1;
        for (short w = start; w <= end; w++) {
            Wall wal = getWall(w);
            Wall wal2 = getNextWall(wal);
            int y1 = wal.getY() - y;
            int y2 = wal2.getY() - y;

            if ((y1 ^ y2) < 0) {
                int x1 = wal.getX() - x;
                int x2 = wal2.getX() - x;
                if ((x1 ^ x2) >= 0) {
                    cnt ^= x1;
                } else {
                    cnt ^= (x1 * y2 - x2 * y1) ^ y2;
                }
            }
        }

        return (cnt >>> 31) != 0;
    }

    public boolean getzsofslope(Sector sec, int dax, int day, AtomicInteger floorZ, AtomicInteger ceilingZ) {
        int floorz = sec.getFloorz();
        int ceilingz = sec.getCeilingz();
        boolean floorSlope = sec.isFloorSlope() && floorZ != null;
        boolean ceilingSlope = sec.isCeilingSlope() && ceilingZ != null;
        if (floorSlope || ceilingSlope) {
            Wall wal = getWall(sec.getWallptr());
            Wall wal2 = getWall(wal.getPoint2());

            int dx = wal2.getX() - wal.getX();
            int dy = wal2.getY() - wal.getY();
            int i = EngineUtils.sqrt(dx * dx + dy * dy) << 5;
            if (i != 0) {
                int j = dx * (day - wal.getY()) - (dy * (dax - wal.getX())) >> 3;
                if (ceilingSlope) {
                    ceilingz += ((long) sec.getCeilingheinum() * j / i);
                }

                if (floorSlope) {
                    floorz += ((long) sec.getFloorheinum() * j / i);
                }
            }
        }

        if (floorZ != null) {
            floorZ.set(floorz);
        }

        if (ceilingZ != null) {
            ceilingZ.set(ceilingz);
        }

        return true;
    }

    public SpriteNode getSectNode(int dasector) {
        return spriteSectMap.getFirst(dasector);
    }

    public Sprite getSprite(int z) {
        return sprite[z];
    }

    public int getflorzofslope(Sector sec, int x, int y) {
        getzsofslope(sec, x, y, floorz, null);
        return floorz.get();
    }

    public int getceilzofslope(Sector sec, int x, int y) {
        getzsofslope(sec, x, y, null, ceilingz);
        return ceilingz.get();
    }

    public int updatesector(int x, int y, int sectnum) {
        return engine.updatesector(x, y, sectnum);
    }

    public int getSectorCount() {
        return numsectors;
    }
}
