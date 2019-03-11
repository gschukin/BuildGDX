package ru.m210projects.Build.Pattern.MenuItems;

import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.FileHandle.Compat.FileUserdir;
import static ru.m210projects.Build.Gameutils.*;
import static ru.m210projects.Build.Strhandler.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import com.badlogic.gdx.Input.Keys;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Pattern.BuildFont;
import ru.m210projects.Build.Pattern.BuildFont.TextAlign;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;
import ru.m210projects.Build.Pattern.Tools.SaveManager;
import ru.m210projects.Build.Pattern.Tools.SaveManager.SaveInfo;

public class MenuSlotList extends MenuList
{
	public boolean deleteQuestion;
	
	public List<SaveInfo> text;
	public MenuProc updateCallback;
	public MenuProc confirmCallback;
	public List<SaveInfo> displayed;
	
	public final boolean saveList;
	public boolean typing;
	public char[] typingBuf = new char[16];
	public String typed;
	public int yHelpText;
	public int specPal;
	public int transparent = 1;
	
	protected SaveManager saveManager;
	protected Engine draw;
	protected int nBackground;
	protected int listPal;
	
	public MenuSlotList(Engine draw, SaveManager saveManager, BuildFont font, int x, int y, int yHelpText, int width, 
			int nListItems, MenuProc updateCallback, MenuProc confirmCallback, int listPal, int specPal, int nBackground, boolean saveList ) {
		
		super(null, font, x, y, width, 0, null, null, nListItems);
		this.draw = draw;
		this.saveManager = saveManager;
		this.nBackground = nBackground;
		
		this.typing = false;
		this.text = saveManager.getList();
		this.nListItems = nListItems;
	
		this.updateCallback = updateCallback;
		this.confirmCallback = confirmCallback;
		this.saveList = saveList;
		this.displayed = new ArrayList<SaveInfo>();
		this.yHelpText = yHelpText;
		this.specPal = specPal;
		this.listPal = listPal;
	}
	
	public String FileName()
	{
		int ptr = l_nFocus;
		if(saveList) ptr--;
		if(ptr == -1 || displayed.size() == 0)
			return "Empty slot";
		return displayed.get(ptr).filename;
	}
	
	public String SaveName()
	{
		int ptr = l_nFocus;
		if(saveList) ptr--;
		if(ptr == -1 || displayed.size() == 0)
			return "Empty slot";
		return displayed.get(ptr).name;
	}
	
	@Override
	public void draw(MenuHandler handler) {
		this.len = displayed.size();
		
		draw.rotatesprite((x + width / 2 - 5) << 16, (y - 3) << 16, 65536, 0, nBackground, 128, 0, 10 | 16 | transparent, 0, 0, coordsConvertXScaled(x + width, ConvertType.Normal), coordsConvertYScaled(y + nListItems * mFontOffset() + 3));
		
		if(displayed.size() > 0) {
			int py = y, pal;
			if(saveList) len += 1;

			for(int i = l_nMin; i >= 0 && i < l_nMin + nListItems && i < len; i++) {	
				int ptr = i;
				if(saveList) ptr -= 1;
				
				int shade = handler.getShade(i == l_nFocus && !deleteQuestion? m_pMenu.m_pItems[m_pMenu.m_nFocus] : null);
				char[] rtext;
				if(i == 0 && saveList)
					rtext = toCharArray("New savegame");
				else rtext = toCharArray(displayed.get(ptr).name);
				
				if(ptr >= 0 && (displayed.get(ptr).filename.equals("autosave.sav") 
						|| displayed.get(ptr).filename.startsWith("quicksav")))
					pal = specPal;
				else pal = listPal;
				
				if ( i == l_nFocus ) {
					if(m_pMenu.mGetFocusedItem(this)) {
						if(typing) {
							Arrays.fill(typingBuf, (char) 0);
							System.arraycopy(getInput().getMessageBuffer(), 0, typingBuf, 0, getInput().getMessageLength()+1);
							rtext = typingBuf;
							shade = -128;
						}
					}
				}
				
				font.drawText(x + width / 2, py, rtext,shade, pal, TextAlign.Left, 2, true);

				py += mFontOffset();
			}
		} else {
			int py = y;	
			int shade = handler.getShade(l_nFocus != -1 ? m_pMenu.m_pItems[m_pMenu.m_nFocus] : null);
			char[] rtext;
			
			
			if(saveList) {
				rtext = toCharArray("New saved game");
				if(typing) {
					Arrays.fill(typingBuf, (char) 0);
					System.arraycopy(getInput().getMessageBuffer(), 0, typingBuf, 0, getInput().getMessageLength()+1);
					rtext = typingBuf;
					shade = -128;
				}
			} else rtext = toCharArray("List is empty");

			font.drawText(x + width / 2, py, rtext,shade, listPal, TextAlign.Left, 2, false);
		}

		pal = 0;
		if(deleteQuestion)
		{
			draw.setpalettefade(0, 0, 0, 48);
			draw.showfade();
			
			int shade = handler.getShade(m_pMenu.m_pItems[m_pMenu.m_nFocus]);
			
			char[] ctext = toCharArray("Do you want to delete \"" + SaveName() + "\"");
			font.drawText(160 - font.getWidth(ctext) / 2, 100, ctext, shade, pal, TextAlign.Left, 2, false);
			ctext = toCharArray("[Y/N]");
			font.drawText(160 - font.getWidth(ctext) / 2, 110, ctext, shade, pal, TextAlign.Left, 2, false);
		} else {
			char[] ctext = toCharArray("Press \"DELETE\" to remove the savegame file");
			
			font.drawText(160 - font.getWidth(ctext) / 2, yHelpText, ctext, 0, pal, TextAlign.Left, 2, false);
		}
	}

	@Override
	public boolean callback(MenuHandler handler, MenuOpt opt) {
		if(deleteQuestion)
		{
			if(getInput().getKey(Keys.Y) != 0 || opt == MenuOpt.ENTER) {
				saveManager.delete(FileName());
				updateList();
			    getInput().setKey(Keys.Y, 0);
			    if(l_nFocus >= displayed.size()) {
			    	int len = displayed.size();
			    	if(saveList) len += 1;
			    	l_nFocus = len - 1;
			    	l_nMin = len - nListItems;
					if(l_nMin < 0) l_nMin = 0;
			    }
			    if(updateCallback != null)
					updateCallback.run(handler, this);
			    deleteQuestion = false;
			}
			if(getInput().getKey(Keys.N) != 0 || opt == MenuOpt.ESC || opt == MenuOpt.RMB) {
				
				getInput().setKey(Keys.N, 0);
				deleteQuestion = false;
			}
			
			return false;
		}
		
		int focus = l_nFocus; 
		int len = displayed.size();
		if(saveList) {
			len += 1;
			focus -= 1;
		}
		if(typing) 
		{
			if(opt != MenuOpt.ESC) {
				if(getInput().putMessage(typingBuf.length, true, false, false) == 1)
				{
					typed = new String(getInput().getMessageBuffer(), 0, getInput().getMessageLength());
					typing = false;
					if(confirmCallback != null)
						confirmCallback.run(handler, this);	
				}
			} else typing = false;
		} else {
			switch(opt)
			{
				case DELETE:
					if((!saveList && displayed.size() > 0) || l_nFocus != 0)
						deleteQuestion = true;
					return false;
				case MWUP:
					if(l_nMin > 0)
						l_nMin--;
					return false;
				case MWDW:
					if(text != null)
						if(l_nMin < len - nListItems)
							l_nMin++;
					return false;
				case UP:
					l_nFocus--;
					if(l_nFocus >= 0 && l_nFocus < l_nMin)
						if(l_nMin > 0) l_nMin--;
					if(l_nFocus < 0) {
						l_nFocus = len - 1;
						l_nMin = len - nListItems;
						if(l_nMin < 0) l_nMin = 0;
					}
					if(updateCallback != null)
						updateCallback.run(handler, this);
					return false;
				case DW:
					l_nFocus++;
					if(l_nFocus >= l_nMin + nListItems && l_nFocus < len)
						l_nMin++;
					if(l_nFocus >= len) {
						l_nFocus = 0;
						l_nMin = 0;
					}
					if(updateCallback != null)
						updateCallback.run(handler, this);
					return false;
				case LEFT:
					m_pMenu.mNavUp();
					if(updateCallback != null)
						updateCallback.run(handler, this);
					return false;
				case RIGHT:
					m_pMenu.mNavDown();
					if(updateCallback != null)
						updateCallback.run(handler, this);
					return false;
				case ENTER:
				case LMB:
					if(l_nFocus != -1 && len > 0) {
						if(saveList) {
							if(l_nFocus == 0) getInput().initMessageInput(null); 
							else getInput().initMessageInput(displayed.get(focus).name);
				        	typing = true;
							
							return false;
						}
						if(confirmCallback != null)
							confirmCallback.run(handler, this);	
						getInput().resetKeyStatus();
					}
					
					return false;
				case ESC:
				case RMB:
					return true;
				case PGUP:
					l_nFocus -= (nListItems - 1);
					if(l_nFocus >= 0 && l_nFocus < l_nMin)
						if(l_nMin > 0) l_nMin -= (nListItems - 1);
					if(l_nFocus < 0 || l_nMin < 0) {
						l_nFocus = 0;
						l_nMin = 0;
					}
					return false;
				case PGDW:
					l_nFocus += (nListItems - 1);
					if(l_nFocus >= l_nMin + nListItems && l_nFocus < len)
						l_nMin += (nListItems - 1);
					if(l_nFocus >= len || l_nMin > len - nListItems) {
						l_nFocus = len - 1;
						if(len >= nListItems)
							l_nMin = len - nListItems;
						else l_nMin = len - 1;
					}
					return false;
				case HOME:
					l_nFocus = 0;
					l_nMin = 0;
					return false;
				case END:
					l_nFocus = len - 1;
					if(len >= nListItems)
						l_nMin = len - nListItems;
					else l_nMin = len - 1;
					return false;
				default:
					return false;
			}
		}
		return false;
	}
	
	@Override
	public void open() {
		l_nMin = l_nFocus = 0;
		
		Iterator<SaveInfo> i = text.iterator();
		while (i.hasNext()) {
			SaveInfo s = i.next();
			File file = new File(FileUserdir+s.filename);
			if(!file.exists())
				i.remove();
		}
		
		updateList();
		
		if(updateCallback != null)
			updateCallback.run(null, this);
	}
	
	public void updateList()
	{
		displayed.clear();
		displayed.addAll(text);
		if(saveList) {
			Iterator<SaveInfo> i = displayed.iterator();
			while (i.hasNext()) { 
				SaveInfo s = i.next();
				if(s.filename.equals("autosave.sav") 
						|| s.filename.startsWith("quicksav"))
					i.remove();
			}
		}
	}
	
	@Override
	public void close() {
		deleteQuestion = false;
		typing = false;
	}

	@Override
	public boolean mouseAction(int mx, int my) {
		if(deleteQuestion || typing)
			return false;

		if(displayed.size() > 0) {
			int px = x, py = y;
			int len = displayed.size();
			if(saveList) len += 1;
			
			int ol_nFocus = l_nFocus;
			for(int i = l_nMin; i >= 0 && i < l_nMin + nListItems && i < len; i++) {	
			    
				if(mx > px && mx < px + width - 14)
					if(my > py && my < py + font.nHeight)
					{
						l_nFocus = i;
						if(ol_nFocus != i && updateCallback != null)
							updateCallback.run(null, this);
						return true;
					}
			    
				py += mFontOffset();
			}
		}
		return false;
	}

}