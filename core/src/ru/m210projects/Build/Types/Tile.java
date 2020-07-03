package ru.m210projects.Build.Types;

public class Tile {

	public enum AnimType {
		Oscil(64), Forward(128), Backward(192), None(0);

		private byte bit;

		AnimType(int bit) {
			this.bit = (byte) bit;
		}

		public int getBit() {
			return bit & 0xFF;
		}

		public boolean hasBit(int picanm) {
			return ((picanm & 192) == (bit & 0xFF));
		}
	};

	public int width, height;
	public int anm;
	public byte[] data;

	public int getSize() {
		return width * height;
	}

	public Tile allocate(int xsiz, int ysiz) {
		int dasiz = xsiz * ysiz;

		data = new byte[dasiz];
		width = xsiz;
		height = ysiz;
		anm = 0;

		return this;
	}

	public Tile clear() {
		data = null;
		width = height = 0;
		anm = 0;

		return this;
	}

	public byte getOffsetX() {
		return (byte) ((anm >> 8) & 0xFF);
	}

	public byte getOffsetY() {
		return (byte) ((anm >> 16) & 0xFF);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Tile setWidth(int width) {
		this.width = width;

		return this;
	}

	public Tile setHeight(int height) {
		this.height = height;

		return this;
	}

	public int getFrames() {
		return anm & 0x3F;
	}

	public int getSpeed() {
		return (anm >> 24) & 15;
	}

	public AnimType getType() {
		switch (anm & 192) {
		case 64:
			return AnimType.Oscil;
		case 128:
			return AnimType.Forward;
		case 192:
			return AnimType.Backward;
		}
		return AnimType.None;
	}
}
