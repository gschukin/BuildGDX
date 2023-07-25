package ru.m210projects.Build.Render.GdxRender.Scanner;

import ru.m210projects.Build.Board;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Wall;

import static ru.m210projects.Build.Engine.*;

public class SectorInfo {

	public enum Clockdir {
		CW(0), CCW(1);

		private final int value;

		Clockdir(int val) {
			this.value = val;
		}

		public int getValue() {
			return value;
		}
	}

	public Clockdir[] loopinfo = new Clockdir[MAXWALLS];
	public boolean[] isOccluder = new boolean[MAXWALLS];
	public boolean[] isContour = new boolean[MAXWALLS];
	public int[] numloops = new int[MAXSECTORS];
	public boolean[] hasOccluders = new boolean[MAXSECTORS];
	public boolean[] isCorrupt = new boolean[MAXSECTORS];

	public void init(Engine engine) {
		Board board = engine.getBoardService().getBoard();
		for (int i = 0; i < board.getSectorCount(); i++) {
			Sector sec = board.getSector(i);

			hasOccluders[i] = false;
			Clockdir dir = clockdir(sec, sec.getWallptr());
			boolean hasContour = false;
			int numloops = 0;
			int startwall = sec.getWallptr();
			int endwall = sec.getEndWall();
			int z = startwall;
			while (z <= endwall) {
				Wall wal = board.getWall(z);
				loopinfo[z] = dir == null ? (dir = clockdir(sec, z)) : dir;
				int nextsector = wal.getNextsector();
				if (dir == Clockdir.CCW && nextsector == -1) {
					isOccluder[z] = true;
					hasOccluders[i] = true;
				} else {
					isOccluder[z] = false;
				}

                isContour[z] = dir == Clockdir.CW && nextsector == -1;

				if (wal.getPoint2() < z) {
					numloops++;
					if (dir == Clockdir.CW) {
						hasContour = true;
					}
					dir = null;
				}
				z++;
			}

			this.numloops[i] = numloops;
			if (numloops > 0 && !hasContour) {
				isCorrupt[i] = true;
				System.err.println("Error: sector " + i + " doesn't have contour!");
			} else {
				isCorrupt[i] = false;
			}
		}
	}

	public Clockdir clockdir(Sector sec, int wallstart) { // Returns: 0 is CW, 1 is CCW
		Wall[] walls = sec.getWalls();

		int minx = Integer.MAX_VALUE;
		Wall themin = null;
		int i = wallstart - sec.getWallptr() - 1;
		if (i < 0) {
			i += sec.getWallnum();
		}

		if (i < 0 || i >= walls.length) {
			return Clockdir.CW;
		}

		do {
			Wall wal = walls[i++];
			int x = wal.getWall2().getX();
			if (x < minx) {
				minx = x;
				themin = wal;
			}
		} while (walls[i].getPoint2() != wallstart);

		int x0 = themin.getX();
		int y0 = themin.getY();
		int x1 = themin.getWall2().getX();
		int y1 = themin.getWall2().getY();
		int x2 = themin.getWall2().getWall2().getX();
		int y2 = themin.getWall2().getWall2().getY();

		if ((y1 >= y2) && (y1 <= y0)) {
			return Clockdir.CW;
		}
		if ((y1 >= y0) && (y1 <= y2)) {
			return Clockdir.CCW;
		}

		int templong = (x0 - x1) * (y2 - y1) - (x2 - x1) * (y0 - y1);
		if (templong < 0) {
			return Clockdir.CW;
		} else {
			return Clockdir.CCW;
		}
	}

	public boolean isCorruptSector(int i) {
		return isCorrupt[i];
	}

	public boolean isInnerWall(int i) {
		return loopinfo[i] == Clockdir.CCW;
	}

	public boolean isContourWall(int i) {
		return isContour[i];
	}

	public boolean isOccluderWall(int i) {
		return isOccluder[i];
	}

	public boolean hasOccluders(int sectnum) {
		return hasOccluders[sectnum];
	}

	public int getNumloops(int sectnum) {
		return numloops[sectnum];
	}
}
