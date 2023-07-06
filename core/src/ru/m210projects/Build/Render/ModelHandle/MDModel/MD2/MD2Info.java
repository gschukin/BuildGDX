package ru.m210projects.Build.Render.ModelHandle.MDModel.MD2;

import ru.m210projects.Build.Render.ModelHandle.MDInfo;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

public class MD2Info extends MDInfo {

	public final MD2Header header;

	public MD2Info(Entry res, String file) throws Exception {
		super(file, Type.Md2);

		this.header = loadHeader(res);
		if ((header.ident != 0x32504449) || (header.version != 8)) {
			throw new Exception("Wrong file header IDP2"); //"IDP2"
		}

		try(InputStream is = res.getInputStream()) {
			StreamUtils.skip(is, header.offsetFrames);
			this.frames = new String[header.numFrames];
			this.numframes = header.numFrames;

			for (int i = 0; i < header.numFrames; i++) {
				StreamUtils.skip(is, 24);
				frames[i] = StreamUtils.readString(is, 16);
				StreamUtils.skip(is, header.numVertices * 4);
			}
		}
	}

	protected MD2Header loadHeader(Entry res) throws IOException {
		try(InputStream is = res.getInputStream()) {
			MD2Header header = new MD2Header();

			header.ident = StreamUtils.readInt(is);
			header.version = StreamUtils.readInt(is);
			header.skinWidth = StreamUtils.readInt(is);
			header.skinHeight = StreamUtils.readInt(is);
			header.frameSize = StreamUtils.readInt(is);
			header.numSkins = StreamUtils.readInt(is);
			header.numVertices = StreamUtils.readInt(is);
			header.numTexCoords = StreamUtils.readInt(is);
			header.numTriangles = StreamUtils.readInt(is);
			header.numGLCommands = StreamUtils.readInt(is);
			header.numFrames = StreamUtils.readInt(is);
			header.offsetSkin = StreamUtils.readInt(is);
			header.offsetTexCoords = StreamUtils.readInt(is);
			header.offsetTriangles = StreamUtils.readInt(is);
			header.offsetFrames = StreamUtils.readInt(is);
			header.offsetGLCommands = StreamUtils.readInt(is);
			header.offsetEnd = StreamUtils.readInt(is);

			return header;
		}
	}
}
