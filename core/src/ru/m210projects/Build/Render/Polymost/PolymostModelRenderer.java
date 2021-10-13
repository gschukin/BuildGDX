/*
 * "POLYMOST" code originally written by Ken Silverman
 * Ken Silverman's official web site: "http://www.advsys.net/ken"
 * See the included license file "BUILDLIC.TXT" for license info.
 *
 * This file has been modified from Ken Silverman's original release
 * by Jonathon Fowler (jf@jonof.id.au)
 * by the EDuke32 team (development@voidpoint.com)
 * by Alexander Makarov-[M210] (m210-2007@mail.ru)
 */
package ru.m210projects.Build.Render.Polymost;

import static com.badlogic.gdx.graphics.GL20.GL_BACK;
import static com.badlogic.gdx.graphics.GL20.GL_BLEND;
import static com.badlogic.gdx.graphics.GL20.GL_CCW;
import static com.badlogic.gdx.graphics.GL20.GL_CULL_FACE;
import static com.badlogic.gdx.graphics.GL20.GL_CW;
import static com.badlogic.gdx.graphics.GL20.GL_FRONT;
import static com.badlogic.gdx.graphics.GL20.GL_GREATER;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static ru.m210projects.Build.Engine.MAXPALOOKUPS;
import static ru.m210projects.Build.Engine.MAXSPRITES;
import static ru.m210projects.Build.Engine.TRANSLUSCENT1;
import static ru.m210projects.Build.Engine.TRANSLUSCENT2;
import static ru.m210projects.Build.Engine.globalang;
import static ru.m210projects.Build.Engine.globalpal;
import static ru.m210projects.Build.Engine.globalposx;
import static ru.m210projects.Build.Engine.globalposy;
import static ru.m210projects.Build.Engine.globalposz;
import static ru.m210projects.Build.Engine.globalshade;
import static ru.m210projects.Build.Engine.globalvisibility;
import static ru.m210projects.Build.Engine.numshades;
import static ru.m210projects.Build.Engine.pitch;
import static ru.m210projects.Build.Engine.sector;
import static ru.m210projects.Build.Engine.sintable;
import static ru.m210projects.Build.Engine.sprite;
import static ru.m210projects.Build.Engine.totalclock;
import static ru.m210projects.Build.Engine.viewingrange;
import static ru.m210projects.Build.Engine.xdimen;
import static ru.m210projects.Build.Render.Types.GL10.GL_ALPHA_TEST;
import static ru.m210projects.Build.Render.Types.GL10.GL_MODELVIEW;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Render.ModelHandle.GLModel;
import ru.m210projects.Build.Render.ModelHandle.Model.Type;
import ru.m210projects.Build.Render.ModelHandle.MDModel.MDModel;
import ru.m210projects.Build.Render.ModelHandle.MDModel.MD3.MD3ModelGL10;
import ru.m210projects.Build.Render.ModelHandle.Voxel.GLVoxel;
import ru.m210projects.Build.Render.Types.GL10;
import ru.m210projects.Build.Render.Types.Palette;
import ru.m210projects.Build.Render.Types.Spriteext;
import ru.m210projects.Build.Script.DefScript;
import ru.m210projects.Build.Types.SPRITE;
import ru.m210projects.Build.Types.Tile;

public class PolymostModelRenderer {

	private Polymost parent;
	private Engine engine;
	private GL10 gl;
	private final Color polyColor = new Color();
	private final float[][] matrix = new float[4][4];

	private final Vector3 dvoxm0 = new Vector3(), modela0 = new Vector3();

	private Vector3 cScale = new Vector3();
	private Vector3 nScale = new Vector3();

	public PolymostModelRenderer(Polymost parent) {
		this.parent = parent;
		this.engine = parent.engine;
	}

	public void init() {
		this.gl = parent.gl;
	}

	public int voxdraw(GLVoxel m, SPRITE tspr) {
		float f, g;

		if (m == null)
			return 0;

		if ((sprite[tspr.owner].cstat & 48) == 32)
			return 0;

		int globalorientation = parent.globalorientation;

		boolean xflip = (globalorientation & 4) != 0;
		boolean yflip = (globalorientation & 8) != 0;

		dvoxm0.x = m.getScale();
		dvoxm0.y = m.getScale();
		dvoxm0.z = m.getScale();
		modela0.x = modela0.y = modela0.z = 0;

		f = (tspr.xrepeat) * (256.0f / 320.0f) / 64.0f;
		if ((sprite[tspr.owner].cstat & 48) == 16)
			f *= 1.25f;
		if ((sprite[tspr.owner].cstat & 48) == 32)
			f *= 1.25f;

		dvoxm0.x *= f;
		modela0.x *= f;
		f = -f;
		dvoxm0.y *= f;
		modela0.y *= f;
		f = (tspr.yrepeat) / 64.0f;
		dvoxm0.z *= f;
		modela0.z *= f;

		float x0 = tspr.x;
		float k0 = tspr.z;
		float xoff = tspr.xoffset;
		float yoff = tspr.yoffset;

		xflip = (globalorientation & 4) != 0;
		if (yflip = (globalorientation & 8) != 0)
			yoff = -yoff;

		if ((globalorientation & 128) == 0)
			// k0 -= (engine.getTile(tspr.picnum).getHeight() * tspr.yrepeat) << 1; GDX this
			// more correct, but disabled for compatible with eduke
			k0 -= ((m.zsiz * tspr.yrepeat) << 1);

		if (yflip && (globalorientation & 16) == 0)
			k0 += ((engine.getTile(tspr.picnum).getHeight() * 0.5f) - m.zpiv) * tspr.yrepeat * 8.0f;

		f = (65536.0f * 512.0f) / (xdimen * viewingrange);
		g = 32.0f / (float) (xdimen * parent.gxyaspect);

		// x0 += xoff * (tspr.xrepeat >> 2);
		// k0 -= yoff * (tspr.yrepeat << 2);

		dvoxm0.y *= f;
		if ((sprite[tspr.owner].cstat & 48) == 32)
			dvoxm0.y *= -1;
		modela0.y = ((x0 - globalposx) / 1024.0f + modela0.y) * f;
		dvoxm0.x *= -f;
		if ((sprite[tspr.owner].cstat & 48) == 32)
			dvoxm0.x *= -1;
		if (xflip)
			dvoxm0.x *= -1;
		modela0.x = ((tspr.y - globalposy) / -1024.0f + modela0.x) * -f;
		dvoxm0.z *= g;
		if (yflip)
			dvoxm0.z *= -1;
		modela0.z = ((k0 - globalposz) / -16384.0f + modela0.z) * g;

		if ((parent.grhalfxdown10x >= 0) ^ yflip ^ xflip)
			gl.glFrontFace(GL_CW);
		else
			gl.glFrontFace(GL_CCW);

		gl.glEnable(GL_CULL_FACE);
		gl.glCullFace(GL_BACK);

		gl.glEnable(GL_TEXTURE_2D);

		gl.glMatrixMode(GL_MODELVIEW); // Let OpenGL (and perhaps hardware :) handle the matrix rotation
		boolean newmatrix = false;

		// ------------ Matrix
		if (!newmatrix)
			md3_vox_calcmat_common(tspr, modela0, f, matrix);
		else {
			md3_vox_calcmat_common(tspr, modela0);
		}

		if (!newmatrix) {
			matrix[0][3] = matrix[1][3] = matrix[2][3] = 0.f;
			matrix[3][3] = 1.f;
			gl.glLoadMatrixf(matrix);
		}

		gl.glScalef(dvoxm0.x / 64.0f, dvoxm0.z / 64.0f, dvoxm0.y / 64.0f);
		gl.glTranslatef(-xoff, yoff, 0);

		if (m.isRotating())
			gl.glRotatef(totalclock % 360, 0, 1, 0);

		// transform to Build coords
		if ((tspr.cstat & 48) == 32) {
			gl.glRotatef(90, 1.0f, 0.0f, 0.0f);
			gl.glTranslatef(-m.xpiv, -m.ypiv, -m.zpiv);
			gl.glRotatef(90, -1.0f, 0.0f, 0.0f);
			gl.glTranslatef(0, -m.ypiv, -m.zpiv);
		} else {
			gl.glRotatef(90, 1.0f, 0.0f, 0.0f);
			gl.glTranslatef(-m.xpiv, -m.ypiv, -m.zpiv);
		}

		parent.globalfog.apply();
		polyColor.r = polyColor.g = polyColor.b = (numshades
				- min(max((globalshade * parent.shadescale)/* + m.shadeoff */, 0), numshades)) / (numshades);

		if (parent.defs != null) {
			Palette p = parent.defs.texInfo.getTints(globalpal);
			polyColor.r *= p.r / 255.0f;
			polyColor.g *= p.g / 255.0f;
			polyColor.b *= p.b / 255.0f;
		}

		if ((tspr.cstat & 2) != 0) {
			if ((tspr.cstat & 512) == 0)
				polyColor.a = TRANSLUSCENT1;
			else
				polyColor.a = TRANSLUSCENT2;
		} else
			polyColor.a = 1.0f;
		m.setColor(polyColor.r, polyColor.g, polyColor.b, polyColor.a);
		boolean rendered = m.render(parent.getShader(), globalpal, globalshade, 0, (int) (parent.globalfog.combvis),
				polyColor.a);

		// ------------
		gl.glDisable(GL_CULL_FACE);
		gl.glLoadIdentity();

		return rendered ? 1 : 0;
	}

	private void modelPrepare(MDModel m, SPRITE tspr, int xoff, int yoff) {
		Tile pic = engine.getTile(tspr.picnum);
		float f = m.interpol;
		float g = 1 - f;

		if (m.getType() == Type.Md2)
			cScale.set(m.getScale(), m.getScale(), m.getScale());
		else
			cScale.set(m.getScale(), -m.getScale(), m.getScale());
		cScale.scl(g, g, g);

		if (m.getType() == Type.Md2)
			nScale.set(m.getScale(), m.getScale(), m.getScale());
		else
			nScale.set(m.getScale(), -m.getScale(), m.getScale());
		nScale.scl(f, f, f);

		int globalorientation = parent.globalorientation;

		modela0.x = modela0.y = 0;
		modela0.z = ((globalorientation & 8) != 0 ? -m.getYOffset(true) : m.getYOffset(true)) * m.getScale();
		float x0 = tspr.x;
		float k0 = tspr.z;
		if ((globalorientation & 128) != 0 && (globalorientation & 48) != 32)
			k0 += (pic.getHeight() * tspr.yrepeat) << 1;

		// Parkar: Changed to use the same method as centeroriented sprites
		if ((globalorientation & 8) != 0) // y-flipping
		{
			yoff = -yoff;
			cScale.scl(1, 1, -1);
			nScale.scl(1, 1, -1);
			modela0.z = -modela0.z;
			k0 -= (pic.getHeight() * tspr.yrepeat) << 2;
		}

		if ((globalorientation & 4) != 0) // x-flipping
		{
			xoff = -xoff;
			cScale.scl(1, -1, 1);
			nScale.scl(1, -1, 1);
			modela0.y = -modela0.y;
		}
		x0 += xoff * (tspr.xrepeat >> 2);
		k0 -= ((yoff * tspr.yrepeat) << 2);

		// yoffset differs from zadd in that it does not follow cstat&8 y-flipping
		modela0.z += m.getYOffset(false) * m.getScale();
		f = (tspr.xrepeat) / 64.0f * m.getBScale();
		float z = (tspr.yrepeat) / 64.0f * m.getBScale();
		cScale.scl(-f, f, z);
		nScale.scl(-f, f, z);
		modela0.scl(f, -f, z);

		// floor aligned
		float k1 = tspr.y;
		if ((globalorientation & 48) == 32) {
			cScale.scl(1, -1, -1);
			nScale.scl(1, -1, -1);
			modela0.scl(1, -1, -1);
			f = modela0.x;
			modela0.x = modela0.z;
			modela0.z = f;
			k1 += (pic.getHeight() * tspr.yrepeat) >> 3;
		}

		f = (65536.0f * 512.0f) / (xdimen * viewingrange);
		g = (float) (32.0f / (xdimen * parent.gxyaspect));
		cScale.scl(f, -f, g);
		nScale.scl(f, -f, g);

		modela0.y = ((x0 - globalposx) / 1024.0f + modela0.y) * f;
		modela0.x = ((k1 - globalposy) / 1024.0f + modela0.x) * f;
		modela0.z = ((k0 - globalposz) / -16384.0f + modela0.z) * g;

//    	md3_vox_calcmat_common(tspr, dvoxa0);
		md3_vox_calcmat_common(tspr, modela0, f, matrix);

		// floor aligned
		if ((globalorientation & 48) == 32) {
			f = matrix[1][0];
			matrix[1][0] = matrix[2][0] * 16.0f;
			matrix[2][0] = -f * (1.0f / 16.0f);
			f = matrix[1][1];
			matrix[1][1] = matrix[2][1] * 16.0f;
			matrix[2][1] = -f * (1.0f / 16.0f);
			f = matrix[1][2];
			matrix[1][2] = matrix[2][2] * 16.0f;
			matrix[2][2] = -f * (1.0f / 16.0f);
		}

		matrix[0][3] = matrix[1][3] = matrix[2][3] = 0.f;
		matrix[3][3] = 1.f;

		gl.glMatrixMode(GL_MODELVIEW); // Let OpenGL (and perhaps hardware :) handle the matrix rotation
		gl.glLoadMatrixf(matrix);
		gl.glRotatef(-90, 0.0f, 1.0f, 0.0f);

		if (m.isRotating())
			gl.glRotatef(totalclock % 360, 0, 1, 0);

		if ((parent.grhalfxdown10x >= 0) ^ ((globalorientation & 8) != 0) ^ ((globalorientation & 4) != 0))
			gl.glFrontFace(GL_CW);
		else
			gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_CULL_FACE);
		if (m.getType() == Type.Md2)
			gl.glCullFace(GL_FRONT);
		else
			gl.glCullFace(GL_BACK);

		gl.glEnable(GL_TEXTURE_2D);

		polyColor.r = polyColor.g = polyColor.b = (numshades
				- min(max((globalshade * parent.shadescale) + m.getShadeOff(), 0), numshades)) / (numshades);

		DefScript defs = parent.defs;
		if (defs != null) {
			if (m.isTintAffected()
					|| (!(tspr.owner >= MAXSPRITES) && sector[sprite[tspr.owner].sectnum].floorpal != 0)) {
				Palette p = defs.texInfo.getTints(globalpal);
				polyColor.r *= p.r / 255.0f;
				polyColor.g *= p.g / 255.0f;
				polyColor.b *= p.b / 255.0f;

				Palette pdetail = defs.texInfo.getTints(MAXPALOOKUPS - 1);
				if (pdetail.r != 255 || pdetail.g != 255 || pdetail.b != 255) {
					polyColor.r *= pdetail.r / 255.0f;
					polyColor.g *= pdetail.g / 255.0f;
					polyColor.b *= pdetail.b / 255.0f;
				}
			}
		}

		if ((tspr.cstat & 2) != 0) {
			if ((tspr.cstat & 512) == 0) {
				polyColor.a = TRANSLUSCENT1;
			} else {
				polyColor.a = TRANSLUSCENT2;
			}
		} else
			polyColor.a = 1.0f;

		if (m.usesalpha) // Sprites with alpha in texture
		{
			float al = 0.0f;
			if (parent.alphahackarray[tspr.picnum] != 0)
				al = parent.alphahackarray[tspr.picnum];
			gl.glEnable(GL_BLEND);
			gl.glEnable(GL_ALPHA_TEST);
			gl.glAlphaFunc(GL_GREATER, al);
		} else if ((tspr.cstat & 2) != 0)
			gl.glEnable(GL_BLEND);

		gl.glColor4f(polyColor.r, polyColor.g, polyColor.b, polyColor.a);
	}

//	public int md2draw(MD2Model m, SPRITE tspr, int xoff, int yoff) {
//		DefScript defs = parent.defs;
//
//		m.updateanimation(defs, tspr);
//
//		modelPrepare(m, tspr, xoff, yoff);
//
//		int rendered = 0, skinnum = defs.mdInfo.getParams(tspr.picnum).skinnum;
//		GLTile texid = m.loadskin(textureCache, defs, skinnum, globalpal, 0);
//		if (texid != null) {
//			textureCache.bind(texid);
//			if (Console.Geti("r_detailmapping") != 0)
//				texid = m.loadskin(textureCache, defs, skinnum, DETAILPAL, 0);
//			else
//				texid = null;
//
//			int texunits = GL_TEXTURE0;
//			if (texid != null) {
//				BuildGdx.gl.glActiveTexture(++texunits);
//				BuildGdx.gl.glEnable(GL_TEXTURE_2D);
//				parent.setupTextureDetail(texid);
//
//				MDSkinmap sk = m.getSkin(DETAILPAL, skinnum, 0);
//				if (sk != null) {
//					float f = sk.param;
//					gl.glMatrixMode(GL_TEXTURE);
//					gl.glLoadIdentity();
//					gl.glScalef(f, f, 1.0f);
//					gl.glMatrixMode(GL_MODELVIEW);
//				}
//			}
//
//			if (Console.Geti("r_glowmapping") != 0)
//				texid = m.loadskin(textureCache, defs, skinnum, GLOWPAL, 0);
//			else
//				texid = null;
//
//			if (texid != null) {
//				BuildGdx.gl.glActiveTexture(++texunits);
//				BuildGdx.gl.glEnable(GL_TEXTURE_2D);
//				parent.setupTextureGlow(texid);
//			}
//
//			MD2Frame cframe = m.frames[m.cframe], nframe = m.frames[m.nframe];
//
//			parent.globalfog.apply();
//			if (r_vertexarrays != 0) {
//				m.verticesBuffer.clear();
//				for (int i = 0; i < m.tris.length; i++) // -60fps, but it's need for animation
//					for (int j = 0; j < 3; j++) {
//						int idx = m.tris[i].vertices[j];
//						float x = cframe.vertices[idx][0] * cScale.x + nframe.vertices[idx][0] * nScale.x;
//						float y = cframe.vertices[idx][1] * cScale.y + nframe.vertices[idx][1] * nScale.y;
//						float z = cframe.vertices[idx][2] * cScale.z + nframe.vertices[idx][2] * nScale.z;
//						m.verticesBuffer.put(x);
//						m.verticesBuffer.put(z);
//						m.verticesBuffer.put(y);
//					}
//				m.verticesBuffer.flip();
//
//				int l = GL_TEXTURE0;
//				do {
//					gl.glClientActiveTexture(l++);
//					gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
//					gl.glTexCoordPointer(2, GL_FLOAT, 0, m.uv);
//				} while (l <= texunits);
//
//				gl.glEnableClientState(GL_VERTEX_ARRAY);
//				gl.glVertexPointer(3, GL_FLOAT, 0, m.verticesBuffer);
//				gl.glDrawElements(GL_TRIANGLES, 0, GL_UNSIGNED_SHORT, m.indicesBuffer);
//			} else {
//				int c = 0, cmd;
//				while ((cmd = m.glcmds[c++]) != 0) {
//					if (cmd < 0) {
//						gl.glBegin(GL_TRIANGLE_FAN);
//						cmd = -cmd;
//					} else
//						gl.glBegin(GL_TRIANGLE_STRIP);
//
//					for ( /* nothing */; cmd > 0; cmd--, c += 3) {
//						float s = Float.intBitsToFloat(m.glcmds[c + 0]);
//						float t = Float.intBitsToFloat(m.glcmds[c + 1]);
//
//						float x = cframe.vertices[m.glcmds[c + 2]][0] * cScale.x
//								+ nframe.vertices[m.glcmds[c + 2]][0] * nScale.x;
//						float y = cframe.vertices[m.glcmds[c + 2]][1] * cScale.y
//								+ nframe.vertices[m.glcmds[c + 2]][1] * nScale.y;
//						float z = cframe.vertices[m.glcmds[c + 2]][2] * cScale.z
//								+ nframe.vertices[m.glcmds[c + 2]][2] * nScale.z;
//
//						gl.glTexCoord2d(s, t);
//						gl.glVertex3d(x, z, y);
//					}
//					gl.glEnd();
//				}
//
////		    	gl.glBegin(GL_TRIANGLES);
////		    	for( int i = 0; i < m.tris.length; i++)
////		    	{
////		    		for( int j = 0; j < 3; j++)
////		    		{
////		    			int vIdx = m.tris[i].vertices[j];
////		    			float x = cframe.vertices[vIdx][0]*m0x + nframe.vertices[vIdx][0]*m1x;
////		    			float y = cframe.vertices[vIdx][1]*m0y + nframe.vertices[vIdx][1]*m1y;
////		    			float z = cframe.vertices[vIdx][2]*m0z + nframe.vertices[vIdx][2]*m1z;
////
////		    			int tIdx = m.tris[i].texCoords[j];
////		    			gl.glTexCoord2d(m.uv.get(2 * tIdx), m.uv.get(2 * tIdx + 1)); //uv rewrited for drawelements
////		    			gl.glVertex3d(x, z, y);
////		    		}
////		    	}
////		    	gl.glEnd();
//			}
//
//			while (texunits > GL_TEXTURE0) {
//				gl.glMatrixMode(GL_TEXTURE);
//				gl.glLoadIdentity();
//				gl.glMatrixMode(GL_MODELVIEW);
//				gl.glTexEnvf(GL_TEXTURE_ENV, GL_RGB_SCALE, 1.0f);
//				gl.glDisable(GL_TEXTURE_2D);
//				if (r_vertexarrays != 0) {
//					gl.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
//					gl.glClientActiveTexture(texunits - 1);
//				}
//				gl.glActiveTexture(--texunits);
//			}
//			rendered = 1;
//		}
//
//		if (m.usesalpha)
//			gl.glDisable(GL_ALPHA_TEST);
//		gl.glDisable(GL_CULL_FACE);
////    	gl.glPopAttrib();
//		gl.glLoadIdentity();
//
//		return rendered;
//	}

	public boolean md3draw(MD3ModelGL10 m, SPRITE tspr, int xoff, int yoff) {
		DefScript defs = parent.defs;

		m.updateanimation(defs.mdInfo, tspr);

		modelPrepare(m, tspr, xoff, yoff);
		cScale.scl(1 / 64.0f);
		nScale.scl(1 / 64.0f);

		int skinnum = defs.mdInfo.getParams(tspr.picnum).skinnum;

		parent.globalfog.apply();
		m.setScale(cScale, nScale);

		return m.render(null, globalpal, globalshade, skinnum, globalvisibility, 1.0f);
	}

	public int mddraw(GLModel vm, SPRITE tspr, int xoff, int yoff) {
		if (vm == null)
			return 0;

		if (vm.getType() == Type.Voxel) {
			return voxdraw((GLVoxel) vm, tspr);
		}

//		if (vm.getType() == Type.Md2) {
//			return md2draw((DefMD2) vm, tspr, xoff, yoff);
//		}
		if (vm.getType() == Type.Md3)
			return md3draw((MD3ModelGL10) vm, tspr, xoff, yoff) ? 1 : 0;

		return 0;
	}

	public void md3_vox_calcmat_common(SPRITE tspr, Vector3 a0, float f, float[][] mat) {
		float g;
		float k0, k1, k2, k3, k4, k5, k6, k7;

		float gcosang2 = parent.gcosang2;
		float gsinang2 = parent.gsinang2;

		float gshang = parent.gshang;
		float gstang = parent.gstang;
		float gctang = parent.gctang;
		float gchang = parent.gchang;

		float gsinang = parent.gsinang;
		float gcosang = parent.gcosang;

		k0 = (tspr.x - globalposx) * f / 1024.0f;
		k1 = (tspr.y - globalposy) * f / 1024.0f;

		f = gcosang2 * gshang;
		g = gsinang2 * gshang;
		Spriteext sprext = parent.defs.mapInfo.getSpriteInfo(tspr.owner);

		k4 = sintable[(tspr.ang + (sprext != null ? sprext.angoff : 0) + 1024) & 2047] / 16384.0f;
		k5 = sintable[(tspr.ang + (sprext != null ? sprext.angoff : 0) + 512) & 2047] / 16384.0f;
		k2 = k0 * (1 - k4) + k1 * k5;
		k3 = k1 * (1 - k4) - k0 * k5;
		k6 = f * gstang - gsinang * gctang;
		k7 = g * gstang + gcosang * gctang;

		// Mirrors
		if (parent.grhalfxdown10x < 0) {
			k6 = -k6;
			k7 = -k7;
		}

		mat[0][0] = k4 * k6 + k5 * k7;
		mat[1][0] = gchang * gstang;
		mat[2][0] = k4 * k7 - k5 * k6;
		mat[3][0] = k2 * k6 + k3 * k7;
		k6 = f * gctang + gsinang * gstang;
		k7 = g * gctang - gcosang * gstang;
		mat[0][1] = k4 * k6 + k5 * k7;
		mat[1][1] = gchang * gctang;
		mat[2][1] = k4 * k7 - k5 * k6;
		mat[3][1] = k2 * k6 + k3 * k7;
		k6 = gcosang2 * gchang;
		k7 = gsinang2 * gchang;
		mat[0][2] = k4 * k6 + k5 * k7;
		mat[1][2] = -gshang;
		mat[2][2] = k4 * k7 - k5 * k6;
		mat[3][2] = k2 * k6 + k3 * k7;

		mat[3][0] += a0.y * mat[0][0] + a0.z * mat[1][0] + a0.x * mat[2][0];
		mat[3][1] += a0.y * mat[0][1] + a0.z * mat[1][1] + a0.x * mat[2][1];
		mat[3][2] += a0.y * mat[0][2] + a0.z * mat[1][2] + a0.x * mat[2][2];
	}

	public void md3_vox_calcmat_common(SPRITE tspr, Vector3 a0, float f, Matrix4 mat) {
		float yaw = globalang / (2048.0f / 360.0f) - 90.0f;
		float roll = parent.gtang * 57.3f; // XXX 57.3f WFT
		Spriteext sprext = parent.defs.mapInfo.getSpriteInfo(tspr.owner);

		float spriteang = ((tspr.ang + (sprext != null ? sprext.angoff : 0) + 512) & 2047) / (2048.0f / 360.0f);

		// gtang tilt rotation (roll)
		// gstang = sin(qtang)
		// gctang = cos(qtang)

		// gchang up/down rotation (pitch)
		// gshang up/down rotation

		/*
		 * double radplayerang = (globalang & 2047) * 2.0f * PI / 2048.0f; (yaw) gsinang
		 * = (float) (Math.cos(radplayerang) / 16.0); gcosang = (float)
		 * (Math.sin(radplayerang) / 16.0); gsinang2 = gsinang * ((double) viewingrange)
		 * / 65536.0; gcosang2 = gcosang * ((double) viewingrange) / 65536.0;
		 */

		mat.idt();
		mat.rotate(0.0f, 0.0f, -1.0f, roll);
		mat.rotate(-1.0f, 0.0f, 0.0f, pitch);
		mat.rotate(0.0f, -1.0f, 0.0f, yaw);
		mat.scale(-1 / 16f, 1.0f, 1 / 16f);
		mat.translate(a0.y, a0.z, a0.x);
		mat.rotate(0.0f, -1.0f, 0.0f, spriteang);

		/*
		 * float[] tmp = m.getValues(); mat[0][0] = tmp[0]; mat[0][1] = tmp[1];
		 * mat[0][2] = tmp[2]; mat[0][3] = tmp[3];
		 *
		 * mat[1][0] = tmp[4]; mat[1][1] = tmp[5]; mat[1][2] = tmp[6]; mat[1][3] =
		 * tmp[7];
		 *
		 * mat[2][0] = tmp[8]; mat[2][1] = tmp[9]; mat[2][2] = tmp[10]; mat[2][3] =
		 * tmp[11];
		 *
		 * mat[3][0] = tmp[12]; mat[3][1] = tmp[13]; mat[3][2] = tmp[14]; mat[3][3] =
		 * tmp[15];
		 */
	}

	public void md3_vox_calcmat_common(SPRITE tspr, Vector3 a0) {
		float yaw = globalang / (2048.0f / 360.0f) - 90.0f;
		float roll = parent.gtang * 57.3f; // XXX 57.3f WTF
		Spriteext sprext = parent.defs.mapInfo.getSpriteInfo(tspr.owner);

		float spriteang = ((tspr.ang + (sprext != null ? sprext.angoff : 0) + 512) & 2047) / (2048.0f / 360.0f);

		gl.glLoadIdentity();
		gl.glRotatef(roll, 0, 0, -1);
		gl.glRotatef(pitch, -1, 0, 0);
		gl.glRotatef(yaw, 0, -1, 0);
		gl.glScalef(-1 / 16f, 1.0f, 1 / 16f);
		gl.glTranslatef(a0.y, a0.z, a0.x);
		gl.glRotatef(spriteang, 0.0f, -1.0f, 0.0f);
	}
}
