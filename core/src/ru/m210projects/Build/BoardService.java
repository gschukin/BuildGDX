package ru.m210projects.Build;

import ru.m210projects.Build.FileHandle.Resource;
import ru.m210projects.Build.Types.BuildPos;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.Types.Wall;
import ru.m210projects.Build.Types.collections.LinkedMap;
import ru.m210projects.Build.Types.collections.MapNode;
import ru.m210projects.Build.Types.collections.SpriteMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BoardService {

    protected Board board;
    protected LinkedMap<Sprite> spriteSectMap;
    protected LinkedMap<Sprite> spriteStatMap;
    protected final AtomicInteger floorz = new AtomicInteger();
    protected final AtomicInteger ceilingz = new AtomicInteger();

    public boolean load(Resource entry) {
        Board oldBoard = board;

        int version = entry.readInt();
        if (version != 7) {
            return false;
        }

        BuildPos startPos = new BuildPos(entry.readInt(),
                entry.readInt(),
                entry.readInt(),
                entry.readShort(),
                entry.readShort());

        Sector[] sectors = new Sector[entry.readShort()];
        for (int i = 0; i < sectors.length; i++) {
            Sector sec = new Sector();

            sec.setWallptr(entry.readShort());
            sec.setWallnum(entry.readShort());
            sec.setCeilingz(entry.readInt());
            sec.setFloorz(entry.readInt());
            sec.setCeilingstat(entry.readShort());
            sec.setFloorstat(entry.readShort());
            sec.setCeilingpicnum(entry.readShort());
            sec.setCeilingheinum(entry.readShort());
            sec.setCeilingshade((byte) entry.readByte());
            sec.setCeilingpal(entry.readByte());
            sec.setCeilingxpanning(entry.readByte());
            sec.setCeilingypanning(entry.readByte());
            sec.setFloorpicnum(entry.readShort());
            sec.setFloorheinum(entry.readShort());
            sec.setFloorshade((byte) entry.readByte());
            sec.setFloorpal(entry.readByte());
            sec.setFloorxpanning(entry.readByte());
            sec.setFloorypanning(entry.readByte());
            sec.setVisibility(entry.readByte());
            sec.setFiller(entry.readByte());
            sec.setLotag(entry.readShort());
            sec.setHitag(entry.readShort());
            sec.setExtra(entry.readShort());

            sectors[i] = sec;
        }

        Wall[] walls = new Wall[entry.readShort()];
        for (int i = 0; i < walls.length; i++) {
            Wall wal = new Wall();

            wal.setX(entry.readInt());
            wal.setY(entry.readInt());
            wal.setPoint2(entry.readShort());
            wal.setNextwall(entry.readShort());
            wal.setNextsector(entry.readShort());
            wal.setCstat(entry.readShort());
            wal.setPicnum(entry.readShort());
            wal.setOverpicnum(entry.readShort());
            wal.setShade((byte) entry.readByte());
            wal.setPal(entry.readByte());
            wal.setXrepeat(entry.readByte());
            wal.setYrepeat(entry.readByte());
            wal.setXpanning(entry.readByte());
            wal.setYpanning(entry.readByte());
            wal.setLotag(entry.readShort());
            wal.setHitag(entry.readShort());
            wal.setExtra(entry.readShort());

            walls[i] = wal;
        }

        int numSprites = entry.readShort();
        List<Sprite> sprites = new ArrayList<>(numSprites * 2);

        for (int i = 0; i < numSprites; i++) {
            Sprite spr = new Sprite();
            spr.setX(entry.readInt());
            spr.setY(entry.readInt());
            spr.setZ(entry.readInt());
            spr.setCstat(entry.readShort());
            spr.setPicnum(entry.readShort());
            spr.setShade((byte) entry.readByte());
            spr.setPal(entry.readByte());
            spr.setClipdist(entry.readByte());
            spr.setDetail(entry.readByte());
            spr.setXrepeat(entry.readByte());
            spr.setYrepeat(entry.readByte());
            spr.setXoffset((byte) entry.readByte());
            spr.setYoffset((byte) entry.readByte());
            spr.setSectnum(entry.readShort());
            spr.setStatnum(entry.readShort());
            spr.setAng(entry.readShort());
            spr.setOwner(entry.readShort());
            spr.setXvel(entry.readShort());
            spr.setYvel(entry.readShort());
            spr.setZvel(entry.readShort());
            spr.setLotag(entry.readShort());
            spr.setHitag(entry.readShort());
            spr.setExtra(entry.readShort());
            sprites.add(spr);
        }

        // sector wall array init with wall checking
        for (int s = 0; s < sectors.length; s++) {
            Sector sec = sectors[s];

            Wall[] sectorWalls = new Wall[sec.getWallnum()];

            int w = 0;
            int startWall = sec.getWallptr();
            int endWall = startWall + sec.getWallnum() - 1;
            for (int i = startWall; i <= endWall; i++) {
                Wall wal = walls[i];
                if (wal == null || wal.getPoint2() < 0 || wal.getPoint2() >= walls.length || walls[wal.getPoint2()] == null) {
                    System.out.println(String.format("Sector %d has corrupt contour", s));
                    sec.setWallnum(0);
                    sec.setWallptr(0);
                    sectorWalls = new Wall[0];
                    break;
                }
                sectorWalls[w++] = wal;
            }
            sec.setWalls(sectorWalls);
        }

        this.board = new Board(startPos, sectors, walls, sprites);

        this.spriteStatMap = new SpriteMap(1024, sprites, 1024, Sprite::setStatnum);
        this.spriteSectMap = new SpriteMap(sectors.length, sprites, 1024, Sprite::setSectnum);

        // init maps for new board
        for (int i = 0; i < numSprites; i++) {
            Sprite spr = sprites.get(i);
            insertsprite(spr.getSectnum(), spr.getStatnum());
        }

        // Must be after loading sectors, etc!
        int sectnum = updatesector(startPos.getX(), startPos.getY(), startPos.getSectnum());
        if (sectnum != startPos.getSectnum()) {
            startPos = new BuildPos(startPos.getX(), startPos.getY(), startPos.getZ(), startPos.getAng(), sectnum);
            this.board = new Board(startPos, sectors, walls, sprites);
        }

        Sector startSector = board.getSector(startPos.getSectnum());
        if (startSector == null || !inside(startPos.getX(), startPos.getY(), startSector)) {
            throw new RuntimeException("Player should be in a sector!");
        }

        return true;


//        this.board = oldBoard;
//        return false;
    }

    public int insertspritesect(int sectnum) {
        return spriteSectMap.insert(sectnum);
    }

    public int insertspritestat(int newstatnum) {
        return spriteStatMap.insert(newstatnum);
    }

    public int insertsprite(int sectnum, int statnum) {
        insertspritestat(statnum);
        return (insertspritesect(sectnum));
    }

    public boolean deletesprite(int spritenum) {
        deletespritestat(spritenum);
        return (deletespritesect(spritenum));
    }

    public boolean changespritesect(int spritenum, int newsectnum) {
        return spriteSectMap.set(spritenum, newsectnum);
    }

    public boolean changespritestat(int spritenum, int newstatnum) {
        return spriteStatMap.set(spritenum, newstatnum);
    }

    public boolean deletespritesect(int spritenum) {
        return spriteSectMap.remove(spritenum);
    }

    public boolean deletespritestat(int spritenum) {
        return spriteStatMap.remove(spritenum);
    }

    public boolean setSprite(int spritenum, int newx, int newy, int newz, boolean checkZ) {
        Sprite sprite = board.getSprite(spritenum);
        if (sprite == null) {
            return false;
        }

        sprite.setX(newx);
        sprite.setY(newy);
        sprite.setZ(newz);
        int sectnum = sprite.getSectnum();
        if (checkZ) {
            sectnum = updatesectorz(newx, newy, newz, sectnum);
        } else {
            sectnum = updatesector(newx, newy, sectnum);
        }

        if (sectnum < 0) {
            return false;
        }

        if (sectnum != sprite.getSectnum()) {
            changespritesect(spritenum, sectnum);
        }

        return true;
    }

    ////////// WALL MANIPULATION FUNCTIONS //////////

    public void dragWall(int index, int dax, int day) {
        final Wall[] walls = board.getWalls();

        walls[index].setX(dax);
        walls[index].setY(day);

        int cnt = board.getWallCount();
        int wallIndex = index; // search points CCW
        do {
            Wall wall = walls[wallIndex];
            if (wall.getNextwall() >= 0) {
                wallIndex = walls[wall.getNextwall()].getPoint2();
                walls[wallIndex].setX(dax);
                walls[wallIndex].setY(day);
            } else {
                wallIndex = index; // search points CW if not searched all the way around
                do {
                    wall = walls[getLastWall(wallIndex)];
                    if (wall.getNextwall() >= 0) {
                        wallIndex = wall.getNextwall();
                        walls[wallIndex].setX(dax);
                        walls[wallIndex].setY(day);
                    } else {
                        break;
                    }
                    cnt--;
                } while ((wallIndex != index) && (cnt > 0));

                break;
            }
            cnt--;
        } while ((wallIndex != index) && (cnt > 0));
    }

    public int getLastWall(int point) {
        final Wall[] walls = board.getWalls();

        if ((point > 0) && (walls[point - 1].getPoint2() == point)) {
            return (point - 1);
        }

        int i = point, j;
        int cnt = board.getWallCount();
        do {
            j = walls[i].getPoint2();
            if (j == point) {
                return (i);
            }
            i = j;
            cnt--;
        } while (cnt > 0);

        return (point);
    }

    ////////// SECTOR MANIPULATION FUNCTIONS ////////
    public void alignSlope(int dasect, int x, int y, int z, boolean isCeilingSlope) {
        final Sector sector = board.getSector(dasect);
        final Wall wal = board.getWall(sector.getWallptr());
        final Wall wal2 = board.getWall(wal.getPoint2());

        int dax = wal2.getX() - wal.getX();
        int day = wal2.getY() - wal.getY();

        int i = (y - wal.getY()) * dax - (x - wal.getX()) * day;
        if (i == 0) {
            return;
        }

        if (isCeilingSlope) {
            sector.setCeilingheinum((int) (((long) (z - sector.getCeilingz()) << 8) * EngineUtils.sqrt(dax * dax + day * day) / i));
            int ceilingstat = sector.getCeilingstat();
            if (sector.getCeilingheinum() == 0) {
                sector.setCeilingstat(ceilingstat & ~2);
            } else {
                sector.setCeilingstat(ceilingstat | 2);
            }
        } else {
            sector.setFloorheinum((int) (((long) (z - sector.getFloorz()) << 8) * EngineUtils.sqrt(dax * dax + day * day) / i));
            int floorstat = sector.getFloorstat();
            if (sector.getFloorheinum() == 0) {
                sector.setFloorstat(floorstat & ~2);
            } else {
                sector.setFloorstat(floorstat | 2);
            }
        }
    }

    public int updatesector(int x, int y, int sector) {
        Sector sec = board.getSector(sector);
        if (sec != null) {
            if (inside(x, y, sec)) {
                return sector;
            }

            for (Wall wall : sec.getWalls()) {
                sector = wall.getNextsector();
                if (inside(x, y, board.getSector(sector))) {
                    return sector;
                }
            }
        }

        for (int i = (board.getSectorCount() - 1); i >= 0; i--) {
            if (inside(x, y, board.getSector(i))) {
                return i;
            }
        }

        return -1;
    }

    public int updatesectorz(int x, int y, int z, int sectnum) {
        Sector sec = board.getSector(sectnum);
        if (sec != null) {
            if (insidez(x, y, z, sec)) {
                return sectnum;
            }

            for (Wall wall : sec.getWalls()) {
                sectnum = wall.getNextsector();
                if (insidez(x, y, z, board.getSector(sectnum))) {
                    return sectnum;
                }
            }
        }

        for (int i = (board.getSectorCount() - 1); i >= 0; i--) {
            if (insidez(x, y, z, board.getSector(i))) {
                return i;
            }
        }

        return -1;
    }

    public boolean getzsofslope(Sector sec, int dax, int day, AtomicInteger floorZ, AtomicInteger ceilingZ) {
        int floorz = sec.getFloorz();
        int ceilingz = sec.getCeilingz();
        boolean floorSlope = sec.isFloorSlope() && floorZ != null;
        boolean ceilingSlope = sec.isCeilingSlope() && ceilingZ != null;
        if (floorSlope || ceilingSlope) {
            Wall wal = board.getWall(sec.getWallptr());
            Wall wal2 = board.getWall(wal.getPoint2());

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

    public int getceilzofslope(Sector sec, int x, int y) {
        getzsofslope(sec, x, y, null, ceilingz);
        return ceilingz.get();
    }

    public int getflorzofslope(Sector sec, int x, int y) {
        getzsofslope(sec, x, y, floorz, null);
        return floorz.get();
    }

    public int sectorOfWall(int theline) {
        Wall wall = board.getWall(theline);
        int i = wall.getNextwall();
        if (i >= 0) {
            return board.getWall(i).getNextsector();
        }

        Sector[] sectors = board.getSectors();
        int gap = (board.getSectorCount() >> 1);
        i = gap;
        while (gap > 1) {
            gap >>= 1;
            if (sectors[i].getWallptr() < theline) {
                i += gap;
            } else {
                i -= gap;
            }
        }

        while (sectors[i].getWallptr() > theline) {
            i--;
        }

        while (sectors[i].getWallptr() + sectors[i].getWallnum() <= theline) {
            i++;
        }
        return (i);
    }

    public boolean insidez(int x, int y, int z, Sector sector) {
        if (sector == null) {
            return false;
        }

        if (getzsofslope(sector, x, y, floorz, ceilingz)
                && (z >= ceilingz.get()) && (z <= floorz.get())) {
            return inside(x, y, sector);
        }
        return false;
    }

    public boolean inside(int x, int y, Sector sector) {
        if (sector == null) {
            return false;
        }

        int cnt = 0;
        for (Wall wal : sector.getWalls()) {
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

    public int nextSectorNeighborZ(int sectnum, int thez, int topbottom, int direction) { // jfBuild
        Sector sec = board.getSector(sectnum);
        if (sec == null) {
            return -1;
        }

        int sectorToUse = -1, testz;
        int nextz = 0x80000000;
        if (direction == 1) {
            nextz = 0x7fffffff;
        }

        for (Wall wal : sec.getWalls()) {
            int nextSectNum = wal.getNextsector();
            if (nextSectNum >= 0) {
                Sector nextSector = board.getSector(nextSectNum);
                if (nextSector != null) {
                    if (topbottom == 1) {
                        testz = nextSector.getFloorz();
                    } else {
                        testz = nextSector.getCeilingz();
                    }

                    if (direction == 1) {
                        if ((testz > thez) && (testz < nextz)) {
                            nextz = testz;
                            sectorToUse = nextSectNum;
                        }
                    } else {
                        if ((testz < thez) && (testz > nextz)) {
                            nextz = testz;
                            sectorToUse = nextSectNum;
                        }
                    }
                }
            }
        }

        return sectorToUse;
    }

    public Board getBoard() {
        return board;
    }

    public MapNode getSectNode(int sector) {
        return spriteSectMap.getFirst(sector);
    }

    public MapNode getStatNode(int statnum) {
        return spriteStatMap.getFirst(statnum);
    }

    public boolean isValidSector(int index) {
        return board.isValidSector(index);
    }

    public boolean isValidWall(int index) {
        return board.isValidWall(index);
    }

    public boolean isValidSprite(int index) {
        return board.isValidSprite(index);
    }

    public Sector getSector(int index) {
        return board.getSector(index);
    }

    public Wall getWall(int index) {
        return board.getWall(index);
    }

    public Sprite getSprite(int index) {
        return board.getSprite(index);
    }

    public Wall getNextWall(Wall wall) {
        return board.getWall(wall.getPoint2());
    }

    public int getSectorCount() {
        return board.getSectorCount();
    }

    public int getWallCount() {
        return board.getWallCount();
    }

    public int getSpriteCount() {
        return board.getSpriteCount();
    }
}
