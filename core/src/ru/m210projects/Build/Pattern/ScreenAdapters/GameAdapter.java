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

package ru.m210projects.Build.Pattern.ScreenAdapters;

import static ru.m210projects.Build.Engine.totalclock;
import static ru.m210projects.Build.Net.Mmulti.*;
import static ru.m210projects.Build.Pattern.BuildNet.*;

import com.badlogic.gdx.ScreenAdapter;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Pattern.BuildConfig;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.BuildNet;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler;

public abstract class GameAdapter extends ScreenAdapter {
	
	protected BuildGame game;
	protected BuildNet net;
	protected MenuHandler menu;
	protected Engine engine;
	protected BuildConfig cfg;
	
	public boolean gPaused;
	
	LoadingAdapter load;

	public GameAdapter(final BuildGame game, LoadingAdapter load)
	{
		this.game = game;
		this.net = game.net;
		this.menu = game.menu;
		this.engine = game.engine;
		this.cfg = game.cfg;
		this.load = load;
	}

	public abstract void ProcessFrame(BuildNet net);
	
	public abstract void DrawFrame(float smooth);
	
	public abstract void KeyHandler();
	
	protected abstract boolean prepareboard(String map);
	
	public void loadboard(final String map)
	{
		net.ready2send = false;
		game.changeScreen(load);
		load.init(new Runnable() {
			@Override
			public void run() {
				if(prepareboard(map))
					startboard();
			}
		});
	}

	protected void startboard() 
	{
		net.WaitForAllPlayers(0);
		engine.sampletimer(); //update timer before reset
		
		totalclock = 0;
		net.ResetTimers();
		net.ready2send = true;
		
		game.changeScreen(this);
		System.gc();
	}
	
	@Override
	public void show() {
		menu.mClose();
	}
	
	@Override
	public void render(float delta) {

		KeyHandler();
		
		if (numplayers > 1) {
			engine.faketimerhandler();
			
			net.GetPackets();
			while (net.gPredictTail < net.gNetFifoHead[myconnectindex] && !gPaused) 
				net.UpdatePrediction(net.gFifoInput[net.gPredictTail & kFifoMask][myconnectindex]); 

		} else net.bufferJitter = 0;
		
		int i;
		while (net.gNetFifoHead[myconnectindex] - net.gNetFifoTail > net.bufferJitter && !game.gExit) {
			for (i = connecthead; i >= 0; i = connectpoint2[i])
				if (net.gNetFifoTail == net.gNetFifoHead[i]) break;
			if (i >= 0) break;

			ProcessFrame(net);
		}
		
		net.CheckSync();
		
		DrawFrame(engine.getsmoothratio());

		if (cfg.gShowFPS)
			engine.printfps(cfg.gFpsScale);
		
		engine.nextpage();
		engine.sampletimer();
	}

}
