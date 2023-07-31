package ru.m210projects.Build.Types;

import ru.m210projects.Build.CRC32;

public class Palette {
	
	private long crc32 = -1;
	private final byte[] bytes;

	public Palette() {
		bytes = new byte[768];
	}
	
	public Palette update(byte[] palette) {
		System.arraycopy(palette, 0, bytes, 0, palette.length);
		return this;
	}
	
	public long getCrc32() {
		if (crc32 == -1) {
			crc32 = CRC32.getChecksum(bytes);
		}
		return crc32;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public int getRed(int index) {
		return bytes[3 * index] & 0xFF;
	}

	public int getGreen(int index) {
		return bytes[3 * index + 1] & 0xFF;
	}

	public int getBlue(int index) {
		return bytes[3 * index + 2] & 0xFF;
	}
	
	public int getRGB(int p) {
		return getRed(p) | ( getGreen(p) << 8 ) | ( getBlue(p) << 16 );
	}
	
	public int getRGBA(int index, byte alphaMask) {
		return getRGB(index) | (alphaMask << 24);
	}
	
	public int getRGBA(int p) {
		return getRGBA(p, (byte) 0xFF);
	}
}
