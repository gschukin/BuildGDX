// This file is part of BuildGDX.
// Copyright (C) 2019  Alexander Makarov-[M210] (m210-2007@mail.ru)
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

package ru.m210projects.Build.Types;

public class BuildPos {
	
	public int x, y, z;
	public int ang, sectnum;

	public BuildPos() {
	}

	public BuildPos(int x, int y, int z, int ang, int sectnum) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.ang = ang;
		this.sectnum = sectnum;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public int getAng() {
		return ang;
	}

	public int getSectnum() {
		return sectnum;
	}
}
