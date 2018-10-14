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

package ru.m210projects.Build.Input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import ru.m210projects.Build.Types.BGraphics;
import ru.m210projects.Build.Types.BDisplay.DisplayType;

public class GPManager {
	
	public final static int MAXBUTTONS = 64;
	public final static int MAXPOV = 4;
	public final static int MAXAXIS = 12;

	private Array<Gamepad> gamepads;
	private float deadZone = 0.01f;

	boolean TestGamepad = false;
	
	public GPManager()
	{
		try {
			gamepads = new Array<Gamepad>();
			Array<Controller> controllers = null;
			if(((BGraphics) Gdx.graphics).getDisplayType() != DisplayType.Software)
				controllers = Controllers.getControllers();
			
			if(controllers != null && controllers.size > 0) {
				for(int i = 0; i < controllers.size; i++) {
					gamepads.add(new Gamepad(controllers.get(i)));
				}
			}
		} catch (Exception e) { }
		
		if(TestGamepad)
			gamepads.add(new Gamepad(new TestController()));
	}

	private void checkDeviceIndex(int index)
	{
		if (index < 0 || index >= gamepads.size)
			throw new IllegalArgumentException("Device index is invalid.");
	}

	public int getControllers()
	{
		return gamepads.size;
	}
	
	public String getControllerName(int deviceIndex)
	{
		checkDeviceIndex(deviceIndex);
		return gamepads.get(deviceIndex).getName();
	}

	public void setDeadZone(float value)
	{
		this.deadZone = value;
	}

	public int getButtonCount(int deviceIndex)
	{
		checkDeviceIndex(deviceIndex);
		return gamepads.get(deviceIndex).getButtonCount();
	}
	
	public boolean getButton(int deviceIndex, int buttonCode)
	{
		checkDeviceIndex(deviceIndex);
		return gamepads.get(deviceIndex).getButton(buttonCode);
	}
	
	public void handler()
	{
		for(int i = 0; i < gamepads.size; i++)
			gamepads.get(i).ButtonHandler();
	}
	
	public void resetButtonStatus()
	{
		for(int i = 0; i < gamepads.size; i++) {
			gamepads.get(i).resetButtonStatus();	
		}
	}
	
	public boolean buttonStatusOnce(int deviceIndex, int buttonCode)
	{
		checkDeviceIndex(deviceIndex);
		return gamepads.get(deviceIndex).buttonStatusOnce(buttonCode);
	}
	
	public boolean buttonPressed(int deviceIndex, int buttonCode)
	{
		checkDeviceIndex(deviceIndex);
		return gamepads.get(deviceIndex).buttonPressed(buttonCode);
	}
	
	public boolean buttonStatus(int deviceIndex, int buttonCode)
	{
		checkDeviceIndex(deviceIndex);
		return gamepads.get(deviceIndex).buttonStatus(buttonCode);
	}

	public Vector2 getStickValue(int deviceIndex, int aCode1, int aCode2)
	{
		checkDeviceIndex(deviceIndex);
		return gamepads.get(deviceIndex).getStickValue(aCode1, aCode2, deadZone);
	}
}
