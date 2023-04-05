package ru.m210projects.Build;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import ru.m210projects.Build.Architecture.BuildFrame;
import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.FileHandle.Compat;
import ru.m210projects.Build.FileHandle.DirectoryEntry;
import ru.m210projects.Build.OnSceenDisplay.Console;
import ru.m210projects.Build.Render.GLRenderer;
import ru.m210projects.Build.Render.GdxRender.GDXRenderer;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Render.Software.Software;
import ru.m210projects.Build.Render.TextureHandle.TileData;
import ru.m210projects.Build.Render.Types.FadeEffect;
import ru.m210projects.Build.Render.Types.GL10;
import ru.m210projects.Build.Settings.BuildSettings;
import ru.m210projects.Build.Types.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

import static java.lang.Math.*;
import static java.lang.Math.pow;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Gameutils.*;
import static ru.m210projects.Build.OnSceenDisplay.Console.OSDTEXT_YELLOW;
import static ru.m210projects.Build.Pragmas.*;
import static ru.m210projects.Build.Pragmas.scale;
import static ru.m210projects.Build.Strhandler.buildString;

public class RenderService {

    public static TileFont pTextfont, pSmallTextfont;

    public static float TRANSLUSCENT1 = 0.66f;
    public static float TRANSLUSCENT2 = 0.33f;
    public static final int MAXXDIM = 4096;
    public static final int MAXYDIM = 3072;

    public static boolean showinvisibility;
    public static boolean offscreenrendering;
    public static int setviewcnt = 0; // interface layers use this now
    public static int[] bakwindowx1, bakwindowy1;
    public static int[] bakwindowx2, bakwindowy2;
    public static int baktile;
    public static TSprite[] tsprite;
    public static byte[] gotpic;
    public static byte[] gotsector;
    public static int spritesortcnt;
    public static int windowx1, windowy1, windowx2, windowy2;
    public static int xdim, ydim;
    public static int yxaspect, viewingrange;
    public static int mirrorx, mirrory;
    public static float mirrorang;
    public static int curbrightness = 0;
    public static int xdimen = -1, halfxdimen, xdimenscale, xdimscale;
    public static int wx1, wy1, wx2, wy2, ydimen;
    protected static byte[][] britable; // JBF 20040207: full 8bit precision
    public static byte[] transluc; //software renderer
    public static int parallaxyoffs, parallaxyscale;
    public static int globalposx, globalposy, globalposz; // polymost
    public static float globalhoriz, globalang;
    public static float pitch;
    public static short globalcursectnum;
    public static int globalvisibility;
    public static int globalshade, globalpal, cosglobalang, singlobalang;
    public static int cosviewingrangeglobalang, sinviewingrangeglobalang;
    public static int beforedrawrooms = 1;
    public static int xyaspect, viewingrangerecip;
    public static boolean inpreparemirror = false;

    public static FadeEffect palfadergb;

    private final Engine engine;
    private final byte[] shortbuf = new byte[2];
    protected int totalclocklock;
    protected HashMap<String, FadeEffect> fades;

    private int[] rdist, gdist, bdist;
    private final int FASTPALGRIDSIZ = 8;
    private byte[] colhere;
    private byte[] colhead;
    private short[] colnext;
    private final byte[] coldist = {0, 1, 2, 3, 4, 3, 2, 1};
    private int[] colscan;

    private final int newaspect_enable = 1;
    private int setaspect_new_use_dimen;
    public Renderer render;
    private float fovFactor = 1.0f;

    public int fpscol = 31;
    private final char[] fpsbuffer = new char[32];
    private long fpstime = 0;
    private int fpsx, fpsy;

    protected Byte[] palcache = new Byte[0x40000]; // buffer 256kb
    protected byte[] temppal = new byte[768];

    public RenderService(Engine engine) {
        this.engine = engine;

        tsprite = new TSprite[MAXSPRITESONSCREEN + 1];
        gotpic = new byte[(MAXTILES + 7) >> 3];
        gotsector = new byte[(MAXSECTORS + 7) >> 3];

        rdist = new int[129];
        gdist = new int[129];
        bdist = new int[129];
        colhere = new byte[((FASTPALGRIDSIZ + 2) * (FASTPALGRIDSIZ + 2) * (FASTPALGRIDSIZ + 2)) >> 3];
        colhead = new byte[(FASTPALGRIDSIZ + 2) * (FASTPALGRIDSIZ + 2) * (FASTPALGRIDSIZ + 2)];
        colnext = new short[256];
        colscan = new int[27];

        bakwindowx1 = new int[4];
        bakwindowy1 = new int[4];
        bakwindowx2 = new int[4];
        bakwindowy2 = new int[4];

        Tables tables = engine.getTables();
        pTextfont = new TextFont(tables.getTextFont());
        pSmallTextfont = new SmallTextFont(tables.getSmallTextFont());
        calcbritable();

        parallaxyoffs = 0;
        parallaxyscale = 65536;

        palfadergb = new FadeEffect(GL10.GL_ONE_MINUS_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA) {
            @Override
            public void update(int intensive) {
            }
        };
    }

    public void setgotpic(int tilenume) { // jfBuild
        gotpic[tilenume >> 3] |= pow2char[tilenume & 7];
    }

    protected void calcbritable() { // jfBuild
        britable = new byte[16][256];

        float a, b;
        for (int i = 0, j; i < 16; i++) {
            a = 8.0f / (i + 8);
            b = (float) (255.0f / pow(255.0f, a));
            for (j = 0; j < 256; j++) {// JBF 20040207: full 8bit precision
                britable[i][j] = (byte) (pow(j, a) * b);
            }
        }
    }

    public int animateoffs(int tilenum, int nInfo) { // jfBuild + gdxBuild
        long clock, index = 0;
        Tile tile = engine.getTile(tilenum);

        int speed = tile.getSpeed();
        if ((nInfo & 0xC000) == 0x8000) { // sprite
            // hash sprite frame by info variable

            shortbuf[0] = (byte) ((nInfo) & 0xFF);
            shortbuf[1] = (byte) ((nInfo >>> 8) & 0xFF);

            clock = (totalclocklock + CRC32.getChecksum(shortbuf)) >> speed;
        } else
            clock = totalclocklock >> speed;

        int frames = tile.getFrames();

        if (frames > 0) {
            switch (tile.getType()) {
                case Oscil:
                    index = clock % (frames * 2L);
                    if (index >= frames)
                        index = frames * 2L - index;
                    break;
                case Forward:
                    index = clock % (frames + 1);
                    break;
                case Backward:
                    index = -(clock % (frames + 1));
                    break;
                default: // None
                    break;
            }
        }
        return (int) index;
    }

    public void initfastcolorlookup(int rscale, int gscale, int bscale) { // jfBuild
        int i, x, y, z;

        int j = 0;
        for (i = 64; i >= 0; i--) {
            rdist[i] = rdist[128 - i] = j * rscale;
            gdist[i] = gdist[128 - i] = j * gscale;
            bdist[i] = bdist[128 - i] = j * bscale;
            j += 129 - (i << 1);
        }

        Arrays.fill(colhere, (byte) 0);
        Arrays.fill(colhead, (byte) 0);

        int pal1 = 768 - 3;
        for (i = 255; i >= 0; i--, pal1 -= 3) {
            int r = palette[pal1] & 0xFF;
            int g = palette[pal1 + 1] & 0xFF;
            int b = palette[pal1 + 2] & 0xFF;
            j = (r >> 3) * FASTPALGRIDSIZ * FASTPALGRIDSIZ + (g >> 3) * FASTPALGRIDSIZ + (b >> 3)
                    + FASTPALGRIDSIZ * FASTPALGRIDSIZ + FASTPALGRIDSIZ + 1;
            if ((colhere[j >> 3] & pow2char[j & 7]) != 0)
                colnext[i] = (short) (colhead[j] & 0xFF);
            else
                colnext[i] = -1;

            colhead[j] = (byte) i;
            colhere[j >> 3] |= pow2char[j & 7];
        }

        i = 0;
        for (x = -FASTPALGRIDSIZ * FASTPALGRIDSIZ; x <= FASTPALGRIDSIZ * FASTPALGRIDSIZ; x += FASTPALGRIDSIZ
                * FASTPALGRIDSIZ)
            for (y = -FASTPALGRIDSIZ; y <= FASTPALGRIDSIZ; y += FASTPALGRIDSIZ)
                for (z = -1; z <= 1; z++)
                    colscan[i++] = x + y + z;
        i = colscan[13];
        colscan[13] = colscan[26];
        colscan[26] = i;
    }

    public byte getclosestcol(byte[] palette, int r, int g, int b) { // jfBuild
        int i, k, dist;
        byte retcol;
        int pal1;

        int j = (r >> 3) * FASTPALGRIDSIZ * FASTPALGRIDSIZ + (g >> 3) * FASTPALGRIDSIZ + (b >> 3)
                + FASTPALGRIDSIZ * FASTPALGRIDSIZ + FASTPALGRIDSIZ + 1;

        int rgb = ((r << 12) | (g << 6) | b);

        int mindist = min(rdist[(coldist[r & 7] & 0xFF) + 64 + 8], gdist[(coldist[g & 7] & 0xFF) + 64 + 8]);
        mindist = min(mindist, bdist[(coldist[b & 7] & 0xFF) + 64 + 8]);
        mindist++;

        Byte out = palcache[rgb & (palcache.length - 1)];
        if (out != null)
            return out;

        r = 64 - r;
        g = 64 - g;
        b = 64 - b;

        retcol = -1;
        for (k = 26; k >= 0; k--) {
            i = colscan[k] + j;
            if ((colhere[i >> 3] & pow2char[i & 7]) == 0)
                continue;

            i = colhead[i] & 0xFF;
            do {
                pal1 = i * 3;
                dist = gdist[(palette[pal1 + 1] & 0xFF) + g];
                if (dist < mindist) {
                    dist += rdist[(palette[pal1] & 0xFF) + r];
                    if (dist < mindist) {
                        dist += bdist[(palette[pal1 + 2] & 0xFF) + b];
                        if (dist < mindist) {
                            mindist = dist;
                            retcol = (byte) i;
                        }
                    }
                }
                i = colnext[i];
            } while (i >= 0);
        }
        if (retcol >= 0) {
            palcache[rgb & (palcache.length - 1)] = retcol;
            return retcol;
        }

        mindist = 0x7fffffff;
        for (i = 255; i >= 0; i--) {
            pal1 = i * 3;
            dist = gdist[(palette[pal1 + 1] & 0xFF) + g];
            if (dist >= mindist)
                continue;

            dist += rdist[(palette[pal1] & 0xFF) + r];
            if (dist >= mindist)
                continue;

            dist += bdist[(palette[pal1 + 2] & 0xFF) + b];
            if (dist >= mindist)
                continue;

            mindist = dist;
            retcol = (byte) i;
        }

        palcache[rgb & (palcache.length - 1)] = retcol;
        return retcol;
    }

    public int drawrooms(float daposx, float daposy, float daposz, float daang, float dahoriz, short dacursectnum) { // eDuke32
        beforedrawrooms = 0;

        globalposx = (int) daposx;
        globalposy = (int) daposy;
        globalposz = (int) daposz;

        globalang = BClampAngle(daang);
        globalhoriz = (dahoriz - 100);
        pitch = (-EngineUtils.getAngle(160, (int) (dahoriz - 100))) / (2048.0f / 360.0f);

        globalcursectnum = dacursectnum;
        totalclocklock = totalclock;

        cosglobalang = (int) BCosAngle(globalang);
        singlobalang = (int) BSinAngle(globalang);

        cosviewingrangeglobalang = mulscale(cosglobalang, viewingrange, 16);
        sinviewingrangeglobalang = mulscale(singlobalang, viewingrange, 16);

        Arrays.fill(gotpic, (byte) 0);
        Arrays.fill(gotsector, (byte) 0);

        render.drawrooms();
        return 0;
    }

    public void drawmasks() { // gdxBuild
        render.drawmasks();
    }

    public void drawmapview(int dax, int day, int zoome, int ang) { // gdxBuild
        render.drawmapview(dax, day, zoome, ang);
    }

    public void drawoverheadmap(int cposx, int cposy, int czoom, short cang) { // gdxBuild
        render.drawoverheadmap(cposx, cposy, czoom, cang);
    }

    // JBF: davidoption now functions as a windowed-mode flag (0 == windowed, 1 ==
    // fullscreen)
    public boolean setgamemode(int davidoption, int daxdim, int daydim) { // jfBuild + gdxBuild
        if (BuildGdx.app.getType() == Application.ApplicationType.Android) {
            daxdim = BuildGdx.graphics.getWidth();
            daydim = BuildGdx.graphics.getHeight();
            davidoption = 0;
        }

        daxdim = max(320, daxdim);
        daydim = max(200, daydim);

        if (render.isInited()
                && ((davidoption == (BuildGdx.graphics.isFullscreen() ? 1 : 0))
                && (BuildGdx.graphics.getWidth() == daxdim) && (BuildGdx.graphics.getHeight() == daydim))
                && xdim == daxdim && ydim == daydim)
            return true;

        xdim = daxdim;
        ydim = daydim;

        setview(0, 0, xdim - 1, ydim - 1);
        setbrightness(curbrightness, palette, GLRenderer.GLInvalidateFlag.All);

        Console.ResizeDisplay(daxdim, daydim);

        if (render instanceof Software) {
            // Software renderer must be reinitialize when resolution is changed
            if (render.isInited())
                render.uninit();
            render.init();
        }

        if (davidoption == 1) {
            Graphics.DisplayMode m = null;
            for (Graphics.DisplayMode mode : BuildGdx.graphics.getDisplayModes()) {
                if (mode.width == daxdim && mode.height == daydim)
                    if (m == null || m.refreshRate < mode.refreshRate) {
                        m = mode;
                    }
            }

            if (m == null) {
                Console.Println("Warning: " + daxdim + "x" + daydim + " fullscreen not supported", OSDTEXT_YELLOW);
                BuildGdx.graphics.setWindowedMode(daxdim, daydim);
                return false;
            } else
                BuildGdx.graphics.setFullscreenMode(m);
        } else
            BuildGdx.graphics.setWindowedMode(daxdim, daydim);

        return true;
    }

    public void registerFade(String fadename, FadeEffect effect) { // gdxBuild
        if (fades == null)
            fades = new HashMap<String, FadeEffect>();
        fades.put(fadename, effect);
    }

    public void updateFade(String fadename, int intensive) // gdxBuild
    {
        FadeEffect effect = fades.get(fadename);
        if (effect != null)
            effect.update(intensive);
    }

    public void showfade() { // gdxBuild
        GLRenderer gl = glrender();
        if (gl != null)
            gl.palfade(fades);
    }

    public void nextpage() {
        render.nextpage();
        totalclocklock = totalclock;
    }

    public void setaspect_new() { // eduke32 aspect
        if (BuildSettings.usenewaspect.get() && newaspect_enable != 0 && (4 * xdim / 5) != ydim) {
            // the correction factor 100/107 has been found
            // out experimentally. squares ftw!
            int yx = (65536 * 4 * 100) / (3 * 107);
            int xd = setaspect_new_use_dimen != 0 ? xdimen : xdim;
            int yd = setaspect_new_use_dimen != 0 ? ydimen : ydim;

            int vr = divscale(xd * 3, yd * 4, 16);

            setaspect(vr, yx);
        } else
            setaspect(65536, divscale(ydim * 320, xdim * 200, 16));
    }

    public void setview(int x1, int y1, int x2, int y2) { // jfBuild
        windowx1 = x1;
        wx1 = (x1 << 12);
        windowy1 = y1;
        wy1 = (y1 << 12);
        windowx2 = x2;
        wx2 = ((x2 + 1) << 12);
        windowy2 = y2;
        wy2 = ((y2 + 1) << 12);

        xdimen = (x2 - x1) + 1;
        halfxdimen = (xdimen >> 1);
        ydimen = (y2 - y1) + 1;

        setaspect_new();

        render.setview(x1, y1, x2, y2);
    }

    public void setaspect(int daxrange, int daaspect) { // jfBuild
        viewingrange = offscreenrendering ? daxrange : (int) (daxrange * fovFactor);
        viewingrangerecip = divscale(1, viewingrange, 32);

        yxaspect = daaspect;
        xyaspect = divscale(1, yxaspect, 32);
        xdimenscale = scale(xdimen, yxaspect, 320);
        xdimscale = scale(320, xyaspect, xdimen);

        if (render.getType() == Renderer.RenderType.PolyGDX) {
            int w = 320;
            if ((4 * xdim / 5) == ydim)
                w = 300;
            float k = daxrange / (float) divscale(xdim * 240, ydim * w, 16);
            ((GDXRenderer) render)
                    .setFieldOfView(offscreenrendering ? 110 : (float) Math.toDegrees(2 * Math.atan(k * fovFactor)));
        }
    }

    public void setFov(int fov) {
        this.fovFactor = (float) Math.tan(fov * Math.PI / 360.0);
        setaspect_new();
    }

    // dastat&1 :translucence
    // dastat&2 :auto-scale mode (use 320*200 coordinates)
    // dastat&4 :y-flip
    // dastat&8 :don't clip to startumost/startdmost
    // dastat&16 :force point passed to be top-left corner, 0:Editart center
    // dastat&32 :reverse translucence
    // dastat&64 :non-masked, 0:masked
    // dastat&128 :draw all pages (permanent)
    // dastat&256 :align to the left (widescreen support)
    // dastat&512 :align to the right (widescreen support)
    // dastat&1024 :stretch to screen resolution (distorts aspect ration)

    public void rotatesprite(int sx, int sy, int z, int a, int picnum, // gdxBuild
                             int dashade, int dapalnum, int dastat, int cx1, int cy1, int cx2, int cy2) {
        render.rotatesprite(sx, sy, z, a, picnum, dashade, dapalnum, dastat, cx1, cy1, cx2, cy2);
    }

    public void setbrightness(int dabrightness, byte[] dapal, GLRenderer.GLInvalidateFlag flags) {
        final GLRenderer gl = glrender();
        curbrightness = BClipRange(dabrightness, 0, 15);

        if ((gl == null || gl.getType().getFrameType() != BuildFrame.FrameType.GL) && curbrightness != 0) {
            for (int i = 0; i < dapal.length; i++)
                temppal[i] = britable[curbrightness][(dapal[i] & 0xFF) << 2];
        } else {
//			if (gl.getTexFormat() == PixelFormat.Rgb) { // Polymost
            System.arraycopy(dapal, 0, temppal, 0, dapal.length);
            for (int i = 0; i < dapal.length; i++)
                temppal[i] <<= 2;
//			} else {
//				for (int i = 0; i < dapal.length; i++)
//					temppal[i] = britable[curbrightness][(dapal[i] & 0xFF) << 2];
//			}
        }

        if (changepalette(temppal)) {
            if (gl != null)
                gl.gltexinvalidateall(flags);

            palfadergb.r = palfadergb.g = palfadergb.b = 0;
            palfadergb.a = 0;
        }
    }

    public boolean changepalette(final byte[] palette) {
        if (render.getType() != Renderer.RenderType.Software && CRC32.getChecksum(palette) == curpalette.getCrc32())
            return false;

        curpalette.update(palette);
        Arrays.fill(palcache, null);

        BuildGdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                render.changepalette(palette);
            }
        });
        return true;
    }

    public void setpalettefade(int r, int g, int b, int offset) { // jfBuild
        palfadergb.r = min(63, r) << 2;
        palfadergb.g = min(63, g) << 2;
        palfadergb.b = min(63, b) << 2;
        palfadergb.a = (min(63, offset) << 2);

        if (glrender() == null) { // if 8bit renderer
            int k = 0;
            for (int i = 0; i < 256; i++) {
                temppal[k++] = (byte) (curpalette.getRed(i) + (((palfadergb.r - curpalette.getRed(i)) * offset) >> 6));
                temppal[k++] = (byte) (curpalette.getGreen(i)
                        + (((palfadergb.g - curpalette.getGreen(i)) * offset) >> 6));
                temppal[k++] = (byte) (curpalette.getBlue(i)
                        + (((palfadergb.b - curpalette.getBlue(i)) * offset) >> 6));
            }

            render.changepalette(temppal);
        }
    }

    public void clearview(int dacol) { // gdxBuild
        render.clearview(dacol);
    }

    public void setviewtotile(int tilenume, int xsiz, int ysiz) // jfBuild
    {
        Tile pic = engine.getTile(tilenume);
        if (render.getType() == Renderer.RenderType.Software) {
            ((Software) render).setviewtotile(tilenume, pic.getWidth(), pic.getHeight());
            return;
        }

        // DRAWROOMS TO TILE BACKUP&SET CODE
        pic.setWidth(xsiz);
        pic.setHeight(ysiz);
        bakwindowx1[setviewcnt] = windowx1;
        bakwindowy1[setviewcnt] = windowy1;
        bakwindowx2[setviewcnt] = windowx2;
        bakwindowy2[setviewcnt] = windowy2;

        if (setviewcnt == 0)
            baktile = tilenume;

        offscreenrendering = true;

        setviewcnt++;
        setview(0, 0, ysiz - 1, xsiz - 1);
        setaspect(65536, 65536);
    }

    public void setviewback() // jfBuild
    {
        if (setviewcnt <= 0)
            return;
        setviewcnt--;

        offscreenrendering = (setviewcnt > 0);

        if (setviewcnt == 0 && render.getType() != Renderer.RenderType.Software) {
            engine.getTile(baktile).data = setviewbuf();
            render.invalidatetile(baktile, -1, -1);
        }

        setview(bakwindowx1[setviewcnt], bakwindowy1[setviewcnt], bakwindowx2[setviewcnt], bakwindowy2[setviewcnt]);

        if (render.getType() == Renderer.RenderType.Software)
            ((Software) render).setviewback();
    }

    public void preparemirror(int dax, int day, int daz, float daang, float dahoriz, int dawall, int dasector) { // jfBuild
        int x = Engine.getWall(dawall).getX();
        int dx = getWall(Engine.getWall(dawall).getPoint2()).getX() - x;
        int y = Engine.getWall(dawall).getY();
        int dy = getWall(Engine.getWall(dawall).getPoint2()).getY() - y;
        int j = dx * dx + dy * dy;
        if (j == 0)
            return;
        int i = (((dax - x) * dx + (day - y) * dy) << 1);
        mirrorx = (x << 1) + scale(dx, i, j) - dax;
        mirrory = (y << 1) + scale(dy, i, j) - day;
        mirrorang = BClampAngle((EngineUtils.getAngle(dx, dy) << 1) - daang);

        inpreparemirror = true;
    }

    public void completemirror() {
        render.completemirror();
    }

    public void printext256(int xpos, int ypos, int col, int backcol, char[] name, int fontsize, float scale) { // gdxBuild
        render.printext(xpos, ypos, col, backcol, name, fontsize, scale);
    }

    public String screencapture(String fn) { // jfBuild + gdxBuild (screenshot)
        int a, b, c, d;
        fn = fn.replaceAll("[^a-zA-Z0-9_. \\[\\]-]", "");

        fn = fn.substring(0, fn.lastIndexOf('.') - 4);

        DirectoryEntry userdir = BuildGdx.compat.getDirectory(Compat.Path.User);

        int capturecount = 0;
        do { // JBF 2004022: So we don't overwrite existing screenshots
            if (capturecount > 9999)
                return null;

            a = ((capturecount / 1000) % 10);
            b = ((capturecount / 100) % 10);
            c = ((capturecount / 10) % 10);
            d = (capturecount % 10);

            if (userdir.checkFile(fn + a + b + c + d + ".png") == null)
                break;
            capturecount++;
        } while (true);

        int w = xdim, h = ydim;
        Pixmap capture = null;
        try {
            capture = new Pixmap(w, h, Pixmap.Format.RGB888);
            ByteBuffer pixels = capture.getPixels();
            pixels.put(render.getFrame(TileData.PixelFormat.Rgb, xdim, -ydim));

            File pci = new File(userdir.getAbsolutePath() + fn + a + b + c + d + ".png");
            PixmapIO.writePNG(new FileHandle(pci), capture);
            userdir.addFile(pci);
            capture.dispose();
            return fn + a + b + c + d + ".png";
        } catch (Throwable e) {
            if (capture != null)
                capture.dispose();
            return null;
        }
    }

    protected byte[] setviewbuf() { // gdxBuild
        Tile pic = engine.getTile(baktile);

        int width = pic.getWidth();
        int heigth = pic.getHeight();
        byte[] data = pic.data;
        if (data == null || data.length < width * heigth)
            data = new byte[width * heigth];

        ByteBuffer frame = render.getFrame(TileData.PixelFormat.Pal8, width, heigth);

        int dptr;
        int sptr = 0;
        for (int i = width - 1, j; i >= 0; i--) {
            dptr = i;
            for (j = 0; j < heigth; j++) {
                data[dptr] = frame.get(sptr++);
                dptr += width;
            }
        }

        return data;
    }

    public boolean setrendermode(Renderer render) { // gdxBuild
        this.render = render;
        render.setDefs(engine.getDefs());
        render.init();
        return render.isInited();
    }

    public Renderer getrender() // gdxBuild
    {
        return render;
    }

    public GLRenderer glrender() {
        if (render != null && getrender().getType().getFrameType() == BuildFrame.FrameType.GL)
            return (GLRenderer) render;

        return null;
    }

    public void printfps(float scale) {
        if (System.currentTimeMillis() - fpstime >= 1000) {
            int fps = BuildGdx.graphics.getFramesPerSecond();
            float rate = BuildGdx.graphics.getDeltaTime() * 1000;
            if (fps <= 9999 && rate <= 9999) {
                int chars = buildString(fpsbuffer, 0, Double.toString(Math.round(rate * 100) / 100.0));
//				int chars = Bitoa((int) rate, fpsbuffer);
                chars = buildString(fpsbuffer, chars, "ms ", fps);
                chars = buildString(fpsbuffer, chars, "fps");
                fpsx = windowx2 - (int) ((chars << 3) * scale);
                fpsy = windowy1 + 1;
            }
            fpstime = System.currentTimeMillis();
        }
        render.printext(fpsx, fpsy, fpscol, -1, fpsbuffer, 0, scale);
    }

    public void uninit() {
        try {
            if (render != null && render.isInited())
                render.uninit();
        } catch (Exception ignored) {
        }
    }
}
