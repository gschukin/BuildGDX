package ru.m210projects.Build;

import ru.m210projects.Build.FileHandle.Resource;
import ru.m210projects.Build.Types.BuildPos;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.Types.Wall;
import ru.m210projects.Build.Types.collections.LinkedMap;
import ru.m210projects.Build.Types.collections.MapNode;
import ru.m210projects.Build.Types.collections.SpriteMap;
import ru.m210projects.Build.Types.collections.ValueSetter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.m210projects.Build.Engine.MAXSPRITES;
import static ru.m210projects.Build.Engine.MAXSTATUS;

public class BoardService {

    protected Board board;
    protected LinkedMap<Sprite> spriteSectMap;
    protected LinkedMap<Sprite> spriteStatMap;
    protected final AtomicInteger floorz = new AtomicInteger();
    protected final AtomicInteger ceilingz = new AtomicInteger();

    protected void initSpriteLists(Board board) {
        List<Sprite> sprites = board.getSprites();
        this.spriteStatMap = createSpriteMap(MAXSTATUS, sprites, MAXSPRITES, Sprite::setStatnum);
        this.spriteSectMap = createSpriteMap(board.getSectorCount(), sprites, MAXSPRITES, Sprite::setSectnum);
    }

    protected Board loadBoard(Resource entry) throws IOException {
        int version = entry.readInt();
        if (version != 7) {
            throw new RuntimeException("Wrong version: " + version);
        }

        BuildPos startPos = new BuildPos(entry.readInt(),
                entry.readInt(),
                entry.readInt(),
                entry.readShort(),
                entry.readShort());

        Sector[] sectors = new Sector[entry.readShort()];
        for (int i = 0; i < sectors.length; i++) {
            sectors[i] = new Sector().readObject(entry);
        }

        Wall[] walls = new Wall[entry.readShort()];
        for (int i = 0; i < walls.length; i++) {
            walls[i] = new Wall().readObject(entry);
        }

        int numSprites = entry.readShort();
        List<Sprite> sprites = new ArrayList<>(numSprites * 2);

        for (int i = 0; i < numSprites; i++) {
            sprites.add(new Sprite().readObject(entry));
        }

        return new Board(startPos, sectors, walls, sprites);
    }

    /**
     * Set loaded board
     */
    public void setBoard(Board board) {
        this.board = board;
        initSpriteLists(board);

        List<Sprite> sprites = board.getSprites();
        for (int i = 0; i < sprites.size(); i++) {
            Sprite spr = sprites.get(i);
            changespritestat(i, spr.getStatnum());
            changespritesect(i, spr.getSectnum());
        }
    }

    /**
     * Set new board
     */
    public void prepareBoard(Board board) {
        Wall[] walls = board.getWalls();
        Sector[] sectors = board.getSectors();
        List<Sprite> sprites = board.getSprites();

        this.board = board;
        initSpriteLists(board);

        // init maps for new board
        for (Sprite spr : sprites) {
            insertsprite(spr.getSectnum(), spr.getStatnum());
        }

        BuildPos startPos = board.getPos();

        // Must be after loading sectors, etc!
        int sectnum = updatesector(startPos.getX(), startPos.getY(), startPos.getSectnum());
        if (sectnum != startPos.getSectnum()) {
            startPos = new BuildPos(startPos.getX(), startPos.getY(), startPos.getZ(), startPos.getAng(), sectnum);
            this.board = new Board(startPos, sectors, walls, sprites);
        }

        Sector startSector = this.board.getSector(startPos.getSectnum());
        if (startSector == null || !startSector.inside(startPos.getX(), startPos.getY())) {
            throw new RuntimeException("Player should be in a sector!");
        }
    }

    protected SpriteMap createSpriteMap(int listCount, List<Sprite> spriteList, int spriteCount, ValueSetter<Sprite> valueSetter) {
        return new SpriteMap(listCount, spriteList, spriteCount, valueSetter);
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
            if (sec.inside(x, y)) {
                return sector;
            }

            for (Wall wall : sec.getWalls()) {
                sector = wall.getNextsector();
                sec = board.getSector(sector);
                if (sec != null && sec.inside(x, y)) {
                    return sector;
                }
            }
        }

        Sector[] sectors = board.getSectors();
        for (int i = sectors.length - 1; i >= 0; i--) {
            if (sectors[i].inside(x, y)) {
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
        if (sec != null) {
            sec.getzsofslope(dax, day, floorZ, ceilingZ);
            return true;
        }
        return false;
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
        return sector.inside(x, y, z);
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

    public MapNode<Sprite> getSectNode(int sector) {
        return spriteSectMap.getFirst(sector);
    }

    public MapNode<Sprite> getStatNode(int statnum) {
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
        if (index == -1) {
            return null;
        }

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
