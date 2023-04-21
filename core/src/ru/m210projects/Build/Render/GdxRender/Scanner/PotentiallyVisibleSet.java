package ru.m210projects.Build.Render.GdxRender.Scanner;

import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.RenderService.*;

import java.util.Arrays;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Gameutils;
import ru.m210projects.Build.Types.Wall;
import ru.m210projects.Build.Render.GdxRender.BuildCamera;
import ru.m210projects.Build.Types.collections.Pool;
import ru.m210projects.Build.Render.GdxRender.WorldMesh;
import ru.m210projects.Build.Render.GdxRender.WorldMesh.Heinum;

public class PotentiallyVisibleSet {

	private final WallFrustum2d[] portqueue;
	private final int queuemask; // pay attention!
	private int pqhead, pqtail;
	private int[] sectorqueue;
	private int secindex = 0;

	private final byte[] handled;
	private final WallFrustum2d[] gotviewport;
	private final byte[] gotwall;

	private final RayCaster ray = new RayCaster();
	protected SectorInfo info = new SectorInfo();

	private final Pool<WallFrustum2d> pWallFrustumPool = new Pool<WallFrustum2d>() {
		@Override
		protected WallFrustum2d newObject() {
			return new WallFrustum2d();
		}
	};

	public PotentiallyVisibleSet() {
		portqueue = new WallFrustum2d[512];
		queuemask = portqueue.length - 1;
		gotviewport = new WallFrustum2d[MAXSECTORS];
		sectorqueue = new int[MAXSECTORS];
		handled = new byte[MAXSECTORS >> 3];
		gotwall = new byte[MAXWALLS >> 3];
		sectorqueue = new int[MAXSECTORS];
	}

	public void process(BuildCamera cam, WorldMesh mesh, int sectnum) {
		if (!Gameutils.isValidSector(sectnum))
			return;

		Arrays.fill(gotviewport, null);
		Gameutils.fill(gotwall, (byte) 0);
		Gameutils.fill(handled, (byte) 0);
		pWallFrustumPool.reset();

		secindex = 0;
		pqhead = pqtail = 0;

		portqueue[(pqtail++) & queuemask] = pWallFrustumPool.obtain().set(sectnum);
		WallFrustum2d pFrustum = portqueue[pqhead];
		gotviewport[sectnum] = pFrustum;

		while (pqhead != pqtail) {
			sectnum = pFrustum.sectnum;

			if (!pFrustum.handled) {
				pFrustum.handled = true;

				if (info.hasOccluders(sectnum)) {
					ray.init(false);

					int startwall = Engine.getSector(sectnum).getWallptr();
					int endwall = Engine.getSector(sectnum).getWallnum() + startwall;
					for (int z = startwall; z < endwall; z++) {
						if (!WallFacingCheck(Engine.getWall(z)))
							continue;
						ray.add(z, null);
					}
					ray.update();
				}

				int startwall = Engine.getSector(sectnum).getWallptr();
				int endwall = Engine.getSector(sectnum).getWallnum() + startwall;
				for (int z = startwall; z < endwall; z++) {
					Wall wal = Engine.getWall(z);
					int nextsectnum = wal.getNextsector();

					if (!WallFacingCheck(wal))
						continue;

					if (!cam.polyInCamera(mesh.getPoints(Heinum.Max, sectnum, z)))
						continue;

					if (pFrustum.wallInFrustum(wal)) {
						if (info.hasOccluders(sectnum) && !ray.check(z))
							continue;

						if (nextsectnum != -1) {
							WallFrustum2d wallFrustum = pWallFrustumPool.obtain().set(wal);
							if (wallFrustum != null && wallFrustum.fieldOfViewClipping(pFrustum)) {
								if (gotviewport[nextsectnum] == null) {
									portqueue[(pqtail++) & queuemask] = wallFrustum;
									gotviewport[nextsectnum] = wallFrustum;
								} else {
									WallFrustum2d nextp = gotviewport[nextsectnum];
									if ((nextp = nextp.fieldOfViewExpand(wallFrustum)) != null) {
										if ((handled[nextsectnum >> 3] & pow2char[nextsectnum & 7]) != 0) {
											portqueue[(pqtail++) & queuemask] = nextp;
										}
									}
								}
							}
						}
						gotwall[z >> 3] |= pow2char[z & 7];
					}
				}
			}

			if (pFrustum.next != null)
				pFrustum = pFrustum.next;
			else
				pFrustum = portqueue[(++pqhead) & queuemask];

			if ((handled[sectnum >> 3] & pow2char[sectnum & 7]) == 0)
				sectorqueue[secindex++] = sectnum;
			handled[sectnum >> 3] |= pow2char[sectnum & 7];
		}

		ray.init(true);
//		for (int i = secindex - 1; i >= 0; i--) {
//		for (int i = 0; i < secindex; i++) {
//			sectnum = sectorqueue[i];
//			if ((pFrustum = gotviewport[sectnum]) != null) {
//				if(!World.info.isCorruptSector(sectnum)) {
//					int startwall = Engine.getSector(sectnum).wallptr;
//					int endwall = Engine.getSector(sectnum).wallnum + startwall;
//					for (int z = startwall; z < endwall; z++) {
//						if ((gotwall[z >> 3] & pow2char[z & 7]) != 0) {
//							ray.add(z, pFrustum);
//						}
//					}
//				}
//			}
//		}

		for (sectnum = 0; sectnum < MAXSECTORS; sectnum++) {
			if ((pFrustum = gotviewport[sectnum]) != null) {
				if (!info.isCorruptSector(sectnum)) {
					int startwall = Engine.getSector(sectnum).getWallptr();
					int endwall = Engine.getSector(sectnum).getWallnum() + startwall;
					for (int z = startwall; z < endwall; z++) {
						if ((gotwall[z >> 3] & pow2char[z & 7]) != 0) {
							Wall w = Engine.getWall(z);
//							if(w.nextsector != -1) { //XXX E2L8 near wall bug fix
							Wall p2 = Engine.getWall(w.getPoint2());
							int dx = p2.getX() - w.getX();
							int dy = p2.getY() - w.getY();
							float i = dx * (globalposx - w.getX()) + dy * (globalposy - w.getY());
							if (i >= 0.0f) {
								float j = dx * dx + dy * dy;
								if (i < j) {
									i /= j;
									int px = (int) (dx * i + w.getX());
									int py = (int) (dy * i + w.getY());

									dx = Math.abs(px - globalposx);
									dy = Math.abs(py - globalposy);

									// closest to camera portals should be rendered
									if (dx + dy < 128)
										continue;
								}
							}
//							}

							ray.add(z, pFrustum);
						}
					}
				}
			}
		}

		ray.update();
	}

	public boolean checkWall(int z) {
		if ((gotwall[z >> 3] & pow2char[z & 7]) != 0)
			return ray.check(z);
		return false; // (gotwall[z >> 3] & pow2char[z & 7]) != 0;

//		return (gotwall[z >> 3] & pow2char[z & 7]) != 0;
	}

	public boolean checkSector(int z) {
		return gotviewport[z] != null;
	}

	private boolean WallFacingCheck(Wall wal) {
		float x1 = wal.getX() - globalposx;
		float y1 = wal.getY() - globalposy;
		float x2 = Engine.getWall(wal.getPoint2()).getX() - globalposx;
		float y2 = Engine.getWall(wal.getPoint2()).getY() - globalposy;

		return (x1 * y2 - y1 * x2) >= 0;
	}
}
