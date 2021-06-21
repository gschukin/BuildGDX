// This file is part of BuildGDX.
// Copyright (C) 2017-2021  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Render.GdxRender;

import static com.badlogic.gdx.graphics.GL20.*;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.OnSceenDisplay.Console.OSDTEXT_GOLD;
import static ru.m210projects.Build.Pragmas.dmulscale;
import static ru.m210projects.Build.Pragmas.mulscale;
import static ru.m210projects.Build.Render.Types.GL10.GL_ALPHA_TEST;
import static ru.m210projects.Build.Render.Types.GL10.GL_MODELVIEW;
import static ru.m210projects.Build.Render.Types.GL10.GL_PROJECTION;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Gameutils;
import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Architecture.BuildApplication.Platform;
import ru.m210projects.Build.Architecture.BuildFrame.FrameType;
import ru.m210projects.Build.Loader.Model;
import ru.m210projects.Build.OnSceenDisplay.Console;
import ru.m210projects.Build.Render.GLInfo;
import ru.m210projects.Build.Render.GLRenderer;
import ru.m210projects.Build.Render.OrphoRenderer;
import ru.m210projects.Build.Render.GdxRender.Tesselator.Type;
import ru.m210projects.Build.Render.GdxRender.WorldMesh.GLSurface;
import ru.m210projects.Build.Render.GdxRender.WorldMesh.Heinum;
import ru.m210projects.Build.Render.TextureHandle.GLTile;
import ru.m210projects.Build.Render.TextureHandle.IndexedShader;
import ru.m210projects.Build.Render.TextureHandle.TextureManager;
import ru.m210projects.Build.Render.TextureHandle.TextureManager.ExpandTexture;
import ru.m210projects.Build.Render.TextureHandle.TileData;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Render.Types.FadeEffect;
import ru.m210projects.Build.Render.Types.GL10;
import ru.m210projects.Build.Render.Types.GLFilter;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Settings.GLSettings;
import ru.m210projects.Build.Types.SECTOR;
import ru.m210projects.Build.Types.SPRITE;
import ru.m210projects.Build.Types.Tile;
import ru.m210projects.Build.Types.TileFont;
import ru.m210projects.Build.Types.Timer;
import ru.m210projects.Build.Types.WALL;
import ru.m210projects.Build.Types.Tile.AnimType;
import ru.m210projects.Build.Render.GdxRender.Scanner.SectorScanner;
import ru.m210projects.Build.Render.GdxRender.Scanner.VisibleSector;

public class GDXRenderer implements GLRenderer {

//	TODO:
//	Sector update fps drops
//	SW textures bug
//	Palfade
//  Top / bottom transparent bug with glass maskedwall
//	ROR / Mirror bugs

//	Overheadmap
//	Scansectors memory leak (WallFrustum)
//	Maskwall sort
//	Orpho renderer 8bit textures
//	Drunk mode
//	Hires + models
//	Skyboxes
//	Sky texture

	protected TextureManager textureCache;
	protected final Engine engine;
	protected boolean isInited = false;
	protected GL20 gl;
	protected float defznear = 0.001f;
	protected float defzfar = 1.0f;
	protected int fov = 90;

	protected float gtang = 0.0f;

	protected WorldMesh world;
	protected SectorScanner scanner;
	protected BuildCamera cam;
	protected SpriteRenderer sprR;
	protected GdxOrphoRen orphoRen;
	protected DefScript defs;
	protected ShaderProgram skyshader;
	protected IndexedShader texshader;

	private ByteBuffer pix32buffer;
	private ByteBuffer pix8buffer;
	private long renderTime, scanTime;
	private Matrix4 transform = new Matrix4();
	private boolean isRORDrawing = false;
	private float glox1, gloy1, glox2, gloy2;

	public GDXRenderer(Engine engine) {
		if (BuildGdx.graphics.getFrameType() != FrameType.GL)
			BuildGdx.app.setFrame(FrameType.GL);
		GLInfo.init();
		this.engine = engine;
		this.textureCache = getTextureManager();
		this.texshader = allocIndexedShader();
		this.textureCache.changePalette(curpalette.getBytes());

		this.gl = BuildGdx.graphics.getGL20();
		this.sprR = new SpriteRenderer(engine, this);
		this.scanner = new SectorScanner(engine) {
			@Override
			protected Matrix4 getSpriteMatrix(SPRITE tspr) {
				return sprR.getMatrix(tspr);
			}
		};

		this.orphoRen = new GdxOrphoRen(engine, textureCache);
		Console.Println(BuildGdx.graphics.getGLVersion().getRendererString() + " " + gl.glGetString(GL_VERSION)
				+ " initialized", OSDTEXT_GOLD);
	}

	protected IndexedShader getTextureShader() {
		return texshader;
	}

	private IndexedShader allocIndexedShader() {
		try {
			FileInputStream fis = new FileInputStream(new File("worldshader_vert.glsl"));
			byte[] data = new byte[fis.available()];
			fis.read(data);
			String vert = new String(data);

			return new IndexedShader(vert, IndexedShader.defaultFragment) {
				@Override
				public void bindPalette() {
					textureCache.getPalette().bind();
				}

				@Override
				public void bindPalookup(int pal) {
					textureCache.getPalookup(pal).bind();
				}
			};
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private void createSkyShader() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(new File("skyshader_frag.glsl"));
			byte[] data = new byte[fis.available()];
			fis.read(data);
			String frag = new String(data);

			fis = new FileInputStream(new File("skyshader_vert.glsl"));
			data = new byte[fis.available()];
			fis.read(data);
			String vert = new String(data);

			skyshader = new ShaderProgram(vert, frag);
			if (!skyshader.isCompiled())
				System.err.println("Shader compile error: " + skyshader.getLog());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init() {
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glPixelStorei(GL_PACK_ALIGNMENT, 1);

		this.cam = new BuildCamera(fov, xdim, ydim, 512, 8192);

		createSkyShader();
		orphoRen.init();

		isInited = true;
	}

	@Override
	public void uninit() {
		orphoRen.uninit();

		textureCache.uninit();
	}

	@Override
	public RenderType getType() {
		return RenderType.RenderGDX;
	}

	@Override
	public PixelFormat getTexFormat() {
		return PixelFormat.Pal8;
	}

	@Override
	public boolean isInited() {
		return isInited;
	}

	private void drawMask(int w) {
		gl.glDepthFunc(GL20.GL_LESS);
		gl.glDepthRangef(0.0001f, 0.99999f);

		drawSurf(world.getMaskedWall(w), 0);

		gl.glDepthFunc(GL20.GL_LESS);
		gl.glDepthRangef(defznear, defzfar);
	}

	@Override
	public void drawmasks() {
		int maskwallcnt = scanner.getMaskwallCount();
		sprR.sort(scanner.getSprites(), spritesortcnt);

		while ((spritesortcnt > 0) && (maskwallcnt > 0)) { // While BOTH > 0
			int j = scanner.getMaskwalls()[maskwallcnt - 1];
			if (!spritewallfront(scanner.getSprites()[spritesortcnt - 1], j))
				drawsprite(--spritesortcnt);
			else {
				// Check to see if any sprites behind the masked wall...
				for (int i = spritesortcnt - 2; i >= 0; i--) {
					if (!spritewallfront(scanner.getSprites()[i], j)) {
						drawsprite(i);
						scanner.getSprites()[i] = null;
					}
				}
				// finally safe to draw the masked wall
				drawmaskwall(--maskwallcnt);
			}
		}

		while (spritesortcnt != 0) {
			spritesortcnt--;
			if (scanner.getSprites()[spritesortcnt] != null) {
				drawsprite(spritesortcnt);
			}
		}

		while (maskwallcnt > 0)
			drawmaskwall(--maskwallcnt);
	}

	public void drawsprite(int i) {
		sprR.begin(textureCache, cam);
		SPRITE tspr = scanner.getSprites()[i];
		if (tspr != null)
			sprR.draw(tspr);
		sprR.end();
	}

	private void drawmaskwall(int i) {
		drawMask(scanner.getMaskwalls()[i]);
	}

	private boolean spritewallfront(SPRITE s, int w) {
		if (s == null)
			return false;

		WALL wal = wall[w];
		int x1 = wal.x;
		int y1 = wal.y;
		wal = wall[wal.point2];
		return (dmulscale(wal.x - x1, s.y - y1, -(s.x - x1), wal.y - y1, 32) >= 0);
	}

	public void resizeglcheck() {
		if ((glox1 != windowx1) || (gloy1 != windowy1) || (glox2 != windowx2) || (gloy2 != windowy2)) {
			glox1 = windowx1;
			gloy1 = windowy1;
			glox2 = windowx2;
			gloy2 = windowy2;

			gl.glViewport(windowx1, ydim - (windowy2 + 1), windowx2 - windowx1 + 1, windowy2 - windowy1 + 1);

			cam.viewportWidth = windowx2;
			cam.viewportHeight = windowy2;

			orphoRen.resize(windowx2, windowy2);
		}
	}

	@Override
	public void drawrooms() {
		if (!isRORDrawing)
			gl.glClear(GL_DEPTH_BUFFER_BIT);
		else
			isRORDrawing = false;

//		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
//		gl.glClearColor(0.0f, 0.5f, 0.5f, 1); // XXX

		gl.glDisable(GL_BLEND);
		gl.glEnable(GL_TEXTURE_2D);
		gl.glEnable(GL_DEPTH_TEST);

		gl.glDepthFunc(GL_LESS);
		gl.glDepthRangef(defznear, defzfar);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CW);
		resizeglcheck();

		cam.setPosition(globalposx, globalposy, globalposz);
		cam.setDirection(globalang, globalhoriz, gtang);
		cam.update(true);

		globalvisibility = visibility << 2;
		if (globalcursectnum >= MAXSECTORS) {
			globalcursectnum -= MAXSECTORS;
			if (!inpreparemirror)
				isRORDrawing = true;
		} else {
			short i = globalcursectnum;
			globalcursectnum = engine.updatesectorz(globalposx, globalposy, globalposz, globalcursectnum);
			if (globalcursectnum < 0)
				globalcursectnum = i;
		}

		scanTime = System.nanoTime();
		ArrayList<VisibleSector> sectors = scanner.process(cam, world, globalcursectnum);
		scanTime = System.nanoTime() - scanTime;

		renderTime = System.nanoTime();

		texshader.begin();
		if (inpreparemirror) {
			inpreparemirror = false;
			gl.glCullFace(GL_FRONT);
			texshader.setUniformi("u_mirror", 1);
		} else {
			gl.glCullFace(GL_BACK);
			texshader.setUniformi("u_mirror", 0);
		}

		texshader.setUniformi("u_drawSprite", 0);
		texshader.setUniformMatrix("u_projTrans", cam.combined);
		texshader.setUniformMatrix("u_modelView", cam.view);
		texshader.setClip(0, 0, xdim, ydim);

		for (int i = 0; i < sectors.size(); i++)
			drawSector(sectors.get(i));
		for (int i = 0; i < sectors.size(); i++)
			drawSkySector(sectors.get(i));
		drawSkyPlanes();
		texshader.end();
		renderTime = System.nanoTime() - renderTime;

		spritesortcnt = scanner.getSpriteCount();
		tsprite = scanner.getSprites();
	}

	private void drawSkyPlanes() {
		gl.glDisable(GL_CULL_FACE);
		gl.glDepthMask(false);
//		gl.glDisable(GL_DEPTH_TEST);

		if (scanner.getSkyPicnum(Heinum.SkyUpper) != -1) {
			textureCache.bind(TileData.PixelFormat.Pal8, scanner.getSkyPicnum(Heinum.SkyUpper),
					scanner.getSkyPal(Heinum.SkyUpper), 0, 0, 0);
			transform.idt();
			transform.translate(cam.position.x, cam.position.y, cam.position.z - 100);
			transform.scale(cam.far, cam.far, 1.0f);

			skyshader.begin();
			skyshader.setUniformMatrix("u_transform", transform);
			world.getSkyPlane().render(skyshader);
			skyshader.end();
		}

		if (scanner.getSkyPicnum(Heinum.SkyLower) != -1) {
			textureCache.bind(TileData.PixelFormat.Pal8, scanner.getSkyPicnum(Heinum.SkyLower),
					scanner.getSkyPal(Heinum.SkyLower), 0, 0, 0);
			transform.idt();
			transform.translate(cam.position.x, cam.position.y, cam.position.z + 100);
			transform.scale(cam.far, cam.far, 1.0f);

			skyshader.begin();
			skyshader.setUniformMatrix("u_transform", transform);
			world.getSkyPlane().render(skyshader);
			skyshader.end();
		}
		transform.idt();

		gl.glEnable(GL_CULL_FACE);
		gl.glDepthMask(true);
//		gl.glEnable(GL_DEPTH_TEST);
	}

	private void drawSector(VisibleSector sec) {
		int sectnum = sec.index;
		gotsector[sectnum >> 3] |= pow2char[sectnum & 7];

		if ((sec.secflags & 1) != 0)
			drawSurf(world.getFloor(sectnum), 0);

		if ((sec.secflags & 2) != 0)
			drawSurf(world.getCeiling(sectnum), 0);

		for (int w = 0; w < sec.walls.size; w++) {
			int flags = sec.wallflags.get(w);
			int z = sec.walls.get(w);
			drawSurf(world.getWall(z, sectnum), flags);
			drawSurf(world.getUpper(z, sectnum), flags);
			drawSurf(world.getLower(z, sectnum), flags);
		}
	}

	public void drawSkySector(VisibleSector sec) {
		for (int w = 0; w < sec.skywalls.size; w++) {
			int z = sec.skywalls.get(w);
			GLSurface ceil = world.getParallaxCeiling(z);
			if (ceil != null) {
				drawSky(ceil, ceil.picnum, ceil.getPal(), ceil.getMethod());
			}

			GLSurface floor = world.getParallaxFloor(z);
			if (floor != null) {
				drawSky(floor, floor.picnum, floor.getPal(), floor.getMethod());
			}
		}
	}

	private void drawSky(GLSurface surf, int picnum, int palnum, int method) {
		if (surf.count == 0)
			return;

		if (engine.getTile(picnum).getType() != AnimType.None)
			picnum += engine.animateoffs(picnum, 0);

		Tile pic = engine.getTile(picnum);
		if (!pic.isLoaded())
			engine.loadtile(picnum);

		engine.setgotpic(picnum);
		GLTile pth = textureCache.bind(TileData.PixelFormat.Pal8, picnum, palnum, 0, 0, method);
		if (pth != null) {
			skyshader.begin();
			gl.glActiveTexture(GL20.GL_TEXTURE1);
			textureCache.getPalette().bind();
			skyshader.setUniformi("u_palette", 1);

			gl.glActiveTexture(GL20.GL_TEXTURE2);
			textureCache.getPalookup(palnum).bind();
			skyshader.setUniformi("u_palookup", 2);
			gl.glActiveTexture(GL20.GL_TEXTURE0);

			skyshader.setUniformf("u_camera", cam.position.x, cam.position.y, cam.position.z);
			skyshader.setUniformMatrix("u_projTrans", cam.combined);
			skyshader.setUniformMatrix("u_transform", transform);

			if (!pic.isLoaded()) {
				skyshader.setUniformf("u_alpha", 0.01f);
				method = 1;
			} else
				skyshader.setUniformf("u_alpha", 1.0f);

			if ((method & 3) == 0) {
				gl.glDisable(GL_BLEND);
				gl.glDisable(GL_ALPHA_TEST);
			} else {
				gl.glEnable(GL_BLEND);
				gl.glEnable(GL_ALPHA_TEST);
			}

			surf.render(skyshader);
			skyshader.end();
		}
	}

	private void drawSurf(GLSurface surf, int flags) {
		if (surf == null)
			return;

		if (surf.count != 0 && (flags == 0 || (surf.visflag & flags) != 0)) {
			int picnum = surf.picnum;

			if (engine.getTile(picnum).getType() != AnimType.None)
				picnum += engine.animateoffs(picnum, 0);

			Tile pic = engine.getTile(picnum);
			if (!pic.isLoaded())
				engine.loadtile(picnum);

			int method = surf.getMethod();
			if (!pic.isLoaded())
				method = 1; // invalid data, HOM

			engine.setgotpic(picnum);
			GLTile pth = textureCache.bind(PixelFormat.Pal8, picnum, surf.getPal(), surf.getShade(), 0, method);
			if (pth != null) {
				int combvis = globalvisibility;
				int vis = surf.getVisibility();
				if (vis != 0)
					combvis = mulscale(globalvisibility, (vis + 16) & 0xFF, 4);
				texshader.setVisibility((int) (-combvis / 64.0f));

				if ((method & 3) == 0) {
					Gdx.gl.glDisable(GL_BLEND);
					Gdx.gl.glDisable(GL_ALPHA_TEST);
				} else {
					Gdx.gl.glEnable(GL_BLEND);
					Gdx.gl.glEnable(GL_ALPHA_TEST);
				}

				surf.render(texshader);
			}
		}
	}

	@Override
	public void clearview(int dacol) {
		gl.glClearColor(curpalette.getRed(dacol) / 255.0f, //
				curpalette.getGreen(dacol) / 255.0f, //
				curpalette.getBlue(dacol) / 255.0f, 0); //
		gl.glClear(GL_COLOR_BUFFER_BIT);
	}

	@Override
	public void changepalette(byte[] palette) {
		textureCache.changePalette(palette);
	}

	@Override
	public void nextpage() {
		if (world != null)
			world.nextpage();

//		orphoRen.palfade(null); // XXX

		orphoRen.nextpage();

		// showTimers();

		beforedrawrooms = 1;
	}

	private void showTimers() {
		float scan = scanTime / 1000000.0f;
		float rend = renderTime / 1000000.0f;

		System.out.println("ScanTime: " + scan + "ms");
		System.out.println("RenderTime: " + rend + "ms");
	}

	@Override
	public void rotatesprite(int sx, int sy, int z, int a, int picnum, int dashade, int dapalnum, int dastat, int cx1,
			int cy1, int cx2, int cy2) {
		orphoRen.rotatesprite(sx, sy, z, a, picnum, dashade, dapalnum, dastat, cx1, cy1, cx2, cy2);
	}

	@Override
	public void drawmapview(int dax, int day, int zoome, int ang) {
		orphoRen.drawmapview(dax, day, zoome, ang);
	}

	@Override
	public void drawoverheadmap(int cposx, int cposy, int czoom, short cang) {
		orphoRen.drawoverheadmap(cposx, cposy, czoom, cang);
	}

	@Override
	public void printext(TileFont font, int xpos, int ypos, char[] text, int col, int shade, Transparent bit,
			float scale) {
		orphoRen.printext(font, xpos, ypos, text, col, shade, bit, scale);
	}

	@Override
	public void printext(int xpos, int ypos, int col, int backcol, char[] text, int fontsize, float scale) {
		orphoRen.printext(xpos, ypos, col, backcol, text, fontsize, scale);
	}

	@Override
	public ByteBuffer getFrame(PixelFormat format, int xsiz, int ysiz) {
		if (pix32buffer != null)
			pix32buffer.clear();

		boolean reverse = false;
		if (ysiz < 0) {
			ysiz *= -1;
			reverse = true;
		}

		int byteperpixel = 3;
		int fmt = GL10.GL_RGB;
		if (BuildGdx.app.getPlatform() == Platform.Android) {
			byteperpixel = 4;
			fmt = GL10.GL_RGBA;
		}

		if (pix32buffer == null || pix32buffer.capacity() < xsiz * ysiz * byteperpixel)
			pix32buffer = BufferUtils.newByteBuffer(xsiz * ysiz * byteperpixel);
		gl.glPixelStorei(GL10.GL_PACK_ALIGNMENT, 1);
		gl.glReadPixels(0, ydim - ysiz, xsiz, ysiz, fmt, GL10.GL_UNSIGNED_BYTE, pix32buffer);

		if (format == PixelFormat.Rgb) {
			if (reverse) {
				int b1, b2 = 0;
				for (int p, x, y = 0; y < ysiz / 2; y++) {
					b1 = byteperpixel * (ysiz - y - 1) * xsiz;
					for (x = 0; x < xsiz; x++) {
						for (p = 0; p < byteperpixel; p++) {
							byte tmp = pix32buffer.get(b1 + p);
							pix32buffer.put(b1 + p, pix32buffer.get(b2 + p));
							pix32buffer.put(b2 + p, tmp);
						}
						b1 += byteperpixel;
						b2 += byteperpixel;
					}
				}
			}
			pix32buffer.rewind();
			return pix32buffer;
		} else if (format == PixelFormat.Pal8) {
			if (pix8buffer != null)
				pix8buffer.clear();
			if (pix8buffer == null || pix8buffer.capacity() < xsiz * ysiz)
				pix8buffer = BufferUtils.newByteBuffer(xsiz * ysiz);

			int base = 0, r, g, b;
			if (reverse) {
				for (int x, y = 0; y < ysiz; y++) {
					base = byteperpixel * (ysiz - y - 1) * xsiz;
					for (x = 0; x < xsiz; x++) {
						r = (pix32buffer.get(base++) & 0xFF) >> 2;
						g = (pix32buffer.get(base++) & 0xFF) >> 2;
						b = (pix32buffer.get(base++) & 0xFF) >> 2;
						pix8buffer.put(engine.getclosestcol(palette, r, g, b));
					}
				}
			} else {
				for (int i = 0; i < pix8buffer.capacity(); i++) {
					r = (pix32buffer.get(base++) & 0xFF) >> 2;
					g = (pix32buffer.get(base++) & 0xFF) >> 2;
					b = (pix32buffer.get(base++) & 0xFF) >> 2;
					if (byteperpixel == 4)
						base++; // Android
					pix8buffer.put(engine.getclosestcol(palette, r, g, b));
				}
			}

			pix8buffer.rewind();
			return pix8buffer;
		}

		return null;
	}

	@Override
	public void drawline256(int x1, int y1, int x2, int y2, int col) {
		orphoRen.drawline256(x1, y1, x2, y2, col);
	}

	@Override
	public void settiltang(int tilt) {
		if (tilt == 0)
			gtang = 0.0f;
		else
			gtang = (float) Gameutils.AngleToDegrees(tilt);
	}

	@Override
	public void setDefs(DefScript defs) {
		this.textureCache.setTextureInfo(defs != null ? defs.texInfo : null);
		if (this.defs != null)
			gltexinvalidateall();
		this.defs = defs;
	}

	@Override
	public TextureManager getTextureManager() {
		if (textureCache == null) {
			textureCache = new TextureManager(engine, ExpandTexture.Vertical) {

				@Override
				public void setTextureParameters(GLTile tile, int tilenum, int pal, int shade, int skybox, int method) {
					if (tile.getPixelFormat() == TileData.PixelFormat.Pal8) {
						if (!texshader.isBinded()) {
							BuildGdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
							texshader.begin();
						}
						texshader.setTextureParams(pal, shade);

						float alpha = 1.0f;
						switch (method & 3) {
						case 2:
							alpha = TRANSLUSCENT1;
							break;
						case 3:
							alpha = TRANSLUSCENT2;
							break;
						}

						if (!engine.getTile(tilenum).isLoaded())
							alpha = 0.01f; // Hack to update Z-buffer for invalid mirror textures

						texshader.setDrawLastIndex((method & 3) == 0 || !textureCache.alphaMode(method));
						texshader.setTransparent(alpha);
					}
				}
			};
		}
		return textureCache;
	}

	@Override
	public void enableShader(boolean enable) {
		// XXX
	}

	@Override
	public void palfade(HashMap<String, FadeEffect> fades) {
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDisable(GL_ALPHA_TEST);
		gl.glDisable(GL_TEXTURE_2D);

		gl.glEnable(GL_BLEND);

	}

	@Override
	public void preload() {
		// TODO Auto-generated method stub
		world = new WorldMesh(engine);
		scanner.init();
	}

	@Override
	public void precache(int dapicnum, int dapalnum, int datype) {
		if ((palookup[dapalnum] == null) && (dapalnum < (MAXPALOOKUPS - RESERVEDPALS)))
			return;

		textureCache.bind(TileData.PixelFormat.Pal8, dapicnum, dapalnum, 0, 0, (datype & 1) << 2); // XXX
	}

	@Override
	public void gltexapplyprops() {
		GLFilter filter = GLSettings.textureFilter.get();
		textureCache.setFilter(filter);

		if (defs == null)
			return;

		int anisotropy = GLSettings.textureAnisotropy.get();
		for (int i = MAXTILES - 1; i >= 0; i--) {
			Model m = defs.mdInfo.getModel(i);
			if (m != null) {
				Iterator<GLTile[]> it = m.getSkins();
				while (it.hasNext()) {
					for (GLTile tex : it.next()) {
						if (tex == null)
							continue;

						textureCache.bind(tex);
						tex.setupTextureFilter(filter, anisotropy);
					}
				}
			}
		}
	}

	@Override
	public void gltexinvalidateall(GLInvalidateFlag... flags) {
		for (int i = 0; i < flags.length; i++) {
			switch (flags[i]) {
			case Uninit:
				textureCache.uninit();
				break;
			case SkinsOnly:
//				clearskins(true); XXX
				break;
			case TexturesOnly:
			case IndexedTexturesOnly:
				textureCache.invalidateall();
				break;
			case Palookup:
				for (int j = 0; j < MAXPALOOKUPS; j++) {
					if (texshader != null)
						textureCache.invalidatepalookup(j);
				}
				break;
			case All:
				textureCache.invalidateall();
				break;
			}
		}
	}

	@Override
	public void gltexinvalidate(int dapicnum, int dapalnum, int dameth) {
		textureCache.invalidate(dapicnum, dapalnum, textureCache.clampingMode(dameth));
	}

	@Override
	public void setdrunk(float intensive) {
		// TODO Auto-generated method stub

	}

	@Override
	public float getdrunk() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void addSpriteCorr(int snum) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeSpriteCorr(int snum) {
		// TODO Auto-generated method stub

	}

	@Override
	public void completemirror() {
		/* nothing */ }
}
