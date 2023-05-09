package ru.m210projects.Build.Render.GdxRender.Scanner;

import ru.m210projects.Build.Board;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Engine.Clockdir;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Wall;

import static ru.m210projects.Build.Engine.*;

public class SectorInfo {

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
			Clockdir dir = engine.clockdir(sec.getWallptr());
			boolean hasContour = false;
			int numloops = 0;
			int startwall = sec.getWallptr();
			int endwall = sec.getEndWall();
			int z = startwall;
			while (z <= endwall) {
				Wall wal = board.getWall(z);
				loopinfo[z] = dir == null ? (dir = engine.clockdir(z)) : dir;
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
