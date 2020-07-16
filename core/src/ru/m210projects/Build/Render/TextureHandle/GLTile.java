package ru.m210projects.Build.Render.TextureHandle;

import static com.badlogic.gdx.graphics.GL20.GL_CLAMP_TO_EDGE;
import static com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA;
import static com.badlogic.gdx.graphics.GL20.GL_REPEAT;
import static com.badlogic.gdx.graphics.GL20.GL_REPLACE;
import static com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA;
import static com.badlogic.gdx.graphics.GL20.GL_SRC_COLOR;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE_MAG_FILTER;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE_MIN_FILTER;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE_WRAP_S;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE_WRAP_T;
import static com.badlogic.gdx.graphics.GL20.GL_UNPACK_ALIGNMENT;
import static com.badlogic.gdx.graphics.GL20.GL_UNSIGNED_BYTE;
import static ru.m210projects.Build.Render.GLInfo.*;
import static ru.m210projects.Build.Engine.DETAILPAL;
import static ru.m210projects.Build.Engine.GLOWPAL;
import static ru.m210projects.Build.Render.Types.GL10.GL_CLAMP;
import static ru.m210projects.Build.Render.Types.GL10.GL_COMBINE_ALPHA_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_COMBINE_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_COMBINE_RGB_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_INTERPOLATE_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_MODULATE;
import static ru.m210projects.Build.Render.Types.GL10.GL_OPERAND0_ALPHA_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_OPERAND0_RGB_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_OPERAND1_RGB_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_OPERAND2_RGB_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_PREVIOUS_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_RGB_SCALE;
import static ru.m210projects.Build.Render.Types.GL10.GL_SOURCE0_ALPHA_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_SOURCE0_RGB_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_SOURCE1_RGB_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_SOURCE2_RGB_ARB;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE_ENV;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE_ENV_MODE;

import java.nio.ByteBuffer;

import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Render.GLInfo;
import ru.m210projects.Build.Render.TextureHandle.TileData.PixelFormat;
import ru.m210projects.Build.Render.Types.GLFilter;
import ru.m210projects.Build.Settings.GLSettings;

public class GLTile implements Comparable<GLTile> {

	public static enum FlagType {
		Clamped(0), HighTile(1), SkyboxFace(2), HasAlpha(3), Invalidated(7);

		private final int bit;

		FlagType(int bit) {
			this.bit = (1 << bit);
		}

		public int getBit() {
			return bit;
		}

		public boolean hasBit(int flags) {
			return (flags & bit) != 0;
		}
	};

	protected int glHandle;
	protected final int glTarget;
	protected final int width, height;
	private boolean isRequireShader;

	protected int flags;
	protected byte skyface;
	protected Hicreplctyp hicr;
	protected float scalex, scaley;

	protected int palnum;
	protected GLTile next;

	public GLTile(int width, int height) {
		this.glTarget = GL_TEXTURE_2D;
		this.glHandle = BuildGdx.gl.glGenTexture();
		this.width = width;
		this.height = height;
		this.isRequireShader = false;
	}

	public GLTile(TileData pic, int palnum, boolean useMipMaps) {
		this(pic.getWidth(), pic.getHeight());
		this.palnum = palnum;
		if (pic.getPixelFormat() == PixelFormat.Pal8 || pic.getPixelFormat() == PixelFormat.Pal8A)
			this.isRequireShader = true;

		BuildGdx.gl.glBindTexture(glTarget, glHandle);

		BuildGdx.gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

		BuildGdx.gl.glTexImage2D(glTarget, 0, pic.getGLInternalFormat(), pic.getWidth(), pic.getHeight(), 0,
				pic.getGLFormat(), pic.getGLType(), pic.getPixels());

		if (useMipMaps)
			generateMipmap(pic, true);

		setupTextureFilter(GLSettings.textureFilter.get(), GLSettings.textureAnisotropy.get());
		setupTextureWrap(!pic.isClamped() ? GL_REPEAT : GLInfo.clamptoedge ? GL_CLAMP_TO_EDGE : GL_CLAMP);

		setClamped(pic.isClamped());
		setHasAlpha(pic.hasAlpha());

		scalex = scaley = 1.0f;
	}

	public void setColor(float r, float g, float b, float a) {
		BuildGdx.gl.glColor4f(r, g, b, a);
	}

	public void update(TileData pic, boolean useMipMaps) {
		BuildGdx.gl.glBindTexture(glTarget, glHandle);

		BuildGdx.gl.glTexSubImage2D(glTarget, 0, 0, 0, pic.getWidth(), pic.getHeight(), pic.getGLFormat(),
				GL_UNSIGNED_BYTE, pic.getPixels());

		if (useMipMaps)
			generateMipmap(pic, false);
	}

	protected int calcMipLevel(int xsiz, int ysiz, int maxsize) {
		int mipLevel = 0;
		while ((xsiz >> mipLevel) > (1 << maxsize)
				|| (ysiz >> mipLevel) > (1 << maxsize))
			mipLevel++;
		return mipLevel;
	}

	protected void generateMipmap(TileData data, boolean doalloc) {
		if (supportsGenerateMipmaps) {
			BuildGdx.gl.glGenerateMipmap(glTarget);
			return;
		}

		int mipLevel = calcMipLevel(data.getWidth(), data.getHeight(), gltexmaxsize);

		int x2 = data.getWidth(), x3;
		int y2 = data.getHeight(), y3;
		int r, g, b, a, k, wpptr, rpptr, wp, rp, index, rgb;

		ByteBuffer pic = data.getPixels();
	    for (int j = 1, x, y; (x2 > 1) || (y2 > 1); j++)
	    {
	        x3 = Math.max(1, x2 >> 1);
	        y3 = Math.max(1, y2 >> 1);		// this came from the GL_ARB_texture_non_power_of_two spec
	        for (y = 0; y < y3; y++)
	        {
	            wpptr = y * x3;
	            rpptr = (y << 1) * x2;
	            for (x = 0; x < x3; x++, wpptr++, rpptr += 2)
	            {
	            	wp = wpptr << 2;
	            	rp = rpptr << 2;
	            	r = g = b = a = k = 0;

	            	index = rp;
	                if (pic.get(index + 3) != 0)
	                {
	                	r += pic.get(index + 0) & 0xFF;
						g += pic.get(index + 1) & 0xFF;
						b += pic.get(index + 2) & 0xFF;
						a += pic.get(index + 3) & 0xFF;
	                	k++;
	                }
	                index = rp + 4;
	                if (((x << 1) + 1 < x2) && (pic.get(index + 3) != 0))
	                {
	                	r += pic.get(index + 0) & 0xFF;
						g += pic.get(index + 1) & 0xFF;
						b += pic.get(index + 2) & 0xFF;
						a += pic.get(index + 3) & 0xFF;
	                	k++;
	                }
	                if ((y << 1) + 1 < y2)
	                {
	                	index = rp + (x2 << 2);
	                    if (pic.get(index + 3) != 0)
	                    {
	                    	r += pic.get(index + 0) & 0xFF;
							g += pic.get(index + 1) & 0xFF;
							b += pic.get(index + 2) & 0xFF;
							a += pic.get(index + 3) & 0xFF;
	                    	k++;
	                    }

	                    index = rp + ((x2 + 1) << 2);
	                    if (((x << 1) + 1 < x2) && pic.get(index + 3) != 0)
	                    {
	                    	r += pic.get(index + 0) & 0xFF;
							g += pic.get(index + 1) & 0xFF;
							b += pic.get(index + 2) & 0xFF;
							a += pic.get(index + 3) & 0xFF;
	                    	k++;
	                    }
	                }
	                switch (k)
	                {
		                case 0:
		                case 1:
					        rgb = ( (a) << 24 ) + ( (b) << 16 ) + ( (g) << 8 ) + ( (r) << 0 );
							break;
						case 2:
							rgb = ( ((a + 1) >> 1) << 24 ) + ( ((b + 1) >> 1) << 16 ) + ( ((g + 1) >> 1) << 8 ) + ( ((r + 1) >> 1) << 0 );
							break;
						case 3:
							rgb = ( ((a * 85 + 128) >> 8) << 24 ) + ( ((b * 85 + 128) >> 8) << 16 ) + ( ((g * 85 + 128) >> 8) << 8 ) + ( ((r * 85 + 128) >> 8) << 0 );
							break;
						case 4:
							rgb = ( ((a + 2) >> 2) << 24 ) + ( ((b + 2) >> 2) << 16 ) + ( ((g + 2) >> 2) << 8 ) + ( ((r + 2) >> 2) << 0 );
							break;
						default:
							continue;
	                }

	                pic.putInt(wp, rgb);
	            }
	        }

	        if (j >= mipLevel)
	        {
	        	if (doalloc) {
	        		BuildGdx.gl.glTexImage2D(GL_TEXTURE_2D, j - mipLevel, data.getGLInternalFormat(), x3, y3, 0, data.getGLFormat(), GL_UNSIGNED_BYTE, pic); // loading 1st time
				} else {
					BuildGdx.gl.glTexSubImage2D(GL_TEXTURE_2D, j - mipLevel, 0, 0, x3, y3, data.getGLFormat(), GL_UNSIGNED_BYTE, pic); // overwrite old texture
				}
	        }
	        x2 = x3; y2 = y3;
	    }

	}

	public void setupTextureWrap(int wrap) {
		BuildGdx.gl.glTexParameteri(glTarget, GL_TEXTURE_WRAP_S, wrap);
		BuildGdx.gl.glTexParameteri(glTarget, GL_TEXTURE_WRAP_T, wrap);
	}

	public void setupTextureWrapS(int wrap) {
		BuildGdx.gl.glTexParameteri(glTarget, GL_TEXTURE_WRAP_S, wrap);
	}

	public void setupTextureWrapT(int wrap) {
		BuildGdx.gl.glTexParameteri(glTarget, GL_TEXTURE_WRAP_T, wrap);
	}

	public void setupTextureFilter(GLFilter filter, int anisotropy) {
		BuildGdx.gl.glTexParameteri(glTarget, GL_TEXTURE_MIN_FILTER, filter.min);
		BuildGdx.gl.glTexParameteri(glTarget, GL_TEXTURE_MAG_FILTER, filter.mag);
		if (anisotropy >= 1) // 1 if you want to disable anisotropy
			BuildGdx.gl.glTexParameteri(glTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy);
	}

	public void setupTextureGlow() {
		if (!isGlowTexture())
			return;

		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE_ARB);
		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_COMBINE_RGB_ARB, GL_INTERPOLATE_ARB);

		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE0_RGB_ARB, GL_PREVIOUS_ARB);
		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND0_RGB_ARB, GL_SRC_COLOR);

		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE1_RGB_ARB, GL_TEXTURE);
		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND1_RGB_ARB, GL_SRC_COLOR);

		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE2_RGB_ARB, GL_TEXTURE);
		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND2_RGB_ARB, GL_ONE_MINUS_SRC_ALPHA);

		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_COMBINE_ALPHA_ARB, GL_REPLACE);
		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA_ARB, GL_PREVIOUS_ARB);
		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA_ARB, GL_SRC_ALPHA);

		setupTextureWrap(GL_REPEAT);
	}

	public void setupTextureDetail() {
		if (!isDetailTexture())
			return;

		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE_ARB);
		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_COMBINE_RGB_ARB, GL_MODULATE);

		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE0_RGB_ARB, GL_PREVIOUS_ARB);
		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND0_RGB_ARB, GL_SRC_COLOR);

		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE1_RGB_ARB, GL_TEXTURE);
		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND1_RGB_ARB, GL_SRC_COLOR);

		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_COMBINE_ALPHA_ARB, GL_REPLACE);
		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_SOURCE0_ALPHA_ARB, GL_PREVIOUS_ARB);
		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA_ARB, GL_SRC_ALPHA);

		BuildGdx.gl.glTexEnvf(GL_TEXTURE_ENV, GL_RGB_SCALE, 2.0f);

		setupTextureWrap(GL_REPEAT);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	protected void unbind() {
		BuildGdx.gl.glBindTexture(glTarget, 0);
	}

	protected boolean bind() {
		if (glHandle != 0) {
			BuildGdx.gl.glBindTexture(glTarget, glHandle);
			return true;
		}

		return false;
	}

	public void delete() {
		if (glHandle != 0) {
			if (BuildGdx.gl != null)
				BuildGdx.gl.glDeleteTexture(glHandle);
			glHandle = 0;
		}
	}

	public boolean isClamped() {
		return FlagType.Clamped.hasBit(flags);
	}

	public void setClamped(boolean mode) {
		setBit(mode, FlagType.Clamped);
	}

	public boolean isHighTile() {
		return FlagType.HighTile.hasBit(flags);
	}

	public void setHighTile(Hicreplctyp si) {
		this.hicr = si;
		setBit(si != null, FlagType.HighTile);
	}

	public boolean isSkyboxFace() {
		return FlagType.SkyboxFace.hasBit(flags);
	}

	public void setSkyboxFace(int facen) {
		this.skyface = (byte) facen;
		if (facen > 0)
			setBit(true, FlagType.SkyboxFace);
	}

	public boolean hasAlpha() {
		return FlagType.HasAlpha.hasBit(flags);
	}

	public void setHasAlpha(boolean mode) {
		setBit(mode, FlagType.HasAlpha);
	}

	public boolean isInvalidated() {
		return FlagType.Invalidated.hasBit(flags);
	}

	public void setInvalidated(boolean mode) {
		setBit(mode, FlagType.Invalidated);
	}

	public int getPal() {
		return palnum;
	}

	public boolean isGlowTexture() {
		return hicr != null && (hicr.palnum == GLOWPAL);
	}

	public boolean isDetailTexture() {
		return hicr != null && (hicr.palnum == DETAILPAL);
	}

	public float getXScale() {
		return scalex;
	}

	public float getYScale() {
		return scaley;
	}

	public float getAlphaCut() {
		return hicr != null ? hicr.alphacut : 0.0f;
	}

	private void setBit(boolean mode, FlagType bit) {
		if (mode)
			flags |= bit.getBit();
		else
			flags &= ~bit.getBit();
	}

	public boolean isRequireShader() {
		return isRequireShader;
	}

	@Override
	public String toString() {
		String out = "id = " + glHandle + " [" + width + "x" + height + ", ";
		out += "pal = " + palnum + ", ";
		out += "clamp = " + isClamped() + "]";

		return out;
	}

	@Override
	public int compareTo(GLTile src) {
		if (src == null)
			return 0;

		return this.palnum - src.palnum;
	}
}
