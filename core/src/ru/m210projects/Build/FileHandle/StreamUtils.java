package ru.m210projects.Build.filehandle;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtils {

    public static String readString(InputStream in, int length) throws IOException {
        byte[] tmp = new byte[length];
        int len = in.read(tmp);
        if (len != length) {
            throw new EOFException();
        }
        return new String(tmp).trim();
    }

    public static int readByte(InputStream in) throws IOException {
        return in.read() & 0xFF;
    }

    public static float readFloat(InputStream in) throws IOException {
        return Float.intBitsToFloat(readInt(in));
    }

    public static long readLong(InputStream in) throws IOException {
        long ch1 = in.read();
        long ch2 = in.read();
        long ch3 = in.read();
        long ch4 = in.read();
        long ch5 = in.read();
        long ch6 = in.read();
        long ch7 = in.read();
        long ch8 = in.read();

        if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0) {
            throw new EOFException();
        }

        return (ch1 | (ch2 << 8) | (ch3 << 16) | (ch4 << 24) | (ch5 << 32) | (ch6 << 40) | (ch7 << 48) | (ch8 << 56));
    }

    public static int readInt(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return (ch1 | (ch2 << 8) | (ch3 << 16) | (ch4 << 24));
    }

    public static int readShort(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (ch1 | (ch2 << 8));
    }

    public static byte[] readBytes(InputStream in, int len) throws IOException {
        byte[] data = new byte[len];
        int l = in.read(data);
        if (l != len) {
            throw new EOFException();
        }

        return data;
    }

    public static void readBytes(InputStream in, byte[] data, int len) throws IOException {
        int l = in.read(data, 0, len);
        if (l != len) {
            throw new EOFException();
        }
    }

    public static void skip(InputStream in, int n) throws IOException {
        long l = in.skip(n);
        if (l != n) {
            throw new EOFException();
        }
    }

    public static void writeInt(OutputStream out, long v) throws IOException {
        out.write((int) (v & 0xff));
        out.write((int) ((v >>> 8) & 0xff));
        out.write((int) ((v >>> 16) & 0xff));
        out.write((int) ((v >>> 24) & 0xff));
    }

    public static void writeShort(OutputStream out, int v) throws IOException {
        out.write(v & 0xff);
        out.write((v >>> 8) & 0xff);
    }

    public static void writeString(OutputStream out, String v) throws IOException {
        out.write(v.getBytes());
    }

}
