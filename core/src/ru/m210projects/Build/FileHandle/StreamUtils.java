package ru.m210projects.Build.filehandle;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class StreamUtils {

    public static String readString(InputStream in, int length) throws IOException {
        return new String(readBytes(in, length)).trim();
    }

    public static int readByte(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) {
            throw new EOFException();
        }
        return b;
    }

    public static byte readSignedByte(InputStream in) throws IOException {
        return (byte) readByte(in);
    }

    public static boolean readBoolean(InputStream in) throws IOException {
        return in.read() == 1;
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
        return readBytes(in, new byte[len]);
    }

    public static byte[] readBytes(InputStream in, byte[] data, int len) throws IOException {
        int pos = 0;
        while (len > 0) {
            int l = in.read(data, pos, len);
            if (l == -1) {
                throw new EOFException();
            }
            len -= l;
            pos += l;
        }
        return data;
    }

    public static byte[] readBytes(InputStream in, byte[] data) throws IOException {
        return readBytes(in, data, data.length);
    }

    public static int readBuffer(InputStream is, ByteBuffer buffer) throws IOException {
        int len = 0;
        int remaining = buffer.remaining();
        byte[] data = new byte[8192];
        while (remaining > 0) {
            int l = is.read(data, 0, Math.min(remaining, 8192));
            buffer.put(data, 0, l);
            remaining -= l;
            len += l;
        }
        buffer.rewind();
        return len;
    }

    public static void skip(InputStream in, int n) throws IOException {
        while (n > 0) {
            long i = in.skip(n);
            if (i == 0) {
                throw new EOFException();
            }
            n -= i;
        }
    }

    public static void writeByte(OutputStream out, int v) throws IOException {
        out.write(v & 0xff);
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

    public static void writeLong(OutputStream out, long v) throws IOException {
        out.write((int) (v & 0xFF));
        out.write((int) ((v >>> 8) & 0xFF));
        out.write((int) ((v >>> 16) & 0xFF));
        out.write((int) ((v >>> 24) & 0xFF));
        out.write((int) ((v >>> 32) & 0xFF));
        out.write((int) ((v >>> 40) & 0xFF));
        out.write((int) ((v >>> 48) & 0xFF));
        out.write((int) ((v >>> 56) & 0xFF));
    }

    public static void writeString(OutputStream out, String v) throws IOException {
        out.write(v.getBytes());
    }

    public static void writeString(OutputStream out, String v, int len) throws IOException {
        writeBytes(out, v.getBytes(), len);
    }

    public static void writeBytes(OutputStream out, byte[] data) throws IOException {
        out.write(data);
    }

    public static void writeFloat(OutputStream os, float v) throws IOException {
        writeInt(os, Float.floatToRawIntBits(v));
    }

    public static void writeBytes(OutputStream out, byte[] data, int len) throws IOException {
        final byte[] buf = new byte[len];
        System.arraycopy(data, 0, buf, 0, Math.min(len, data.length));
        out.write(buf);
    }
}
