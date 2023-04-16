// "Build Engine & Tools" Copyright (c) 1993-1997 Ken Silverman
// Ken Silverman's official web site: "http://www.advsys.net/ken"
// See the included license file "BUILDLIC.TXT" for license info.
//
// This file has been modified from Ken Silverman's original release
// by Jonathon Fowler (jf@jonof.id.au)
// by Alexander Makarov-[M210] (m210-2007@mail.ru)

package ru.m210projects.Build;

import static ru.m210projects.Build.Engine.MAXSTATUS;
import static ru.m210projects.Build.Pragmas.dmulscale;
import static ru.m210projects.Build.Pragmas.scale;

import java.nio.ByteBuffer;
import java.util.Arrays;

import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.FileHandle.Resource;
import ru.m210projects.Build.Types.BuildPos;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.Types.Wall;

public abstract class Board {

	public static class SlopeZ {
		public int floorz;
		public int ceilingz;

		public void setFloor(int value) {
			this.floorz = value;
		}

		public void setCeiling(int value) {
			this.ceilingz = value;
		}

		public int getFloor() {
			return floorz;
		}

		public int getCeiling() {
			return ceilingz;
		}
	}

	protected int SETSPRITEZ = 0;

	protected final int mapversion;
	protected int numsectors, numwalls, numsprites;

	protected final short[] headspritesect, headspritestat;
	protected final short[] prevspritesect, prevspritestat;
	protected final short[] nextspritesect, nextspritestat;

	protected final Sector[] sector;
	protected final Wall[] wall;
	protected final Sprite[] sprite;

	protected BuildPos dapos;
	protected final String name;

	protected final ByteBuffer buffer;

	protected final SlopeZ zofslope;

	protected abstract int sqrt(int i);

	protected abstract boolean load(Resource bb);

	protected abstract int getMaxSprites();

	protected abstract int getMaxSectors();

	protected abstract int getMaxWalls();

	////////// INITIALIZATION FUNCTIONS //////////

	public Board(String filename) throws Exception {
		this(filename, BuildGdx.cache.open(filename, 0));
	}

	public Board(String name, Resource bb) throws Exception {
		sector = new Sector[getMaxSectors()];
		wall = new Wall[getMaxWalls()];
		sprite = new Sprite[getMaxSprites()];

		headspritesect = new short[getMaxSectors() + 1];
		headspritestat = new short[MAXSTATUS + 1];
		prevspritesect = new short[getMaxSprites()];
		prevspritestat = new short[getMaxSprites()];
		nextspritesect = new short[getMaxSprites()];
		nextspritestat = new short[getMaxSprites()];

		zofslope = new SlopeZ();
		dapos = new BuildPos();

		initspritelists();

		this.name = name;
		this.mapversion = bb.readInt();
		if (load(bb)) 
			this.buffer = initmapbuffer();
		else this.buffer = null;
	}
	
	protected void initspritelists() {
		Arrays.fill(headspritesect, (short) -1); // Init doubly-linked sprite sector lists
		headspritesect[getMaxSectors()] = 0;

		for (int i = 0; i < getMaxSprites(); i++) {
			sprite[i] = new Sprite();
			prevspritesect[i] = (short) (i - 1);
			nextspritesect[i] = (short) (i + 1);
			sprite[i].setSectnum(getMaxSectors());
		}
		prevspritesect[0] = -1;
		nextspritesect[getMaxSprites() - 1] = -1;

		Arrays.fill(headspritestat, (short) -1); // Init doubly-linked sprite status lists
		headspritestat[MAXSTATUS] = 0;
		for (int i = 0; i < getMaxSprites(); i++) {
			prevspritestat[i] = (short) (i - 1);
			nextspritestat[i] = (short) (i + 1);
			sprite[i].setStatnum(MAXSTATUS);
		}
		prevspritestat[0] = -1;
		nextspritestat[getMaxSprites() - 1] = -1;
	}

	protected ByteBuffer initmapbuffer() {
		int size = 20 + // Point
				(2 + (numwalls * Wall.getSizeof())) + // walls
				(2 + (numsectors * Sector.sizeof)) + // sectors
				(2 + (getMaxSprites() * Sprite.sizeof)) + // sprites
				 2 * (getMaxSectors() + 1) + 2 * (MAXSTATUS + 1) + 8 * getMaxSprites(); // sprite array

		return ByteBuffer.allocate(size);
	}

	////////// SPRITE LIST MANIPULATION FUNCTIONS //////////

	public int insertspritesect(short sectnum) {
		if ((sectnum >= getMaxSectors()) || (headspritesect[getMaxSectors()] == -1))
			return (-1); // list full

		short blanktouse = headspritesect[getMaxSectors()];

		headspritesect[getMaxSectors()] = nextspritesect[blanktouse];
		if (headspritesect[getMaxSectors()] >= 0)
			prevspritesect[headspritesect[getMaxSectors()]] = -1;

		prevspritesect[blanktouse] = -1;
		nextspritesect[blanktouse] = headspritesect[sectnum];
		if (headspritesect[sectnum] >= 0)
			prevspritesect[headspritesect[sectnum]] = blanktouse;
		headspritesect[sectnum] = blanktouse;

		sprite[blanktouse].setSectnum(sectnum);

		return (blanktouse);
	}

	public int insertspritestat(short newstatnum) {
		if ((newstatnum >= MAXSTATUS) || (headspritestat[MAXSTATUS] == -1))
			return (-1); // list full

		short blanktouse = headspritestat[MAXSTATUS];

		headspritestat[MAXSTATUS] = nextspritestat[blanktouse];
		if (headspritestat[MAXSTATUS] >= 0)
			prevspritestat[headspritestat[MAXSTATUS]] = -1;

		prevspritestat[blanktouse] = -1;
		nextspritestat[blanktouse] = headspritestat[newstatnum];
		if (headspritestat[newstatnum] >= 0)
			prevspritestat[headspritestat[newstatnum]] = blanktouse;
		headspritestat[newstatnum] = blanktouse;

		sprite[blanktouse].setStatnum(newstatnum);

		return (blanktouse);
	}

	public int insertsprite(short sectnum, short statnum) {
		insertspritestat(statnum);
		return (insertspritesect(sectnum));
	}

	public boolean deletesprite(short spritenum) {
		deletespritestat(spritenum);
		return (deletespritesect(spritenum));
	}

	public boolean changespritesect(short spritenum, short newsectnum) {
		if ((newsectnum < 0) || (newsectnum > getMaxSectors()))
			return false;
		if (sprite[spritenum].getSectnum() == newsectnum)
			return true;
		if (sprite[spritenum].getSectnum() == getMaxSectors())
			return false;
		if (!deletespritesect(spritenum))
			return false;
		insertspritesect(newsectnum);
		return true;
	}

	public boolean changespritestat(short spritenum, short newstatnum) {
		if ((newstatnum < 0) || (newstatnum > MAXSTATUS))
			return false;
		if (sprite[spritenum].getStatnum() == newstatnum)
			return true;
		if (sprite[spritenum].getStatnum() == MAXSTATUS)
			return false;
		if (!deletespritestat(spritenum))
			return false;
		insertspritestat(newstatnum);
		return true;
	}

	public boolean deletespritesect(short spritenum) {
		if (sprite[spritenum].getSectnum() == getMaxSectors())
			return false;

		if (headspritesect[sprite[spritenum].getSectnum()] == spritenum)
			headspritesect[sprite[spritenum].getSectnum()] = nextspritesect[spritenum];

		if (prevspritesect[spritenum] >= 0)
			nextspritesect[prevspritesect[spritenum]] = nextspritesect[spritenum];
		if (nextspritesect[spritenum] >= 0)
			prevspritesect[nextspritesect[spritenum]] = prevspritesect[spritenum];

		if (headspritesect[getMaxSectors()] >= 0)
			prevspritesect[headspritesect[getMaxSectors()]] = spritenum;
		prevspritesect[spritenum] = -1;
		nextspritesect[spritenum] = headspritesect[getMaxSectors()];
		headspritesect[getMaxSectors()] = spritenum;

		sprite[spritenum].setSectnum(getMaxSectors());
		return true;
	}

	public boolean deletespritestat(short spritenum) {
		if (sprite[spritenum].getStatnum() == MAXSTATUS)
			return false;

		if (headspritestat[sprite[spritenum].getStatnum()] == spritenum)
			headspritestat[sprite[spritenum].getStatnum()] = nextspritestat[spritenum];

		if (prevspritestat[spritenum] >= 0)
			nextspritestat[prevspritestat[spritenum]] = nextspritestat[spritenum];
		if (nextspritestat[spritenum] >= 0)
			prevspritestat[nextspritestat[spritenum]] = prevspritestat[spritenum];

		if (headspritestat[MAXSTATUS] >= 0)
			prevspritestat[headspritestat[MAXSTATUS]] = spritenum;
		prevspritestat[spritenum] = -1;
		nextspritestat[spritenum] = headspritestat[MAXSTATUS];
		headspritestat[MAXSTATUS] = spritenum;

		sprite[spritenum].setStatnum(MAXSTATUS);
		return true;
	}

	public boolean setsprite(short spritenum, int newx, int newy, int newz) {
		sprite[spritenum].setX(newx);
		sprite[spritenum].setY(newy);
		sprite[spritenum].setZ(newz);

		short tempsectnum = sprite[spritenum].getSectnum();
		if (SETSPRITEZ == 1)
			tempsectnum = updatesectorz(newx, newy, newz, tempsectnum);
		else
			tempsectnum = updatesector(newx, newy, tempsectnum);
		if (tempsectnum < 0)
			return false;
		if (tempsectnum != sprite[spritenum].getSectnum())
			changespritesect(spritenum, tempsectnum);

		return true;
	}

	////////// WALL MANIPULATION FUNCTIONS //////////

	public void dragpoint(short pointhighlight, int dax, int day) {
		wall[pointhighlight].setX(dax);
		wall[pointhighlight].setY(day);

		int cnt = getMaxWalls();
		short tempshort = pointhighlight; // search points CCW
		do {
			if (wall[tempshort].getNextwall() >= 0) {
				tempshort = wall[wall[tempshort].getNextwall()].getPoint2();
				wall[tempshort].setX(dax);
				wall[tempshort].setY(day);
			} else {
				tempshort = pointhighlight; // search points CW if not searched all the way around
				do {
					if (wall[lastwall(tempshort)].getNextwall() >= 0) {
						tempshort = wall[lastwall(tempshort)].getNextwall();
						wall[tempshort].setX(dax);
						wall[tempshort].setY(day);
					} else {
						break;
					}
					cnt--;
				} while ((tempshort != pointhighlight) && (cnt > 0));
				break;
			}
			cnt--;
		} while ((tempshort != pointhighlight) && (cnt > 0));
	}
	
	////////// SECTOR MANIPULATION FUNCTIONS ////////

	public void alignceilslope(short dasect, int x, int y, int z) {
		Wall wal = wall[sector[dasect].getWallptr()];
		int dax = wall[wal.getPoint2()].getX() - wal.getX();
		int day = wall[wal.getPoint2()].getY() - wal.getY();

		int i = (y - wal.getY()) * dax - (x - wal.getX()) * day;
		if (i == 0)
			return;
		sector[dasect].setCeilingheinum((short) scale((z - sector[dasect].getCeilingz()) << 8, sqrt(dax * dax + day * day), i));

		if (sector[dasect].getCeilingheinum() == 0)
			sector[dasect].setCeilingstat(sector[dasect].getCeilingstat() & ~2);
		else
			sector[dasect].setCeilingstat(sector[dasect].getCeilingstat() | 2);
	}

	public void alignflorslope(short dasect, int x, int y, int z) {
		Wall wal = wall[sector[dasect].getWallptr()];
		int dax = wall[wal.getPoint2()].getX() - wal.getX();
		int day = wall[wal.getPoint2()].getY() - wal.getY();

		int i = (y - wal.getY()) * dax - (x - wal.getX()) * day;
		if (i == 0)
			return;
		sector[dasect].setFloorheinum((short) scale((z - sector[dasect].getFloorz()) << 8, sqrt(dax * dax + day * day), i));

		if (sector[dasect].getFloorheinum() == 0)
			sector[dasect].setFloorstat(sector[dasect].getFloorstat() & ~2);
		else
			sector[dasect].setFloorstat(sector[dasect].getFloorstat() | 2);
	}

	public int getceilzofslope(short sectnum, int dax, int day) {
		if (sectnum == -1 || sector[sectnum] == null)
			return 0;
		if ((sector[sectnum].getCeilingstat() & 2) == 0)
			return (sector[sectnum].getCeilingz());

		Wall wal = wall[sector[sectnum].getWallptr()];
		int dx = wall[wal.getPoint2()].getX() - wal.getX();
		int dy = wall[wal.getPoint2()].getY() - wal.getY();
		int i = sqrt(dx * dx + dy * dy) << 5;
		if (i == 0)
			return (sector[sectnum].getCeilingz());
		long j = dmulscale(dx, day - wal.getY(), -dy, dax - wal.getX(), 3);

		return sector[sectnum].getCeilingz() + (scale(sector[sectnum].getCeilingheinum(), j, i));
	}

	public int getflorzofslope(short sectnum, int dax, int day) {
		if (sectnum == -1 || sector[sectnum] == null)
			return 0;
		if ((sector[sectnum].getFloorstat() & 2) == 0)
			return (sector[sectnum].getFloorz());

		Wall wal = wall[sector[sectnum].getWallptr()];
		int dx = wall[wal.getPoint2()].getX() - wal.getX();
		int dy = wall[wal.getPoint2()].getY() - wal.getY();
		int i = sqrt(dx * dx + dy * dy) << 5;
		if (i == 0)
			return (sector[sectnum].getFloorz());
		long j = dmulscale(dx, day - wal.getY(), -dy, dax - wal.getX(), 3);
		return sector[sectnum].getFloorz() + (scale(sector[sectnum].getFloorheinum(), j, i));
	}
	
	////////// MAP MANIPULATION FUNCTIONS //////////

	public short updatesector(int x, int y, short sectnum) {
		if (inside(x, y, sectnum) == 1)
			return sectnum;

		if ((sectnum >= 0) && (sectnum < numsectors)) {
			short wallid = sector[sectnum].getWallptr(), i;
			int j = sector[sectnum].getWallnum();
			if (wallid < 0)
				return -1;
			do {
				if (wallid >= getMaxWalls())
					break;
				Wall wal = wall[wallid];
				if (wal == null) {
					wallid++;
					j--;
					continue;
				}
				i = wal.getNextsector();
				if (i >= 0)
					if (inside(x, y, i) == 1) {
						return i;
					}
				wallid++;
				j--;
			} while (j != 0);
		}

		for (short i = (short) (numsectors - 1); i >= 0; i--)
			if (inside(x, y, i) == 1) {
				return i;
			}

		return -1;
	}

	public short updatesectorz(int x, int y, int z, short sectnum) {
		SlopeZ zs = getzsofslope(sectnum, x, y);
		if ((z >= zs.getCeiling()) && (z <= zs.getFloor()))
			if (inside(x, y, sectnum) != 0)
				return sectnum;

		if ((sectnum >= 0) && (sectnum < numsectors)) {
			if (sector[sectnum] == null)
				return -1;
			short wallid = sector[sectnum].getWallptr(), i;
			int j = sector[sectnum].getWallnum();
			do {
				if (wallid >= getMaxWalls())
					break;
				Wall wal = wall[wallid];
				if (wal == null) {
					wallid++;
					j--;
					continue;
				}
				i = wal.getNextsector();
				if (i >= 0) {
					zs = getzsofslope(i, x, y);
					if ((z >= zs.getCeiling()) && (z <= zs.getFloor()))
						if (inside(x, y, i) == 1) {
							return i;
						}
				}
				wallid++;
				j--;
			} while (j != 0);
		}

		for (short i = (short) (numsectors - 1); i >= 0; i--) {
			zs = getzsofslope(i, x, y);
			if ((z >= zs.getCeiling()) && (z <= zs.getFloor()))
				if (inside(x, y, i) == 1) {
					return i;
				}
		}

		return -1;
	}

	public int inside(int x, int y, short sectnum) {
		if ((sectnum < 0) || (sectnum >= numsectors))
			return (-1);

		int cnt = 0;
		int wallid = sector[sectnum].getWallptr();
		if (wallid < 0)
			return -1;
		int i = sector[sectnum].getWallnum();
		int x1, y1, x2, y2;

		do {
			Wall wal = wall[wallid];
			if (wal == null || wal.getPoint2() < 0 || wall[wal.getPoint2()] == null)
				return -1;
			y1 = wal.getY() - y;
			y2 = wall[wal.getPoint2()].getY() - y;

			if ((y1 ^ y2) < 0) {
				x1 = wal.getX() - x;
				x2 = wall[wal.getPoint2()].getX() - x;
				if ((x1 ^ x2) >= 0)
					cnt ^= x1;
				else
					cnt ^= (x1 * y2 - x2 * y1) ^ y2;
			}
			wallid++;
			i--;
		} while (i != 0);

		return (cnt >>> 31);
	}

	public SlopeZ getzsofslope(short sectnum, int dax, int day) {
		if (sectnum == -1 || sector[sectnum] == null)
			return null;

		Sector sec = sector[sectnum];
		if (sec == null)
			return null;

		int floorz = sec.getFloorz();
		int ceilingz = sec.getCeilingz();
		zofslope.setFloor(floorz);
		zofslope.setCeiling(ceilingz);
		if (((sec.getCeilingstat() | sec.getFloorstat()) & 2) != 0) {
			Wall wal = wall[sec.getWallptr()];
			Wall wal2 = wall[wal.getPoint2()];
			int dx = wal2.getX() - wal.getX();
			int dy = wal2.getY() - wal.getY();
			int i = sqrt(dx * dx + dy * dy) << 5;
			if (i == 0)
				return zofslope;
			long j = dmulscale(dx, day - wal.getY(), -dy, dax - wal.getX(), 3);

			if ((sec.getCeilingstat() & 2) != 0)
				ceilingz += scale(sec.getCeilingheinum(), j, i);
			if ((sec.getFloorstat() & 2) != 0)
				floorz += scale(sec.getFloorheinum(), j, i);
		}

		zofslope.setFloor(floorz);
		zofslope.setCeiling(ceilingz);
		return zofslope;
	}

	public int lastwall(int point) {
		if ((point > 0) && (wall[point - 1].getPoint2() == point))
			return (point - 1);

		int i = point, j;
		int cnt = getMaxWalls();
		do {
			j = wall[i].getPoint2();
			if (j == point)
				return (i);
			i = j;
			cnt--;
		} while (cnt > 0);
		return (point);
	}

	public int sectorofwall(short theline) {
		if ((theline < 0) || (theline >= numwalls))
			return (-1);

		int i = wall[theline].getNextwall();
		if (i >= 0)
			return (wall[i].getNextsector());

		int gap = (numsectors >> 1);
		i = gap;
		while (gap > 1) {
			gap >>= 1;
			if (sector[i].getWallptr() < theline)
				i += gap;
			else
				i -= gap;
		}
		while (sector[i].getWallptr() > theline)
			i--;
		while (sector[i].getWallptr() + sector[i].getWallnum() <= theline)
			i++;
		return (i);
	}
	
	// COMMON FUNCTIONS

	public Sprite getSprite(int num) {
		return sprite[num];
	}

	public Sector getSector(int num) {
		return sector[num];
	}

	public Wall getWall(int num) {
		return wall[num];
	}

	public short headspritesect(short sectnum) {
		return headspritesect[sectnum];
	}

	public short headspritestat(short statnum) {
		return headspritestat[statnum];
	}

	public short nextspritesect(short spritenum) {
		return nextspritesect[spritenum];
	}

	public short nextspritestat(short spritenum) {
		return nextspritestat[spritenum];
	}

	public byte[] getBytes() {
		buffer.clear();
		buffer.putInt(mapversion);
		buffer.putInt(dapos.x);
		buffer.putInt(dapos.y);
		buffer.putInt(dapos.z);
		buffer.putShort(dapos.ang);
		buffer.putShort(dapos.sectnum);

		buffer.putShort((short) numwalls);
		for (int w = 0; w < numwalls; w++)
			buffer.put(wall[w].getBytes());

		buffer.putShort((short) numsectors);
		for (int s = 0; s < numsectors; s++)
			buffer.put(sector[s].getBytes());

		buffer.putShort((short) numsprites);
		for (int i = 0; i < getMaxSprites(); i++)
			buffer.put(sprite[i].getBytes());

		for (int i = 0; i <= getMaxSectors(); i++)
			buffer.putShort(headspritesect[i]);
		for (int i = 0; i <= MAXSTATUS; i++)
			buffer.putShort(headspritestat[i]);
		for (int i = 0; i < getMaxSprites(); i++) {
			buffer.putShort(prevspritesect[i]);
			buffer.putShort(prevspritestat[i]);
			buffer.putShort(nextspritesect[i]);
			buffer.putShort(nextspritestat[i]);
		}

		return buffer.array();
	}

	public String getName() {
		return name;
	}
}
