package ru.m210projects.Build;

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
        return new String(tmp);
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
        if(l != len) {
            throw new EOFException();
        }

        return data;
    }

    public static void skip(InputStream in, int n) throws IOException {
        long l = in.skip(n);
        if(l != n) {
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
