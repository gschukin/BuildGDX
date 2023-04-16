package ru.m210projects.Build;

import ru.m210projects.Build.FileHandle.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static ru.m210projects.Build.Pragmas.scale;

public class Tables {

    private final short[] sintable;
    private final short[] radarang;
    private final short[] sqrtable;
    private final short[] shlookup;
    private final byte[] textfont;
    private final byte[] smalltextfont;

    public Tables(Resource res) throws IOException {
        if(res == null) {
            throw new FileNotFoundException("Failed to load \"tables.dat\"!");
        }

        sqrtable = new short[4096];
        shlookup = new short[4096 + 256];
        sintable = new short[2048];
        radarang = new short[1280];

        byte[] buf = new byte[2048 * 2];
        res.read(buf);
        ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sintable);

        buf = new byte[640 * 2];
        res.read(buf);
        ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(radarang, 0, 640);
        for (int i = 0; i < 640; i++)
            radarang[1279 - i] = (short) -radarang[i];

        textfont = new byte[2048];
        smalltextfont = new byte[2048];
        res.read(textfont, 0, 1024);
        res.read(smalltextfont, 0, 1024);

//        try(InputStream is = entry.getInputStream()) {
//            ByteBuffer.wrap(StreamUtils.readBytes(is, sintable.length * 2)).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sintable);
//            ByteBuffer.wrap(StreamUtils.readBytes(is, radarang.length)).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(radarang, 0,  radarang.length / 2);
//            this.textfont = StreamUtils.readBytes(is, 1024);
//            this.smalltextfont = StreamUtils.readBytes(is, 1024);
//        }

        initSqrtTable();
        for (int i = 0; i < 640; i++) {
            radarang[1279 - i] = (short) -radarang[i];
        }
    }

    public byte[] getTextFont() {
        return textfont;
    }

    public byte[] getSmallTextFont() {
        return smalltextfont;
    }

    private void initSqrtTable() {
        int i, j = 1, k = 0;
        for (i = 0; i < 4096; i++) {
            if (i >= j) {
                j <<= 2;
                k++;
            }

            sqrtable[i] = (short) ((int) Math.sqrt(((i << 18) + 131072)) << 1);
            shlookup[i] = (short) ((k << 1) + ((10 - k) << 8));
            if (i < 256) {
                shlookup[i + 4096] = (short) (((k + 6) << 1) + ((10 - (k + 6)) << 8));
            }
        }
    }

    public int sin(int angle) {
        return sintable[angle & 2047];
    }

    public int cos(int angle) {
        return sin(angle + 512);
    }

    public int sqrt(int a) {
        long out = a & 0xFFFFFFFFL;
        int value;
        if ((out & 0xFF000000) != 0) {
            value = shlookup[(int) ((out >> 24) + 4096)] & 0xFFFF;
        } else {
            value = shlookup[(int) (out >> 12)] & 0xFFFF;
        }

        out >>= value & 0xff;
        out = (out & 0xffff0000) | (sqrtable[(int) out] & 0xFFFF);
        out >>= ((value & 0xff00) >> 8);

        return (int) out;
    }

    public int getAngle(int xvect, int yvect) { // jfBuild
        if ((xvect | yvect) == 0) {
            return (0);
        }

        if (xvect == 0) {
            return (512 + ((yvect < 0 ? 1 : 0) << 10));
        }

        if (yvect == 0) {
            return ((xvect < 0 ? 1 : 0) << 10);
        }

        if (xvect == yvect) {
            return (256 + ((xvect < 0 ? 1 : 0) << 10));
        }

        if (xvect == -yvect) {
            return (768 + ((xvect > 0 ? 1 : 0) << 10));
        }

        if (Math.abs((long) xvect) > Math.abs((long) yvect)) { // GDX 26.11.2021 Integer.MIN issue fix
            return (((radarang[640 + scale(160, yvect, xvect)] >> 6) + ((xvect < 0 ? 1 : 0) << 10)) & 2047);
        }
        return (((radarang[640 - scale(160, xvect, yvect)] >> 6) + 512 + ((yvect < 0 ? 1 : 0) << 10)) & 2047);
    }

    public int getRadarAng(int value) {
        if (value < 0 || value >= radarang.length) {
            return 0;
        }

        return radarang[value];
    }
}
