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

import ru.m210projects.Build.Architecture.BuildFrame.FrameType;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;

import java.nio.ByteBuffer;

public interface Renderer {

    PixelFormat getTexFormat();

    void init();

    void uninit();

    boolean isInited();

    void drawmasks();

    void drawrooms();

    void clearview(int dacol);

    void changepalette(byte[] palette);

    void nextpage();

    void setview(int x1, int y1, int x2, int y2);

    void invalidatetile(int tilenume, int pal, int how);

    void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat, int cx1,
                      int cy1, int cx2, int cy2);

    void completemirror();

    void drawoverheadmap(int cposx, int cposy, int czoom, short cang);

    void drawmapview(int dax, int day, int zoome, int ang);

    int printext(Font font, int x, int y, char[] text, float scale, int shade, int palnum, TextAlign align, Transparent transparent);

    ByteBuffer getFrame(PixelFormat format, int xsiz, int ysiz);

    byte[] screencapture(int newwidth, int newheight);

    void drawline256(int x1, int y1, int x2, int y2, int col);

    void settiltang(int tilt);

    void setDefs(DefScript defs);

    RenderType getType();

    enum RenderType {
        Software(FrameType.Canvas, "Classic"), Polymost(FrameType.GL, "Polymost"), PolyGDX(FrameType.GL, "PolyGDX");

        FrameType type;
        String name;

        RenderType(FrameType type, String name) {
            this.type = type;
            this.name = name;
        }

        public FrameType getFrameType() {
            return type;
        }

        public String getName() {
            return name;
        }
    }
}
