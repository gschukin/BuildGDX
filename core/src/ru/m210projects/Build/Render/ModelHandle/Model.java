package ru.m210projects.Build.Render.ModelHandle;

public class Model {

	public static final int MD_ROTATE = 2;

	public enum Type { Voxel, Md2, Md3 }

	protected final String filename;
	protected final Type type;

	protected int flags;
	protected float scale;
	// yoffset differs from zadd in that it does not follow cstat&8 y-flipping
	protected float yoffset, zadd;

	public Model(String filename, Type type) {
		this.filename = filename;
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public void setMisc(float scale, float zadd, float yoffset, int flags) {
	    this.scale = scale;
	    this.zadd = zadd;
	    this.yoffset = yoffset;
	    this.flags = flags;
	}

	public boolean isRotating() {
		return (flags & MD_ROTATE) != 0;
	}

	public float getScale() {
		return scale;
	}
}
