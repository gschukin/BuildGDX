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

package ru.m210projects.Build.Pattern;

import ru.m210projects.Build.Input.GPManager;
import ru.m210projects.Build.OnSceenDisplay.OSDFunc;
import ru.m210projects.Build.Render.Renderer;
import ru.m210projects.Build.Render.Renderer.RenderType;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler;
import ru.m210projects.Build.Pattern.MenuItems.SliderDrawable;

public abstract class BuildFactory {
	
	public BuildFactory() {
		throw new UnsupportedOperationException("not implemented"); 
	}

	public String[] resources;
	public BuildFactory(String... resources) {
		this.resources = resources;
	}

	public abstract int getInitTile();
	
	public abstract BuildEngine engine() throws Exception;
	
	public abstract Renderer renderer(RenderType type);
	
	public abstract BuildControls input(GPManager gpmanager);
	
	public abstract OSDFunc console();
	
	public abstract MenuHandler menus();
	
	public abstract FontHandler fonts();
	
	public abstract BuildNet net();
	
	public abstract SliderDrawable slider();

}
