package ru.m210projects.Build.Types;

import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Engine;
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
    protected Engine engine;

    public TileManager(Engine engine) {
        this.tiles = new ArtEntry[MAXTILES];
        this.engine = engine;
    }

    public String getTilesPath() {
        return tilesPath;
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
        CachedArtEntry entry = new CachedArtEntry(engine, tilenume, data, xsiz, ysiz);
        tiles[tilenume] = entry;
        return entry;
    }

    public ArtEntry getTile(int tilenum) {
        if (tilenum < 0 || tilenum >= tiles.length || tiles[tilenum] == null) {
            return DUMMY_ART_FILE;
        }
        return tiles[tilenum];
    }

    public void copytilepiece(int tilenume1, int sx1, int sy1, int xsiz, int ysiz, // jfBuild
                              int tilenume2, int sx2, int sy2) {

        ArtEntry pic1 = getTile(tilenume1);
        ArtEntry pic2 = getTile(tilenume2);

        int xsiz1 = pic1.getWidth();
        int ysiz1 = pic1.getHeight();
        int xsiz2 = pic2.getWidth();
        int ysiz2 = pic2.getHeight();
        if ((xsiz1 > 0) && (ysiz1 > 0) && (xsiz2 > 0) && (ysiz2 > 0)) {
            byte[] data1 = pic1.getBytes();
            byte[] data2 = pic2.getBytes();

            int x1 = sx1;
            for (int i = 0; i < xsiz; i++) {
                int y1 = sy1;
                for (int j = 0; j < ysiz; j++) {
                    int x2 = sx2 + i;
                    int y2 = sy2 + j;
                    if ((x2 >= 0) && (y2 >= 0) && (x2 < xsiz2) && (y2 < ysiz2)) {
                        byte ptr = data1[x1 * ysiz1 + y1];
                        if (ptr != (byte) 255) {
                            data2[x2 * ysiz2 + y2] = ptr;
                        }
                    }

                    y1++;
                    if (y1 >= ysiz1) {
                        y1 = 0;
                    }
                }
                x1++;
                if (x1 >= xsiz1) {
                    x1 = 0;
                }
            }
        }
    }

    public void squarerotatetile(int tilenume) {
        ArtEntry pic = getTile(tilenume);
        int xsiz = pic.getWidth();
        int ysiz = pic.getHeight();

        // supports square tiles only for rotation part
        if (xsiz == ysiz) {
            int k = (xsiz << 1);
            int ptr1, ptr2;
            for (int i = xsiz - 1, j; i >= 0; i--) {
                ptr1 = i * (xsiz + 1);
                ptr2 = ptr1;
                if ((i & 1) != 0) {
                    ptr1--;
                    ptr2 -= xsiz;
                    squarerotatetileswap(tilenume, ptr1, ptr2);
                }
                for (j = (i >> 1) - 1; j >= 0; j--) {
                    ptr1 -= 2;
                    ptr2 -= k;
                    squarerotatetileswap(tilenume, ptr1, ptr2);
                    squarerotatetileswap(tilenume, ptr1 + 1, ptr2 + xsiz);
                }
            }
        }
    }

    private void squarerotatetileswap(int tilenume, int p1, int p2) {
        ArtEntry pic = getTile(tilenume);
        if (pic != null) {
            byte[] data = pic.getBytes();

            if (p1 < data.length && p2 < data.length) {
                byte tmp = data[p1];
                data[p1] = data[p2];
                data[p2] = tmp;
            }
        }
    }
}
