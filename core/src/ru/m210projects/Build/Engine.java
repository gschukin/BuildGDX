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
import ru.m210projects.Build.FileHandle.FileResource;
import ru.m210projects.Build.FileHandle.Resource;
import ru.m210projects.Build.FileHandle.Resource.Whence;
import ru.m210projects.Build.Input.KeyInput;
import ru.m210projects.Build.OnSceenDisplay.Console;
import ru.m210projects.Build.OnSceenDisplay.DEFOSDFUNC;
import ru.m210projects.Build.Render.GLRenderer;
import ru.m210projects.Build.Render.GLRenderer.GLInvalidateFlag;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Render.Types.FadeEffect;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.Types.collections.SpriteMap;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Iterator;

import static java.lang.Math.*;
import static ru.m210projects.Build.Gameutils.isValidSector;
import static ru.m210projects.Build.Gameutils.isValidWall;
import static ru.m210projects.Build.Net.Mmulti.uninitmultiplayer;
import static ru.m210projects.Build.Pragmas.*;
import static ru.m210projects.Build.RenderService.transluc;
import static ru.m210projects.Build.Strhandler.buildString;

public abstract class Engine {

    /*
     * TODO:
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

    public static final String version = "21.113"; // XX. - year, XX - month, X - build

    public static final byte CEIL = 0;
    public static final byte FLOOR = 1;
    public static Hitscan pHitInfo;
    public static Neartag neartag;
    private static KeyInput input; // FIXME static
    public static int clipmoveboxtracenum = 3;
    protected final GetZRange getZRange = new GetZRange(this);
    protected ClipMover clipmove = new ClipMover(this);
    protected final EngineService engineService = new EngineService(this);
    protected final PushMover pushMover = new PushMover(this);
    protected final BoardService boardService = new BoardService(this);
    protected RenderService renderService;
    public static int hitscangoalx = (1 << 29) - 1, hitscangoaly = (1 << 29) - 1;
    private final HitScanner scanner = new HitScanner(this);
    private final NearScanner nearScanner = new NearScanner(this);
    private DefScript defs;

    public static int zr_ceilz, zr_ceilhit, zr_florz, zr_florhit;
    public static int clipmove_x, clipmove_y, clipmove_z;
    public static short clipmove_sectnum;

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

    // palette
    public static short numshades;
    public static byte[] palette;
    public static byte[][] palookup;
    public static byte[][] palookupfog;
    public static Palette curpalette;
    public static int paletteloaded = 0;



    // board
    public static short numsectors, numwalls, numsprites;
    public static short[] pskyoff;
    public static short[] zeropskyoff;
    public static short pskybits;
    public static byte parallaxtype;
    public static int visibility, parallaxvisibility;
    protected static Sector[] sector;
    protected static Wall[] wall;
    protected static Sprite[] sprite;
    public static SpriteMap spriteSectMap;
    public static SpriteMap spriteStatMap;

    // automapping
    public static byte automapping;
    public static byte[] show2dsector;
    public static byte[] show2dwall;
    public static byte[] show2dsprite;

    // timer
    public static int totalclock;
    protected int timerfreq;
    protected long timerlastsample;
    public static int timerticspersec;

    // tiles
    public String tilesPath = "tilesXXX.art";
    protected Tile[] tiles;
    public static int[] picsiz;
    protected int numtiles;
    private final char[] artfilename = new char[12];
    protected Resource artfil = null;
    protected int artfilnum;
    protected int artfilplc;
    protected int[] tilefilenum;
    protected int[] tilefileoffs;

    protected int randomseed;
    protected static int[] zofslope;
    public static final short[] pow2char = {1, 2, 4, 8, 16, 32, 64, 128};
    public static final int[] pow2long = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483647,};

    public static Sector getSector(int index) {
        return sector[index];
    }

    public static Wall getWall(int index) {
        return wall[index];
    }

    public static Sprite getSprite(int index) {
        return sprite[index];
    }

    // FIXME: delete this in the future
    public static void setWall(int index, Wall wal) {
        wall[index] = wal;
    }

    // FIXME: delete this in the future
    public static void setSector(int index, Sector sec) {
        sector[index] = sec;
    }

    // FIXME: delete this in the future
    public static void setSprite(int index, Sprite sec) {
        sprite[index] = sec;
    }


    // Engine.c

    public void InitArrays() { // gdxBuild
        zofslope = new int[2];
        palookupfog = new byte[MAXPALOOKUPS][3];
        pskyoff = new short[MAXPSKYTILES];
        zeropskyoff = new short[MAXPSKYTILES];

        tiles = new Tile[MAXTILES];

        show2dsector = new byte[(MAXSECTORS + 7) >> 3];
        show2dwall = new byte[(MAXWALLS + 7) >> 3];
        show2dsprite = new byte[(MAXSPRITES + 7) >> 3];
        sector = new Sector[MAXSECTORS];
        wall = new Wall[MAXWALLS];
        sprite = new Sprite[MAXSPRITES];

        pHitInfo = new Hitscan();
        neartag = new Neartag();
        picsiz = new int[MAXTILES];
        tilefilenum = new int[MAXTILES];
        tilefileoffs = new int[MAXTILES];

        Arrays.fill(show2dsector, (byte) 0);
        Arrays.fill(show2dsprite, (byte) 0);
        Arrays.fill(show2dwall, (byte) 0);
    }

    public Engine() throws Exception { // gdxBuild
        InitArrays();

        // loadtables
        EngineUtils.init(this);
        this.renderService = new RenderService(this);

        parallaxtype = 2;
        pskybits = 0;
        paletteloaded = 0;
        automapping = 0;
        totalclock = 0;
        visibility = 512;
        parallaxvisibility = 512;

        loadpalette();

        initkeys();

        Console.setFunction(new DEFOSDFUNC(this));
        randomseed = 1; // random for voxels
    }

    public Tables getTables() {
        return EngineUtils.tables;
    }

    public boolean rIntersect(int xs, int ys, int zs, int vx, int vy, int vz, int x1, int y1, int x2, int y2, Variable rx, Variable ry, Variable rz) {
        return engineService.rIntersect(xs, ys, zs, vx, vy, vz, x1, y1, x2, y2, rx, ry, rz);
    }

    public BoardService getBoardService() {
        return boardService;
    }

    public int getpalookup(int davis, int dashade) { // jfBuild
        return (min(max(dashade + (davis >> 8), 0), numshades - 1));
    }

    protected Tables loadtables() throws Exception { // jfBuild
        return new Tables(BuildGdx.cache.open("tables.dat", 0));
    }

    public void loadpalette() throws Exception // jfBuild
    {
        Resource fil;
        if (paletteloaded != 0) return;

        palette = new byte[768];
        curpalette = new Palette();
        palookup = new byte[MAXPALOOKUPS][];

        Console.Println("Loading palettes");
        if ((fil = BuildGdx.cache.open("palette.dat", 0)) == null)
            throw new Exception("Failed to load \"palette.dat\"!");

        fil.read(palette);

        numshades = fil.readShort();

        palookup[0] = new byte[numshades << 8];
        transluc = new byte[65536];

        Console.Println("Loading gamma correction tables");
        fil.read(palookup[0], 0, numshades << 8);
        Console.Println("Loading translucency table");

        fil.read(transluc);

        fil.close();

        initfastcolorlookup(30, 59, 11);

        paletteloaded = 1;
    }




    ////////// SPRITE LIST MANIPULATION FUNCTIONS //////////

    public short insertspritesect(int sectnum) {
        return (short) spriteSectMap.insert(sectnum);
    }

    public short insertspritestat(int newstatnum) {
        return (short) spriteStatMap.insert(newstatnum);
    }

    public short insertsprite(int sectnum, int statnum) // jfBuild
    {
        insertspritestat(statnum);
        return (insertspritesect(sectnum));
    }

    public short deletesprite(int spritenum) // jfBuild
    {
        GLRenderer gl = glrender();
        if (gl != null) gl.removeSpriteCorr(spritenum);
        deletespritestat(spritenum);
        deletespritesect(spritenum);
        return 0;
    }

    public short changespritesect(int spritenum, int newsectnum) // jfBuild
    {
        if ((newsectnum < 0) || (newsectnum > MAXSECTORS)) return (-1);
        if (Engine.getSprite(spritenum).getSectnum() == newsectnum) return (0);
        if (Engine.getSprite(spritenum).getSectnum() == MAXSECTORS) return (-1);
        if (!deletespritesect((short) spritenum)) return (-1);
        insertspritesect(newsectnum);
        return (0);
    }

    public short changespritestat(int spritenum, int newstatnum) // jfBuild
    {
        if ((newstatnum < 0) || (newstatnum > MAXSTATUS)) return (-1);
        if (Engine.getSprite(spritenum).getStatnum() == newstatnum) return (0);
        if (Engine.getSprite(spritenum).getStatnum() == MAXSTATUS) return (-1);
        if (!deletespritestat((short) spritenum)) return (-1);
        insertspritestat(newstatnum);
        return (0);
    }

    public boolean deletespritesect(int spritenum) {
        return spriteSectMap.remove(spritenum);
    }

    public boolean deletespritestat(int spritenum) {
        return spriteStatMap.remove(spritenum);
    }

    public boolean lIntersect(int x1, int y1, int z1, int x2, int y2, int z2, int x3, // jfBuild
                              int y3, int x4, int y4, Variable x, Variable y, Variable z) {
        return engineService.lIntersect(x1, y1, z1, x2, y2, z2, x3, y3, x4, y4, x, y, z);
    }

    //
    // Exported Engine Functions
    //

    public void uninit() // gdxBuild
    {
        Iterator<TileFont> it;
        while ((it = TileFont.managedFont.iterator()).hasNext()) {
            TileFont font = it.next();
            font.dispose();
        }

        renderService.uninit();

        if (artfil != null) artfil.close();

        for (int i = 0; i < MAXPALOOKUPS; i++)
            if (palookup[i] != null) palookup[i] = null;

        uninitmultiplayer();

        BuildGdx.audio.dispose();
        BuildGdx.message.dispose();
    }

    public void initspritelists() // jfBuild
    {
//		this.spriteSectMap = new SpriteLinkedMap(MAXSECTORS) {
//			@Override
//			protected void put(Sprite spr, int value) {
//				spr.setSectnum(value);
//			}
//
//			@Override
//			protected int get(Sprite spr) {
//				return spr.getSectnum();
//			}
//		};
//
//		this.spriteStatMap = new SpriteLinkedMap(MAXSTATUS) {
//			@Override
//			protected void put(Sprite spr, int value) {
//				spr.setStatnum(value);
//			}
//
//			@Override
//			protected int get(Sprite spr) {
//				return spr.getStatnum();
//			}
//		};
    }



    public BuildPos loadboard(String filename) throws InvalidVersionException, FileNotFoundException, RuntimeException {
        Resource fil;
        if ((fil = BuildGdx.cache.open(filename, 0)) == null) {
            throw new FileNotFoundException("Map " + filename + " not found!");
        }

        int mapversion = fil.readInt();
        switch (mapversion) {
            case 6:
                return loadoldboard(fil);
            case 7:
                break;
            case 8:
                if (MAXSECTORS == MAXSECTORSV8) break;
            default:
                fil.close();
                throw new InvalidVersionException(filename + ": invalid map version( v" + mapversion + " )!");
        }

        BuildPos pos = new BuildPos();

        initspritelists();

        Arrays.fill(show2dsector, (byte) 0);
        Arrays.fill(show2dsprite, (byte) 0);
        Arrays.fill(show2dwall, (byte) 0);

        pos.x = fil.readInt();
        pos.y = fil.readInt();
        pos.z = fil.readInt();
        pos.ang = fil.readShort();
        pos.sectnum = fil.readShort();

        numsectors = fil.readShort();
        for (int i = 0; i < numsectors; i++)
            sector[i] = new Sector(fil);

        numwalls = fil.readShort();
        for (int w = 0; w < numwalls; w++)
            wall[w] = new Wall(fil);

        numsprites = fil.readShort();
        for (int s = 0; s < numsprites; s++)
            Engine.getSprite(s).buildSprite(fil);

        for (int i = 0; i < numsprites; i++)
            insertsprite(Engine.getSprite(i).getSectnum(), Engine.getSprite(i).getStatnum());

        // Must be after loading sectors, etc!
        pos.sectnum = updatesector(pos.x, pos.y, pos.sectnum);

        fil.close();

        if (inside(pos.x, pos.y, pos.sectnum) == -1) throw new RuntimeException("Player should be in a sector!");

        return pos;
    }

    public BuildPos loadoldboard(Resource fil) throws RuntimeException {
        BuildPos pos = new BuildPos();

        initspritelists();

        Arrays.fill(show2dsector, (byte) 0);
        Arrays.fill(show2dsprite, (byte) 0);
        Arrays.fill(show2dwall, (byte) 0);

        pos.x = fil.readInt();
        pos.y = fil.readInt();
        pos.z = fil.readInt();
        pos.ang = fil.readShort();
        pos.sectnum = fil.readShort();

        numsectors = fil.readShort();
        for (int i = 0; i < numsectors; i++) {
            Sector sec = new Sector();

            sec.setWallptr(fil.readShort());
            sec.setWallnum(fil.readShort());
            sec.setCeilingpicnum(fil.readShort());
            sec.setFloorpicnum(fil.readShort());
            int ceilingheinum = fil.readShort();
            sec.setCeilingheinum((short) max(min(ceilingheinum << 5, 32767), -32768));
            int floorheinum = fil.readShort();
            sec.setFloorheinum((short) max(min(floorheinum << 5, 32767), -32768));
            sec.setCeilingz(fil.readInt());
            sec.setFloorz(fil.readInt());
            sec.setCeilingshade(fil.readByte());
            sec.setFloorshade(fil.readByte());
            sec.setCeilingxpanning((short) (fil.readByte() & 0xFF));
            sec.setFloorxpanning((short) (fil.readByte() & 0xFF));
            sec.setCeilingypanning((short) (fil.readByte() & 0xFF));
            sec.setFloorypanning((short) (fil.readByte() & 0xFF));
            sec.setCeilingstat(fil.readByte());
            if ((sec.getCeilingstat() & 2) == 0) sec.setCeilingheinum(0);
            sec.setFloorstat(fil.readByte());
            if ((sec.getFloorstat() & 2) == 0) sec.setFloorheinum(0);
            sec.setCeilingpal(fil.readByte());
            sec.setFloorpal(fil.readByte());
            sec.setVisibility(fil.readByte());
            sec.setLotag(fil.readShort());
            sec.setHitag(fil.readShort());
            sec.setExtra(fil.readShort());

            sector[i] = sec;
        }

        numwalls = fil.readShort();
        for (int w = 0; w < numwalls; w++) {
            Wall wal = new Wall();

            wal.setX(fil.readInt());
            wal.setY(fil.readInt());
            wal.setPoint2(fil.readShort());
            wal.setNextsector(fil.readShort());
            wal.setNextwall(fil.readShort());
            wal.setPicnum(fil.readShort());
            wal.setOverpicnum(fil.readShort());
            wal.setShade(fil.readByte());
            wal.setPal((short) (fil.readByte() & 0xFF));
            wal.setCstat(fil.readShort());
            wal.setXrepeat((short) (fil.readByte() & 0xFF));
            wal.setYrepeat((short) (fil.readByte() & 0xFF));
            wal.setXpanning((short) (fil.readByte() & 0xFF));
            wal.setYpanning((short) (fil.readByte() & 0xFF));
            wal.setLotag(fil.readShort());
            wal.setHitag(fil.readShort());
            wal.setExtra(fil.readShort());

            wall[w] = wal;
        }

        numsprites = fil.readShort();
        for (int s = 0; s < numsprites; s++) {
            Sprite spr = Engine.getSprite(s);

            spr.setX(fil.readInt());
            spr.setY(fil.readInt());
            spr.setZ(fil.readInt());
            spr.setCstat(fil.readShort());
            spr.setShade(fil.readByte());
            spr.setPal(fil.readByte());
            spr.setClipdist(fil.readByte());
            spr.setXrepeat((short) (fil.readByte() & 0xFF));
            spr.setYrepeat((short) (fil.readByte() & 0xFF));
            spr.setXoffset((short) (fil.readByte() & 0xFF));
            spr.setYoffset((short) (fil.readByte() & 0xFF));
            spr.setPicnum(fil.readShort());
            spr.setAng(fil.readShort());
            spr.setXvel(fil.readShort());
            spr.setYvel(fil.readShort());
            spr.setZvel(fil.readShort());
            spr.setOwner(fil.readShort());
            spr.setSectnum(fil.readShort());
            spr.setStatnum(fil.readShort());
            spr.setLotag(fil.readShort());
            spr.setHitag(fil.readShort());
            spr.setExtra(fil.readShort());
        }

        for (int i = 0; i < numsprites; i++)
            insertsprite(Engine.getSprite(i).getSectnum(), Engine.getSprite(i).getStatnum());

        // Must be after loading sectors, etc!
        pos.sectnum = updatesector(pos.x, pos.y, pos.sectnum);

        fil.close();

        if (inside(pos.x, pos.y, pos.sectnum) == -1) throw new RuntimeException("Player should be in a sector!");

        return pos;
    }

    public void saveboard(FileResource fil, int daposx, int daposy, int daposz, int daang, int dacursectnum) {
        fil.writeInt(7); // mapversion

        fil.writeInt(daposx);
        fil.writeInt(daposy);
        fil.writeInt(daposz);
        fil.writeShort(daang);
        fil.writeShort(dacursectnum);

        fil.writeShort(numsectors);
        for (int s = 0; s < numsectors; s++)
            fil.writeBytes(Engine.getSector(s).getBytes());

        fil.writeShort(numwalls);
        for (int s = 0; s < numwalls; s++)
            fil.writeBytes(Engine.getWall(s).getBytes());

        fil.writeShort(numsprites);
        for (int s = 0; s < numsprites; s++)
            fil.writeBytes(Engine.getSprite(s).getBytes());
    }



    public void inittimer(int tickspersecond) { // jfBuild
        if (timerfreq != 0) return; // already installed

        timerfreq = 1000;
        timerticspersec = tickspersecond;
        timerlastsample = System.nanoTime() * timerticspersec / (timerfreq * 1000000L);
    }

    public void sampletimer() { // jfBuild
        if (timerfreq == 0) return;

        long n = (System.nanoTime() * timerticspersec / (timerfreq * 1000000)) - timerlastsample;
        if (n > 0) {
            totalclock += n;
            timerlastsample += n;
        }
    }

    public long getticks() { // gdxBuild
        return System.currentTimeMillis();
    }



    public synchronized void loadpic(String filename) // gdxBuild
    {
        Resource fil;
        if ((fil = BuildGdx.cache.open(filename, 0)) != null) {
            int artversion = fil.readInt();
            if (artversion != 1) return;
            numtiles = fil.readInt();
            int localtilestart = fil.readInt();
            int localtileend = fil.readInt();

            for (int i = localtilestart; i <= localtileend; i++) {
                getTile(i).setWidth(fil.readShort());
            }
            for (int i = localtilestart; i <= localtileend; i++) {
                getTile(i).setHeight(fil.readShort());
            }
            for (int i = localtilestart; i <= localtileend; i++) {
                getTile(i).anm = fil.readInt();
            }

            for (int i = localtilestart; i <= localtileend; i++) {
                int dasiz = getTile(i).getSize();
                if (dasiz > 0) {
                    Tile pic = getTile(i);
                    pic.data = new byte[dasiz];
                    fil.read(pic.data);
                }
                setpicsiz(i);
            }
            fil.close();
        }
    }

    public void setpicsiz(int tilenum) // jfBuild
    {
        Tile pic = getTile(tilenum);
        int j = 15;
        while ((j > 1) && (pow2long[j] > pic.getWidth())) j--;
        picsiz[tilenum] = j;
        j = 15;
        while ((j > 1) && (pow2long[j] > pic.getHeight())) j--;
        picsiz[tilenum] += (j << 4);
    }

    public synchronized int loadpics() { // jfBuild
        int offscount, localtilestart, localtileend, dasiz;
        int i, k;

        buildString(artfilename, 0, tilesPath);

        int numtilefiles = 0;
        Resource fil;
        do {
            k = numtilefiles;

            artfilename[7] = (char) ((k % 10) + 48);
            artfilename[6] = (char) (((k / 10) % 10) + 48);
            artfilename[5] = (char) (((k / 100) % 10) + 48);
            String name = String.copyValueOf(artfilename);

            if ((fil = BuildGdx.cache.open(name, 0)) != null) {
                int artversion = fil.readInt();
                if (artversion != 1) return (-1);

                numtiles = fil.readInt();
                localtilestart = fil.readInt();
                localtileend = fil.readInt();

                for (i = localtilestart; i <= localtileend; i++) {
                    getTile(i).setWidth(fil.readShort());
                }
                for (i = localtilestart; i <= localtileend; i++) {
                    getTile(i).setHeight(fil.readShort());
                }
                for (i = localtilestart; i <= localtileend; i++) {
                    getTile(i).anm = fil.readInt();
                }
                offscount = 4 + 4 + 4 + 4 + ((localtileend - localtilestart + 1) << 3);
                for (i = localtilestart; i <= localtileend; i++) {
                    tilefilenum[i] = k;
                    tilefileoffs[i] = offscount;
                    dasiz = getTile(i).getSize();
                    offscount += dasiz;
                }

                numtilefiles++;
                fil.close();
            }
        } while (k != numtilefiles);

        for (i = 0; i < MAXTILES; i++)
            setpicsiz(i);

        if (artfil != null) artfil.close();
        artfil = null;
        artfilnum = -1;
        artfilplc = 0;

        return (numtilefiles);
    }

    public synchronized byte[] loadtile(int tilenume) { // jfBuild
        if (tilenume >= MAXTILES) return null;

        Tile pic = getTile(tilenume);
        int dasiz = pic.getSize();

        if (dasiz <= 0) return null;

        int i = tilefilenum[tilenume];

        if (i != artfilnum) {
            if (artfil != null) artfil.close();
            artfilnum = i;
            artfilplc = 0;

            artfilename[7] = (char) ((i % 10) + 48);
            artfilename[6] = (char) (((i / 10) % 10) + 48);
            artfilename[5] = (char) (((i / 100) % 10) + 48);

            artfil = BuildGdx.cache.open(new String(artfilename), 0);

            faketimerhandler();
        }

        if (artfil == null) return null;

        if (pic.data == null) pic.data = new byte[dasiz];

        if (artfilplc != tilefileoffs[tilenume]) {
            artfil.seek(tilefileoffs[tilenume] - artfilplc, Whence.Current);
            faketimerhandler();
        }

        if (artfil.read(pic.data) == -1) return null;

        faketimerhandler();
        artfilplc = tilefileoffs[tilenume] + dasiz;

        return pic.data;
    }

    public byte[] allocatepermanenttile(int tilenume, int xsiz, int ysiz) { // jfBuild
        if ((xsiz <= 0) || (ysiz <= 0) || (tilenume >= MAXTILES)) return null;

        Tile pic = getTile(tilenume);
        pic.allocate(xsiz, ysiz);
        setpicsiz(tilenume);

        return (pic.data);
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

    public int inside(int x, int y, int sectnum) { // jfBuild
        return boardService.inside(x, y, Engine.getSector(sectnum)) ? 1 : 0;
    }

    protected static int SETSPRITEZ = 0;

    public short setsprite(int spritenum, int newx, int newy, int newz) // jfBuild
    {
        Engine.getSprite(spritenum).setX(newx);
        Engine.getSprite(spritenum).setY(newy);
        Engine.getSprite(spritenum).setZ(newz);

        short tempsectnum = Engine.getSprite(spritenum).getSectnum();
        if (SETSPRITEZ == 1) tempsectnum = updatesectorz(newx, newy, newz, tempsectnum);
        else tempsectnum = updatesector(newx, newy, tempsectnum);
        if (tempsectnum < 0) return (-1);
        if (tempsectnum != Engine.getSprite(spritenum).getSectnum()) changespritesect((short) spritenum, tempsectnum);

        return (0);
    }

    public int nextsectorneighborz(int sectnum, int thez, int topbottom, int direction) { // jfBuild
        int nextz = 0x80000000;
        if (direction == 1) nextz = 0x7fffffff;

        short sectortouse = -1;

        short wallid = Engine.getSector(sectnum).getWallptr();
        int i = Engine.getSector(sectnum).getWallnum(), testz;
        do {
            Wall wal = Engine.getWall(wallid);
            if (wal.getNextsector() >= 0) {
                if (topbottom == 1) {
                    testz = getSector(wal.getNextsector()).getFloorz();
                } else {
                    testz = getSector(wal.getNextsector()).getCeilingz();
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

    public boolean cansee(int x1, int y1, int z1, int sect1, int x2, int y2, int z2, int sect2) { // eduke32
        return engineService.canSee(x1, y1, z1, sect1, x2, y2, z2, sect2);
    }


    public int hitscan(int xs, int ys, int zs, int sectnum, int vx, int vy, int vz, // jfBuild
                       Hitscan hit, int cliptype) {
        scanner.setGoal(hitscangoalx, hitscangoaly);
        boolean result = scanner.run(xs, ys, zs, sectnum, vx, vy, vz, cliptype);
        HitInfo is = scanner.getInfo();
        hit.set(is.x, is.y, is.z, is.sector, is.wall, is.sprite);
        return result ? 0 : -1;
    }

    public void nextpage() { // gdxBuild
        faketimerhandler();
        Console.draw(this);
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
//        int vx = mulscale(sintable[(ange + 2560) & 2047], neartagrange, 14);
//        int xe = xs + vx;
//        int vy = mulscale(sintable[(ange + 2048) & 2047], neartagrange, 14);
//        int ye = ys + vy;
//        int vz = 0;
//        int ze = 0;
//        IntSet sectorSet = new IntSet(MAXSECTORS);
//
//        sectorSet.addValue(sectnum);
//        for (int dacnt = 0; dacnt < sectorSet.size(); dacnt++) {
//            dasector = (short) sectorSet.getValue(dacnt);
//
//            startwall = Engine.getSector(dasector).getWallptr();
//            endwall = (startwall + Engine.getSector(dasector).getWallnum() - 1);
//            if (startwall < 0 || endwall < 0) {
//                continue;
//            }
//            for (int z = startwall; z <= endwall; z++) {
//                Wall wal = Engine.getWall(z);
//                Wall wal2 = Engine.getWall(wal.getPoint2());
//                x1 = wal.getX();
//                y1 = wal.getY();
//                x2 = wal2.getX();
//                y2 = wal2.getY();
//
//                nextsector = wal.getNextsector();
//
//                good = 0;
//                if (nextsector >= 0) {
//                    if (((tagsearch & 1) != 0) && Engine.getSector(nextsector).getLotag() != 0)
//                        good |= 1;
//                    if (((tagsearch & 2) != 0) && Engine.getSector(nextsector).getHitag() != 0)
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
//                if (lintersect(xs, ys, zs, xe, ye, ze, x1, y1, x2, y2, rx, ry, rz)) {
//                    if (good != 0) {
//                        if ((good & 1) != 0)
//                            near.tagsector = nextsector;
//                        if ((good & 2) != 0)
//                            near.tagwall = (short) z;
//                        near.taghitdist = dmulscale(rx.get() - xs, sintable[(ange + 2560) & 2047], ry.get() - ys,
//                                sintable[(ange + 2048) & 2047], 14);
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
//            for (SpriteNode node = spriteSectMap.getFirst(dasector); node != null; node = node.getNext()) {
//                int z = node.getIndex();
//                Sprite spr = Engine.getSprite(z);
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
//                            Tile pic = getTile(spr.getPicnum());
//                            i = pic.getHeight() * spr.getYrepeat();
//                            if ((spr.getCstat() & 128) != 0)
//                                z1 += (i << 1);
//                            if ((pic.anm & 0x00ff0000) != 0)
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
//                                        near.taghitdist = dmulscale(intx - xs, sintable[(ange + 2560) & 2047],
//                                                inty - ys, sintable[(ange + 2048) & 2047], 14);
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

    public int qdist(long dx, long dy) { // gdxBuild
        dx = abs(dx);
        dy = abs(dy);

        if (dx > dy) dy = (3 * dy) >> 3;
        else dx = (3 * dx) >> 3;

        return (int) (dx + dy);
    }

    public void dragpoint(int pointhighlight, int dax, int day) { // jfBuild
        Engine.getWall(pointhighlight).setX(dax);
        Engine.getWall(pointhighlight).setY(day);

        int cnt = MAXWALLS;
        int tempshort = pointhighlight; // search points CCW
        do {
            if (Engine.getWall(tempshort).getNextwall() >= 0) {
                tempshort = getWall(Engine.getWall(tempshort).getNextwall()).getPoint2();
                Engine.getWall(tempshort).setX(dax);
                Engine.getWall(tempshort).setY(day);
            } else {
                tempshort = pointhighlight; // search points CW if not searched all the way around
                do {
                    if (Engine.getWall(lastwall(tempshort)).getNextwall() >= 0) {
                        tempshort = Engine.getWall(lastwall(tempshort)).getNextwall();
                        Engine.getWall(tempshort).setX(dax);
                        Engine.getWall(tempshort).setY(day);
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
        if ((point > 0) && (Engine.getWall(point - 1).getPoint2() == point)) return (point - 1);

        int i = point, j;
        int cnt = MAXWALLS;
        do {
            j = Engine.getWall(i).getPoint2();
            if (j == point) return (i);
            i = j;
            cnt--;
        } while (cnt > 0);
        return (point);
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

    public static int pushmove_x, pushmove_y, pushmove_z;
    public static short pushmove_sectnum;

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

    public short updatesector(int x, int y, int sectnum) { // jfBuild
        if (inside(x, y, sectnum) == 1) return (short) sectnum;

        short i;
        if (isValidSector(sectnum)) {
            short wallid = Engine.getSector(sectnum).getWallptr();
            int j = Engine.getSector(sectnum).getWallnum();
            do {
                if (!isValidWall(wallid)) break;

                Wall wal = Engine.getWall(wallid);
                i = wal.getNextsector();
                if (i >= 0) if (inside(x, y, i) == 1) {
                    return i;
                }
                wallid++;
                j--;
            } while (j != 0);
        }

        for (i = (short) (numsectors - 1); i >= 0; i--)
            if (inside(x, y, i) == 1) {
                return i;
            }

        return -1;
    }

    public short updatesectorz(int x, int y, int z, int sectnum) { // jfBuild
        getzsofslope(sectnum, x, y, zofslope);
        if ((z >= zofslope[CEIL]) && (z <= zofslope[FLOOR])) if (inside(x, y, sectnum) != 0) return (short) sectnum;

        short i;
        if (isValidSector(sectnum)) {
            short wallid = Engine.getSector(sectnum).getWallptr();
            int j = Engine.getSector(sectnum).getWallnum();
            do {
                if (!isValidWall(wallid)) break;

                Wall wal = Engine.getWall(wallid);
                i = wal.getNextsector();
                if (i >= 0) {
                    getzsofslope(i, x, y, zofslope);
                    if ((z >= zofslope[CEIL]) && (z <= zofslope[FLOOR])) if (inside(x, y, i) == 1) {
                        return i;
                    }
                }
                wallid++;
                j--;
            } while (j != 0);
        }

        for (i = (short) (numsectors - 1); i >= 0; i--) {
            getzsofslope(i, x, y, zofslope);
            if ((z >= zofslope[CEIL]) && (z <= zofslope[FLOOR])) if (inside(x, y, i) == 1) {
                return i;
            }
        }

        return -1;
    }

    protected Point rotatepoint = new Point();

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

    public void getzrange(int x, int y, int z, int sectnum, // jfBuild
                          int walldist, int cliptype) {

        RangeZInfo info = getZRange.invoke(x, y, z, sectnum, walldist, cliptype);
        zr_ceilz = info.getCeilz();
        zr_ceilhit = info.getCeilhit();
        zr_florz = info.getFlorz();
        zr_florhit = info.getFlorhit();
    }



    public void makepalookup(final int palnum, byte[] remapbuf, int r, int g, int b, int dastat) // jfBuild
    {
        if (paletteloaded == 0) return;

        // Allocate palookup buffer
        if (palookup[palnum] == null) palookup[palnum] = new byte[numshades << 8];

        if (dastat == 0) return;
        if ((r | g | b | 63) != 63) return;

        if ((r | g | b) == 0) {
            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < numshades; j++) {
                    palookup[palnum][i + j * 256] = palookup[0][(remapbuf[i] & 0xFF) + j * 256];
                }
            }
            palookupfog[palnum][0] = 0;
            palookupfog[palnum][1] = 0;
            palookupfog[palnum][2] = 0;
        } else {
            byte[] pal = new byte[768];
            System.arraycopy(curpalette.getBytes(), 0, pal, 0, 768);
            for (int j = 0; j < 768; j++)
                pal[j] = (byte) ((pal[j] & 0xFF) >> 2);

            for (int i = 0; i < numshades; i++) {
                int palscale = divscale(i, numshades, 16);
                for (int j = 0; j < 256; j++) {
                    int rptr = pal[3 * (remapbuf[j] & 0xFF)] & 0xFF;
                    int gptr = pal[3 * (remapbuf[j] & 0xFF) + 1] & 0xFF;
                    int bptr = pal[3 * (remapbuf[j] & 0xFF) + 2] & 0xFF;

                    palookup[palnum][j + i * 256] = getclosestcol(pal, rptr + mulscale(r - rptr, palscale, 16), gptr + mulscale(g - gptr, palscale, 16), bptr + mulscale(b - bptr, palscale, 16));
                }
            }
            palookupfog[palnum][0] = (byte) r;
            palookupfog[palnum][1] = (byte) g;
            palookupfog[palnum][2] = (byte) b;
        }

        BuildGdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                final GLRenderer gl = glrender();
                if (gl != null) {
                    gl.getTextureManager().invalidatepalookup(palnum);
                }
            }
        });
    }
    public void squarerotatetile(int tilenume) {
        Tile pic = getTile(tilenume);
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
        Tile pic = getTile(tilenume);

        byte tmp = pic.data[p1];
        pic.data[p1] = pic.data[p2];
        pic.data[p2] = tmp;
    }


    public short sectorofwall(int theline) { // jfBuild
        if ((theline < 0) || (theline >= numwalls)) return (-1);
        short i = Engine.getWall(theline).getNextwall();
        if (i >= 0) return (Engine.getWall(i).getNextsector());

        int gap = (numsectors >> 1);
        i = (short) gap;
        while (gap > 1) {
            gap >>= 1;
            if (Engine.getSector(i).getWallptr() < theline) i += gap;
            else i -= gap;
        }
        while (Engine.getSector(i).getWallptr() > theline) i--;
        while (Engine.getSector(i).getWallptr() + Engine.getSector(i).getWallnum() <= theline) i++;
        return (i);
    }

    public int getceilzofslope(int sectnum, int dax, int day) { // jfBuild
        if (sectnum < 0 || sectnum >= MAXSECTORS || Engine.getSector(sectnum) == null) return 0;
        if ((Engine.getSector(sectnum).getCeilingstat() & 2) == 0) return (Engine.getSector(sectnum).getCeilingz());

        Wall wal = Engine.getWall(Engine.getSector(sectnum).getWallptr());
        int dx = Engine.getWall(wal.getPoint2()).getX() - wal.getX();
        int dy = Engine.getWall(wal.getPoint2()).getY() - wal.getY();
        int i = (EngineUtils.sqrt(dx * dx + dy * dy) << 5);
        if (i == 0) return (Engine.getSector(sectnum).getCeilingz());
        long j = dmulscale(dx, day - wal.getY(), -dy, dax - wal.getX(), 3);

        return Engine.getSector(sectnum).getCeilingz() + (scale(Engine.getSector(sectnum).getCeilingheinum(), j, i));
    }

    public int getflorzofslope(int sectnum, int dax, int day) { // jfBuild
        if (sectnum < 0 || sectnum >= MAXSECTORS || Engine.getSector(sectnum) == null) return 0;
        if ((Engine.getSector(sectnum).getFloorstat() & 2) == 0) return (Engine.getSector(sectnum).getFloorz());

        Wall wal = Engine.getWall(Engine.getSector(sectnum).getWallptr());
        int dx = Engine.getWall(wal.getPoint2()).getX() - wal.getX();
        int dy = Engine.getWall(wal.getPoint2()).getY() - wal.getY();
        int i = (EngineUtils.sqrt(dx * dx + dy * dy) << 5);
        if (i == 0) return (Engine.getSector(sectnum).getFloorz());
        long j = dmulscale(dx, day - wal.getY(), -dy, dax - wal.getX(), 3);
        return Engine.getSector(sectnum).getFloorz() + (scale(Engine.getSector(sectnum).getFloorheinum(), j, i));
    }

    public void getzsofslope(int sectnum, int dax, int day, int[] outz) {
        if (sectnum < 0 || sectnum >= MAXSECTORS || Engine.getSector(sectnum) == null) return;

        Sector sec = Engine.getSector(sectnum);
        if (sec == null) return;
        outz[CEIL] = sec.getCeilingz();
        outz[FLOOR] = sec.getFloorz();
        if (((sec.getCeilingstat() | sec.getFloorstat()) & 2) != 0) {
            Wall wal = Engine.getWall(sec.getWallptr());
            Wall wal2 = Engine.getWall(wal.getPoint2());
            int dx = wal2.getX() - wal.getX();
            int dy = wal2.getY() - wal.getY();
            int i = (EngineUtils.sqrt(dx * dx + dy * dy) << 5);
            if (i == 0) return;
            long j = dmulscale(dx, day - wal.getY(), -dy, dax - wal.getX(), 3);

            if ((sec.getCeilingstat() & 2) != 0) outz[CEIL] += scale(sec.getCeilingheinum(), j, i);
            if ((sec.getFloorstat() & 2) != 0) outz[FLOOR] += scale(sec.getFloorheinum(), j, i);
        }
    }

    public void alignceilslope(int dasect, int x, int y, int z) { // jfBuild
        Wall wal = Engine.getWall(Engine.getSector(dasect).getWallptr());
        int dax = Engine.getWall(wal.getPoint2()).getX() - wal.getX();
        int day = Engine.getWall(wal.getPoint2()).getY() - wal.getY();

        int i = (y - wal.getY()) * dax - (x - wal.getX()) * day;
        if (i == 0) return;
        Engine.getSector(dasect).setCeilingheinum((short) scale((z - Engine.getSector(dasect).getCeilingz()) << 8, EngineUtils.sqrt(dax * dax + day * day), i));

        if (Engine.getSector(dasect).getCeilingheinum() == 0) Engine.getSector(dasect).setCeilingstat(Engine.getSector(dasect).getCeilingstat() & ~2);
        else Engine.getSector(dasect).setCeilingstat(Engine.getSector(dasect).getCeilingstat() | 2);
    }

    public void alignflorslope(int dasect, int x, int y, int z) { // jfBuild
        Wall wal = Engine.getWall(Engine.getSector(dasect).getWallptr());
        int dax = Engine.getWall(wal.getPoint2()).getX() - wal.getX();
        int day = Engine.getWall(wal.getPoint2()).getY() - wal.getY();

        int i = (y - wal.getY()) * dax - (x - wal.getX()) * day;
        if (i == 0) return;
        Engine.getSector(dasect).setFloorheinum((short) scale((z - Engine.getSector(dasect).getFloorz()) << 8, EngineUtils.sqrt(dax * dax + day * day), i));

        if (Engine.getSector(dasect).getFloorheinum() == 0) Engine.getSector(dasect).setFloorstat(Engine.getSector(dasect).getFloorstat() & ~2);
        else Engine.getSector(dasect).setFloorstat(Engine.getSector(dasect).getFloorstat() | 2);
    }

    public int loopnumofsector(int sectnum, int wallnum) { // jfBuild
        int numloops = 0;
        int startwall = Engine.getSector(sectnum).getWallptr();
        int endwall = startwall + Engine.getSector(sectnum).getWallnum();
        for (int i = startwall; i < endwall; i++) {
            if (i == wallnum) return (numloops);
            if (Engine.getWall(i).getPoint2() < i) numloops++;
        }
        return (-1);
    }

    public void copytilepiece(int tilenume1, int sx1, int sy1, int xsiz, int ysiz, // jfBuild
                              int tilenume2, int sx2, int sy2) {

        Tile pic1 = getTile(tilenume1);
        Tile pic2 = getTile(tilenume2);

        int xsiz1 = pic1.getWidth();
        int ysiz1 = pic1.getHeight();
        int xsiz2 = pic2.getWidth();
        int ysiz2 = pic2.getHeight();
        if ((xsiz1 > 0) && (ysiz1 > 0) && (xsiz2 > 0) && (ysiz2 > 0)) {
            if (pic1.data == null) loadtile(tilenume1);
            if (pic2.data == null) loadtile(tilenume2);

            int x1 = sx1;
            for (int i = 0; i < xsiz; i++) {
                int y1 = sy1;
                for (int j = 0; j < ysiz; j++) {
                    int x2 = sx2 + i;
                    int y2 = sy2 + j;
                    if ((x2 >= 0) && (y2 >= 0) && (x2 < xsiz2) && (y2 < ysiz2)) {
                        byte ptr = pic1.data[x1 * ysiz1 + y1];
                        if (ptr != 255) pic2.data[x2 * ysiz2 + y2] = ptr;
                    }

                    y1++;
                    if (y1 >= ysiz1) y1 = 0;
                }
                x1++;
                if (x1 >= xsiz1) x1 = 0;
            }
        }
    }

    public abstract void faketimerhandler();



    public enum Clockdir {
        CW(0), CCW(1);

        private final int value;

        Clockdir(int val) {
            this.value = val;
        }

        public int getValue() {
            return value;
        }
    }

    public Clockdir clockdir(int wallstart) // Returns: 0 is CW, 1 is CCW
    {
        int minx = 0x7fffffff;
        int themin = -1;
        int i = wallstart - 1;
        do {
            if (!Gameutils.isValidWall(++i)) break;

            if (Engine.getWall(Engine.getWall(i).getPoint2()).getX() < minx) {
                minx = Engine.getWall(Engine.getWall(i).getPoint2()).getX();
                themin = i;
            }
        } while (Engine.getWall(i).getPoint2() != wallstart);

        int x0 = Engine.getWall(themin).getX();
        int y0 = Engine.getWall(themin).getY();
        int x1 = Engine.getWall(Engine.getWall(themin).getPoint2()).getX();
        int y1 = Engine.getWall(Engine.getWall(themin).getPoint2()).getY();
        int x2 = Engine.getWall(Engine.getWall(Engine.getWall(themin).getPoint2()).getPoint2()).getX();
        int y2 = Engine.getWall(Engine.getWall(Engine.getWall(themin).getPoint2()).getPoint2()).getY();

        if ((y1 >= y2) && (y1 <= y0)) return Clockdir.CW;
        if ((y1 >= y0) && (y1 <= y2)) return Clockdir.CCW;

        int templong = (x0 - x1) * (y2 - y1) - (x2 - x1) * (y0 - y1);
        if (templong < 0) return Clockdir.CW;
        else return Clockdir.CCW;
    }

    public int loopinside(int x, int y, int startwall) {
        int cnt = clockdir(startwall).getValue();
        int i = startwall;

        int x1, x2, y1, y2, templong;
        do {
            x1 = Engine.getWall(i).getX();
            x2 = Engine.getWall(Engine.getWall(i).getPoint2()).getX();
            if ((x1 >= x) || (x2 >= x)) {
                y1 = Engine.getWall(i).getY();
                y2 = Engine.getWall(Engine.getWall(i).getPoint2()).getY();
                if (y1 > y2) {
                    templong = x1;
                    x1 = x2;
                    x2 = templong;
                    templong = y1;
                    y1 = y2;
                    y2 = templong;
                }
                if ((y1 <= y) && (y2 > y)) if (x1 * (y - y2) + x2 * (y1 - y) <= x * (y1 - y2)) cnt ^= 1;
            }
            i = Engine.getWall(i).getPoint2();
        } while (i != startwall);
        return (cnt);
    }

    public void flipwalls(int numwalls, int newnumwalls) {
        int j, tempint;
        int nume = newnumwalls - numwalls;

        for (int i = numwalls; i < numwalls + (nume >> 1); i++) {
            j = numwalls + newnumwalls - i - 1;
            tempint = Engine.getWall(i).getX();
            Engine.getWall(i).setX(Engine.getWall(j).getX());
            Engine.getWall(j).setX(tempint);
            tempint = Engine.getWall(i).getY();
            Engine.getWall(i).setY(Engine.getWall(j).getY());
            Engine.getWall(j).setY(tempint);
        }
    }

    public static KeyInput getInput() // gdxBuild
    {
        return input;
    }

    public void handleevents() { // gdxBuild
        input.handleevents();
        Console.HandleScanCode();

        sampletimer();
    }

    public void initkeys() { // gdxBuild
        input = new KeyInput();
    }
    public Tile getTile(int tilenum) {
        if (tiles[tilenum] == null) tiles[tilenum] = new Tile();
        return tiles[tilenum];
    }

    public void setDefs(DefScript defs) {
        this.defs = defs;
        if (getrender() == null) throw new NullPointerException("Renderer is not initialized!");
        getrender().setDefs(defs);
    }

    public DefScript getDefs() {
        return defs;
    }












    ////////// RENDERER MANIPULATION FUNCTIONS //////////

    public void initfastcolorlookup(int rscale, int gscale, int bscale) { // jfBuild
        renderService.initfastcolorlookup(rscale, gscale, bscale);
    }

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

    public byte getclosestcol(byte[] palette, int r, int g, int b) { // jfBuild
        return renderService.getclosestcol(palette, r, g, b);
    }

    public int drawrooms(float daposx, float daposy, float daposz, float daang, float dahoriz, short dacursectnum) { // eDuke32
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

    public boolean changepalette(final byte[] palette) {
        return renderService.changepalette(palette);
    }

    public void setpalettefade(int r, int g, int b, int offset) { // jfBuild
        renderService.setpalettefade(r, g, b, offset);
    }

    public void clearview(int dacol) { // gdxBuild
        renderService.clearview(dacol);
    }

    public void setviewtotile(int tilenume, int xsiz, int ysiz) { // jfBuild
        renderService.setviewtotile(tilenume, xsiz, ysiz);
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

    public void printext256(int xpos, int ypos, int col, int backcol, char[] name, int fontsize, float scale) { // gdxBuild
        renderService.printext256(xpos, ypos, col, backcol, name, fontsize, scale);
    }

    public String screencapture(String fn) { // jfBuild + gdxBuild (screenshot)
        return renderService.screencapture(fn);
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

    public abstract static class SpriteLinkedMap extends LinkedMap {

        public SpriteLinkedMap(int listCount) {
            super(listCount, MAXSPRITES);
        }

        protected abstract void put(Sprite spr, int value);

        protected abstract int get(Sprite spr);

        @Override
        protected void put(int element, int value) {
            put(Engine.getSprite(element), value);
        }

        @Override
        protected int get(int element) {
            return get(Engine.getSprite(element));
        }
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
