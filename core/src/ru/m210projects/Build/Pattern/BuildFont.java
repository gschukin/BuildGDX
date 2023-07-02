//This file is part of BuildGDX.
//Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
//
//BuildGDX is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//BuildGDX is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.Pattern;

import static ru.m210projects.Build.Pragmas.scale;
import static ru.m210projects.Build.Strhandler.toCharArray;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Types.TileFont;
import ru.m210projects.Build.Types.font.TextAlign;

public class  BuildFont {

	public static final BuildChar DUMMY_CHAR = new BuildChar(null) {
		@Override
		public int draw(int x, int y, int scale, int shade, int pal, int nBits, boolean shadow) {
			return 0;
		}
	};

	public static final int nSpace = -2;

	protected int nHeight;
	protected int nFlags;
	protected final BuildChar[] charInfo;
	protected final Engine draw;

	protected BuildFont(Engine draw) {
		this.draw = draw;
		this.charInfo = initCharInfo(draw);
	}

	public TileFont.FontType getFontType() {
		return null;
	}

	protected BuildChar[] initCharInfo(Engine draw) {
		BuildChar[] charInfo = new BuildChar[256];
		for (int i = 0; i < charInfo.length; i++) {
			charInfo[i] = new BuildChar(this);
		}
		return charInfo;
	}

	protected BuildFont(Engine draw, int nHeigth, int nFlags) {
		this(draw, nHeigth, 65536, nFlags);
	}

	protected BuildFont(Engine draw, int nHeigth, int nScale, int nFlags) {
		this.draw = draw;
		this.nHeight = nHeigth;
		this.nFlags = nFlags;
		this.charInfo = initCharInfo(draw);
	}

	protected void addChar(char ch, int nTile, int nWidth, int xOffset, int yOffset) {
		charInfo[ch].nTile = nTile;
		charInfo[ch].xOffset = (short) xOffset;
		charInfo[ch].yOffset = (short) yOffset;
		charInfo[ch].width = (short) nWidth;
	}

	protected void addChar(char ch, int nTile) {
		charInfo[ch].nTile = nTile;
		charInfo[ch].xOffset = 0;
		charInfo[ch].yOffset = 0;
		charInfo[ch].width = (short) draw.getTile(nTile).getWidth();
	}

	public void invalidate(int palnum) {
		for (int i = 0; i < 256; i++) {
			int tile = getTile(i);
			if (tile >= 0) {
				draw.getrender().invalidatetile(tile, palnum, -1);
			}
		}
	}

	public BuildChar getCharInfo(char ch) {
		if (!charBounds(ch)) {
			return DUMMY_CHAR;
		}

		return this.charInfo[ch];
	}

	public int drawText(int x, int y, String text, int shade, int pal, TextAlign textAlign, int nBits, boolean shadow) {
		return drawText(x, y, toCharArray(text), 0x10000, shade, pal, textAlign, nBits, shadow);
	}

	public int drawText(int x, int y, char[] text, int shade, int pal, TextAlign textAlign, int nBits, boolean shadow) {
		return drawText(x, y, text, 0x10000, shade, pal, textAlign, nBits, shadow);
	}

	public int drawText(int x, int y, String text, int scale, int shade, int pal, TextAlign textAlign, int nBits, boolean shadow) {
		return drawText(x, y, toCharArray(text), scale, shade, pal, textAlign, nBits, shadow);
	}

	public int drawText(int x, int y, char[] text, int scale, int shade, int pal, TextAlign textAlign, int nBits, boolean shadow) {
		if (text == null || text.length == 0) {
			return 0;
		}

		if (textAlign != TextAlign.Left) {
			int nWidth = getWidth(text, scale);
			if (textAlign == TextAlign.Center) {
				nWidth >>= 1;
			}
			x -= nWidth;
		}

		int alignx = 0;
		for (int i = 0; i < text.length && text[i] != 0; i++) {
			if (!charBounds(text[i])) {
				continue;
			}

			BuildChar charInfo = this.charInfo[text[i]];
			alignx += charInfo.draw(x + alignx, y, scale, shade, pal, nFlags | nBits, shadow);
		}
		return alignx;
	}

	public int getWidth(char[] text) {
		return getWidth(text, 0x10000);
	}

	public int getWidth(char[] text, int scale) {
		int width = 0;
		if (text != null) {
			for (int pos = 0; pos < text.length && text[pos] != 0; pos++) {
				if (!charBounds(text[pos]) || charInfo[text[pos]].nTile == -1) {
					continue;
				}
				width += charInfo[text[pos]].getWidth(scale);
			}
		}
		return width;
	}

	public int getWidth(String text) {
		return getWidth(toCharArray(text), 0x10000);
	}

	public int getWidth(String text, int scale) {
		return getWidth(toCharArray(text), scale);
	}

	public int getHeight() {
		return getHeight(0x10000);
	}
	public int getHeight(int scale) {
		return scale(nHeight, scale, 0x10000);
	}

	protected boolean charBounds(int ch) {
		return ch >= 0 && ch < 256;
	}

	protected int getTile(int ch) {
		if (!charBounds(ch)) {
			return -1;
		}

		return charInfo[ch].nTile;
	}
}
