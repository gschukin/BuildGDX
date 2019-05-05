// This file is part of BuildGDX.
// Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
//
// BuildGDX is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// BuildGDX is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.Render;

import ru.m210projects.Build.Render.Renderer.Transparent;
import ru.m210projects.Build.Types.TileFont;

public interface Renderer2D {
	
	public void init();
	
	public void uninit();
	
	public void drawmapview(int dax, int day, int zoome, int ang);

	public void printext(TileFont font, int xpos, int ypos, char[] text, int col, int shade, Transparent bit, float scale);
	
	public void printext(int xpos, int ypos, int col, int backcol, char[] text, int fontsize, float scale);
	
	public void drawline256(int x1, int y1, int x2, int y2, int col);
	
	public void rotatesprite(int sx, int sy, int z, int a, int picnum,
			int dashade, int dapalnum, int dastat, int cx1, int cy1, int cx2,
			int cy2);
}
