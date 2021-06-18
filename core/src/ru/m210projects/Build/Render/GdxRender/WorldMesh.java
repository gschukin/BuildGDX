package ru.m210projects.Build.Render.GdxRender;

import static ru.m210projects.Build.Engine.CEIL;
import static ru.m210projects.Build.Engine.FLOOR;
import static ru.m210projects.Build.Engine.MAXSECTORS;
import static ru.m210projects.Build.Engine.MAXWALLS;
import static ru.m210projects.Build.Engine.numsectors;
import static ru.m210projects.Build.Engine.sector;
import static ru.m210projects.Build.Engine.wall;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.FloatArray;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Render.GdxRender.Tesselator.SurfaceInfo;
import ru.m210projects.Build.Render.GdxRender.Tesselator.Type;
import ru.m210projects.Build.Render.GdxRender.Tesselator.Vertex;
import ru.m210projects.Build.Types.SECTOR;
import ru.m210projects.Build.Types.Timer;
import ru.m210projects.Build.Types.WALL;

public class WorldMesh {

	private Tesselator tess;
	private Mesh mesh;
	protected Engine engine;
	private int maxVertices;
	private int meshOffset;
	protected GLSurface lastSurf;
	private boolean validateMesh = false;

	public enum Heinum {
		MaxWall, Max, Lower, Upper, Portal, SkyLower, SkyUpper
	};

	private final FloatArray vertices = new FloatArray();

	private int[] floorhash = new int[MAXSECTORS];
	private int[] ceilinghash = new int[MAXSECTORS];
	private int[] wallhash = new int[MAXWALLS];

	private GLSurface[] walls = new GLSurface[MAXWALLS];
	private GLSurface[] upper_walls = new GLSurface[MAXWALLS];
	private GLSurface[] lower_walls = new GLSurface[MAXWALLS];
	private GLSurface[] maskwalls = new GLSurface[MAXWALLS];
	private GLSurface[] upper_skies = new GLSurface[MAXWALLS];
	private GLSurface[] lower_skies = new GLSurface[MAXWALLS];
	private GLSurface[] floors = new GLSurface[MAXSECTORS];
	private GLSurface[] ceilings = new GLSurface[MAXSECTORS];
	private GLSurface skyPlane;

	private final int[] zofslope = new int[2];
	private static final int CEILING1 = 0;
	private static final int CEILING2 = 1;
	private static final int FLOOR2 = 2;
	private static final int FLOOR1 = 3;
	private final Vertex[] pol = new Vertex[] { new Vertex(0, 0), new Vertex(1, 0), new Vertex(1, 1),
			new Vertex(0, 1) };
	private ArrayList<Vertex> pointList = new ArrayList<Vertex>();

	protected final float scalexy = 512.0f;
	protected final float scalez = 8192.0f;

	public WorldMesh(Engine engine) {
		this.engine = engine;
		this.tess = new Tesselator(this, VertexAttribute.Position(), VertexAttribute.TexCoords(0));

		Timer.start();
		FloatArray vertices = new FloatArray();
		lastSurf = null;
		maxVertices = 0;
		meshOffset = 0;

		skyPlane = addSkyPlane(vertices);

		for (short s = 0; s < numsectors; s++) {
			SECTOR sec = sector[s];
			if (sec.floorz == sec.ceilingz)
				continue;

			tess.setSector(s, true);

			addFloor(vertices, s);
			floorhash[s] = getFloorHash(s);
			addCeiling(vertices, s);
			ceilinghash[s] = getCeilingHash(s);

			for (int w = sec.wallptr; w < sec.wallptr + sec.wallnum; w++) {
				wallhash[w] = getWallHash(s, w);

				addMiddle(vertices, s, w);
				addUpper(vertices, s, w);
				addLower(vertices, s, w);
				addMaskedWall(vertices, s, w);
			}

			if (sec.isParallaxCeiling() || sec.isParallaxFloor()) {
				for (int w = sec.wallptr; w < sec.wallptr + sec.wallnum; w++) {
					addParallaxCeiling(vertices, s, w);
					addParallaxFloor(vertices, s, w);
				}
			}

			maxVertices += tess.getMaxVertices();
		}

		Timer.result("WorldMesh built in: ");
		mesh = new Mesh(false, maxVertices, 0, tess.attributes);

		mesh.setVertices(vertices.items, 0, maxVertices * tess.getVertexSize());
	}

	public ArrayList<Vertex> getPoints(Heinum heinum, int sectnum, int z) {
		int fz1, fz2, cz1, cz2;
		SECTOR sec = sector[sectnum];
		WALL wal = wall[z];
		WALL wal2 = wall[wal.point2];
		int nextsector = wal.nextsector;

		switch (heinum) {
		case Max:
		case MaxWall:
			engine.getzsofslope((short) sectnum, wal.x, wal.y, zofslope);
			pol[CEILING1].set(wal, zofslope[CEIL], 0, 0);
			pol[FLOOR1].set(wal, zofslope[FLOOR], 0, 1);

			engine.getzsofslope((short) sectnum, wal2.x, wal2.y, zofslope);
			pol[FLOOR2].set(wal2, zofslope[FLOOR], 1, 1);
			pol[CEILING2].set(wal2, zofslope[CEIL], 1, 0);

			if (heinum == Heinum.Max) {
				if (sec.isParallaxCeiling())
					pol[CEILING1].z = pol[CEILING2].z = Integer.MIN_VALUE;
				if (sec.isParallaxFloor())
					pol[FLOOR1].z = pol[FLOOR2].z = Integer.MAX_VALUE;
			}
			break;
		case Lower:
			fz1 = engine.getflorzofslope((short) sectnum, wal.x, wal.y);
			cz1 = engine.getflorzofslope((short) nextsector, wal.x, wal.y);
			fz2 = engine.getflorzofslope((short) sectnum, wal2.x, wal2.y);
			cz2 = engine.getflorzofslope((short) nextsector, wal2.x, wal2.y);

			if (fz1 < cz1 && fz2 < cz2)
				return null;

			pol[CEILING1].set(wal, cz1, 0, 0);
			pol[FLOOR1].set(wal, fz1, 0, 1);
			pol[FLOOR2].set(wal2, fz2, 1, 1);
			pol[CEILING2].set(wal2, cz2, 1, 0);
			break;
		case SkyLower:
			fz1 = engine.getflorzofslope((short) sectnum, wal.x, wal.y);
			pol[CEILING1].set(wal, fz1, 0, 1);
			pol[FLOOR1].set(wal, fz1 + 0x8000000, 0, 0);

			fz1 = engine.getflorzofslope((short) sectnum, wal2.x, wal2.y);
			pol[FLOOR2].set(wal2, fz1 + 0x8000000, 1, 1);
			pol[CEILING2].set(wal2, fz1, 1, 0);
			break;
		case SkyUpper:
			cz1 = engine.getceilzofslope((short) sectnum, wal.x, wal.y);
			pol[FLOOR1].set(wal, cz1, 0, 0);
			pol[CEILING1].set(wal, cz1 - 0x8000000, 0, 1);

			cz1 = engine.getceilzofslope((short) sectnum, wal2.x, wal2.y);
			pol[FLOOR2].set(wal2, cz1, 1, 1);
			pol[CEILING2].set(wal2, cz1 - 0x8000000, 1, 0);
			break;
		case Upper:
			fz1 = engine.getceilzofslope((short) sectnum, wal.x, wal.y);
			cz1 = engine.getceilzofslope((short) nextsector, wal.x, wal.y);
			fz2 = engine.getceilzofslope((short) sectnum, wal2.x, wal2.y);
			cz2 = engine.getceilzofslope((short) nextsector, wal2.x, wal2.y);

			if (fz1 >= cz1 && fz2 >= cz2)
				return null;

			pol[CEILING1].set(wal, fz1, 0, 0);
			pol[FLOOR1].set(wal, cz1, 0, 1);
			pol[FLOOR2].set(wal2, cz2, 1, 1);
			pol[CEILING2].set(wal2, fz2, 1, 0);
			break;
		case Portal:
			engine.getzsofslope((short) nextsector, wal.x, wal.y, zofslope);
			fz1 = zofslope[FLOOR];
			cz1 = zofslope[CEIL];
			engine.getzsofslope((short) nextsector, wal2.x, wal2.y, zofslope);
			fz2 = zofslope[FLOOR];
			cz2 = zofslope[CEIL];

			engine.getzsofslope((short) sectnum, wal.x, wal.y, zofslope);
			int fz3 = zofslope[FLOOR];
			int cz3 = zofslope[CEIL];
			engine.getzsofslope((short) sectnum, wal2.x, wal2.y, zofslope);
			int fz4 = zofslope[FLOOR];
			int cz4 = zofslope[CEIL];

			if (fz3 <= fz1 && fz4 <= fz2) {
				fz1 = fz3;
				fz2 = fz4;
			}

			if (cz3 >= cz1 && cz4 >= cz2) {
				cz1 = cz3;
				cz2 = cz4;
			}

			pol[CEILING1].set(wal, cz1, 0, 0);
			pol[FLOOR1].set(wal, fz1, 0, 1);
			pol[FLOOR2].set(wal2, fz2, 1, 1);
			pol[CEILING2].set(wal2, cz2, 1, 0);
			break;
		}

		pointList.clear();
		if (pol[FLOOR1].z == pol[CEILING1].z && pol[FLOOR2].z == pol[CEILING2].z) {
			if (sec.isParallaxFloor() || sec.isParallaxCeiling()) {
				pointList.add(pol[CEILING1]);
				pointList.add(pol[CEILING2]);
				return pointList;
			}
			return null;
		}

		float dz0 = pol[FLOOR1].z - pol[CEILING1].z;
		float dz1 = pol[FLOOR2].z - pol[CEILING2].z;
		if (dz0 > 0.0f) {
			pointList.add(pol[CEILING1]);
			if (dz1 > 0.0f) {
				pointList.add(pol[CEILING2]);
				pointList.add(pol[FLOOR2]);
				pointList.add(pol[FLOOR1]);
				return pointList; // 4
			} else {
				float f = dz0 / (dz0 - dz1);
				pol[CEILING2].x = (pol[CEILING2].x - pol[CEILING1].x) * f + pol[CEILING1].x;
				pol[CEILING2].y = (pol[CEILING2].y - pol[CEILING1].y) * f + pol[CEILING1].y;
				pol[CEILING2].z = (pol[CEILING2].z - pol[CEILING1].z) * f + pol[CEILING1].z;
				pol[CEILING2].u = (pol[CEILING2].u - pol[CEILING1].u) * f + pol[CEILING1].u;
				pol[CEILING2].v = (pol[CEILING2].v - pol[CEILING1].v) * f + pol[CEILING1].v;
				pointList.add(pol[CEILING2]);
				pointList.add(pol[FLOOR1]);
				return pointList; // 3
			}
		}
		if (dz1 <= 0.0f)
			return null; // do not include null case for rendering

		float f = dz0 / (dz0 - dz1);
		pol[CEILING1].x = (pol[CEILING2].x - pol[CEILING1].x) * f + pol[CEILING1].x;
		pol[CEILING1].y = (pol[CEILING2].y - pol[CEILING1].y) * f + pol[CEILING1].y;
		pol[CEILING1].z = (pol[CEILING2].z - pol[CEILING1].z) * f + pol[CEILING1].z;
		pol[CEILING1].u = (pol[CEILING2].u - pol[CEILING1].u) * f + pol[CEILING1].u;
		pol[CEILING1].v = (pol[CEILING2].v - pol[CEILING1].v) * f + pol[CEILING1].v;
		pointList.add(pol[CEILING1]);
		pointList.add(pol[CEILING2]);
		pointList.add(pol[FLOOR2]);

		return pointList; // 3
	}

	public Mesh getMesh() {
		return mesh;
	}

	private GLSurface addParallaxFloor(FloatArray vertices, int sectnum, int wallnum) {
		final WALL wal = wall[wallnum];
		final SECTOR sec = sector[sectnum];

		boolean isParallaxFloor = sec.isParallaxFloor();
		if (!isParallaxFloor)
			return setNull(lower_skies, wallnum);

		int nextsector = wal.nextsector;
		boolean isParallaxNext = nextsector != -1 && (sector[nextsector].isParallaxFloor());

		GLSurface surf = null;
		if (isParallaxFloor && (nextsector == -1 || !isParallaxNext)) {
			SurfaceInfo info = tess.getSurface(Type.Sky.setHeinum(Heinum.SkyLower), wallnum, vertices);
			if (info == null)
				return setNull(lower_skies, wallnum);

			surf = getSurface(lower_skies, wallnum, info.getSize(), info.getLimit());
			if (surf != null) {
				surf.picnum = info.picnum;
				surf.ptr = info.obj;
				surf.type = Type.Floor;
				surf.vis_ptr = sectnum;
				surf.visflag = 0;
			}
		}

		if (surf != null && surf.count == 0)
			return null;

		return surf;
	}

	private GLSurface addParallaxCeiling(FloatArray vertices, int sectnum, int wallnum) {
		final WALL wal = wall[wallnum];
		final SECTOR sec = sector[sectnum];

		boolean isParallaxCeiling = sec.isParallaxCeiling();
		if (!isParallaxCeiling)
			return setNull(upper_skies, wallnum);

		int nextsector = wal.nextsector;
		boolean isParallaxNext = nextsector != -1 && (sector[nextsector].isParallaxCeiling());

		GLSurface surf = null;
		if (isParallaxCeiling && (nextsector == -1 || !isParallaxNext)) {
			SurfaceInfo info = tess.getSurface(Type.Sky.setHeinum(Heinum.SkyUpper), wallnum, vertices);

			if (info == null)
				return setNull(upper_skies, wallnum);

			surf = getSurface(upper_skies, wallnum, info.getSize(), info.getLimit());
			if (surf != null) {
				surf.picnum = info.picnum;
				surf.ptr = info.obj;
				surf.type = Type.Ceiling;
				surf.vis_ptr = sectnum;
				surf.visflag = 0;
			}
		}

		if (surf != null && surf.count == 0)
			return null;

		return surf;
	}

	private GLSurface addMiddle(FloatArray vertices, int sectnum, int wallnum) {
		GLSurface surf = null;
		final int nextsector = wall[wallnum].nextsector;
		if (nextsector != -1)
			return setNull(walls, wallnum);

		SurfaceInfo info = tess.getSurface(Type.Wall.setHeinum(Heinum.MaxWall), wallnum, vertices);
		if (info == null)
			return setNull(walls, wallnum);

		surf = getSurface(walls, wallnum, info.getSize(), info.getLimit());
		if (surf != null) {
			surf.picnum = info.picnum;
			surf.ptr = info.obj;
			surf.type = Type.Wall;
			surf.vis_ptr = sectnum;

			surf.visflag = 0;
		}

		if (surf != null && surf.count == 0)
			return null;

		return surf;
	}

	private GLSurface addUpper(FloatArray vertices, int sectnum, int wallnum) {
		final int nextsector = wall[wallnum].nextsector;
		if (nextsector == -1 || (sector[nextsector].isParallaxCeiling() && sector[sectnum].isParallaxCeiling()))
			return setNull(upper_walls, wallnum);

		SurfaceInfo info = tess.getSurface(Type.Wall.setHeinum(Heinum.Upper), wallnum, vertices);
		if (info == null)
			return setNull(upper_walls, wallnum);

		GLSurface surf = getSurface(upper_walls, wallnum, info.getSize(), info.getLimit());
		if (surf != null) {
			surf.picnum = info.picnum;
			surf.ptr = info.obj;
			surf.type = Type.Wall;
			surf.vis_ptr = sectnum;

			surf.visflag = 2;
		}

		if (surf != null && surf.count == 0)
			return null;

		return surf;
	}

	private GLSurface addLower(FloatArray vertices, int sectnum, int wallnum) {
		final int nextsector = wall[wallnum].nextsector;
		if (nextsector == -1 || (sector[nextsector].isParallaxFloor() && sector[sectnum].isParallaxFloor()))
			return setNull(lower_walls, wallnum);

		SurfaceInfo info = tess.getSurface(Type.Wall.setHeinum(Heinum.Lower), wallnum, vertices);
		if (info == null)
			return setNull(lower_walls, wallnum);

		GLSurface surf = getSurface(lower_walls, wallnum, info.getSize(), info.getLimit());
		if (surf != null) {
			surf.picnum = info.picnum;
			surf.ptr = info.obj;
			surf.type = Type.Wall;
			surf.vis_ptr = sectnum;

			surf.visflag = 1;
		}

		if (surf != null && surf.count == 0)
			return null;

		return surf;
	}

	private GLSurface addMaskedWall(FloatArray vertices, int sectnum, int wallnum) {
		final WALL wal = wall[wallnum];
		GLSurface surf = null;

		if ((wal.isMasked() || wal.isOneWay()) && wal.nextsector != -1) {
			SurfaceInfo info = tess.getSurface(Type.Wall.setHeinum(Heinum.Portal), wallnum, vertices);
			if (info == null)
				return setNull(maskwalls, wallnum);

			surf = getSurface(maskwalls, wallnum, info.getSize(), info.getLimit());
			if (surf != null) {
				surf.picnum = info.picnum;
				surf.ptr = info.obj;
				surf.type = Type.Wall;
				surf.vis_ptr = sectnum;

				surf.method = 1;
				if (!wal.isOneWay() && wal.isTransparent()) {
					if (!wal.isTransparent2())
						surf.method = 2;
					else
						surf.method = 3;
				}
			}
		}

		if (surf != null && surf.count == 0)
			return null;

		return surf;
	}

	private GLSurface addFloor(FloatArray vertices, int sectnum) {
		if (sector[sectnum].isParallaxFloor())
			return setNull(floors, sectnum);

		SurfaceInfo info = tess.getSurface(Type.Floor, sectnum, vertices);
		if (info == null)
			return setNull(floors, sectnum);

		GLSurface surf = getSurface(floors, sectnum, info.getSize(), info.getLimit());
		if (surf != null) {
			surf.picnum = info.picnum;
			surf.ptr = info.obj;
			surf.type = Type.Floor;
			surf.vis_ptr = sectnum;
		}

		if (surf != null && surf.count == 0)
			return null;

		return surf;
	}

	private GLSurface addSkyPlane(FloatArray vertices) {
		float SIZEX = 1.0f / 2;
		float SIZEY = 1.0f / 2;

		float[] spr_vertices = { //
				-SIZEX, SIZEY, 0, 1, 1, //
				SIZEX, SIZEY, 0, 0, 1, //
				SIZEX, -SIZEY, 0, 0, 0, //
				-SIZEX, -SIZEY, 0, 1, 0, //
		};

		vertices.addAll(spr_vertices[0], spr_vertices[1], spr_vertices[2], spr_vertices[3], spr_vertices[4]); // 0
		vertices.addAll(spr_vertices[5], spr_vertices[6], spr_vertices[7], spr_vertices[8], spr_vertices[9]); // 1
		vertices.addAll(spr_vertices[10], spr_vertices[11], spr_vertices[12], spr_vertices[13], spr_vertices[14]); // 2

		vertices.addAll(spr_vertices[0], spr_vertices[1], spr_vertices[2], spr_vertices[3], spr_vertices[4]); // 0
		vertices.addAll(spr_vertices[10], spr_vertices[11], spr_vertices[12], spr_vertices[13], spr_vertices[14]); // 2
		vertices.addAll(spr_vertices[15], spr_vertices[16], spr_vertices[17], spr_vertices[18], spr_vertices[19]); // 3

		GLSurface surf = new GLSurface(meshOffset);
		surf.count = 6;
		surf.limit = 6;
		surf.type = Type.Sky;
		meshOffset += surf.limit;

		return surf;
	}

	private GLSurface addCeiling(FloatArray vertices, int sectnum) {
		if (sector[sectnum].isParallaxCeiling())
			return setNull(ceilings, sectnum);

		SurfaceInfo info = tess.getSurface(Type.Ceiling, sectnum, vertices);
		if (info == null)
			return setNull(ceilings, sectnum);

		GLSurface surf = getSurface(ceilings, sectnum, info.getSize(), info.getLimit());
		if (surf != null) {
			surf.picnum = info.picnum;
			surf.ptr = info.obj;
			surf.type = Type.Ceiling;
			surf.vis_ptr = sectnum;
		}

		if (surf != null && surf.count == 0)
			return null;

		return surf;
	}

	public GLSurface getWall(int wallnum, int sectnum) {
		int hash = getWallHash(sectnum, wallnum);

		if (wallhash[wallnum] != hash) {
			wallhash[wallnum] = hash;

			tess.setSector(sectnum, false);

			vertices.clear();
			GLSurface surf = addMiddle(vertices, sectnum, wallnum);
			if (surf != null)
				mesh.updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);

			vertices.clear();
			surf = addUpper(vertices, sectnum, wallnum);
			if (surf != null)
				mesh.updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);

			vertices.clear();
			surf = addLower(vertices, sectnum, wallnum);
			if (surf != null)
				mesh.updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);

			vertices.clear();
			surf = addMaskedWall(vertices, sectnum, wallnum);
			if (surf != null)
				mesh.updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);

			vertices.clear();
			surf = addParallaxCeiling(vertices, sectnum, wallnum);
			if (surf != null)
				mesh.updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);

			vertices.clear();
			surf = addParallaxFloor(vertices, sectnum, wallnum);
			if (surf != null)
				mesh.updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);

			checkValidate();
		}

		return walls[wallnum];
	}

	protected void checkValidate() {
		if (validateMesh) {
			FloatBuffer buffer = mesh.getVerticesBuffer();
			int newLimit = (meshOffset + tess.getMaxVertices()) * tess.getVertexSize();
			if (newLimit > buffer.capacity())
				newLimit = buffer.capacity();
			buffer.limit(newLimit);
			validateMesh = false;
		}
	}

	public GLSurface getUpper(int wallnum, int sectnum) {
		return upper_walls[wallnum];
	}

	public GLSurface getLower(int wallnum, int sectnum) {
		return lower_walls[wallnum];
	}

	public GLSurface getMaskedWall(int wallnum) {
		return maskwalls[wallnum];
	}

	public GLSurface getParallaxFloor(int wallnum) {
		return lower_skies[wallnum];
	}

	public GLSurface getParallaxCeiling(int wallnum) {
		return upper_skies[wallnum];
	}

	public GLSurface getSkyPlane() {
		return skyPlane;
	}

	public GLSurface getFloor(int sectnum) {
		int hash = getFloorHash(sectnum);
		GLSurface surf = floors[sectnum];
		if (floorhash[sectnum] != hash) {
			floorhash[sectnum] = hash;

			tess.setSector(sectnum, true);
			vertices.clear();
			surf = addFloor(vertices, sectnum);
			if (surf != null) {
				mesh.updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);
			}

			checkValidate();
		}

		return surf;
	}

	public GLSurface getCeiling(int sectnum) {
		int hash = getCeilingHash(sectnum);
		GLSurface surf = ceilings[sectnum];
		if (ceilinghash[sectnum] != hash) {
			ceilinghash[sectnum] = hash;

			tess.setSector(sectnum, true);
			vertices.clear();
			surf = addCeiling(vertices, sectnum);
			if (surf != null) {
				mesh.updateVertices(surf.offset * tess.getVertexSize(), vertices.items, 0, vertices.size);
			}

			checkValidate();
		}

		return surf;
	}

	private int getCeilingHash(int sectnum) {
		int hash = 1;
		final int prime = 31;
		final SECTOR sec = sector[sectnum];

		final int startwall = sec.wallptr;
		final int endwall = sec.wallnum + startwall;
		for (int z = startwall; z < endwall; z++) {
			WALL wal = wall[z];
			hash = prime * hash + wal.x;
			hash = prime * hash + wal.y;
		}

		hash = prime * hash + sec.ceilingz;
		hash = prime * hash + sec.ceilingstat;
		hash = prime * hash + sec.ceilingheinum;
		hash = prime * hash + sec.ceilingpicnum;
		hash = prime * hash + sec.ceilingxpanning;
		hash = prime * hash + sec.ceilingypanning;

		return hash;
	}

	private int getFloorHash(int sectnum) {
		int hash = 1;
		final int prime = 31;
		final SECTOR sec = sector[sectnum];

		final int startwall = sec.wallptr;
		final int endwall = sec.wallnum + startwall;
		for (int z = startwall; z < endwall; z++) {
			WALL wal = wall[z];
			hash = prime * hash + wal.x;
			hash = prime * hash + wal.y;
		}

		hash = prime * hash + sec.floorz;
		hash = prime * hash + sec.floorstat;
		hash = prime * hash + sec.floorheinum;
		hash = prime * hash + sec.floorpicnum;
		hash = prime * hash + sec.floorxpanning;
		hash = prime * hash + sec.floorypanning;

		return hash;
	}

	private int getWallHash(int sectnum, int z) {
		final SECTOR sec = sector[sectnum];
		final WALL wal = wall[z];

		int hash = 1;
		final int prime = 31;

		hash = prime * hash + wal.x;
		hash = prime * hash + wal.y;
		hash = prime * hash + wall[wal.point2].x;
		hash = prime * hash + wall[wal.point2].y;
		hash = prime * hash + wal.cstat;
		hash = prime * hash + wal.xpanning;
		hash = prime * hash + wal.ypanning;
		hash = prime * hash + wal.xrepeat;
		hash = prime * hash + wal.yrepeat;
		hash = prime * hash + wal.picnum; // upper texture
		hash = prime * hash + wal.overpicnum; // middle texture

		if (wal.isSwapped() && wal.nextwall != -1) {
			final WALL swal = wall[wal.nextwall];
			hash = prime * hash + swal.cstat;
			hash = prime * hash + swal.xpanning;
			hash = prime * hash + swal.ypanning;
			hash = prime * hash + swal.xrepeat;
			hash = prime * hash + swal.yrepeat;
			hash = prime * hash + swal.picnum;
		}

		hash = prime * hash + sec.floorz;
		hash = prime * hash + sec.floorheinum;
		hash = prime * hash + (sec.isSlopedFloor() ? 1 : 0);
		hash = prime * hash + (sec.isParallaxFloor() ? 1 : 0);

		hash = prime * hash + sec.ceilingz;
		hash = prime * hash + sec.ceilingheinum;
		hash = prime * hash + (sec.isSlopedCeiling() ? 1 : 0);
		hash = prime * hash + (sec.isParallaxCeiling() ? 1 : 0);

		if (wal.nextsector != -1) {
			final SECTOR nsec = sector[wal.nextsector];
			final WALL swal = wall[wal.nextwall];

			hash = prime * hash + swal.x; // TODO: Why does it need
			hash = prime * hash + swal.y;
			hash = prime * hash + wall[swal.point2].x;
			hash = prime * hash + wall[swal.point2].y;

			hash = prime * hash + nsec.floorz;
			hash = prime * hash + nsec.floorheinum;
			hash = prime * hash + (nsec.isSlopedFloor() ? 1 : 0);
			hash = prime * hash + (nsec.isParallaxFloor() ? 1 : 0);

			hash = prime * hash + nsec.ceilingz;
			hash = prime * hash + nsec.ceilingheinum;
			hash = prime * hash + (nsec.isSlopedCeiling() ? 1 : 0);
			hash = prime * hash + (nsec.isParallaxCeiling() ? 1 : 0);
		}

		return hash;
	}

	private GLSurface setNull(GLSurface[] array, int num) {
		GLSurface src = array[num];
		if (src != null) {
			src.count = 0;
		}
		return null;
	}

	private GLSurface getSurface(GLSurface[] array, int num, int count, int limit) {
		if (array[num] == null) {
			if (count == 0)
				return null;

			GLSurface surf = new GLSurface(meshOffset);
			surf.count = count;
			surf.limit = limit;
			meshOffset += surf.limit;
			array[num] = surf;
			if (lastSurf != null)
				lastSurf.next = surf;
			lastSurf = surf;
			validateMesh = true;

//			if (mesh != null)
//				System.err.println("new meshOffset: " + (meshOffset * tess.getVertexSize()) + " "
//						+ mesh.getVerticesBuffer().limit() + " size: " + mesh.getVerticesBuffer().capacity());
			return surf;
		} else if (array[num].limit < count) {
			int shift = count - array[num].limit;
			shiftFrom(array[num].next, shift);
			meshOffset += shift;
			array[num].limit = count;
		}

		array[num].count = count;
		return array[num];
	}

	public void nextpage() {
		tess.setSector(-1, false);
	}

	private void shiftFrom(GLSurface surf, int shift) {
		if (surf == null)
			return;

		int size = meshOffset;
		int newSize = size - surf.offset;
		float[] newItems = new float[newSize * tess.getVertexSize()];
		mesh.getVertices(surf.offset * tess.getVertexSize(), newItems);
		surf.offset += shift;
		validateMesh = true;
		mesh.updateVertices(surf.offset * tess.getVertexSize(), newItems, 0, newItems.length);

		surf = surf.next;
		while (surf != null) {
			surf.offset += shift;
			surf = surf.next;
		}
	}

	public class GLSurface {
		public int offset;
		public int count, limit;
		public int method = 0;
		public int visflag = 0; // 1 - lower, 2 - upper, 0 - white

		public int picnum;
		private Object ptr;
		private Type type;
		private int vis_ptr;

		protected GLSurface next;

		public GLSurface(int offset) {
			this.offset = offset;
		}

		public int getVisibility() {
			return sector[vis_ptr].visibility;
		}

		public void render(ShaderProgram shader) {
			mesh.render(shader, GL20.GL_TRIANGLES, offset, count);
		}

		public short getPal() {
			switch (type) {
			case Floor:
				return ((SECTOR) ptr).floorpal;
			case Ceiling:
				return ((SECTOR) ptr).ceilingpal;
			case Wall:
				return ((WALL) ptr).pal;
			default:
				return 0;
			}
		}

		public byte getShade() {
			switch (type) {
			case Floor:
				return ((SECTOR) ptr).floorshade;
			case Ceiling:
				return ((SECTOR) ptr).ceilingshade;
			case Wall:
				return ((WALL) ptr).shade;
			default:
				return 0;
			}
		}
	}
}
