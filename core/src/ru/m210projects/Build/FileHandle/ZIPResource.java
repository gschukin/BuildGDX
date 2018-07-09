// This file is part of BuildGDX.
// Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.FileHandle;

import static ru.m210projects.Build.FileHandle.Cache1D.*;
import static ru.m210projects.Build.FileHandle.Compat.*;
import static ru.m210projects.Build.Strhandler.Bstrcasecmp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ru.m210projects.Build.OnSceenDisplay.Console;
import ru.m210projects.Build.Types.LittleEndian;

import com.badlogic.gdx.utils.BufferUtils;

public class ZIPResource extends IResource {

	private class ZRESHANDLE extends RESHANDLE
	{
		public BufferedInputStream bis;
		public int pos;

		public ZRESHANDLE(ZipEntry entry) throws IOException {
			BufferedInputStream bis = new BufferedInputStream(ZFile.getInputStream(entry));
			size = (int) entry.getSize();
			filename = toLowerCase(entry.getName());
			if(filename.contains("/")) filename = filename.replace("/", File.separator);
			fileformat = toLowerCase(filename.substring(filename.lastIndexOf('.') + 1));
			this.fileid = -1;
			String fileid = entry.getComment();
			if(fileid != null && !fileid.isEmpty()) {
				fileid = fileid.replaceAll("[^0-9]", ""); 
				this.fileid = Integer.parseInt(fileid);
			}
			paktype = ZIP;
			this.bis = bis;
		}
	}

	private int NumFiles;
	private ZipFile ZFile;
	private List<ZRESHANDLE> files = new ArrayList<ZRESHANDLE>();

	public ZIPResource(String FileName) throws Exception
	{
		if(FileName != null && !FileName.isEmpty()) {
			ZFile = new ZipFile(FilePath + FileName);
			ArrayList<ZipEntry> zfilelist = new ArrayList<ZipEntry>(Collections.list(ZFile.entries()));
			NumFiles = zfilelist.size();
			if(NumFiles != 0) {
				Console.Println("Found " + NumFiles + " files in " + FileName + " archive", 0);
				for(int i = 0; i < NumFiles; i++) {
					ZipEntry entry = zfilelist.get(i);
					ZRESHANDLE res = new ZRESHANDLE(entry);
					files.add(res);
				}
			}
		}
	}

	@Override
	public int Lookup(String filename) {
		for(int i = NumFiles - 1; i >= 0; i--)
		{
			boolean bad = false;
			for(int j = 0; j < filename.length(); j++)
			{
				if (filename != null && filename.isEmpty()) break;
				String compare = files.get(i).filename;
				if (Compare(filename, compare, j))
					{ bad = true; break; }
			}
			if(bad) continue;

			files.get(i).pos = 0;
			return i;
		}
		return -1;
	}
	
	@Override
	public int Read(int filenum, byte[] buffer, int leng)
	{
		if(filenum < 0) return -1;
		
		ZRESHANDLE file = files.get(filenum);
		byte[] buf = Lock(filenum);
		if(file.pos >= buf.length) return -1;
		int pos = file.pos;
		leng = Math.min(leng, file.size-file.pos);
		System.arraycopy(buf, pos, buffer, 0, leng);
		file.pos += leng;
		return(leng);
	}
	
	@Override
	public int Read(int filenum, int len) {
		if(filenum < 0) return -1;
		
		ZRESHANDLE file = files.get(filenum);
		byte[] buf = Lock(filenum);
		if(file.pos >= buf.length) return -1;
		int pos = file.pos;
		
		file.pos += len;

		if(len == 1)
			return buf[pos] & 0xFF;
		else if(len == 2) 
			return LittleEndian.getShort(buf, pos);
		else if(len == 4) 
			return LittleEndian.getInt(buf, pos);
		
		return 0;
	}
	
	private byte[] getBytes(ZRESHANDLE file)
	{
		if(file.buffer == null) {
			file.buffer = new byte[file.size];
			try {
				file.bis.read(file.buffer);
				file.bis.close();
				file.bis = null;
			} catch(Exception e) { e.printStackTrace(); return null; }
		}
		return file.buffer;
	}
	
	@Override
	public byte[] Lock(int handle) {
		if(handle == -1) return null;
		ZRESHANDLE file = files.get(handle);
		return getBytes(file);
	}
	
	@Override
	public ByteBuffer bLock(int handle) {
		if(handle == -1) return null;
		ZRESHANDLE file = files.get(handle);
		if(file.byteBuffer == null) {
			file.byteBuffer = BufferUtils.newByteBuffer(file.size);
			file.byteBuffer.put(getBytes(file));
		}
		file.byteBuffer.rewind();
		return file.byteBuffer;
	}

	@Override
	public int Seek(int handle, int offset, int whence) {
		if(handle < 0) return -1;
		
		ZRESHANDLE file = files.get(handle);
		switch (whence)
        {
        	case SEEK_SET:
        		file.pos = offset; break;
        	case SEEK_END:
        		file.pos = file.size+offset;
	            break;
        	case SEEK_CUR:
        		file.pos += offset; break;
        }
        return file.pos;
	}
	
	@Override
	public int Size(int filenum) {
		if(filenum < 0) return -1;
		
		ZRESHANDLE file = files.get(filenum);
		return file.size;
	}

	@Override
	public String Name(int handle) {
		if(handle < 0) return null;
		ZRESHANDLE file = files.get(handle);
		return file.filename;
	}

	@Override
	public int Lookup(int fileId, String type) {
		if(type == null) {
			System.err.println("type == null");
			return -1;
		}

		for(int i = 0; i < NumFiles; i++) {
			ZRESHANDLE file = files.get(i);
			if(Bstrcasecmp(type, file.fileformat) == 0) {
				if(fileId == file.fileid) 
					return i;
			} 
		}
		return -1;
	}
	
	@Override
	public int FileId(int handle) {
		if(handle < 0) return -1;
		
		ZRESHANDLE file = files.get(handle);
		return file.fileid;
	}
	
	@Override
	public int Pos() {
		return -1;
	}
	
	@Override
	public int Close(int handle) {
		return 0;
	}

	@Override
	public List<RESHANDLE> fList() {
		List<RESHANDLE> list = new ArrayList<RESHANDLE>();
		list.addAll(files);
		return list;
	}

	@Override
	public void Dispose() {
		try {
			ZFile.close();
		} catch (IOException e) {}
	}
	

	@Override
	public int FilePos(int handle) {
		if(handle < 0) return -1;
		ZRESHANDLE file = files.get(handle);
		return file.pos;
	}
}