package ru.m210projects.Build;

import ru.m210projects.Build.Types.BuildPos;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.Types.Wall;

import java.util.List;

public class Board {

    private final BuildPos pos;
    private final Sector[] sectors;
    private final Wall[] walls;
    private final List<Sprite> sprites;

    public Board(BuildPos pos, Sector[] sectors, Wall[] walls, List<Sprite> sprites) {
        this.pos = pos;
        this.sectors = sectors;
        this.walls = walls;
        this.sprites = sprites;
    }

    public BuildPos getPos() {
        return pos;
    }

    public List<Sprite> getSprites() {
        return sprites;
    }

    public Sector[] getSectors() {
        return sectors;
    }

    public Wall[] getWalls() {
        return walls;
    }

    public Sprite getSprite(int index) {
        if(index < 0 || index >= sprites.size()) {
            return null;
        }
        return sprites.get(index);
    }

    public Wall getWall(int index) {
        if(index < 0 || index >= walls.length) {
            return null;
        }
        return walls[index];
    }


    public Sector getSector(int index) {
        if(index < 0 || index >= sectors.length) {
            return null;
        }
        return sectors[index];
    }

    public int getWallCount() {
        return walls.length;
    }

    public int getSectorCount() {
        return sectors.length;
    }

    public int getSpriteCount() {
        return sprites.size();
    }

    public boolean isValidSector(int sectnum) {
        return sectnum >= 0 && sectnum < sectors.length;
    }

    public boolean isValidWall(int wallnum) {
        return wallnum >= 0 && wallnum < walls.length;
    }

    public boolean isValidSprite(int spritenum) {
        return spritenum >= 0 && spritenum < sprites.size();
    }
}
