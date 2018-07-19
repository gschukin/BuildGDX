/*
 * TextureUtils by Kirill Klimenko-KLIMaka 
 * Based on parts of "Polymost" by Ken Silverman
 * 
 * Ken Silverman's official web site: http://www.advsys.net/ken
 * See the included license file "BUILDLIC.TXT" for license info.
 */

package ru.m210projects.Build.Render;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Render.ImageUtils.fixtransparency;
import static ru.m210projects.Build.Render.Types.GL10.GL_LINEAR;
import static ru.m210projects.Build.Render.Types.GL10.GL_LINEAR_MIPMAP_LINEAR;
import static ru.m210projects.Build.Render.Types.GL10.GL_LINEAR_MIPMAP_NEAREST;
import static ru.m210projects.Build.Render.Types.GL10.GL_MAX_TEXTURE_SIZE;
import static ru.m210projects.Build.Render.Types.GL10.GL_NEAREST;
import static ru.m210projects.Build.Render.Types.GL10.GL_NEAREST_MIPMAP_LINEAR;
import static ru.m210projects.Build.Render.Types.GL10.GL_NEAREST_MIPMAP_NEAREST;
import static ru.m210projects.Build.Render.Types.GL10.GL_RGBA;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE_2D;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE_MAG_FILTER;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE_MIN_FILTER;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE_WRAP_S;
import static ru.m210projects.Build.Render.Types.GL10.GL_TEXTURE_WRAP_T;
import static ru.m210projects.Build.Render.Types.GL10.GL_UNSIGNED_BYTE;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import ru.m210projects.Build.Render.Types.BTexture;
import ru.m210projects.Build.Render.Types.GLFilter;
import ru.m210projects.Build.Types.Palette;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

public class TextureUtils {

	private static final int TEX_MAX_SIZE = 1024;

	private static ByteBuffer tmp_buffer;
	private static byte[] tmp_array;
	private static int gltexmaxsize = 0;

	private static GLFilter[] glfiltermodes = {
			new GLFilter("GL_NEAREST", GL_NEAREST, GL_NEAREST), // 0
			new GLFilter("GL_LINEAR", GL_LINEAR, GL_LINEAR), // 1
			new GLFilter("GL_NEAREST_MIPMAP_NEAREST", GL_NEAREST_MIPMAP_NEAREST, GL_NEAREST), // 2
			new GLFilter("GL_LINEAR_MIPMAP_NEAREST", GL_LINEAR_MIPMAP_NEAREST, GL_LINEAR), // 3
			new GLFilter("GL_NEAREST_MIPMAP_LINEAR", GL_NEAREST_MIPMAP_LINEAR, GL_NEAREST), // 4
			new GLFilter("GL_LINEAR_MIPMAP_LINEAR", GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR) }; // 5

	public static GLFilter getGlFilter(int mode) {
		mode = mode < 0 ? 0 : mode >= glfiltermodes.length ? glfiltermodes.length - 1 : mode;
		return glfiltermodes[mode];
	}

	public static int getGlFilterCount() {
		return glfiltermodes.length;
	}

	private static ByteBuffer getTmpBuffer() {
		if (tmp_buffer == null) {
			tmp_buffer = BufferUtils.newByteBuffer(TEX_MAX_SIZE * TEX_MAX_SIZE * 4);
		}
		return tmp_buffer;
	}

	private static byte[] getTmpArray() {
		if (tmp_array == null) {
			tmp_array = new byte[TEX_MAX_SIZE * TEX_MAX_SIZE * 4];
		}
		return tmp_array;
	}

	public static ByteBuffer wrap(byte[] data) {
		return wrap(data, data.length);
	}

	public static ByteBuffer wrap(byte[] data, int size) {
		ByteBuffer buffer = getTmpBuffer();
		buffer.clear();
		buffer.put(data, 0, size * 4);
		buffer.flip();
		return buffer;
	}

	public static byte[] tmpArray(int w, int h) {
		byte[] arr = getTmpArray();
		Arrays.fill(arr, 0, w * h * 4, (byte) 0);
		return arr;
	}

	public static BTexture gloadtex(int[] picbuf, int xsiz, int ysiz, int dapal) {
		byte[] pic = tmpArray(xsiz, ysiz);

		if (palookup[dapal] == null)
			dapal = 0;

		for (int y = 0; y < ysiz; y++) {
			int wpptr = y * xsiz;
			for (int x = 0; x < xsiz; x++, wpptr++) {
				int wp = 4 * wpptr;

				int dacol = (int) ((picbuf[wpptr] & 0xFFFFFFFFL) >> 24); // FIXME actually picbuf need to be byte[]
				if(UseBloodPal && dapal == 1) //Blood's pal 1
				{
					int shade = (min(max(globalshade/*+(davis>>8)*/,0),numshades-1));
					dacol = palookup[dapal][dacol + (shade << 8)] & 0xFF;
				} else
					dacol = palookup[dapal][dacol] & 0xFF; 

				Palette color = curpalette[dacol];
				if (gammabrightness != 0) {
					pic[wp + 0] = (byte) (color.r);
					pic[wp + 1] = (byte) (color.g);
					pic[wp + 2] = (byte) (color.b);
					pic[wp + 3] = (byte) 0xFF;
				} else {
					int[] bightness = britable[curbrightness];
					pic[wp + 0] = (byte) bightness[color.r];
					pic[wp + 1] = (byte) bightness[color.g];
					pic[wp + 2] = (byte) bightness[color.b];
					pic[wp + 3] = (byte) 0xFF;
				}
			}
		}

		BTexture rtexid = new BTexture();
		bindTexture(rtexid);
		uploadBoundTexture(true, xsiz, ysiz, GL_RGBA, GL_RGBA, pic, xsiz, ysiz);
		setupBoundTexture(0, 0);
		return rtexid;
	}

	private static int getTextureMaxSize() {
		if (gltexmaxsize <= 0) {
			IntBuffer buffer = BufferUtils.newIntBuffer(16); // FIXME 16?
			Gdx.gl.glGetIntegerv(GL_MAX_TEXTURE_SIZE, buffer);
			int i = buffer.get(0);
			if (i == 0) {
				gltexmaxsize = 6; // 2^6 = 64 == default GL max texture size
			} else {
				gltexmaxsize = 0;
				for (; i > 1; i >>= 1)
					gltexmaxsize++;
			}
		}
		return gltexmaxsize;
	}

	public static void bindTexture(BTexture tex) {
		tex.bind();
	}

	public static void deleteTexture(BTexture tex) {
		tex.dispose();
	}

	public static void uploadBoundTexture(boolean doalloc, int xsiz, int ysiz, int intexfmt, int texfmt, byte[] pic, int tsizx, int tsizy) {
		int mipLevel = calcMipLevel(xsiz, ysiz, getTextureMaxSize());
		if (mipLevel == 0) {
			if (doalloc) {
				Gdx.gl.glTexImage2D(GL_TEXTURE_2D, 0, intexfmt, xsiz, ysiz, 0, texfmt, GL_UNSIGNED_BYTE, wrap(pic, xsiz * ysiz)); // loading 1st time
			} else {
				Gdx.gl.glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, xsiz, ysiz, texfmt, GL_UNSIGNED_BYTE, wrap(pic, xsiz * ysiz)); // overwrite old texture
			}
		} else {
			System.err.println("Uploading non-zero mipmap level textures is unimplemented");
		}
		
		//Build 2D Mipmaps
		int x2 = xsiz; 
		int y2 = ysiz;
		int r, g, b, a, k;
	    for (int j = 1; (x2 > 1) || (y2 > 1); j++)
	    {
	        int x3 = Math.max(1, x2 >> 1); 
	        int y3 = Math.max(1, y2 >> 1);		// this came from the GL_ARB_texture_non_power_of_two spec
	        for (int y = 0; y < y3; y++)
	        {
	            int wpptr = y * x3; 
	            int rpptr = (y << 1) * x2;
	            for (int x = 0; x < x3; x++, wpptr++, rpptr += 2)
	            {
	            	int wp = 4 * wpptr;
	            	int rp = 4 * rpptr;
	            	r = g = b = a = k = 0;
	            	
	                if (pic[rp + 3] != 0) 
	                { 
	                	r += pic[rp + 0] & 0xFF; 
	                	g += pic[rp + 1] & 0xFF; 
	                	b += pic[rp + 2] & 0xFF; 
	                	a += pic[rp + 3] & 0xFF; 
	                	k++; 
	                }
	                if ((x + x + 1 < x2) && (pic[rp + 4 + 3] != 0)) 
	                { 
	                	r += pic[rp + 4 + 0] & 0xFF; 
	                	g += pic[rp + 4 + 1] & 0xFF; 
	                	b += pic[rp + 4 + 2] & 0xFF; 
	                	a += pic[rp + 4 + 3] & 0xFF; 
	                	k++; 
	                }
	                if (y + y + 1 < y2)
	                {
	                    if (pic[rp + 4 * x2 + 3] != 0) 
	                    { 
	                    	r += pic[rp + 4 * x2 + 0] & 0xFF; 
	                    	g += pic[rp + 4 * x2 + 1] & 0xFF; 
	                    	b += pic[rp + 4 * x2 + 2] & 0xFF; 
	                    	a += pic[rp + 4 * x2 + 3] & 0xFF; 
	                    	k++; 
	                    }
	                    if ((x + x + 1 < x2) && pic[rp + 4 * (x2 + 1) + 3] != 0) 
	                    { 
	                    	r += pic[rp + 4 * (x2 + 1) + 0] & 0xFF; 
	                    	g += pic[rp + 4 * (x2 + 1) + 1] & 0xFF; 
	                    	b += pic[rp + 4 * (x2 + 1) + 2] & 0xFF; 
	                    	a += pic[rp + 4 * (x2 + 1) + 3] & 0xFF; 
	                    	k++; 
	                    }
	                }
	                switch (k)
	                {
		                case 0:
		                case 1:
		                	pic[wp] = (byte) r;
		                	pic[wp + 1] = (byte) g;
		                	pic[wp + 2] = (byte) b;
		                	pic[wp + 3] = (byte) a;
		                    break;
		                case 2:
		                	pic[wp] = (byte) ((r + 1) >> 1);
		                	pic[wp + 1] = (byte) ((g + 1) >> 1);
		                	pic[wp + 2] = (byte) ((b + 1) >> 1);
		                	pic[wp + 3] = (byte) ((a + 1) >> 1);
		                    break;
		                case 3:
		                	pic[wp] = (byte) ((r * 85 + 128) >> 8);
		                	pic[wp + 1] = (byte) ((g * 85 + 128) >> 8);
		                	pic[wp + 2] = (byte) ((b * 85 + 128) >> 8);
		                	pic[wp + 3] = (byte) ((a * 85 + 128) >> 8);
		                    break;
		                case 4:
		                	pic[wp] = (byte) ((r + 2) >> 2);
		                	pic[wp + 1] = (byte) ((g + 2) >> 2);
		                	pic[wp + 2] = (byte) ((b + 2) >> 2);
		                	pic[wp + 3] = (byte) ((a + 2) >> 2);
		                    break;
		                default:
		                    break;
	                }
	            }
	        }
	        
	        if (tsizx >= 0) 
	        	fixtransparency(pic,(tsizx+(1<<j)-1)>>j,(tsizy+(1<<j)-1)>>j,x3,y3, false); //dameth FIXME
	        if (j >= mipLevel)
	        {
	        	if (doalloc) {
					Gdx.gl.glTexImage2D(GL_TEXTURE_2D, j - mipLevel, intexfmt, x3, y3, 0, texfmt, GL_UNSIGNED_BYTE, wrap(pic, x3 * y3)); // loading 1st time
				} else {
					Gdx.gl.glTexSubImage2D(GL_TEXTURE_2D, j - mipLevel, 0, 0, x3, y3, texfmt, GL_UNSIGNED_BYTE, wrap(pic, x3 * y3)); // overwrite old texture
				}
	        }
	        x2 = x3; y2 = y3;
	    }
	}

	private static int calcMipLevel(int xsiz, int ysiz, int maxsize) {
		int mipLevel = 0;
		while ((xsiz >> mipLevel) > (1 << maxsize)
				|| (ysiz >> mipLevel) > (1 << maxsize))
			mipLevel++;
		return mipLevel;
	}

	public static void setupBoundTexture(int filterMode, int anisotropy) {
		GLFilter filter = getGlFilter(filterMode);
		Gdx.gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter.mag);
		Gdx.gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter.min);
		if (anisotropy >= 1) { // 1 if you want to disable anisotropy
			Gdx.gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy);
		}
	}

	public static void setupBoundTextureWrap(int wrap) {
		Gdx.gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
		Gdx.gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);
	}
}
