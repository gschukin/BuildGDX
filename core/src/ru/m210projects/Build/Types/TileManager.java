package ru.m210projects.Build.Types;

import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.ArtFile;
import ru.m210projects.Build.filehandle.art.CachedArtEntry;

import java.util.Arrays;

import static ru.m210projects.Build.Engine.MAXTILES;
import static ru.m210projects.Build.filehandle.art.ArtFile.DUMMY_ART_FILE;

public class TileManager {

    // tiles
    protected String tilesPath = "tilesXXX.art";
    protected ArtEntry[] tiles;

    public TileManager() {
        this.tiles = new ArtEntry[MAXTILES];
    }

    public boolean loadpic(Entry artFile) { // gdxBuild
        if (artFile.exists()) {
            ArtFile art = new ArtFile(artFile.getName(), artFile::getInputStream);
            for (Entry artEntry : art.getEntries()) {
                ArtEntry tile = ((ArtEntry) artEntry);
                tiles[tile.getNum()] = tile;
            }
            return true;
        }

        return false;
    }

    public int loadpics() {
        char[] artFileName = tilesPath.toCharArray();
        Arrays.fill(tiles, null);

        int k;
        int numtilefiles = 0;
        do {
            k = numtilefiles;

            artFileName[7] = (char) ((k % 10) + 48);
            artFileName[6] = (char) (((k / 10) % 10) + 48);
            artFileName[5] = (char) (((k / 100) % 10) + 48);
            String name = String.copyValueOf(artFileName);
            if (!loadpic(BuildGdx.cache.getEntry(name, true))) {
                break;
            }
            numtilefiles++;
        } while (k != numtilefiles);

        return (numtilefiles);
    }

    public byte[] loadtile(int tilenume) { // jfBuild
        ArtEntry pic = getTile(tilenume);
        return pic.getBytes();
    }

    public CachedArtEntry allocatepermanenttile(int tilenume, int xsiz, int ysiz) { // jfBuild
        if ((xsiz < 0) || (ysiz < 0) || (tilenume >= MAXTILES)) {
            return null;
        }

        int dasiz = xsiz * ysiz;
        byte[] data = new byte[dasiz];
        CachedArtEntry entry = new CachedArtEntry(this, tilenume, data, xsiz, ysiz);
        tiles[tilenume] = entry;
        return entry;
    }

    public ArtEntry getTile(int tilenum) {
        if (tilenum < 0 || tilenum >= tiles.length || tiles[tilenum] == null) {
            return DUMMY_ART_FILE;
        }
        return tiles[tilenum];
    }
}
