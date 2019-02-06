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

import static ru.m210projects.Build.Engine.getInput;
import static ru.m210projects.Build.Engine.xdim;
import static ru.m210projects.Build.Engine.ydim;
import static ru.m210projects.Build.Render.VideoMode.validmodes;
import static ru.m210projects.Build.Gameutils.*;

import java.util.List;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Pattern.BuildFont;
import ru.m210projects.Build.Pattern.BuildFont.Align;
import ru.m210projects.Build.Pattern.BuildFont.TextAlign;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;

public class MenuResolutionList extends MenuList {
	
	private int touchY;
	private int scrollX;
	public boolean scrollTouch;
	
	private int nBackground;
	protected Engine draw;

	public MenuResolutionList(Engine draw, List<char[]> text, BuildFont font, int x, int y,
			int width, int align, int nItemHeight, BuildMenu nextMenu,
			MenuProc specialCall, int nListItems, int nBackground) {
		super(text, font, x, y, width, align, nItemHeight, nextMenu, specialCall, nListItems);
		this.nBackground = nBackground;
		this.draw = draw;
	}
	
	@Override
	public void open() {
		l_nFocus = -1;
		for (int m = 0; m < validmodes.size(); m++) {
			if ((validmodes.get(m).xdim == xdim)
					&& (validmodes.get(m).ydim == ydim)) {
				l_nFocus = m;
				break;
			}
		}
		
		if (l_nFocus != -1) {
			if (l_nFocus >= l_nMin + nListItems)
				l_nMin = l_nFocus - nListItems + 1;
		} 
	}
	
	@Override
	public void draw(MenuHandler handler) {
		draw.rotatesprite((x - 10) << 16, (y - 8) << 16, 65536, 0, nBackground, 128, 0, 10 | 16 | 33, 0, 0, coordsConvertXScaled(x+width+12, 0), coordsConvertYScaled(y+114));
		Align ali = font.getAlign(null);
		
		if(text.size() > 0) {
			int px = x, py = y;
			for(int i = l_nMin; i >= 0 && i < l_nMin + nListItems && i < text.size(); i++) {	
				int pal = handler.getPal(font, true);
				int shade = 0;
				if ( i == l_nFocus ) 
					shade = handler.getShade(m_pMenu.mGetFocusedItem(this));
			    if(align == 1) 
			        px = width / 2 + x - font.getAlign(text.get(i)).x / 2;
			    if(align == 2) 
			        px = x + width - 1 - font.getAlign(text.get(i)).x;
			    font.drawText(px, py, text.get(i), shade, pal, TextAlign.Left, 0, true);

				py += ali.y + nItemHeight;
			}
		} else 
		{
			int pal = handler.getPal(font, true);
			String text = "List is empty";
			
			int px = x, py = y;		
			if(align == 1) 
		        px = width / 2 + x - font.getAlign(text.toCharArray()).x / 2;
		    if(align == 2) 
		        px = x + width - 1 - font.getAlign(text.toCharArray()).x;
		    int shade = handler.getShade(m_pMenu.mGetFocusedItem(this));
		    
		    font.drawText(px, py, text.toCharArray(), shade, pal, TextAlign.Left, 0, true);
		}
		
		//Scroller
		int nList = BClipLow(text.size() - nListItems, 1);
		int posy = ((nListItems * ali.y - 13)) * l_nMin / nList;

		scrollX = x + width;
		handler.mDrawSlider(scrollX, y, posy, 87, true);
	}
	
	@Override
	public boolean callback(MenuHandler handler, MenuOpt opt) {
		switch(opt)
		{
			case MWUP:
				if(l_nMin > 0)
					l_nMin--;
				return false;
			case MWDW:
				if(text != null)
					if(l_nMin < text.size() - nListItems)
						l_nMin++;
				return false;
			case UP:
				l_nFocus--;
				if(l_nFocus >= 0 && l_nFocus < l_nMin)
					if(l_nMin > 0) l_nMin--;
				if(l_nFocus < 0) {
					l_nFocus = text.size() - 1;
					l_nMin = text.size() - nListItems;
					if(l_nMin < 0) l_nMin = 0;
				}
				return false;
			case DW:
				l_nFocus++;
				if(l_nFocus >= l_nMin + nListItems && l_nFocus < text.size())
					l_nMin++;
				if(l_nFocus >= text.size()) {
					l_nFocus = 0;
					l_nMin = 0;
				}
				return false;
			case LEFT:
				m_pMenu.mNavUp();
				return false;
			case RIGHT:
				m_pMenu.mNavDown();
				return false;
			case ENTER:
			case LMB:
				
				if(opt == MenuOpt.LMB && scrollTouch)
				{
					l_nFocus = -1;
					int nList = BClipLow(text.size() - nListItems, 1);
					int nRange = nListItems * font.getAlign(null).y - 16;
					int py = y;
					float dr = (float)(touchY - py) / nRange;

					l_nMin = (int) BClipRange(dr * nList, 0, nList);
					
					return false;
				}

				if(l_nFocus != -1 && text.size() > 0) {
					specialCall.run(handler, this);
					if ( nextMenu != null )
				    	handler.mOpen(nextMenu, -1);
				}
				getInput().resetKeyStatus();
				return false;
			case ESC:
			case RMB:
				//l_nFocus = l_nMin = 0;
				return true;
			default: 
				return false;
		}
	}

	@Override
	public boolean mouseAction(int mx, int my) {
		
		if(!BuildGdx.input.isTouched()) 
			scrollTouch= false;
		
		touchY = my;
		if(mx > scrollX && mx < scrollX + 14) 
		{
			if(BuildGdx.input.isTouched())
				scrollTouch = true;
			else scrollTouch = false;
			return true;
		}
		
		if(!scrollTouch && text.size() > 0) {
			Align ali = font.getAlign(null);
			int px = x, py = y;
			for(int i = l_nMin; i >= 0 && i < l_nMin + nListItems && i < text.size(); i++) {	
				if(align == 1) {
			    	ali = font.getAlign(text.get(i));
			        px = width / 2 + x - ali.x / 2;
			    }
			    if(align == 2) {
			    	ali = font.getAlign(text.get(i));
			        px = x + width - 1 - ali.x;
			    }

			    if(mx > px && mx < px + ali.x)
					if(my > py && my < py + ali.y)
					{
						l_nFocus = i;
						return true;
					}
			    
				py += ali.y + nItemHeight;
			}
		}
		return false;
	}
}
