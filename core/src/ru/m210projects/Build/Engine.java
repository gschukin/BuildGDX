// "Build Engine & Tools" Copyright (c) 1993-1997 Ken Silverman
// Ken Silverman's official web site: "http://www.advsys.net/ken"
// See the included license file "BUILDLIC.TXT" for license info.
//
// This file has been modified from Ken Silverman's original release
// by Jonathon Fowler (jf@jonof.id.au)
// by the EDuke32 team (development@voidpoint.com)
// by Alexander Makarov-[M210] (m210-2007@mail.ru)

package ru.m210projects.Build;

import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Input.KeyInput;
import ru.m210projects.Build.Render.GLRenderer;
import ru.m210projects.Build.Render.GLRenderer.GLInvalidateFlag;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Render.Types.FadeEffect;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.ArtFile;
import ru.m210projects.Build.filehandle.art.CachedArtEntry;
import ru.m210projects.Build.filehandle.fs.Directory;
import ru.m210projects.Build.osd.Console;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.m210projects.Build.Net.Mmulti.uninitmultiplayer;
import static ru.m210projects.Build.Pragmas.*;
import static ru.m210projects.Build.RenderService.*;
import static ru.m210projects.Build.Strhandler.buildString;
import static ru.m210projects.Build.filehandle.art.ArtFile.DUMMY_ART_FILE;

public abstract class Engine {

    /*
     * TODO:
     * Tile in VoxelSkin
     *
     * Add try/catch to findsaves, load, save in each game
     * Software renderer: and the draw distance for voxel detail is really low
     * Software renderer: You might want to look at wall sprites. I noticed a lot of them clipping through geometry in classic render
     * Software renderer: Voxel is not clipped by ceiling geometry
     *
     * osdrows в сохранения конфига
     * Туман зависит от разрешения экрана (Polymost)
     * History list for last used IP's (client could be better for multiplayer) or copy paste IP if possible.
     * brz фильтр
     * broadcast
     * Some sort of anti-aliasing option. The NVidia control panel's anti-aliasing works, but it introduces artifacting on voxels.
     * бинд в консоль
     * плохой перенос строк в консоли если строк больше 2х
     * оптимизировать Bsprintf
     * сохранения в папку (почему то не находит файл)
     * 2д карта подглюкивает вылазиют
     * полигоны за скайбокс потолок
     * floor-alignment voxels for maphack
     *
     * Architecture:
     * 	Engine
     * 		messages
     * 		filecache
     * 		filegroup		-> 		filecache
     * 		(net) mmulti
     * 		audiomanger (BAudio)
     * 			midimodule
     * 			openal
     * 		renderer
     * 			polymost	->    texturecache
     * 			software
     * 		input
     * 			keyboard -> input()
     * 			gamepad	-> gpmanager
     *
     * 		OnScreenDisplay
     * 			Console
     * 		loader
     * 			md2
     * 			md3
     * 			vox
     * 			wav
     *
     *
     *  Utils
     *  	string handler
     *  	def loader + common + scriptfile	-> texturecache / mdloader
     *  	pragmas
     *  	bithandler
     */

    public static final String version = "22.071"; // XX. - year, XX - month, X - build

    public static Hitscan pHitInfo;
    public static Neartag neartag;
    private static KeyInput input; // FIXME static
    public static int clipmoveboxtracenum = 3;
    protected final GetZRange getZRange = new GetZRange(this);
    protected ClipMover clipmove = new ClipMover(this);
    protected final EngineService engineService = new EngineService(this);
    protected final PushMover pushMover = new PushMover(this);
    protected final BoardService boardService;
    protected final RenderService renderService;
    public static int hitscangoalx = (1 << 29) - 1, hitscangoaly = (1 << 29) - 1;
    private final HitScanner scanner = new HitScanner(this);
    private final NearScanner nearScanner = new NearScanner(this);
    private DefScript defs;

    public static int zr_ceilz, zr_ceilhit, zr_florz, zr_florhit;
    public static int clipmove_x, clipmove_y, clipmove_z;
    public static short clipmove_sectnum;

    public static int pushmove_x, pushmove_y, pushmove_z;
    public static short pushmove_sectnum;

    protected Point rotatepoint = new Point();

    // Constants

    public static Object lock = new Object();
    public static final int CLIPMASK0 = (((1) << 16) + 1);
    public static final int CLIPMASK1 = (((256) << 16) + 64);
    public static final int MAXPSKYTILES = 256;
    public static final int MAXPALOOKUPS = 256;
    public static int USERTILES = 256;
    public static int MAXTILES = 9216 + USERTILES;
    public static final int MAXSTATUS = 1024;
    public static final int DETAILPAL = (MAXPALOOKUPS - 1);
    public static final int GLOWPAL = (MAXPALOOKUPS - 2);
    public static final int SPECULARPAL = (MAXPALOOKUPS - 3);
    public static final int NORMALPAL = (MAXPALOOKUPS - 4);
    public static final int RESERVEDPALS = 4; // don't forget to increment this when adding reserved pals
    public static final int MAXSECTORSV8 = 4096;
    public static final int MAXWALLSV8 = 16384;
    public static final int MAXSPRITESV8 = 16384;
    public static final int MAXSECTORSV7 = 1024;
    public static final int MAXWALLSV7 = 8192;
    public static final int MAXSPRITESV7 = 4096;
    public static int MAXSECTORS = MAXSECTORSV7;
    public static int MAXWALLS = MAXWALLSV7;
    public static int MAXSPRITES = MAXSPRITESV7;
    public static final int MAXSPRITESONSCREEN = 1024;
    public static final int MAXVOXELS = MAXSPRITES;
    public static final int MAXUNIQHUDID = 256; // Extra slots so HUD models can store animation state without messing game sprites
    public static final int MAXPSKYMULTIS = 8;
    public static final int MAXPLAYERS = 16;

    // board variables
    public static short[] pskyoff;
    public static short[] zeropskyoff;
    public static short pskybits;
    public static byte parallaxtype;
    public static int visibility, parallaxvisibility;

    // automapping
    public static byte automapping;
    public static byte[] show2dsector;
    public static byte[] show2dwall;
    public static byte[] show2dsprite;

    // timer
    protected Timer timer;

    // tiles
    public String tilesPath = "tilesXXX.art";
    protected ArtEntry[] tiles;

    protected int randomseed;
    public static final short[] pow2char = {1, 2, 4, 8, 16, 32, 64, 128};
    public static final int[] pow2long = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483647,};

    protected PaletteManager paletteManager;

    // Engine.c

    public void InitArrays() { // gdxBuild
        pskyoff = new short[MAXPSKYTILES];
        zeropskyoff = new short[MAXPSKYTILES];

        tiles = new ArtEntry[MAXTILES];

        show2dsector = new byte[(MAXSECTORS + 7) >> 3];
        show2dwall = new byte[(MAXWALLS + 7) >> 3];
        show2dsprite = new byte[(MAXSPRITES + 7) >> 3];

        pHitInfo = new Hitscan();
        neartag = new Neartag();

        Arrays.fill(show2dsector, (byte) 0);
        Arrays.fill(show2dsprite, (byte) 0);
        Arrays.fill(show2dwall, (byte) 0);
    }

    protected Tables loadtables() throws Exception {
        return new Tables(BuildGdx.cache.getEntry("tables.dat", true));
    }

    public Engine() throws Exception { // gdxBuild
        InitArrays();

        EngineUtils.init(this); // loadtables
        this.paletteManager = loadpalette();
        this.renderService = createRenderService();
        this.boardService = createBoardService();

        parallaxtype = 2;
        pskybits = 0;
        automapping = 0;
        visibility = 512;
        parallaxvisibility = 512;

        initkeys();

        randomseed = 1; // random for voxels
    }

    public abstract void faketimerhandler();

    protected RenderService createRenderService() {
        return new RenderService(this);
    }

    protected BoardService createBoardService() {
        return new BoardService();
    }

    public PaletteManager loadpalette() throws Exception { // jfBuild
        return new DefaultPaletteManager(this, BuildGdx.cache.getEntry("palette.dat", true));
    }

    //
    // Exported Engine Functions
    //

    public void uninit() // gdxBuild
    {
        renderService.uninit();
        uninitmultiplayer();

        BuildGdx.audio.dispose();
        BuildGdx.message.dispose();
    }

    public long getticks() { // gdxBuild
        return System.currentTimeMillis();
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

    public int nextsectorneighborz(int sectnum, int thez, int topbottom, int direction) { // jfBuild
        int nextz = 0x80000000;
        if (direction == 1) {
            nextz = 0x7fffffff;
        }

        short sectortouse = -1;

        short wallid = boardService.getSector(sectnum).getWallptr();
        int i = boardService.getSector(sectnum).getWallnum(), testz;
        do {
            Wall wal = boardService.getWall(wallid);
            if (wal.getNextsector() >= 0) {
                if (topbottom == 1) {
                    testz = boardService.getSector(wal.getNextsector()).getFloorz();
                } else {
                    testz = boardService.getSector(wal.getNextsector()).getCeilingz();
                }
                if (direction == 1) {
                    if ((testz > thez) && (testz < nextz)) {
                        nextz = testz;
                        sectortouse = wal.getNextsector();
                    }
                } else {
                    if ((testz < thez) && (testz > nextz)) {
                        nextz = testz;
                        sectortouse = wal.getNextsector();
                    }
                }
            }
            wallid++;
            i--;
        } while (i != 0);

        return (sectortouse);
    }

    public void nextpage() { // gdxBuild
        faketimerhandler();
        Console.out.draw();
        BuildGdx.audio.update();
        renderService.nextpage();
    }

    public int neartag(int xs, int ys, int zs, int sectnum, int ange, Neartag near, int neartagrange, int tagsearch) { // jfBuild
        int result = nearScanner.scan(xs, ys, zs, sectnum, ange, neartagrange, tagsearch);
        NearInfo info = nearScanner.getInfo();

        near.tagsector = (short) info.getSector();
        near.tagwall = (short) info.getWall();
        near.tagsprite = (short) info.getSprite();
        near.taghitdist = info.getDistance();

        return result;

//        int i, zz, x1, y1, z1, x2, y2, endwall;
//        int topt, topu, bot, dist, offx, offy;
//        short dasector, startwall;
//        short nextsector, good;
//
//        Variable rx = new Variable();
//        Variable ry = new Variable();
//        Variable rz = new Variable();
//
//
//        near.tagsector = -1;
//        near.tagwall = -1;
//        near.tagsprite = -1;
//        near.taghitdist = 0;
//
//        if (sectnum < 0 || sectnum >= MAXSECTORS || (tagsearch & 3) == 0)
//            return 0;
//
//        int vx = mulscale(EngineUtils.sin((ange + 2560) & 2047), neartagrange, 14);
//        int xe = xs + vx;
//        int vy = mulscale(EngineUtils.sin((ange + 2048) & 2047), neartagrange, 14);
//        int ye = ys + vy;
//        int vz = 0;
//        int ze = 0;
//        IntSet sectorSet = new IntSet(MAXSECTORS);
//
//        sectorSet.addValue(sectnum);
//        for (int dacnt = 0; dacnt < sectorSet.size(); dacnt++) {
//            dasector = (short) sectorSet.getValue(dacnt);
//
//            startwall = boardService.getSector(dasector).getWallptr();
//            endwall = (startwall + boardService.getSector(dasector).getWallnum() - 1);
//            if (startwall < 0 || endwall < 0) {
//                continue;
//            }
//            for (int z = startwall; z <= endwall; z++) {
//                Wall wal = boardService.getWall(z);
//                Wall wal2 = boardService.getWall(wal.getPoint2());
//                x1 = wal.getX();
//                y1 = wal.getY();
//                x2 = wal2.getX();
//                y2 = wal2.getY();
//
//                nextsector = wal.getNextsector();
//
//                good = 0;
//                if (nextsector >= 0) {
//                    if (((tagsearch & 1) != 0) && boardService.getSector(nextsector).getLotag() != 0)
//                        good |= 1;
//                    if (((tagsearch & 2) != 0) && boardService.getSector(nextsector).getHitag() != 0)
//                        good |= 1;
//                }
//                if (((tagsearch & 1) != 0) && wal.getLotag() != 0)
//                    good |= 2;
//                if (((tagsearch & 2) != 0) && wal.getHitag() != 0)
//                    good |= 2;
//
//                if ((good == 0) && (nextsector < 0))
//                    continue;
//                if ((x1 - xs) * (y2 - ys) < (x2 - xs) * (y1 - ys))
//                    continue;
//
//                if (lIntersect(xs, ys, zs, xe, ye, ze, x1, y1, x2, y2, rx, ry, rz)) {
//                    if (good != 0) {
//                        if ((good & 1) != 0)
//                            near.tagsector = nextsector;
//                        if ((good & 2) != 0)
//                            near.tagwall = (short) z;
//                        near.taghitdist = dmulscale(rx.get() - xs, EngineUtils.sin((ange + 2560) & 2047), ry.get() - ys,
//                                EngineUtils.sin((ange + 2048) & 2047), 14);
//                        xe = rx.get();
//                        ye = ry.get();
//                        ze = rz.get();
//                    }
//                    if (nextsector >= 0) {
//                        sectorSet.addValue(nextsector);
//                    }
//                }
//            }
//
//            for (MapNode<Sprite> node = boardService.getSectNode(dasector); node != null; node = node.getNext()) {
//                int z = node.getIndex();
//                Sprite spr = boardService.getSprite(z);
//
//                good = 0;
//                if (((tagsearch & 1) != 0) && spr.getLotag() != 0)
//                    good |= 1;
//                if (((tagsearch & 2) != 0) && spr.getHitag() != 0)
//                    good |= 1;
//                if (good != 0) {
//                    x1 = spr.getX();
//                    y1 = spr.getY();
//                    z1 = spr.getZ();
//
//                    topt = vx * (x1 - xs) + vy * (y1 - ys);
//                    if (topt > 0) {
//                        bot = vx * vx + vy * vy;
//                        if (bot != 0) {
//                            int intz = zs + scale(vz, topt, bot);
//                            ArtEntry pic = getTile(spr.getPicnum());
//                            i = pic.getHeight() * spr.getYrepeat();
//                            if ((spr.getCstat() & 128) != 0)
//                                z1 += (i << 1);
//                            if ((pic.hasYOffset()))
//                                z1 -= (pic.getOffsetY() * spr.getYrepeat() << 2);
//                            if ((intz <= z1) && (intz >= z1 - (i << 2))) {
//                                topu = vx * (y1 - ys) - vy * (x1 - xs);
//                                offx = scale(vx, topu, bot);
//                                offy = scale(vy, topu, bot);
//                                dist = offx * offx + offy * offy;
//                                i = (pic.getWidth() * spr.getXrepeat());
//                                i *= i;
//                                if (dist <= (i >> 7)) {
//                                    int intx = xs + scale(vx, topt, bot);
//                                    int inty = ys + scale(vy, topt, bot);
//                                    if (klabs(intx - xs) + klabs(inty - ys) < klabs(xe - xs) + klabs(ye - ys)) {
//                                        near.tagsprite = (short) z;
//                                        near.taghitdist = dmulscale(intx - xs, EngineUtils.sin((ange + 2560) & 2047),
//                                                inty - ys, EngineUtils.sin((ange + 2048) & 2047), 14);
//                                        xe = intx;
//                                        ye = inty;
//                                        ze = intz;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return (0);
    }
    public void dragpoint(int pointhighlight, int dax, int day) { // jfBuild
        boardService.getWall(pointhighlight).setX(dax);
        boardService.getWall(pointhighlight).setY(day);

        int cnt = MAXWALLS;
        int tempshort = pointhighlight; // search points CCW
        do {
            if (boardService.getWall(tempshort).getNextwall() >= 0) {
                tempshort = boardService.getWall(boardService.getWall(tempshort).getNextwall()).getPoint2();
                boardService.getWall(tempshort).setX(dax);
                boardService.getWall(tempshort).setY(day);
            } else {
                tempshort = pointhighlight; // search points CW if not searched all the way around
                do {
                    if (boardService.getWall(lastwall(tempshort)).getNextwall() >= 0) {
                        tempshort = boardService.getWall(lastwall(tempshort)).getNextwall();
                        boardService.getWall(tempshort).setX(dax);
                        boardService.getWall(tempshort).setY(day);
                    } else {
                        break;
                    }
                    cnt--;
                } while ((tempshort != pointhighlight) && (cnt > 0));
                break;
            }
            cnt--;
        } while ((tempshort != pointhighlight) && (cnt > 0));
    }

    public int lastwall(int point) { // jfBuild
        if ((point > 0) && (boardService.getWall(point - 1).getPoint2() == point)) {
            return (point - 1);
        }

        int i = point, j;
        int cnt = MAXWALLS;
        do {
            j = boardService.getWall(i).getPoint2();
            if (j == point) {
                return (i);
            }
            i = j;
            cnt--;
        } while (cnt > 0);
        return (point);
    }

    public Point rotatepoint(int xpivot, int ypivot, int x, int y, int daang) { // jfBuild
        int dacos = EngineUtils.cos(daang + 2048);
        int dasin = EngineUtils.sin(daang + 2048);
        x -= xpivot;
        y -= ypivot;
        rotatepoint.x = dmulscale(x, dacos, -y, dasin, 14) + xpivot;
        rotatepoint.y = dmulscale(y, dacos, x, dasin, 14) + ypivot;

        return rotatepoint;
    }

    public void srand(int seed) // gdxBuild
    {
        randomseed = seed;
    }

    public int getrand() // gdxBuild
    {
        return randomseed;
    }

    public int krand() { // jfBuild
        randomseed = (randomseed * 27584621) + 1;
        return (int) ((randomseed & 0xFFFFFFFFL) >> 16);
    }

    public int rand() // gdxBuild
    {
        return (int) (Math.random() * 32767);
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

    public int loopnumofsector(int sectnum, int wallnum) { // jfBuild
        int numloops = 0;
        int startwall = boardService.getSector(sectnum).getWallptr();
        int endwall = startwall + boardService.getSector(sectnum).getWallnum();
        for (int i = startwall; i < endwall; i++) {
            if (i == wallnum) {
                return (numloops);
            }
            if (boardService.getWall(i).getPoint2() < i) {
                numloops++;
            }
        }
        return (-1);
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

    public static KeyInput getInput() // gdxBuild
    {
        return input;
    }

    public void handleevents() { // gdxBuild
        input.handleevents();
        Console.out.handleevents();
        sampletimer();
    }

    public void initkeys() { // gdxBuild
        input = new KeyInput();
    }

    public ArtEntry getTile(int tilenum) {
        if (tilenum < 0 || tilenum >= tiles.length || tiles[tilenum] == null) {
            return DUMMY_ART_FILE;
        }
        return tiles[tilenum];
    }

    public void setDefs(DefScript defs) {
        this.defs = defs;
        if (getrender() == null) {
            throw new NullPointerException("Renderer is not initialized!");
        }
        getrender().setDefs(defs);
    }

    public DefScript getDefs() {
        return defs;
    }







    public PaletteManager getPaletteManager() {
        return paletteManager;
    }

    public void inittimer(int tickspersecond) { // jfBuild
        this.timer = new Timer(tickspersecond);
    }

    public Timer getTimer() {
        return timer;
    }

    public int getTotalClock() {
        return timer.getTotalClock();
    }

    public void sampletimer() { // jfBuild
        timer.update();
    }

    public boolean rIntersect(int xs, int ys, int zs, int vx, int vy, int vz, int x1, int y1, int x2, int y2, Variable rx, Variable ry, Variable rz) {
        return engineService.rIntersect(xs, ys, zs, vx, vy, vz, x1, y1, x2, y2, rx, ry, rz);
    }

    public int clipInsideBox(int x, int y, int wallnum, int walldist) {
        return clipinsidebox(x, y, wallnum, walldist);
    }

    public int clipinsidebox(int x, int y, int wallnum, int walldist) { // jfBuild
        return engineService.clipInsideBox(x, y, wallnum, walldist);
    }

    public int clipInsideBoxLine(int x, int y, int x1, int y1, int x2, int y2, int walldist) { // jfBuild
        return clipinsideboxline(x, y, x1, y1, x2, y2, walldist);
    }

    public int clipinsideboxline(int x, int y, int x1, int y1, int x2, int y2, int walldist) { // jfBuild
        return engineService.clipInsideBoxLine(x, y, x1, y1, x2, y2, walldist);
    }

    public boolean cansee(int x1, int y1, int z1, int sect1, int x2, int y2, int z2, int sect2) { // eduke32
        return engineService.canSee(x1, y1, z1, sect1, x2, y2, z2, sect2);
    }

    public boolean lIntersect(int x1, int y1, int z1, int x2, int y2, int z2, int x3, // jfBuild
                              int y3, int x4, int y4, Variable x, Variable y, Variable z) {
        return engineService.lIntersect(x1, y1, z1, x2, y2, z2, x3, y3, x4, y4, x, y, z);
    }

    public int hitscan(int xs, int ys, int zs, int sectnum, int vx, int vy, int vz, // jfBuild
                       Hitscan hit, int cliptype) {
        scanner.setGoal(hitscangoalx, hitscangoaly);
        boolean result = scanner.run(xs, ys, zs, sectnum, vx, vy, vz, cliptype);
        HitInfo is = scanner.getInfo();
        hit.set(is.x, is.y, is.z, is.sector, is.wall, is.sprite);
        return result ? 0 : -1;
    }

    public int clipmove(int x, int y, int z, int sectnum, // jfBuild
                        long xvect, long yvect, int walldist, int ceildist, int flordist, int cliptype) {
        int result = clipmove.invoke(x, y, z, sectnum, xvect, yvect, walldist, ceildist, flordist, cliptype);

        ClipInfo info = clipmove.getInfo();
        clipmove_x = info.getX();
        clipmove_y = info.getY();
        clipmove_z = info.getZ();
        clipmove_sectnum = (short) info.getSectnum();

        return result;
    }

    public int pushmove(int x, int y, int z, int sectnum, // jfBuild
                        int walldist, int ceildist, int flordist, int cliptype) {
        int result = pushMover.move(x, y, z, sectnum, walldist, ceildist, flordist, cliptype);

        ClipInfo info = pushMover.getInfo();
        pushmove_x = info.getX();
        pushmove_y = info.getY();
        pushmove_z = info.getZ();
        pushmove_sectnum = (short) info.getSectnum();

        return result;
    }

    public void getzrange(int x, int y, int z, int sectnum, // jfBuild
                          int walldist, int cliptype) {

        RangeZInfo info = getZRange.invoke(x, y, z, sectnum, walldist, cliptype);
        zr_ceilz = info.getCeilz();
        zr_ceilhit = info.getCeilhit();
        zr_florz = info.getFlorz();
        zr_florhit = info.getFlorhit();
    }

    ////////// BOARD MANIPULATION FUNCTIONS //////////

    public Board loadboard(Entry fil) {
        Board board;
        try {
            board = boardService.loadBoard(fil);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load the map: " + fil.getName() + ". Cause: " + e);
        }

        boardService.prepareBoard(board);

        Arrays.fill(show2dsector, (byte) 0);
        Arrays.fill(show2dsprite, (byte) 0);
        Arrays.fill(show2dwall, (byte) 0);

        return board;
    }

    public Board loadboard(String filename) throws FileNotFoundException {
        Entry fil = BuildGdx.cache.getEntry(filename, true);
        if (!fil.exists()) {
            throw new FileNotFoundException("Map " + filename + " not found!");
        }
        return loadboard(fil);
    }

    public BoardService getBoardService() {
        return boardService;
    }

    public int insertsprite(int sectnum, int statnum) { // jfBuild
        return boardService.insertsprite(sectnum, statnum);
    }

    public int deletesprite(int spritenum) { // jfBuild
        GLRenderer gl = glrender();
        if (gl != null) {
            gl.removeSpriteCorr(spritenum);
        }
        boardService.deletesprite(spritenum);
        return 0;
    }

    public int changespritesect(int spritenum, int newsectnum) { // jfBuild
        boardService.changespritesect(spritenum, newsectnum);
        return (0);
    }

    public boolean changespritestat(int spritenum, int newstatnum) { // jfBuild
        return boardService.changespritestat(spritenum, newstatnum);
    }

    public boolean deletespritesect(int spritenum) {
        return boardService.deletespritesect(spritenum);
    }

    public boolean deletespritestat(int spritenum) {
        return boardService.deletespritestat(spritenum);
    }

    public int inside(int x, int y, int sectnum) { // jfBuild
        return boardService.inside(x, y, boardService.getSector(sectnum)) ? 1 : 0;
    }

    protected static int SETSPRITEZ = 0;

    public boolean setsprite(int spritenum, int newx, int newy, int newz) { // jfBuild
        return boardService.setSprite(spritenum, newx, newy, newz, SETSPRITEZ != 0);
    }

    public int sectorofwall(int theline) { // jfBuild
        return boardService.sectorOfWall(theline);
    }

    public int getceilzofslope(int sectnum, int dax, int day) { // jfBuild
        return boardService.getceilzofslope(boardService.getSector(sectnum), dax, day);
    }

    public int getflorzofslope(int sectnum, int dax, int day) { // jfBuild
        return boardService.getflorzofslope(boardService.getSector(sectnum), dax, day);
    }

    public void getzsofslope(int sectnum, int dax, int day, AtomicInteger fz, AtomicInteger cz) {
        Sector sec = boardService.getSector(sectnum);
        fz.set(sec.getFloorz());
        cz.set(sec.getCeilingz());
        boardService.getzsofslope(sec, dax, day, fz, cz);
    }

    public void alignceilslope(int dasect, int x, int y, int z) { // jfBuild
        boardService.alignSlope(dasect, x, y, z,true);
    }

    public void alignflorslope(int dasect, int x, int y, int z) { // jfBuild
        boardService.alignSlope(dasect, x, y, z, false);
    }

    public int updatesector(int x, int y, int sectnum) { // jfBuild
        return boardService.updatesector(x, y, sectnum);
    }

    public int updatesectorz(int x, int y, int z, int sectnum) { // jfBuild
        return boardService.updatesectorz(x, y, z, sectnum);
    }

    ////////// RENDERER MANIPULATION FUNCTIONS //////////

    public void printfps(float scale) {
        renderService.printfps(scale);
    }

    public int animateoffs(int tilenum, int nInfo) { // jfBuild + gdxBuild
        return renderService.animateoffs(tilenum, nInfo);
    }

    // JBF: davidoption now functions as a windowed-mode flag (0 == windowed, 1 ==
    // fullscreen)
    public boolean setgamemode(int davidoption, int daxdim, int daydim) { // jfBuild + gdxBuild
        return renderService.setgamemode(davidoption, daxdim, daydim);
    }

    public int drawrooms(float daposx, float daposy, float daposz, float daang, float dahoriz, int dacursectnum) { // eDuke32
        return renderService.drawrooms(daposx, daposy, daposz, daang, dahoriz, dacursectnum);
    }

    public void drawmasks() { // gdxBuild
        renderService.drawmasks();
    }

    public void drawmapview(int dax, int day, int zoome, int ang) { // gdxBuild
        renderService.drawmapview(dax, day, zoome, ang);
    }

    public void drawoverheadmap(int cposx, int cposy, int czoom, short cang) { // gdxBuild
        renderService.drawoverheadmap(cposx, cposy, czoom, cang);
    }

    public void registerFade(String fadename, FadeEffect effect) { // gdxBuild
        renderService.registerFade(fadename, effect);
    }

    public void updateFade(String fadename, int intensive) {// gdxBuild
        renderService.updateFade(fadename, intensive);
    }

    public void showfade() { // gdxBuild
        renderService.showfade();
    }

    public void setaspect_new() { // eduke32 aspect
        renderService.setaspect_new();
    }

    public void setview(int x1, int y1, int x2, int y2) { // jfBuild
        renderService.setview(x1, y1, x2, y2);
    }

    public void setaspect(int daxrange, int daaspect) { // jfBuild
        renderService.setaspect(daxrange, daaspect);
    }

    public void setFov(int fov) {
        renderService.setFov(fov);
    }

    public void rotatesprite(int sx, int sy, int z, int a, int picnum, // gdxBuild
                             int dashade, int dapalnum, int dastat, int cx1, int cy1, int cx2, int cy2) {
        renderService.rotatesprite(sx, sy, z, a, picnum, dashade, dapalnum, dastat, cx1, cy1, cx2, cy2);
    }

    public void rotatesprite(int sx, int sy, int z, int a, int picnum, // gdxBuild
                             int dashade, int dapalnum, int dastat) {
        renderService.rotatesprite(sx, sy, z, a, picnum, dashade, dapalnum, dastat, 0, 0, xdim - 1, ydim - 1);
    }

    public void setpalettefade(int r, int g, int b, int offset) { // jfBuild
        renderService.setpalettefade(r, g, b, offset);
    }

    public void clearview(int dacol) { // gdxBuild
        renderService.clearview(dacol);
    }

    public void setviewtotile(int tilenume) { // jfBuild
        renderService.setviewtotile(tilenume);
    }

    public void setviewback() { // jfBuild
        renderService.setviewback();
    }

    public void preparemirror(int dax, int day, int daz, float daang, float dahoriz, int dawall, int dasector) { // jfBuild
        renderService.preparemirror(dax, day, daz, daang, dahoriz, dawall, dasector);
    }

    public void completemirror() {
        renderService.completemirror();
    }

    public void setbrightness(int dabrightness, byte[] dapal, GLInvalidateFlag flags) {
        renderService.setbrightness(dabrightness, dapal, flags);
    }

    public String screencapture(Directory dir, String fn) { // jfBuild + gdxBuild (screenshot)
        return renderService.screencapture(dir, fn);
    }

    protected byte[] setviewbuf() { // gdxBuild
        return renderService.setviewbuf();
    }

    public boolean setrendermode(Renderer render) { // gdxBuild
        return renderService.setrendermode(render);
    }

    public Renderer getrender() { // gdxBuild
        return renderService.getrender();
    }

    public GLRenderer glrender() {
        return renderService.glrender();
    }

    public void setgotpic(int tilenume) { // jfBuild
        renderService.setgotpic(tilenume);
    }

    public static class Point {
        private int x, y, z;

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public Point set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;

            return this;
        }

        public Point set(int x, int y) {
            this.x = x;
            this.y = y;
            this.z = 0;

            return this;
        }

        public boolean equals(int x, int y) {
            return this.x == x && this.y == y;
        }

        public boolean equals(int x, int y, int z) {
            return this.x == x && this.y == y && this.z == z;
        }
    }
}
