package ru.m210projects.Build.Render;

import static com.badlogic.gdx.graphics.GL20.GL_DONT_CARE;
import static com.badlogic.gdx.graphics.GL20.GL_LINEAR;
import static com.badlogic.gdx.graphics.GL20.GL_NICEST;
import static ru.m210projects.Build.Render.Types.GL10.GL_FOG;
import static ru.m210projects.Build.Render.Types.GL10.GL_FOG_COLOR;
import static ru.m210projects.Build.Render.Types.GL10.GL_FOG_END;
import static ru.m210projects.Build.Render.Types.GL10.GL_FOG_HINT;
import static ru.m210projects.Build.Render.Types.GL10.GL_FOG_MODE;
import static ru.m210projects.Build.Render.Types.GL10.GL_FOG_START;


import com.badlogic.gdx.Gdx;
import ru.m210projects.Build.Render.Types.Color;
import ru.m210projects.Build.Render.Types.GL10;
import ru.m210projects.Build.Types.PaletteManager;

public class GLFog {

	// For GL_LINEAR fog:
	protected int FOGDISTCONST = 48;
	protected final float FULLVIS_BEGIN = (float) 2.9e30;
	protected final float FULLVIS_END = (float) 3.0e30;

	public int shade, pal;
	public float combvis;

	public boolean nofog, isEnabled;
	protected PaletteManager paletteManager;

	protected final float[] color = new float[4];
	protected float start, end;
	protected float curstart;
	protected float curend;
	protected float[] curcolor = new float[4];

	public void init(PaletteManager paletteManager) {
		if (Gdx.graphics.getGLVersion().getVendorString().compareTo("NVIDIA Corporation") == 0) {
			Gdx.gl.glHint(GL_FOG_HINT, GL_NICEST);
		} else {
			Gdx.gl.glHint(GL_FOG_HINT, GL_DONT_CARE);
		}
		((GL10) Gdx.gl).glFogi(GL_FOG_MODE, GL_LINEAR); // GL_EXP
		this.paletteManager = paletteManager;
	}

	public void copy(GLFog src) {
		this.shade = src.shade;
		this.combvis = src.combvis;
		this.pal = src.pal;
	}

	public void clear() {
		shade = 0;
		combvis = 0;
		pal = 0;
	}

	public void calc() {
		int numshades = paletteManager.getShadeCount();
		if (combvis == 0) {
			start = FULLVIS_BEGIN;
			end = FULLVIS_END;
		} else if (shade >= numshades - 1) {
			start = -1;
			end = 0.001f;
		} else {
			start = (shade > 0) ? 0 : -(FOGDISTCONST * shade) / combvis;
			end = (FOGDISTCONST * (numshades - 1 - shade)) / combvis;
		}

		Color palookupfog = paletteManager.getFogColor(pal);
		color[0] = (palookupfog.r / 63.f);
		color[1] = (palookupfog.g / 63.f);
		color[2] = (palookupfog.b / 63.f);
		color[3] = 1;

//		if (manager.getShader() != null)
//			manager.getShader().setFogParams(true, start, end, color);
		((GL10) Gdx.gl).glFogfv(GL_FOG_COLOR, color, 0);
		((GL10) Gdx.gl).glFogf(GL_FOG_START, start);
		((GL10) Gdx.gl).glFogf(GL_FOG_END, end);
	}

	public void setFogScale(int var) {
		FOGDISTCONST = var;
	}

	public void apply() {

	}

	public void enable() {
		if (!nofog) {
			isEnabled = true;
			Gdx.gl.glEnable(GL_FOG);
		}
	}

	public void disable() {
		isEnabled = false;
		Gdx.gl.glDisable(GL_FOG);
//		if (manager.getShader() != null)
//			manager.getShader().setFogParams(false, 0.0f, 0.0f, null);
	}
}
