//This file is part of BuildGDX.
//Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
//
//BuildGDX is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//BuildGDX is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.Pattern.MenuItems;

import java.util.HashMap;
import java.util.List;

import ru.m210projects.Build.FileHandle.FileEntry;

public abstract class BrowserFileType {

	public final int pal;
	public final String extension;

	public BrowserFileType(String extension, int pal)
	{
		this.extension = extension;
		this.pal = pal;
	}
	
	public abstract void callback(MenuFileBrowser item);
	
	public abstract void init(FileEntry file, List<String> list, HashMap<String, BrowserFileType> typeHash);
}
