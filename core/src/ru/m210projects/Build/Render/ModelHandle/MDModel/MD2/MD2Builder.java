package ru.m210projects.Build.Render.ModelHandle.MDModel.MD2;

import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

public class MD2Builder {

	public final MD2Header header;
	public MD2Triangle[] triangles;
	public float[][] texCoords;
	public MD2Frame[] frames;
	public int[] glcmds;

	public MD2Builder(MD2Info md) throws IOException {
		Entry bb = BuildGdx.cache.getEntry(md.getFilename(), true);
		this.header = md.header;

		this.triangles = loadTriangles(bb);
		this.texCoords = loadTexCoords(bb);
		this.frames = loadFrames(bb);
		this.glcmds = loadGLCommands(bb);
	}

	private MD2Frame[] loadFrames(Entry bb) throws IOException {
		try(InputStream is = bb.getInputStream()) {
			StreamUtils.skip(is, header.offsetFrames);
			MD2Frame[] frames = new MD2Frame[header.numFrames];

			for (int i = 0; i < header.numFrames; i++) {
				MD2Frame frame = new MD2Frame();
				frame.vertices = new float[header.numVertices][3];

				float scaleX = StreamUtils.readFloat(is), scaleY = StreamUtils.readFloat(is), scaleZ = StreamUtils.readFloat(is);
				float transX = StreamUtils.readFloat(is), transY = StreamUtils.readFloat(is), transZ = StreamUtils.readFloat(is);
				frame.name = StreamUtils.readString(is, 16);

				for (int j = 0; j < header.numVertices; j++) {
					float x = StreamUtils.readByte(is) * scaleX + transX;
					float y = StreamUtils.readByte(is) * scaleY + transY;
					float z = StreamUtils.readByte(is) * scaleZ + transZ;
					StreamUtils.readByte(is); // normal index

					frame.vertices[j][0] = x;
					frame.vertices[j][1] = y;
					frame.vertices[j][2] = z;
				}

				frames[i] = frame;
			}
			return frames;
		}
	}

	private MD2Triangle[] loadTriangles(Entry bb) throws IOException {
		try(InputStream is = bb.getInputStream()) {
			StreamUtils.skip(is, header.offsetTriangles);
			MD2Triangle[] triangles = new MD2Triangle[header.numTriangles];

			for (int i = 0; i < header.numTriangles; i++) {
				MD2Triangle triangle = new MD2Triangle();
				triangle.vertices[0] = (short) StreamUtils.readShort(is);
				triangle.vertices[1] = (short) StreamUtils.readShort(is);
				triangle.vertices[2] = (short) StreamUtils.readShort(is);
				triangle.texCoords[0] = (short) StreamUtils.readShort(is);
				triangle.texCoords[1] = (short) StreamUtils.readShort(is);
				triangle.texCoords[2] = (short) StreamUtils.readShort(is);
				triangles[i] = triangle;
			}

			return triangles;
		}
	}

	private int[] loadGLCommands(Entry bb) throws IOException {
		try(InputStream is = bb.getInputStream()) {
			StreamUtils.skip(is, header.offsetGLCommands);
			int[] glcmds = new int[header.numGLCommands];

			for (int i = 0; i < header.numGLCommands; i++) {
				glcmds[i] = StreamUtils.readInt(is);
			}
			return glcmds;
		}
	}

	private float[][] loadTexCoords(Entry bb) throws IOException {
		try(InputStream is = bb.getInputStream()) {
			StreamUtils.skip(is, header.offsetTexCoords);
			float[][] texCoords = new float[header.numTexCoords][2];
			float width = header.skinWidth;
			float height = header.skinHeight;

			for (int i = 0; i < header.numTexCoords; i++) {
				short u = (short) StreamUtils.readShort(is);
				short v = (short) StreamUtils.readShort(is);
				texCoords[i][0] = (u / width);
				texCoords[i][1] = (v / height);
			}
			return texCoords;
		}
	}
}
