package ru.m210projects.Build.Render.GdxRender.Scanner;

import static ru.m210projects.Build.Engine.*;

import java.util.ArrayList;
import java.util.Arrays;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Plane.PlaneSide;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import ru.m210projects.Build.*;
import ru.m210projects.Build.Types.*;
import ru.m210projects.Build.Types.QuickSort.IntComparator;
import ru.m210projects.Build.Render.GdxRender.BuildCamera;
import ru.m210projects.Build.Types.collections.Pool;
import ru.m210projects.Build.Render.GdxRender.Tesselator.Vertex;
import ru.m210projects.Build.Render.GdxRender.WorldMesh;
import ru.m210projects.Build.Render.GdxRender.WorldMesh.Heinum;
import ru.m210projects.Build.Types.collections.MapNode;

import static ru.m210projects.Build.RenderService.*;

public abstract class SectorScanner {

	private final Pool<WallFrustum3d> pFrustumPool = new Pool<WallFrustum3d>() {
		@Override
		protected WallFrustum3d newObject() {
			return new WallFrustum3d();
		}
	};

	private final Pool<VisibleSector> pSectorPool = new Pool<VisibleSector>() {
		@Override
		protected VisibleSector newObject() {
			return new VisibleSector();
		}
	};

	private final Vector2 projPoint = new Vector2();

	private final PotentiallyVisibleSet pvs;

	private final WallFrustum3d[] portqueue; // to linkedlist
	private final int queuemask; // pay attention!
	private int pqhead, pqtail;

	private final VisibleSector[] handled;
	private final WallFrustum3d[] gotviewport;
	private final WallFrustum3d[] skyviewport;
	private final byte[] gotwall;
	private final byte[] wallflags;
	protected Engine engine;

	public int[] maskwall = new int[MAXWALLS];
	public int maskwallcnt;

	private Sector skyFloor, skyCeiling;

	private final PolygonClipper cl = new PolygonClipper();

	public SectorScanner(Engine engine) {
		this.engine = engine;
		pvs = new PotentiallyVisibleSet(engine);

		portqueue = new WallFrustum3d[512];
		queuemask = portqueue.length - 1;
		tsprite = new TSprite[MAXSPRITESONSCREEN + 1];

		gotviewport = new WallFrustum3d[MAXSECTORS];
		skyviewport = new WallFrustum3d[MAXSECTORS];
		handled = new VisibleSector[MAXSECTORS];
		gotwall = new byte[MAXWALLS >> 3];
		wallflags = new byte[MAXWALLS];
	}

	public void init() {
		pvs.info.init(engine);
	}

	public void clear() {
		pSectorPool.reset();
		pFrustumPool.reset();
	}

	public void process(ArrayList<VisibleSector> sectors, BuildCamera cam, WorldMesh mesh, int sectnum) {
		BoardService boardService = engine.getBoardService();
		if (!boardService.isValidSector(sectnum)) {
			return;
		}

		pvs.process(cam, mesh, sectnum);

		Arrays.fill(gotviewport, null);
		Gameutils.fill(gotwall, (byte) 0);
		Gameutils.fill(wallflags, (byte) 0);
		Arrays.fill(handled, null);

		skyFloor = skyCeiling = null;

		maskwallcnt = 0;
		spritesortcnt = 0;

		pqhead = pqtail = 0;

		int cursectnum = sectnum;
		portqueue[(pqtail++) & queuemask] = pFrustumPool.obtain().set(cam, sectnum);
		WallFrustum3d pFrustum = portqueue[pqhead];
		gotviewport[sectnum] = pFrustum;

		BoardService service = engine.getBoardService();
		while (pqhead != pqtail) {
			sectnum = pFrustum.sectnum;

			VisibleSector sec = handled[sectnum];
			if (handled[sectnum] == null) {
				sec = pSectorPool.obtain().set(sectnum);
			}

			if (!pFrustum.handled) {
				pFrustum.handled = true;

				int startwall = boardService.getSector(sectnum).getWallptr();
				int endwall = boardService.getSector(sectnum).getWallnum() + startwall;
				for (int z = startwall; z < endwall; z++) {
					Wall wal = boardService.getWall(z);
					if (!pvs.checkWall(z)) {
						continue;
					}

					int nextsectnum = wal.getNextsector();
					if (pFrustum.wallInFrustum(mesh.getPoints(Heinum.Max, sectnum, z))) {
						gotwall[z >> 3] |= pow2char[z & 7];

						if ((boardService.getSector(sectnum).isParallaxFloor()
								&& (nextsectnum == -1 || !boardService.getSector(nextsectnum).isParallaxFloor()))
								&& pFrustum.wallInFrustum(mesh.getPoints(Heinum.SkyLower, sectnum, z))) {
							wallflags[z] |= 8;
						}
						if ((boardService.getSector(sectnum).isParallaxCeiling()
								&& (nextsectnum == -1 || !boardService.getSector(nextsectnum).isParallaxCeiling()))
								&& pFrustum.wallInFrustum(mesh.getPoints(Heinum.SkyUpper, sectnum, z))) {
							wallflags[z] |= 16;
						}

						if (nextsectnum != -1) {
							if (!checkWallRange(nextsectnum, wal.getNextwall())) {
								int theline = wal.getNextwall();
								int gap = (service.getSectorCount() >> 1);
								short i = (short) gap;
								while (gap > 1) {
									gap >>= 1;
									if (boardService.getSector(i).getWallptr() < theline) {
										i += gap;
									} else {
										i -= gap;
									}
								}
								while (boardService.getSector(i).getWallptr() > theline) {
									i--;
								}
								while (boardService.getSector(i).getWallptr() + boardService.getSector(i).getWallnum() <= theline) {
									i++;
								}
								nextsectnum = i;

								System.err.println("Error on " + i);
								wal.setNextsector(i); // XXX
							}

							if (pFrustum.wallInFrustum(mesh.getPoints(Heinum.Lower, sectnum, z))) {
								wallflags[z] |= 1;
							}
							if (pFrustum.wallInFrustum(mesh.getPoints(Heinum.Upper, sectnum, z))) {
								wallflags[z] |= 2;
							}

							if (!pvs.checkSector(nextsectnum)) {
								continue;
							}

							WallFrustum3d portal = null;
							if ((((boardService.getSector(sectnum).getCeilingstat() & boardService.getSector(nextsectnum).getCeilingstat()) & 1) != 0)
									|| (((boardService.getSector(sectnum).getFloorstat() & boardService.getSector(nextsectnum).getFloorstat()) & 1) != 0)) {
								portal = pFrustum.clone(pFrustumPool);
								portal.sectnum = nextsectnum;
							} else {
								// Handle the next portal
								ArrayList<Vertex> points;
								if ((points = mesh.getPoints(Heinum.Portal, sectnum, z)) == null) {
									continue;
								}

								WallFrustum3d clip = null;
								boolean bNearPlaneClipped;
								if (bNearPlaneClipped = NearPlaneCheck(cam, points)) {
									float posx = globalposx;
									float posy = globalposy;

									if ((boardService.getSector(sectnum).isParallaxCeiling()) || (boardService.getSector(sectnum).isParallaxFloor())
											|| (projectionToWall(posx, posy, wal, projPoint)
													&& Math.abs(posx - projPoint.x) + Math
															.abs(posy - projPoint.y) <= cam.near * cam.xscale * 2)) {
										clip = pFrustum.clone(pFrustumPool);
										clip.sectnum = nextsectnum;
									}
								}

								if ((sectnum == cursectnum || bNearPlaneClipped) && clip == null) {
									points = cl.ClipPolygon(cam.frustum, points);
									if (points.size() < 3) {
										continue;
									}
								}

								if (wal.isOneWay() && clip == null) {
									continue;
								}

								if (clip != null) {
									portal = clip;
								} else {
									Sector nextsec = boardService.getSector(nextsectnum);
									if (!nextsec.isParallaxCeiling() && !nextsec.isParallaxFloor()) {
										if (!pFrustum.wallInFrustum(points)) {
											continue;
										}
									}
									portal = pFrustum.build(cam, pFrustumPool, points, nextsectnum);
								}
							}

							if (portal != null) { // is in frustum
								wallflags[z] |= 4;
								if (gotviewport[nextsectnum] == null) {
									portqueue[(pqtail++) & queuemask] = (gotviewport[nextsectnum] = portal);
								} else {
									WallFrustum3d nextp = gotviewport[nextsectnum];
									if ((nextp = nextp.expand(portal)) != null) {
										if (handled[nextsectnum] != null) {
											portqueue[(pqtail++) & queuemask] = nextp;
										}
									}
								}
							}
						}
					}
				}
			}

			if (handled[sectnum] == null) {
				handled[sectnum] = sec;
			}

			if (pFrustum.next != null) {
				pFrustum = pFrustum.next;
			} else {
				pFrustum = portqueue[(++pqhead) & queuemask];
			}
		}

		pqhead = pqtail = 0;
		sectnum = cursectnum;
		portqueue[(pqtail++) & queuemask] = gotviewport[cursectnum];
		skyviewport[cursectnum] = gotviewport[cursectnum];
		gotviewport[cursectnum] = null;

		do {
			pFrustum = portqueue[(pqhead++) & queuemask];
			sectnum = pFrustum.sectnum;
			VisibleSector sec = handled[sectnum];

			if (automapping == 1) {
				show2dsector[sectnum >> 3] |= pow2char[sectnum & 7];
			}

			boolean isParallaxCeiling = boardService.getSector(sectnum).isParallaxCeiling();
			boolean isParallaxFloor = boardService.getSector(sectnum).isParallaxFloor();
			int startwall = boardService.getSector(sectnum).getWallptr();
			int endwall = boardService.getSector(sectnum).getWallnum() + startwall;
			for (int z = startwall; z < endwall; z++) {
				Wall wal = boardService.getWall(z);
				int nextsectnum = wal.getNextsector();

				if ((gotwall[z >> 3] & pow2char[z & 7]) == 0) {
					continue;
				}

				if (nextsectnum != -1) {
					if (gotviewport[nextsectnum] != null) {
						portqueue[(pqtail++) & queuemask] = gotviewport[nextsectnum];
						skyviewport[nextsectnum] = gotviewport[nextsectnum];
						gotviewport[nextsectnum] = null;
					}
				}
				if (wal.isMasked() || wal.isOneWay()) {
					maskwall[maskwallcnt++] = z;
				}

				if ((wallflags[z] & (8 | 16)) != 0) {
					wallflags[z] &= ~(8 | 16);

					if (isParallaxCeiling) {
						if (engine.getTile(boardService.getSector(sectnum).getCeilingpicnum()).hasSize()) {
							skyCeiling = boardService.getSector(sectnum);
						}
					}

					if (isParallaxFloor) {
						if (engine.getTile(boardService.getSector(sectnum).getFloorpicnum()).hasSize()) {
							skyFloor = boardService.getSector(sectnum);
						}
					}

					sec.skywalls.add(z);
				}

				sec.walls.add(z);
				sec.wallflags.add(wallflags[z]);
			}

			byte secflags = 0;
			if (!isParallaxFloor && isSectorVisible(pFrustum, cam.frustum.planes[0], true, sectnum)) {
				secflags |= 1;
			}
			if (!isParallaxCeiling && isSectorVisible(pFrustum, cam.frustum.planes[0], false, sectnum)) {
				secflags |= 2;
			}

			checkSprites(pFrustum, sectnum);

			sec.secflags = secflags;
			sec.setFrustum(pFrustum.getPlanes());
			sectors.add(sec);
		} while (pqhead != pqtail);

		QuickSort.sort(maskwall, maskwallcnt, wallcomp);
	}

	protected IntComparator wallcomp = new IntComparator() {
		@Override
		public int compare(int o1, int o2) {
			BoardService boardService = engine.getBoardService();
			if (!wallfront(boardService.getWall(o1), boardService.getWall(o2))) {
				return -1;
			}
			return 0;
		}
	};

	protected boolean wallfront(Wall w1, Wall w2) {
		Wall wp1 = w1.getWall2();
		float x11 = w1.getX();
		float y11 = w1.getY();
		float x21 = wp1.getX();
		float y21 = wp1.getY();

		Wall wp2 = w2.getWall2();
		float x12 = w2.getX();
		float y12 = w2.getY();
		float x22 = wp2.getX();
		float y22 = wp2.getY();

		float dx = x21 - x11;
		float dy = y21 - y11;

		final double f = 0.001;
		final double invf = 1.0 - f;
		double px = (x12 * invf) + (x22 * f);
		double py = (y12 * invf) + (y22 * f);

		double cross = dx * (py - y11) - dy * (px - x11);
		boolean t1 = (cross < 0.00001); // p1(l2) vs. l1

		px = (x22 * invf) + (x12 * f);
		py = (y22 * invf) + (y12 * f);
		double cross1 = dx * (py - y11) - dy * (px - x11);
		boolean t2 = (cross1 < 0.00001); // p2(l2) vs. l1

		if (t1 == t2) {
			t1 = (dx * (globalposy - y11) - dy * (globalposx - x11) < 0.00001); // pos vs. l1
			if (t2 == t1) {
				return true;
			}
		}

		dx = x22 - x12;
		dy = y22 - y12;

		px = (x11 * invf) + (x21 * f);
		py = (y11 * invf) + (y21 * f);

		double cross3 = dx * (py - y12) - dy * (px - x12);
		t1 = (cross3 < 0.00001); // p1(l1) vs. l2

		px = (x21 * invf) + (x11 * f);
		py = (y21 * invf) + (y11 * f);
		double cross4 = dx * (py - y12) - dy * (px - x12);
		t2 = (cross4 < 0.00001); // p2(l1) vs. l2

		if (t1 == t2) {
			t1 = (dx * (globalposy - y12) - dy * (globalposx - x12) < 0.00001); // pos vs. l2
			return t2 != t1;
		}

		return false;
	}

	public Sector getLastSkySector(Heinum h) {
		if (h == Heinum.SkyLower) {
			return skyFloor;
		}
		return skyCeiling;
	}

	private boolean checkWallRange(int sectnum, int z) {
		BoardService boardService = engine.getBoardService();
		return z >= boardService.getSector(sectnum).getWallptr() && z < (boardService.getSector(sectnum).getWallptr() + boardService.getSector(sectnum).getWallnum());
	}

	private void checkSprites(WallFrustum3d pFrustum, int sectnum) {
		BoardService service = engine.getBoardService();
		for (MapNode<Sprite> node = service.getSectNode(sectnum); node != null; node = node.getNext()) {
			int z = node.getIndex();
			Sprite spr = node.get();

			if ((((spr.getCstat() & 0x8000) == 0) || showinvisibility) && (spr.getXrepeat() > 0) && (spr.getYrepeat() > 0)
					&& (spritesortcnt < MAXSPRITESONSCREEN)) {
				int xs = spr.getX() - globalposx;
				int ys = spr.getY() - globalposy;
				if ((spr.getCstat() & (64 + 48)) != (64 + 16) || Pragmas.dmulscale(EngineUtils.cos(spr.getAng()), -xs,
						EngineUtils.sin(spr.getAng()), -ys, 6) > 0) {
					if (spriteInFrustum(pFrustum, spr)) {
						Sprite tspr = addTSprite();
						tspr.set(spr);
						tspr.setOwner((short) z);
					}
				}
			}
		}
	}

	private static final Vector3[] tmpVec = { new Vector3(), new Vector3(), new Vector3(), new Vector3() };

	public boolean spriteInFrustum(WallFrustum3d frustum, Sprite tspr) {
		Vector3[] points = tmpVec;
		float SIZEX = 0.5f;
		float SIZEY = 0.5f;

		Matrix4 mat = getSpriteMatrix(tspr);
		if (mat != null) {
			points[0].set(-SIZEX, SIZEY, 0).mul(mat);
			points[1].set(SIZEX, SIZEY, 0).mul(mat);
			points[2].set(SIZEX, -SIZEY, 0).mul(mat);
			points[3].set(-SIZEX, -SIZEY, 0).mul(mat);

			WallFrustum3d n = frustum;
			do {
				if (n.wallInFrustum(points, 4)) {
					return true;
				}
				n = n.next;
			} while (n != null);
		}

		return false;
	}

	protected abstract Matrix4 getSpriteMatrix(Sprite tspr);

	private Sprite addTSprite() {
		if (tsprite[spritesortcnt] == null) {
			tsprite[spritesortcnt] = new TSprite();
		}
		return tsprite[spritesortcnt++];
	}

	private boolean isSectorVisible(WallFrustum3d frustum, Plane near, boolean isFloor, int sectnum) {
		frustum.rebuild();
		BoardService boardService = engine.getBoardService();
		Plane: for (int i = near == null ? 0 : -1; i < frustum.planes.length; i++) {
			Plane plane = (i == -1) ? near : frustum.planes[i];

			int startwall = boardService.getSector(sectnum).getWallptr();
			int endwall = boardService.getSector(sectnum).getWallnum() + startwall;
			for (int z = startwall; z < endwall; z++) {
				Wall wal = boardService.getWall(z);
				int wz = isFloor ? engine.getflorzofslope((short) sectnum, wal.getX(), wal.getY())
						: engine.getceilzofslope((short) sectnum, wal.getX(), wal.getY());

				if ((isFloor && !boardService.getSector(sectnum).isFloorSlope() && globalposz > wz)
						|| (!isFloor && !boardService.getSector(sectnum).isCeilingSlope() && globalposz < wz)) {
					continue;
				}

				if (plane.testPoint(wal.getX(), wal.getY(), wz) != PlaneSide.Back) {
					continue Plane;
				}
			}

			if (frustum.next != null) {
				return isSectorVisible(frustum.next, null, isFloor, sectnum);
			}

			return false;
		}
		return true;
	}

	private boolean NearPlaneCheck(BuildCamera cam, ArrayList<? extends Vector3> points) {
		Plane near = cam.frustum.planes[0];
		for (int i = 0; i < points.size(); i++) {
			if (near.testPoint(points.get(i)) == PlaneSide.Back) {
				return true;
			}
		}
		return false;
	}

	public boolean projectionToWall(float posx, float posy, Wall w, Vector2 n) {
		Wall p2 = w.getWall2();
		int dx = p2.getX() - w.getX();
		int dy = p2.getY() - w.getY();

		float i = dx * (posx - w.getX()) + dy * (posy - w.getY());

		if (i < 0) {
			n.set(w.getX(), w.getY());
			return false;
		}

		float j = dx * dx + dy * dy;
		if (i > j) {
			n.set(p2.getX(), p2.getY());
			return false;
		}

		i /= j;

		n.set(dx * i + w.getX(), dy * i + w.getY());
		return true;
	}

	public int getSpriteCount() {
		return spritesortcnt;
	}

	public TSprite[] getSprites() {
		return tsprite;
	}

	public int getMaskwallCount() {
		return maskwallcnt;
	}

	public int[] getMaskwalls() {
		return maskwall;
	}
}
