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

import static ru.m210projects.Build.Net.Mmulti.connecthead;
import static ru.m210projects.Build.Net.Mmulti.inet;
import static ru.m210projects.Build.Net.Mmulti.initmultiplayers;
import static ru.m210projects.Build.Net.Mmulti.myconnectindex;

import com.badlogic.gdx.ScreenAdapter;

import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Architecture.BuildGraphics.Option;
import ru.m210projects.Build.Architecture.BuildFrame.FrameType;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.osd.OsdColor;

public abstract class ConnectAdapter extends ScreenAdapter {

	protected BuildGame game;
	private NetFlag gNetFlag;
	private String[] gNetParam;
	private int ConnectStep = 0;
	private final int nTile;
	private final Font style;
	
	public enum NetFlag {
		Create, Connect
    }
	
	public ConnectAdapter(BuildGame game, int nTile, Font style)
	{
		this.game = game;
		this.nTile = nTile;
		this.style = style;
	}

	public abstract void back();
	
	public abstract void connect();
	
	@Override
	public void show() {
		game.pNet.ResetNetwork();
		initmultiplayers(gNetParam, 0);
		ConnectStep = 0;
	}

	public ScreenAdapter setFlag(NetFlag flag, String[] param) {
		this.gNetFlag = flag;
		this.gNetParam = param;
		return this;
	}

	@Override
	public void render(float delta) {
		game.pEngine.clearview(0);
		game.pEngine.rotatesprite(160 << 16, 100 << 16, 65536, 0, nTile, 0, 0, 2 | 8 | 64);

		switch (gNetFlag) {
		case Create:
		case Connect:
			if (inet.waiting()) {
				if (myconnectindex == connecthead) {
					style.drawTextScaled(160, 150, "Local IP: " + inet.myip, 1.0f, -128, 0, TextAlign.Center, Transparent.None, ConvertType.Normal, false);
					if (inet.useUPnP) {
						String extip = "Public IP: ";
						if (inet.extip != null) {
							extip += inet.extip;
						}

						style.drawTextScaled(160, 160, extip, 1.0f, -128, 0, TextAlign.Center, Transparent.None, ConvertType.Normal, false);
					}
				}

				if (inet.message != null && !inet.message.isEmpty()) {
					style.drawTextScaled(160, 180, inet.message, 1.0f, -128, 0, TextAlign.Center, Transparent.None, ConvertType.Normal, false);
				} else {
					style.drawTextScaled(160, 180, "Initializing...", 1.0f, -128, 0, TextAlign.Center, Transparent.None, ConvertType.Normal, false);
				}
					
				game.pEngine.nextpage();
				return;
			}

			if (inet.netready == 0) {
				Console.out.println(inet.message, OsdColor.YELLOW);
				back();
				
				game.pEngine.nextpage();
				return;
			}

			if (ConnectStep == 0) {
				if (inet.message != null) {
					style.drawTextScaled(160, 180, inet.message, 1.0f, -128, 0, TextAlign.Center, Transparent.None, ConvertType.Normal, false);
				} else {
					style.drawTextScaled(160, 180, "Connected! Waiting for other players...", 1.0f, -128, 0, TextAlign.Center, Transparent.None, ConvertType.Normal, false);
				}
				ConnectStep = 1;

				game.pNet.StartWaiting(5000);
				
				game.pEngine.nextpage();
				return;
			}

			connect();
		}

		game.pEngine.nextpage();
	}
	
	@Override
	public void pause () {
//		if (BuildGdx.graphics.getFrameType() == FrameType.GL) {
//			BuildGdx.graphics.extra(Option.GLDefConfiguration);
//		}
	}

	@Override
	public void resume () {
		game.updateColorCorrection();
	}
}
