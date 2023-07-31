package ru.m210projects.Build;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import ru.m210projects.Build.Architecture.BuildFrame;
import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.filehandle.art.ArtEntry;
import ru.m210projects.Build.filehandle.art.CachedArtEntry;
import ru.m210projects.Build.filehandle.fs.Directory;
import ru.m210projects.Build.osd.Console;import ru.m210projects.Build.Render.GLRenderer;
import ru.m210projects.Build.Render.GdxRender.GDXRenderer;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Render.Software.Software;
import ru.m210projects.Build.Render.TextureHandle.TileData;
import ru.m210projects.Build.Render.Types.FadeEffect;
import ru.m210projects.Build.Render.Types.GL10;
import ru.m210projects.Build.Settings.BuildSettings;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.osd.OsdColor;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import static java.lang.Math.*;
import static java.lang.Math.pow;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Gameutils.*;
import static ru.m210projects.Build.Pragmas.*;
import static ru.m210projects.Build.Pragmas.scale;
import static ru.m210projects.Build.Strhandler.buildString;

public class RenderService {

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

    public int animateoffs(int tilenum, int nInfo) { // jfBuild + gdxBuild
        long clock, index = 0;
        ArtEntry tile = engine.getTile(tilenum);

        int speed = tile.getAnimSpeed();
        if ((nInfo & 0xC000) == 0x8000) { // sprite
            // hash sprite frame by info variable

            shortbuf[0] = (byte) ((nInfo) & 0xFF);
            shortbuf[1] = (byte) ((nInfo >>> 8) & 0xFF);

            clock = (totalclocklock + CRC32.getChecksum(shortbuf)) >> speed;
        } else {
            clock = totalclocklock >> speed;
        }

        int frames = tile.getAnimFrames();

        if (frames > 0) {
            switch (tile.getType()) {
                case OSCIL:
                    index = clock % (frames * 2L);
                    if (index >= frames) {
                        index = frames * 2L - index;
                    }
                    break;
                case FORWARD:
                    index = clock % (frames + 1);
                    break;
                case BACKWARD:
                    index = -(clock % (frames + 1));
                    break;
                default: // None
                    break;
            }
        }
        return (int) index;
    }

    public int drawrooms(float daposx, float daposy, float daposz, float daang, float dahoriz, int dacursectnum) { // eDuke32
        beforedrawrooms = 0;

        globalposx = (int) daposx;
        globalposy = (int) daposy;
        globalposz = (int) daposz;

        globalang = BClampAngle(daang);
        globalhoriz = (dahoriz - 100);
        pitch = (-EngineUtils.getAngle(160, (int) (dahoriz - 100))) / (2048.0f / 360.0f);

        globalcursectnum = (short) dacursectnum;
        totalclocklock = engine.getTotalClock();

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
                && xdim == daxdim && ydim == daydim) {
            return true;
        }

        xdim = daxdim;
        ydim = daydim;

        setview(0, 0, xdim - 1, ydim - 1);
        setbrightness(curbrightness, engine.getPaletteManager().getBasePalette(), GLRenderer.GLInvalidateFlag.All);

        Console.out.revalidate();

        if (render instanceof Software) {
            // Software renderer must be reinitialize when resolution is changed
            if (render.isInited()) {
                render.uninit();
            }
            render.init();
        }

        if (davidoption == 1) {
            Graphics.DisplayMode m = null;
            for (Graphics.DisplayMode mode : BuildGdx.graphics.getDisplayModes()) {
                if (mode.width == daxdim && mode.height == daydim) {
                    if (m == null || m.refreshRate < mode.refreshRate) {
                        m = mode;
                    }
                }
            }

            if (m == null) {
                Console.out.println("Warning: " + daxdim + "x" + daydim + " fullscreen not supported", OsdColor.YELLOW);
                BuildGdx.graphics.setWindowedMode(daxdim, daydim);
                return false;
            } else {
                BuildGdx.graphics.setFullscreenMode(m);
            }
        } else {
            BuildGdx.graphics.setWindowedMode(daxdim, daydim);
        }

        return true;
    }

    public void registerFade(String fadename, FadeEffect effect) { // gdxBuild
        if (fades == null) {
            fades = new HashMap<String, FadeEffect>();
        }
        fades.put(fadename, effect);
    }

    public void updateFade(String fadename, int intensive) // gdxBuild
    {
        FadeEffect effect = fades.get(fadename);
        if (effect != null) {
            effect.update(intensive);
        }
    }

    public void showfade() { // gdxBuild
        GLRenderer gl = glrender();
        if (gl != null) {
            gl.palfade(fades);
        }
    }

    public void nextpage() {
        render.nextpage();
        totalclocklock = engine.getTotalClock();
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
        } else {
            setaspect(65536, divscale(ydim * 320, xdim * 200, 16));
        }
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
            if ((4 * xdim / 5) == ydim) {
                w = 300;
            }
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
        curbrightness = BClipRange(dabrightness, 0, 15);
        PaletteManager paletteManager = engine.getPaletteManager();
        byte[][] britable = paletteManager.getBritableBuffer();

        if (render instanceof Software && curbrightness != 0) {
            for (int i = 0; i < dapal.length; i++) {
                temppal[i] = britable[curbrightness][(dapal[i] & 0xFF) << 2];
            }
        } else {
//			if (gl.getTexFormat() == PixelFormat.Rgb) { // Polymost
            System.arraycopy(dapal, 0, temppal, 0, dapal.length);
            for (int i = 0; i < dapal.length; i++) {
                temppal[i] <<= 2;
            }
//			} else {
//				for (int i = 0; i < dapal.length; i++)
//					temppal[i] = britable[curbrightness][(dapal[i] & 0xFF) << 2];
//			}
        }

        if (engine.getPaletteManager().changePalette(temppal)) {
            if (render instanceof GLRenderer) {
                ((GLRenderer) render).gltexinvalidateall(flags);
            }

            palfadergb.r = palfadergb.g = palfadergb.b = 0;
            palfadergb.a = 0;
        }
    }

    public void setpalettefade(int r, int g, int b, int offset) { // jfBuild
        palfadergb.r = min(63, r) << 2;
        palfadergb.g = min(63, g) << 2;
        palfadergb.b = min(63, b) << 2;
        palfadergb.a = (min(63, offset) << 2);

        Palette curpalette = engine.getPaletteManager().getCurrentPalette();
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

    public void setviewtotile(int tilenume) // jfBuild
    {
        ArtEntry pic = engine.getTile(tilenume);
        if (render.getType() == Renderer.RenderType.Software) {
            ((Software) render).setviewtotile(tilenume);
            return;
        }

        // DRAWROOMS TO TILE BACKUP&SET CODE
        bakwindowx1[setviewcnt] = windowx1;
        bakwindowy1[setviewcnt] = windowy1;
        bakwindowx2[setviewcnt] = windowx2;
        bakwindowy2[setviewcnt] = windowy2;

        if (setviewcnt == 0) {
            baktile = tilenume;
        }

        offscreenrendering = true;

        setviewcnt++;
        setview(0, 0, pic.getHeight() - 1, pic.getWidth() - 1);
        setaspect(65536, 65536);
    }

    public void setviewback() // jfBuild
    {
        if (setviewcnt <= 0) {
            return;
        }
        setviewcnt--;

        offscreenrendering = (setviewcnt > 0);

        if (setviewcnt == 0 && render.getType() != Renderer.RenderType.Software) {
            ArtEntry artEntry = engine.getTile(baktile);
            if (artEntry instanceof CachedArtEntry) {
                ((CachedArtEntry) artEntry).copyData(setviewbuf());
            }
            render.invalidatetile(baktile, -1, -1);
        }

        setview(bakwindowx1[setviewcnt], bakwindowy1[setviewcnt], bakwindowx2[setviewcnt], bakwindowy2[setviewcnt]);

        if (render.getType() == Renderer.RenderType.Software) {
            ((Software) render).setviewback();
        }
    }

    public void preparemirror(int dax, int day, int daz, float daang, float dahoriz, int dawall, int dasector) { // jfBuild
        BoardService boardService = engine.getBoardService();
        int x = boardService.getWall(dawall).getX();
        int dx = boardService.getWall(boardService.getWall(dawall).getPoint2()).getX() - x;
        int y = boardService.getWall(dawall).getY();
        int dy = boardService.getWall(boardService.getWall(dawall).getPoint2()).getY() - y;
        int j = dx * dx + dy * dy;
        if (j == 0) {
            return;
        }
        int i = (((dax - x) * dx + (day - y) * dy) << 1);
        mirrorx = (x << 1) + scale(dx, i, j) - dax;
        mirrory = (y << 1) + scale(dy, i, j) - day;
        mirrorang = BClampAngle((EngineUtils.getAngle(dx, dy) << 1) - daang);

        inpreparemirror = true;
    }

    public void completemirror() {
        render.completemirror();
    }

    public String screencapture(Directory dir, String fn) { // jfBuild + gdxBuild (screenshot)
        int a, b, c, d;
        fn = fn.replaceAll("[^a-zA-Z0-9_. \\[\\]-]", "");

        fn = fn.substring(0, fn.lastIndexOf('.') - 4);

        int capturecount = 0;
        do { // JBF 2004022: So we don't overwrite existing screenshots
            if (capturecount > 9999) {
                return null;
            }

            a = ((capturecount / 1000) % 10);
            b = ((capturecount / 100) % 10);
            c = ((capturecount / 10) % 10);
            d = (capturecount % 10);

            if(!dir.getEntry(fn + a + b + c + d + ".png").exists()) {
                break;
            }

            capturecount++;
        } while (true);

        int w = xdim, h = ydim;
        Pixmap capture = null;
        try {
            capture = new Pixmap(w, h, Pixmap.Format.RGB888);
            ByteBuffer pixels = capture.getPixels();
            pixels.put(render.getFrame(TileData.PixelFormat.Rgb, xdim, -ydim));
            Path path = dir.getPath().resolve(fn + a + b + c + d + ".png");
            PixmapIO.writePNG(new FileHandle(path.toFile()), capture);
            dir.revalidate();
            capture.dispose();
            return fn + a + b + c + d + ".png";
        } catch (Throwable e) {
            if (capture != null) {
                capture.dispose();
            }
            return null;
        }
    }

    protected byte[] setviewbuf() { // gdxBuild
        ArtEntry pic = engine.getTile(baktile);

        int width = pic.getWidth();
        int heigth = pic.getHeight();
        byte[] data = pic.getBytes();
        if (data == null || data.length < width * heigth) {
            data = new byte[width * heigth];
        }

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
        if (render != null && getrender().getType().getFrameType() == BuildFrame.FrameType.GL) {
            return (GLRenderer) render;
        }

        return null;
    }

    public void printfps(float scale) {
        if (System.currentTimeMillis() - fpstime >= 1000) {
            int fps = BuildGdx.graphics.getFramesPerSecond();
            float rate = BuildGdx.graphics.getDeltaTime() * 1000;
            if (fps <= 9999 && rate <= 9999) {
                buildString(fpsbuffer, 0, String.format(Locale.US, "%.2fms %dfps", Math.round(rate * 100) / 100.0, fps));
            }
            fpstime = System.currentTimeMillis();
        }
        EngineUtils.getLargeFont().drawText(windowx2 - 1, windowy1 + 1, fpsbuffer, scale, 0, fpscol, TextAlign.Right, Transparent.None, false);
    }

    public void uninit() {
        try {
            if (render != null && render.isInited()) {
                render.uninit();
            }
        } catch (Exception ignored) {
        }
    }
}
