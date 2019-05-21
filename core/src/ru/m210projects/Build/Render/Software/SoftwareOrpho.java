/*
 * Software renderer code originally written by Ken Silverman
 * Ken Silverman's official web site: "http://www.advsys.net/ken"
 * See the included license file "BUILDLIC.TXT" for license info.
 *
 * This file has been modified from Ken Silverman's original release
 * by Jonathon Fowler (jf@jonof.id.au)
 * by Alexander Makarov-[M210] (m210-2007@mail.ru)
 */

package ru.m210projects.Build.Render.Software;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Pragmas.*;

import java.util.Arrays;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Render.OrphoRenderer;
import ru.m210projects.Build.Render.Renderer.Transparent;
import ru.m210projects.Build.Types.SECTOR;
import ru.m210projects.Build.Types.SPRITE;
import ru.m210projects.Build.Types.TileFont;
import ru.m210projects.Build.Types.WALL;

public class SoftwareOrpho extends OrphoRenderer {

	private Software parent;
	private Engine engine;
	private final int MAXPERMS;

	protected final int MAXNODESPERLINE = 42; // Warning: This depends on MAXYSAVES & MAXYDIM!
	protected short dotp1[] = new short[MAXYDIM], dotp2[] = new short[MAXYDIM];

	protected final int MAXWALLSB = ((MAXWALLS >> 2) + (MAXWALLS >> 3));
	protected int[] xb1 = new int[MAXWALLSB];
	protected int[] xb2 = new int[MAXWALLSB];
	protected int[] rx1 = new int[MAXWALLSB];
	protected int[] ry1 = new int[MAXWALLSB];

	protected short globalpicnum;
	protected int globalorientation;
	protected int globalx1;
	protected int globaly1;
	protected int globalx2;
	protected int globaly2;
	protected int asm1;
	protected int asm2;

	public int[] nrx1 = new int[8], nry1 = new int[8], nrx2 = new int[8], nry2 = new int[8]; // JBF 20031206: Thanks Ken

	public SoftwareOrpho(Software parent) {
		this.parent = parent;
		this.engine = parent.engine;
		this.MAXPERMS = parent.MAXPERMS;
	}

	@Override
	public void printext(TileFont font, int xpos, int ypos, char[] text, int col, int shade, Transparent bit,
			float scale) {
		// TODO Auto-generated method stub

	}

	@Override
	public void printext(int xpos, int ypos, int col, int backcol, char[] text, int fontsize, float scale) {
		int stx = xpos;
		int charxsiz = 8;
		byte[] fontptr = textfont;
		if (fontsize != 0) {
			fontptr = smalltextfont;
			charxsiz = 4;
		}

		for (int i = 0; i < text.length && text[i] != 0; i++) {
			int ptr = parent.bytesperline * (ypos + 7) + (stx - fontsize);
			if (ptr < 0)
				continue;

			for (int y = 7; y >= 0; y--) {
				for (int x = charxsiz - 1; x >= 0; x--) {
					if ((fontptr[y + (text[i] << 3)] & pow2char[7 - fontsize - x]) != 0) {
						parent.frameplace[ptr + x] = (byte) col;
					} else if (backcol >= 0) {
						parent.frameplace[ptr + x] = (byte) backcol;
					}
				}
				ptr -= parent.bytesperline;
			}
			stx += charxsiz;
		}
	}

	@Override
	public void drawline256(int x1, int y1, int x2, int y2, int c) {
		int dx, dy, i, j, inc, plc, daend;

		byte col = palookup[0][c];

		dx = x2 - x1;
		dy = y2 - y1;
		if (dx >= 0) {
			if ((x1 >= wx2) || (x2 < wx1))
				return;
			if (x1 < wx1) {
				y1 += scale(wx1 - x1, dy, dx);
				x1 = wx1;
			}
			if (x2 > wx2) {
				y2 += scale(wx2 - x2, dy, dx);
				x2 = wx2;
			}
		} else {
			if ((x2 >= wx2) || (x1 < wx1))
				return;
			if (x2 < wx1) {
				y2 += scale(wx1 - x2, dy, dx);
				x2 = wx1;
			}
			if (x1 > wx2) {
				y1 += scale(wx2 - x1, dy, dx);
				x1 = wx2;
			}
		}
		if (dy >= 0) {
			if ((y1 >= wy2) || (y2 < wy1))
				return;
			if (y1 < wy1) {
				x1 += scale(wy1 - y1, dx, dy);
				y1 = wy1;
			}
			if (y2 > wy2) {
				x2 += scale(wy2 - y2, dx, dy);
				y2 = wy2;
			}
		} else {
			if ((y2 >= wy2) || (y1 < wy1))
				return;
			if (y2 < wy1) {
				x2 += scale(wy1 - y2, dx, dy);
				y2 = wy1;
			}
			if (y1 > wy2) {
				x1 += scale(wy2 - y1, dx, dy);
				y1 = wy2;
			}
		}

		if (klabs(dx) >= klabs(dy)) {
			if (dx == 0)
				return;
			if (dx < 0) {
				i = x1;
				x1 = x2;
				x2 = i;
				i = y1;
				y1 = y2;
				y2 = i;
			}

			inc = (int) divscale(dy, dx, 12);
			plc = y1 + mulscale((2047 - x1) & 4095, inc, 12);
			i = ((x1 + 2048) >> 12);
			daend = ((x2 + 2048) >> 12);

			for (; i < daend; i++) {
				j = (plc >> 12);
				if ((j >= parent.startumost[i]) && (j < parent.startdmost[i]))
					parent.a.drawpixel(parent.frameplace, parent.ylookup[j] + i, col);
				plc += inc;
			}
		} else {
			if (dy < 0) {
				i = x1;
				x1 = x2;
				x2 = i;
				i = y1;
				y1 = y2;
				y2 = i;
			}

			inc = (int) divscale(dx, dy, 12);
			plc = x1 + mulscale((2047 - y1) & 4095, inc, 12);
			i = ((y1 + 2048) >> 12);
			daend = ((y2 + 2048) >> 12);

			int p = parent.ylookup[i];

			for (; i < daend; i++) {
				j = (plc >> 12);
				if ((i >= parent.startumost[j]) && (i < parent.startdmost[j]))
					parent.a.drawpixel(parent.frameplace, p + j, col);
				plc += inc;
				p += parent.ylookup[1];
			}
		}
	}

	@Override
	public void drawmapview(int dax, int day, int zoome, int ang) {
		WALL wal;
		SECTOR sec = null;

		int i, j, x, y, bakx1, baky1;
		int s, w, ox, oy, startwall, cx1, cy1, cx2, cy2;
		int bakgxvect, bakgyvect, npoints;
		int xvect, yvect, xvect2, yvect2, daslope;

		int tilenum, xoff, yoff, k, l, cosang, sinang, xspan, yspan;
		int xrepeat, yrepeat, x1, y1, x2, y2, x3, y3, x4, y4;

		beforedrawrooms = 0;

		Arrays.fill(gotsector, (byte) 0);

		cx1 = (windowx1 << 12);
		cy1 = (windowy1 << 12);
		cx2 = ((windowx2 + 1) << 12) - 1;
		cy2 = ((windowy2 + 1) << 12) - 1;
		zoome <<= 8;
		bakgxvect = (int) divscale(sintable[(1536 - ang) & 2047], zoome, 28);
		bakgyvect = (int) divscale(sintable[(2048 - ang) & 2047], zoome, 28);
		xvect = mulscale(sintable[(2048 - ang) & 2047], zoome, 8);
		yvect = mulscale(sintable[(1536 - ang) & 2047], zoome, 8);
		xvect2 = mulscale(xvect, yxaspect, 16);
		yvect2 = mulscale(yvect, yxaspect, 16);

		int sortnum = 0;

		for (s = 0; s < numsectors; s++) {
			sec = sector[s];

			if (fullmap || (show2dsector[s >> 3] & pow2char[s & 7]) != 0) {
				npoints = 0;
				i = 0;
				startwall = sec.wallptr;

				j = startwall;
				if (startwall < 0)
					continue;
				for (w = sec.wallnum; w > 0; w--, j++) {
					wal = wall[j];
					if (wal == null)
						continue;
					ox = wal.x - dax;
					oy = wal.y - day;
					x = dmulscale(ox, xvect, -oy, yvect, 16) + (xdim << 11);
					y = dmulscale(oy, xvect2, ox, yvect2, 16) + (ydim << 11);
					i |= getclipmask(x - cx1, cx2 - x, y - cy1, cy2 - y);
					rx1[npoints] = x;
					ry1[npoints] = y;
					xb1[npoints] = wal.point2 - startwall;
					if (xb1[npoints] < 0)
						xb1[npoints] = 0;

					npoints++;
				}

				if ((i & 0xf0) != 0xf0)
					continue;

				bakx1 = (int) rx1[0];
				baky1 = mulscale((int) ry1[0] - (ydim << 11), xyaspect, 16) + (ydim << 11);

				if (showflspr) {
					// Collect floor sprites to draw
					for (i = headspritesect[s]; i >= 0; i = nextspritesect[i])
						if ((sprite[i].cstat & 48) == 32) {
							if (sortnum >= MAXSPRITESONSCREEN)
								continue;
							if ((sprite[i].cstat & (64 + 8)) == (64 + 8))
								continue;

							if (tsprite[sortnum] == null)
								tsprite[sortnum] = new SPRITE();
							tsprite[sortnum].set(sprite[i]);
							tsprite[sortnum++].owner = (short) i;
						}
				}

				gotsector[s >> 3] |= pow2char[s & 7];

				globalorientation = sec.floorstat;
				if ((globalorientation & 1) != 0)
					continue;
				globalpal = sec.floorpal;

				globalpicnum = sec.floorpicnum;
				if (globalpicnum >= MAXTILES)
					globalpicnum = 0;
				engine.setgotpic(globalpicnum);
				if ((tilesizx[globalpicnum] <= 0) || (tilesizy[globalpicnum] <= 0))
					continue;

				if ((picanm[globalpicnum] & 192) != 0)
					globalpicnum += engine.animateoffs(globalpicnum, s); // FIXME
				if (waloff[globalpicnum] == null)
					engine.loadtile(globalpicnum);

				globalshade = max(min(sec.floorshade, numshades - 1), 0);

				if ((globalorientation & 64) == 0) {
					globalposx = dax;
					globalx1 = bakgxvect;
					globaly1 = bakgyvect;
					globalposy = day;
					globalx2 = bakgxvect;
					globaly2 = bakgyvect;
				} else {
					ox = wall[wall[startwall].point2].x - wall[startwall].x;
					oy = wall[wall[startwall].point2].y - wall[startwall].y;
					i = engine.ksqrt(ox * ox + oy * oy);
					if (i == 0)
						continue;
					i = 1048576 / i;
					globalx1 = mulscale(dmulscale(ox, bakgxvect, oy, bakgyvect, 10), i, 10);
					globaly1 = mulscale(dmulscale(ox, bakgyvect, -oy, bakgxvect, 10), i, 10);
					ox = (bakx1 >> 4) - (xdim << 7);
					oy = (baky1 >> 4) - (ydim << 7);
					globalposx = dmulscale(-oy, (int) globalx1, -ox, (int) globaly1, 28);
					globalposy = dmulscale(-ox, (int) globalx1, oy, (int) globaly1, 28);
					globalx2 = -globalx1;
					globaly2 = -globaly1;

					daslope = sector[s].floorheinum;
					i = engine.ksqrt(daslope * daslope + 16777216);
					globalposy = mulscale(globalposy, i, 12);
					globalx2 = mulscale((int) globalx2, i, 12);
					globaly2 = mulscale((int) globaly2, i, 12);
				}
				int globalxshift = (8 - (picsiz[globalpicnum] & 15));
				int globalyshift = (8 - (picsiz[globalpicnum] >> 4));
				if ((globalorientation & 8) != 0) {
					globalxshift++;
					globalyshift++;
				}

				if ((globalorientation & 0x4) > 0) {
					i = globalposx;
					globalposx = -globalposy;
					globalposy = -i;
					i = (int) globalx2;
					globalx2 = globaly1;
					globaly1 = i;
					i = (int) globalx1;
					globalx1 = -globaly2;
					globaly2 = -i;
				}
				if ((globalorientation & 0x10) > 0) {
					globalx1 = -globalx1;
					globaly1 = -globaly1;
					globalposx = -globalposx;
				}
				if ((globalorientation & 0x20) > 0) {
					globalx2 = -globalx2;
					globaly2 = -globaly2;
					globalposy = -globalposy;
				}
				asm1 = (int) (globaly1 << globalxshift);
				asm2 = (int) (globalx2 << globalyshift);
				globalx1 <<= globalxshift;
				globaly2 <<= globalyshift;
				globalposx = (globalposx << (20 + globalxshift)) + ((sec.floorxpanning) << 24);
				globalposy = (globalposy << (20 + globalyshift)) - ((sec.floorypanning) << 24);

				fillpolygon(npoints);
			}
		}

		if (showspr) {
			// Sort sprite list
			int gap = 1;
			while (gap < sortnum)
				gap = (gap << 1) + 1;
			for (gap >>= 1; gap > 0; gap >>= 1)
				for (i = 0; i < sortnum - gap; i++)
					for (j = i; j >= 0; j -= gap) {
						if (sprite[tsprite[j].owner].z <= sprite[tsprite[j + gap].owner].z)
							break;

						short tmp = tsprite[j].owner;
						tsprite[j].owner = tsprite[j + gap].owner;
						tsprite[j + gap].owner = tmp;
					}

			for (s = sortnum - 1; s >= 0; s--) {
				SPRITE spr = sprite[tsprite[s].owner];
				if ((spr.cstat & 48) == 32) {
					npoints = 0;

					tilenum = spr.picnum;
					xoff = (byte) ((picanm[tilenum] >> 8) & 255) + spr.xoffset;
					yoff = (byte) ((picanm[tilenum] >> 16) & 255) + spr.yoffset;
					if ((spr.cstat & 4) > 0)
						xoff = -xoff;
					if ((spr.cstat & 8) > 0)
						yoff = -yoff;

					k = spr.ang & 2047;
					cosang = sintable[(k + 512) & 2047];
					sinang = sintable[k];
					xspan = tilesizx[tilenum];
					xrepeat = spr.xrepeat;
					yspan = tilesizy[tilenum];
					yrepeat = spr.yrepeat;
					ox = ((xspan >> 1) + xoff) * xrepeat;
					oy = ((yspan >> 1) + yoff) * yrepeat;
					x1 = spr.x + mulscale(sinang, ox, 16) + mulscale(cosang, oy, 16);
					y1 = spr.y + mulscale(sinang, oy, 16) - mulscale(cosang, ox, 16);
					l = xspan * xrepeat;
					x2 = x1 - mulscale(sinang, l, 16);
					y2 = y1 + mulscale(cosang, l, 16);
					l = yspan * yrepeat;
					k = -mulscale(cosang, l, 16);
					x3 = x2 + k;
					x4 = x1 + k;
					k = -mulscale(sinang, l, 16);
					y3 = y2 + k;
					y4 = y1 + k;

					xb1[0] = 1;
					xb1[1] = 2;
					xb1[2] = 3;
					xb1[3] = 0;
					npoints = 4;

					i = 0;

					ox = x1 - dax;
					oy = y1 - day;
					x = dmulscale(ox, xvect, -oy, yvect, 16) + (xdim << 11);
					y = dmulscale(oy, xvect2, ox, yvect2, 16) + (ydim << 11);
					i |= getclipmask(x - cx1, cx2 - x, y - cy1, cy2 - y);
					rx1[0] = x;
					ry1[0] = y;

					ox = x2 - dax;
					oy = y2 - day;
					x = dmulscale(ox, xvect, -oy, yvect, 16) + (xdim << 11);
					y = dmulscale(oy, xvect2, ox, yvect2, 16) + (ydim << 11);
					i |= getclipmask(x - cx1, cx2 - x, y - cy1, cy2 - y);
					rx1[1] = x;
					ry1[1] = y;

					ox = x3 - dax;
					oy = y3 - day;
					x = dmulscale(ox, xvect, -oy, yvect, 16) + (xdim << 11);
					y = dmulscale(oy, xvect2, ox, yvect2, 16) + (ydim << 11);
					i |= getclipmask(x - cx1, cx2 - x, y - cy1, cy2 - y);
					rx1[2] = x;
					ry1[2] = y;

					x = (int) (rx1[0] + rx1[2] - rx1[1]);
					y = (int) (ry1[0] + ry1[2] - ry1[1]);
					i |= getclipmask(x - cx1, cx2 - x, y - cy1, cy2 - y);
					rx1[3] = x;
					ry1[3] = y;

					if ((i & 0xf0) != 0xf0)
						continue;
					bakx1 = (int) rx1[0];
					baky1 = mulscale((int) ry1[0] - (ydim << 11), xyaspect, 16) + (ydim << 11);

					globalpicnum = spr.picnum;
					globalpal = spr.pal; // GL needs this, software doesn't
					if (globalpicnum >= MAXTILES)
						globalpicnum = 0;
					engine.setgotpic(globalpicnum);
					if ((tilesizx[globalpicnum] <= 0) || (tilesizy[globalpicnum] <= 0))
						continue;
					if ((picanm[globalpicnum] & 192) != 0)
						globalpicnum += engine.animateoffs(globalpicnum, s);
					if (waloff[globalpicnum] == null)
						engine.loadtile(globalpicnum);

					// 'loading' the tile doesn't actually guarantee that it's there afterwards.
					// This can really happen when drawing the second frame of a floor-aligned
					// 'storm icon' sprite (4894+1)

					if ((sector[spr.sectnum].ceilingstat & 1) > 0)
						globalshade = ((int) sector[spr.sectnum].ceilingshade);
					else
						globalshade = ((int) sector[spr.sectnum].floorshade);
					globalshade = max(min(globalshade + spr.shade + 6, numshades - 1), 0);

					// relative alignment stuff
					ox = x2 - x1;
					oy = y2 - y1;
					i = ox * ox + oy * oy;
					if (i == 0)
						continue;
					i = (65536 * 16384) / i;
					globalx1 = mulscale(dmulscale(ox, bakgxvect, oy, bakgyvect, 10), i, 10);
					globaly1 = mulscale(dmulscale(ox, bakgyvect, -oy, bakgxvect, 10), i, 10);
					ox = y1 - y4;
					oy = x4 - x1;
					i = ox * ox + oy * oy;
					if (i == 0)
						continue;
					i = (65536 * 16384) / i;
					globalx2 = mulscale(dmulscale(ox, bakgxvect, oy, bakgyvect, 10), i, 10);
					globaly2 = mulscale(dmulscale(ox, bakgyvect, -oy, bakgxvect, 10), i, 10);

					ox = picsiz[globalpicnum];
					oy = ((ox >> 4) & 15);
					ox &= 15;
					if (pow2long[ox] != xspan) {
						ox++;
						globalx1 = mulscale(globalx1, xspan, ox);
						globaly1 = mulscale(globaly1, xspan, ox);
					}

					bakx1 = (bakx1 >> 4) - (xdim << 7);
					baky1 = (baky1 >> 4) - (ydim << 7);
					globalposx = dmulscale(-baky1, globalx1, -bakx1, globaly1, 28);
					globalposy = dmulscale(bakx1, globalx2, -baky1, globaly2, 28);

					if ((spr.cstat & 0x4) > 0) {
						globalx1 = -globalx1;
						globaly1 = -globaly1;
						globalposx = -globalposx;
					}
					asm1 = (int) (globaly1 << 2);
					globalx1 <<= 2;
					globalposx <<= (20 + 2);
					asm2 = (int) (globalx2 << 2);
					globaly2 <<= 2;
					globalposy <<= (20 + 2);

					// so polymost can get the translucency. ignored in software mode:
					globalorientation = ((spr.cstat & 2) << 7) | ((spr.cstat & 512) >> 2);

					fillpolygon(npoints);
				}
			}
		}
	}

	private void fillpolygon(int npoints) {
		int z, zz, x1, y1, x2, y2, miny, maxy, y, xinc, cnt;
		int ox, oy, bx, by, p, day1, day2;

		miny = 0x7fffffff;
		maxy = 0x80000000;
		for (z = npoints - 1; z >= 0; z--) {
			y = ry1[z];
			miny = min(miny, y);
			maxy = max(maxy, y);
		}
		miny = (miny >> 12);
		maxy = (maxy >> 12);
		if (miny < 0)
			miny = 0;
		if (maxy >= ydim)
			maxy = ydim - 1;

		int ptr = 0; // They're pointers! - watch how you optimize this thing
		for (y = miny; y <= maxy; y++) {
			dotp1[y] = (short) ptr;
			dotp2[y] = (short) (ptr + (MAXNODESPERLINE >> 1));
			ptr += MAXNODESPERLINE;
		}

		for (z = npoints - 1; z >= 0; z--) {
			zz = xb1[z];
			y1 = ry1[z];
			day1 = (y1 >> 12);
			y2 = ry1[zz];
			day2 = (y2 >> 12);
			if (day1 != day2) {
				x1 = rx1[z];
				x2 = rx1[zz];
				xinc = (int) divscale(x2 - x1, y2 - y1, 12);
				if (day2 > day1) {
					x1 += mulscale((day1 << 12) + 4095 - y1, xinc, 12);
					for (y = day1; y < day2; y++) {
						parent.smost[dotp2[y]++] = (short) (x1 >> 12);
						x1 += xinc;
					}
				} else {
					x2 += mulscale((day2 << 12) + 4095 - y2, xinc, 12);
					for (y = day2; y < day1; y++) {
						System.err.println(y);
						parent.smost[dotp1[y]++] = (short) (x2 >> 12);
						x2 += xinc;
					}
				}
			}
		}

		globalx1 = mulscale(globalx1, xyaspect, 16);
		globaly2 = mulscale(globaly2, xyaspect, 16);

		oy = miny + 1 - (ydim >> 1);
		globalposx += oy * globalx1;
		globalposy += oy * globaly2;

		parent.a.setuphlineasm4(asm1, asm2);

		ptr = 0;
		int ptr2;
		for (y = miny; y <= maxy; y++) {
			cnt = dotp1[y] - ptr;
			ptr2 = ptr + (MAXNODESPERLINE >> 1);
			for (z = cnt - 1; z >= 0; z--) {
				day1 = 0;
				day2 = 0;
				for (zz = z; zz > 0; zz--) {
					if (parent.smost[ptr + zz] < parent.smost[ptr + day1])
						day1 = zz;
					if (parent.smost[ptr2 + zz] < parent.smost[ptr2 + day2])
						day2 = zz;
				}
				x1 = parent.smost[ptr + day1];
				parent.smost[ptr + day1] = parent.smost[ptr + z];
				x2 = parent.smost[ptr2 + day2] - 1;
				parent.smost[ptr2 + day2] = parent.smost[ptr2 + z];
				if (x1 > x2)
					continue;

//				if (globalpolytype < 1)
//				{
//						//maphline
//					ox = x2+1-(xdim>>1);
//					bx = ox*asm1 + globalposx;
//					by = ox*asm2 - globalposy;
//
//					p = parent.ylookup[y]+x2;
//					parent.a.hlineasm4(x2-x1,-1,globalshade<<8,by,bx,p);
//				}
//				else
				{
					// maphline
					ox = x1 + 1 - (xdim >> 1);
					bx = ox * asm1 + globalposx;
					by = ox * asm2 - globalposy;

					p = parent.ylookup[y] + x1;
//					if (globalpolytype == 1)
//						parent.a.mhline(parent.globalbufplc,bx,(x2-x1)<<16,0,by,p);
//					else
					{
						parent.a.thline(parent.globalbufplc, bx, (x2 - x1) << 16, 0, by, p);
					}
				}
			}
			globalposx += globalx1;
			globalposy += globaly2;
			ptr += MAXNODESPERLINE;
		}
		engine.faketimerhandler();
	}

	@Override
	public void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat, int cx1,
			int cy1, int cx2, int cy2) {
		int i;
		PermFifo per, per2;

		if (picnum >= MAXTILES)
			return;

		if ((cx1 > cx2) || (cy1 > cy2))
			return;
		if (z <= 16)
			return;
		if ((picanm[picnum] & 192) != 0)
			picnum += engine.animateoffs((short) picnum, 0xc000);
		if ((tilesizx[picnum] <= 0) || (tilesizy[picnum] <= 0))
			return;

		// Experimental / development bits. ONLY FOR INTERNAL USE!
		// bit RS_CENTERORIGIN: see dorotspr_handle_bit2
		////////////////////

		if (((dastat & 128) == 0) || (parent.numpages < 2) || (beforedrawrooms != 0)) {
			dorotatesprite(sx, sy, z, a, picnum, dashade, dapalnum, dastat, cx1, cy1, cx2, cy2, parent.guniqhudid);
		}

		if (((dastat & 64) != 0) && (cx1 <= 0) && (cy1 <= 0) && (cx2 >= xdim - 1) && (cy2 >= ydim - 1)
				&& (sx == (160 << 16)) && (sy == (100 << 16)) && (z == 65536L) && (a == 0) && ((dastat & 1) == 0))
			parent.permhead = parent.permtail = 0;

		if ((dastat & 128) == 0)
			return;
		if (parent.numpages >= 2) {
			per = parent.permfifo[parent.permhead];
			if (per == null)
				per = new PermFifo();
			per.sx = sx;
			per.sy = sy;
			per.z = z;
			per.a = (short) a;
			per.picnum = (short) picnum;
			per.dashade = (short) dashade;
			per.dapalnum = (short) dapalnum;
			per.dastat = (short) dastat;
			per.pagesleft = (short) (parent.numpages + ((beforedrawrooms & 1) << 7));
			per.cx1 = cx1;
			per.cy1 = cy1;
			per.cx2 = cx2;
			per.cy2 = cy2;
			per.uniqid = parent.guniqhudid; // JF extension

			// Would be better to optimize out true bounding boxes
			if ((dastat & 64) != 0) // If non-masking write, checking for overlapping cases
			{
				for (i = parent.permtail; i != parent.permhead; i = ((i + 1) & (MAXPERMS - 1))) {
					per2 = parent.permfifo[i];
					if (per2 == null)
						per2 = new PermFifo();
					if ((per2.pagesleft & 127) == 0)
						continue;
					if (per2.sx != per.sx)
						continue;
					if (per2.sy != per.sy)
						continue;
					if (per2.z != per.z)
						continue;
					if (per2.a != per.a)
						continue;
					if (tilesizx[per2.picnum] > tilesizx[per.picnum])
						continue;
					if (tilesizy[per2.picnum] > tilesizy[per.picnum])
						continue;
					if (per2.cx1 < per.cx1)
						continue;
					if (per2.cy1 < per.cy1)
						continue;
					if (per2.cx2 > per.cx2)
						continue;
					if (per2.cy2 > per.cy2)
						continue;
					per2.pagesleft = 0;
				}
				if ((per.z == 65536) && (per.a == 0))
					for (i = parent.permtail; i != parent.permhead; i = ((i + 1) & (MAXPERMS - 1))) {
						per2 = parent.permfifo[i];
						if (per2 == null)
							per2 = new PermFifo();
						if ((per2.pagesleft & 127) == 0)
							continue;
						if (per2.z != 65536)
							continue;
						if (per2.a != 0)
							continue;
						if (per2.cx1 < per.cx1)
							continue;
						if (per2.cy1 < per.cy1)
							continue;
						if (per2.cx2 > per.cx2)
							continue;
						if (per2.cy2 > per.cy2)
							continue;
						if ((per2.sx >> 16) < (per.sx >> 16))
							continue;
						if ((per2.sy >> 16) < (per.sy >> 16))
							continue;
						if ((per2.sx >> 16) + tilesizx[per2.picnum] > (per.sx >> 16) + tilesizx[per.picnum])
							continue;
						if ((per2.sy >> 16) + tilesizy[per2.picnum] > (per.sy >> 16) + tilesizy[per.picnum])
							continue;
						per2.pagesleft = 0;
					}
			}

			parent.permhead = ((parent.permhead + 1) & (MAXPERMS - 1));
		}
	}

	private void dorotatesprite(int sx, int sy, int z, int ang, int picnum, int dashade, int dapalnum, int dastat,
			int cx1, int cy1, int cx2, int cy2, int uniqid) {
		int xoff = 0, yoff = 0;
		int x, y;

		if (palookup[dapalnum] == null)
			dapalnum = 0;

		if (cx1 < 0)
			cx1 = 0;
		if (cy1 < 0)
			cy1 = 0;
		if (cx2 > xdim - 1)
			cx2 = xdim - 1;
		if (cy2 > ydim - 1)
			cy2 = ydim - 1;

		int xsiz = tilesizx[picnum];
		int ysiz = tilesizy[picnum];
		if ((dastat & 16) != 0) {
			xoff = 0;
			yoff = 0;
		} else {
			xoff = (int) ((byte) ((picanm[picnum] >> 8) & 255)) + (xsiz >> 1);
			yoff = (int) ((byte) ((picanm[picnum] >> 16) & 255)) + (ysiz >> 1);
		}

		if ((dastat & 4) != 0)
			yoff = ysiz - yoff;

		int cosang = sintable[(ang + 512) & 2047];
		int sinang = sintable[ang & 2047];

		if ((dastat & 2) != 0) // Auto window size scaling
		{
			if ((dastat & 8) == 0) {
				x = xdimenscale; // = scale(xdimen,yxaspect,320);
				sx = ((cx1 + cx2 + 2) << 15) + scale(sx - (320 << 15), xdimen, 320);
				sy = ((cy1 + cy2 + 2) << 15) + mulscale(sy - (200 << 15), x, 16);
			} else {
				// If not clipping to startmosts, & auto-scaling on, as a
				// hard-coded bonus, scale to full screen instead
				x = scale(xdim, yxaspect, 320);
				sx = (xdim << 15) + 32768 + scale(sx - (320 << 15), xdim, 320);
				sy = (ydim << 15) + 32768 + mulscale(sy - (200 << 15), x, 16);
			}
			z = mulscale(z, x, 16);
		}

		int xv = mulscale(cosang, z, 14), xv2;
		int yv = mulscale(sinang, z, 14), yv2;

		if (((dastat & 2) != 0) || ((dastat & 8) == 0)) // Don't aspect unscaled perms
		{
			xv2 = mulscale(xv, xyaspect, 16);
			yv2 = mulscale(yv, xyaspect, 16);
		} else {
			xv2 = xv;
			yv2 = yv;
		}

		nry1[0] = sy - (yv * xoff + xv * yoff);
		nry1[1] = nry1[0] + yv * xsiz;
		nry1[3] = nry1[0] + xv * ysiz;
		nry1[2] = nry1[1] + nry1[3] - nry1[0];
		int i = (cy1 << 16);
		if ((nry1[0] < i) && (nry1[1] < i) && (nry1[2] < i) && (nry1[3] < i))
			return;
		i = (cy2 << 16);
		if ((nry1[0] > i) && (nry1[1] > i) && (nry1[2] > i) && (nry1[3] > i))
			return;

		nrx1[0] = sx - (xv2 * xoff - yv2 * yoff);
		nrx1[1] = nrx1[0] + xv2 * xsiz;
		nrx1[3] = nrx1[0] - yv2 * ysiz;
		nrx1[2] = nrx1[1] + nrx1[3] - nrx1[0];
		i = (cx1 << 16);
		if ((nrx1[0] < i) && (nrx1[1] < i) && (nrx1[2] < i) && (nrx1[3] < i))
			return;
		i = (cx2 << 16);
		if ((nrx1[0] > i) && (nrx1[1] > i) && (nrx1[2] > i) && (nrx1[3] > i))
			return;

		int gx1 = nrx1[0];
		int gy1 = nry1[0]; // back up these before clipping

		int npoints;
		if ((npoints = clippoly4(cx1 << 16, cy1 << 16, (cx2 + 1) << 16, (cy2 + 1) << 16)) < 3)
			return;

		int lx = nrx1[0];
		int rx = nrx1[0];

		int nextv = 0;
		for (int v = npoints - 1; v >= 0; v--) {
			int x1 = nrx1[v];
			int x2 = nrx1[nextv];
			int dax1 = (x1 >> 16);
			if (x1 < lx)
				lx = x1;
			int dax2 = (x2 >> 16);
			if (x1 > rx)
				rx = x1;
			if (dax1 != dax2) {
				int y1 = nry1[v];
				int y2 = nry1[nextv];
				long yinc = divscale(y2 - y1, x2 - x1, 16);

				if (dax2 > dax1) {
					int yplc = y1 + mulscale((dax1 << 16) + 65535 - x1, yinc, 16);
					parent.qinterpolatedown16short(parent.uplc, dax1, dax2 - dax1, yplc, yinc);
				} else {
					int yplc = y2 + mulscale((dax2 << 16) + 65535 - x2, yinc, 16);
					parent.qinterpolatedown16short(parent.dplc, dax2, dax1 - dax2, yplc, yinc);
				}
			}
			nextv = v;
		}

		if (waloff[picnum] == null)
			engine.loadtile(picnum);
		engine.setgotpic(picnum);
		byte[] bufplc = waloff[picnum];

		int palookupshade = engine.getpalookup(0, dashade) << 8;

		i = (int) divscale(1, z, 32);
		xv = mulscale(sinang, i, 14);
		yv = mulscale(cosang, i, 14);
		if (((dastat & 2) != 0) || ((dastat & 8) == 0)) // Don't aspect unscaled perms
		{
			yv2 = mulscale(-xv, yxaspect, 16);
			xv2 = mulscale(yv, yxaspect, 16);
		} else {
			yv2 = -xv;
			xv2 = yv;
		}

		int x1 = (lx >> 16);
		int x2 = (rx >> 16);

		int oy = 0;
		x = (x1 << 16) - 1 - gx1;
		y = (oy << 16) + 65535 - gy1;
		int bx = dmulscale(x, xv2, y, xv, 16);
		int by = dmulscale(x, yv2, y, yv, 16);

		if ((dastat & 4) != 0) {
			yv = -yv;
			yv2 = -yv2;
			by = (ysiz << 16) - 1 - by;
		}

		if ((dastat & 1) == 0) {
			if ((dastat & 64) != 0)
				parent.a.setupspritevline(dapalnum, palookupshade, xv, yv, ysiz);
			else
				parent.a.msetupspritevline(dapalnum, palookupshade, xv, yv, ysiz);
		} else {
			parent.a.tsetupspritevline(dapalnum, palookupshade, xv, yv, ysiz);
			if ((dastat & 32) != 0)
				parent.a.settransreverse();
			else
				parent.a.settransnormal();
		}

		for (x = x1; x < x2; x++) {
			bx += xv2;
			by += yv2;
			int y1 = parent.uplc[x];
			int y2 = parent.dplc[x];
			if ((dastat & 8) == 0) {
				if (parent.startumost[x] > y1)
					y1 = parent.startumost[x];
				if (parent.startdmost[x] < y2)
					y2 = parent.startdmost[x];
			}
			if (y2 <= y1)
				continue;

			switch (y1 - oy) {
			case -1:
				bx -= xv;
				by -= yv;
				oy = y1;
				break;
			case 0:
				break;
			case 1:
				bx += xv;
				by += yv;
				oy = y1;
				break;
			default:
				bx += xv * (y1 - oy);
				by += yv * (y1 - oy);
				oy = y1;
				break;
			}

			int p = parent.ylookup[y1] + x;
			if ((dastat & 1) == 0) {
				if ((dastat & 64) != 0) {
					parent.a.spritevline(bx & 65535, by & 65535, y2 - y1 + 1, bufplc, (bx >> 16) * ysiz + (by >> 16),
							p);
				} else {
					parent.a.mspritevline(bx & 65535, by & 65535, y2 - y1 + 1, bufplc, (bx >> 16) * ysiz + (by >> 16),
							p);
				}
			} else {
				parent.a.tspritevline(bx & 65535, by & 65535, y2 - y1 + 1, bufplc, (bx >> 16) * ysiz + (by >> 16), p);
			}
			engine.faketimerhandler();
		}
	}

	private int clippoly4(int cx1, int cy1, int cx2, int cy2) {
		int n, nn, z, zz, x, x1, x2, y, y1, y2, t;

		nn = 0;
		z = 0;
		do {
			zz = ((z + 1) & 3);
			x1 = nrx1[z];
			x2 = nrx1[zz] - x1;

			if ((cx1 <= x1) && (x1 <= cx2)) {
				nrx2[nn] = x1;
				nry2[nn] = nry1[z];
				nn++;
			}

			if (x2 <= 0)
				x = cx2;
			else
				x = cx1;
			t = x - x1;
			if (((t - x2) ^ t) < 0) {
				nrx2[nn] = x;
				nry2[nn] = nry1[z] + scale(t, nry1[zz] - nry1[z], x2);
				nn++;
			}

			if (x2 <= 0)
				x = cx1;
			else
				x = cx2;
			t = x - x1;
			if (((t - x2) ^ t) < 0) {
				nrx2[nn] = x;
				nry2[nn] = nry1[z] + scale(t, nry1[zz] - nry1[z], x2);
				nn++;
			}

			z = zz;
		} while (z != 0);
		if (nn < 3)
			return (0);

		n = 0;
		z = 0;
		do {
			zz = z + 1;
			if (zz == nn)
				zz = 0;
			y1 = nry2[z];
			y2 = nry2[zz] - y1;

			if ((cy1 <= y1) && (y1 <= cy2)) {
				nry1[n] = y1;
				nrx1[n] = nrx2[z];
				n++;
			}

			if (y2 <= 0)
				y = cy2;
			else
				y = cy1;
			t = y - y1;
			if (((t - y2) ^ t) < 0) {
				nry1[n] = y;
				nrx1[n] = nrx2[z] + scale(t, nrx2[zz] - nrx2[z], y2);
				n++;
			}

			if (y2 <= 0)
				y = cy1;
			else
				y = cy2;
			t = y - y1;
			if (((t - y2) ^ t) < 0) {
				nry1[n] = y;
				nrx1[n] = nrx2[z] + scale(t, nrx2[zz] - nrx2[z], y2);
				n++;
			}

			z = zz;
		} while (z != 0);
		return (n);
	}

	@Override
	public void nextpage() {
		/* nothing */ }

	@Override
	public void init() {
		/* nothing */ }

	@Override
	public void uninit() {
		/* nothing */ }

}
