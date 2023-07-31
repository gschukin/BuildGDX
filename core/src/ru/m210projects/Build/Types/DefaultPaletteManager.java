package ru.m210projects.Build.Types;

import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.CRC32;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Render.GLRenderer;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Render.Types.Color;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.StreamUtils;
import ru.m210projects.Build.osd.Console;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.*;
import static ru.m210projects.Build.Engine.MAXPALOOKUPS;
import static ru.m210projects.Build.Pragmas.divscale;
import static ru.m210projects.Build.Pragmas.mulscale;

public class DefaultPaletteManager implements PaletteManager {

    private final Palette currentPalette;
    private final FastColorLookup fastColorLookup;
    private final byte[] basePalette;
    private final byte[][] palookup = new byte[MAXPALOOKUPS][];
    private final byte[] transluc;
    private final int shadesCount;
    private final byte[][] britable;
    private final Color[] palookupfog;
    private final Engine engine;

    public DefaultPaletteManager(Engine engine, Entry entry) throws IOException {
        if (!entry.exists()) {
            throw new FileNotFoundException("Failed to load \"palette.dat\"!");
        }

        try (InputStream is = entry.getInputStream()) {
            Console.out.println("Loading palettes");
            this.basePalette = StreamUtils.readBytes(is, 768);
            Console.out.println("Loading palookup tables");
            this.shadesCount = StreamUtils.readShort(is);
            this.palookup[0] = StreamUtils.readBytes(is, shadesCount << 8);
            Console.out.println("Loading translucency table");
            this.transluc = StreamUtils.readBytes(is, 65536);
        }

        this.engine = engine;
        this.britable = new byte[16][256];
        this.palookupfog = new Color[MAXPALOOKUPS];
        currentPalette = new Palette().update(basePalette);
        this.fastColorLookup = createFastColorLookup(30, 59, 11);
        calcBritable();
    }

    @Override
    public Color getFogColor(int pal) {
        return palookupfog[pal];
    }

    @Override
    public FastColorLookup createFastColorLookup(int rscale, int gscale, int bscale) { // jfBuild
        DefaultFastColorLookup fastColorLookup = new DefaultFastColorLookup();
        fastColorLookup.initFastColorLookup(this, rscale, gscale, bscale);
        return fastColorLookup;
    }

    protected void calcBritable() { // jfBuild
        for (int i = 0; i < 16; i++) {
            float a = 8.0f / (i + 8);
            float b = (float) (255.0f / pow(255.0f, a));
            for (int j = 0; j < 256; j++) {// JBF 20040207: full 8bit precision
                britable[i][j] = (byte) (pow(j, a) * b);
            }
        }
    }

    @Override
    public boolean isValidPalette(int paletteIndex) {
        if (paletteIndex < 0 || paletteIndex >= palookup.length) {
            return false;
        }
        return palookup[paletteIndex] != null;
    }

    @Override
    public boolean changePalette(final byte[] palette) {
        Renderer renderer = engine.getrender();
        if (renderer.getType() != Renderer.RenderType.Software && CRC32.getChecksum(palette) == currentPalette.getCrc32()) {
            return false;
        }

        currentPalette.update(palette);
        fastColorLookup.invalidate();
        BuildGdx.app.postRunnable(() -> renderer.changepalette(palette));
        return true;
    }

    @Override
    public byte[] makePalookup(final int palnum, byte[] remapbuf, int r, int g, int b, int dastat) { // jfBuild
        if (!isValidPalette(palnum)) {
            palookup[palnum] = new byte[shadesCount << 8];
        }

        if (dastat == 0 || (r | g | b | 63) != 63) {
            return palookup[palnum];
        }

        if ((r | g | b) == 0) {
            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < shadesCount; j++) {
                    palookup[palnum][i + j * 256] = palookup[0][(remapbuf[i] & 0xFF) + j * 256];
                }
            }
        } else {
            for (int i = 0; i < shadesCount; i++) {
                int palscale = divscale(i, shadesCount, 16);
                for (int j = 0; j < 256; j++) {
                    int rptr = basePalette[3 * (remapbuf[j] & 0xFF)] & 0xFF;
                    int gptr = basePalette[3 * (remapbuf[j] & 0xFF) + 1] & 0xFF;
                    int bptr = basePalette[3 * (remapbuf[j] & 0xFF) + 2] &  0xFF;

                    palookup[palnum][j + i * 256] = fastColorLookup.getClosestColorIndex(basePalette, rptr + mulscale(r - rptr, palscale, 16), gptr + mulscale(g - gptr, palscale, 16), bptr + mulscale(b - bptr, palscale, 16));
                }
            }
        }
        palookupfog[palnum] = new Color(r, g, b, 0);

        Renderer renderer = engine.getrender();
        if (renderer instanceof GLRenderer) {
            BuildGdx.app.postRunnable(() -> ((GLRenderer) renderer).getTextureManager().invalidatepalookup(palnum));
        }

        return palookup[palnum];
    }

    @Override
    public byte[] getBasePalette() {
        return basePalette;
    }

    @Override
    public Palette getCurrentPalette() {
        return currentPalette;
    }

    @Override
    public FastColorLookup getFastColorLookup() {
        return fastColorLookup;
    }

    @Override
    public int getColorIndex(int pal, int colorIndex) {
        if (colorIndex >= palookup[pal].length) {
            return 0;
        }
        
        return palookup[pal][colorIndex] & 0xFF;
    }

    @Override
    public byte[][] getPalookupBuffer() {
        return palookup;
    }

    @Override
    public byte[] getTranslucBuffer() {
        return transluc;
    }

    @Override
    public int getShadeCount() {
        return shadesCount;
    }

    @Override
    public byte[][] getBritableBuffer() {
        return britable;
    }
}
