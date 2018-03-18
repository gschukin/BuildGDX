package ru.m210projects.Build.Input;

import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Input.Keymap.*;
import static ru.m210projects.Build.Strhandler.isalpha;
import static ru.m210projects.Build.Strhandler.isdigit;

import java.util.Arrays;

import ru.m210projects.Build.Types.MessageCallback;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

public class KeyInput {
	public static final int KEYFIFOSIZ = 64;
	public byte keystatus[] = new byte[256 + 1], 
		keyfifo[] = new byte[KEYFIFOSIZ], keyfifoplc, keyfifoend;
	public int locmessagelen;
	public static final char[] lockeybuf = new char[40]; 
	
	public boolean keyPressed = false;
	public boolean[] hitkey = new boolean[256];
	
	public static char gdxscantoasc[] = { 0, 0, 0, 0, 0, 0, 0, '0', '1', '2',
			'3', '4', '5', '6', '7', '8', '9', '*', 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
			'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
			'z', ',', '.', 0, 0, 0, 0, 0, 32, 0, 0, 0, 0, 0, '`', '-', '=',
			'[', ']', '\\', ';', 39, '/', 0, 0, 0, 0, '+', 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };
	
	public static char gdxscantoascwithshift[] = { 0, 0, 0, 0, 0, 0, 0, ')', '!', '@',
		'#', '$', '%', '^', '&', '*', '(', '*', 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
		'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
		'Z', '<', '>', 0, 0, 0, 0, 0, 32, 0, 0, 0, 0, 0, '~', '_', '+',
		'{', '}', '|', ':', 34, '?', 0, 0, 0, 0, '+', 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };
	
	public int getKey(int key)
	{
		return keystatus[key];
	}

	public void setKey(int key, int state) {
		keystatus[key] = (byte) state;
		if (state != 0) {
			keyfifo[keyfifoend] = (byte) key;
			keyfifo[(keyfifoend + 1) & (KEYFIFOSIZ - 1)] = (byte) state;
			keyfifoend = (byte) ((keyfifoend + 2) & (KEYFIFOSIZ - 1));
		}
	}
	
	public void resetKeyStatus()
	{
		Arrays.fill(keystatus, (byte)0);
	}
	
	public int handleevents() { //need make as part of Engine XXX
		if(Gdx.input == null) //not initialized
			return 0;

		int rv = 0;
		keyPressed = false;
		keystatus[ANYKEY] = 0;
		for (int kb = 1; kb < 256; kb++) {
			if(kb >= MOUSE_WHELLUP && kb <= MOUSE_BUTTON11)
				continue;
			
			if (Gdx.input.isKeyPressed(kb)) {
				keyPressed = true;
				if (!hitkey[kb]) {
					keystatus[ANYKEY] = 1;
					setKey(kb, 1);
					hitkey[kb] = true;
				}
			} else {
				setKey(kb, 0);
				hitkey[kb] = false;
			}
		}

		//Mouse buttons handler
		for (int kb = 0; kb < 10; kb++) {
			int index = MOUSE_LBUTTON + kb;
			if (Gdx.input.isButtonPressed(kb)) {
				keyPressed = true;
				if (!hitkey[index]) {
					keystatus[ANYKEY] = 1;
					setKey(index, 1);
					hitkey[index] = true;
				}
			} else {
				setKey(index, 0);
				hitkey[index] = false;
			}
		}
		
		//Mouse wheel handler
		int wheel = (int)Gdx.input.getRoll();
		if(wheel > 0) {
			keyPressed = true;
			if (!hitkey[MOUSE_WHELLUP]) {
				keystatus[ANYKEY] = 1;
				setKey(MOUSE_WHELLUP, 1);
				hitkey[MOUSE_WHELLUP] = true;
			}
		} else if(wheel < 0) {
			keyPressed = true;
			if (!hitkey[MOUSE_WHELLDN]) {
				keystatus[ANYKEY] = 1;
				setKey(MOUSE_WHELLDN, 1);
				hitkey[MOUSE_WHELLDN] = true;
			}
		}
		else {
			setKey(MOUSE_WHELLUP, 0);
			hitkey[MOUSE_WHELLUP] = false;
			setKey(MOUSE_WHELLDN, 0);
			hitkey[MOUSE_WHELLDN] = false;
		}

		return rv;
	}
	
	public boolean keyStatusOnce(int keyId)
	{
		if(keyId > 0 && getKey(keyId) == 1) {
			setKey(keyId, 0);
			return true;
		}
		return false;
	}
	
	public boolean keyStatus(int keyId)
	{
		if(keyId > 0 && getKey(keyId) == 1) 
			return true;

		return false;
	}
	
	public boolean keyPressed()
	{
		return keyPressed;
	}
	
	public boolean keyPressed(int kb)
	{
		return hitkey[kb];
	}
	
	public void keyPressed(int kb, boolean press)
	{
		hitkey[kb] = press;
	}
	
	public void initMessageInput(String text)
	{
		keyfifoplc = keyfifoend = 0;
		if(text == null) {
			locmessagelen = 0;
	    	lockeybuf[locmessagelen] = 0;
		} else
		{
			if(!text.isEmpty() && !text.equals("Empty")) {
        		int len = 0;
        		for(int i = 0; i < text.length(); i++)
        			if(text.charAt(i) != 0)
        				lockeybuf[len++] = text.charAt(i);
        		locmessagelen = len;
        	} 
		}
	}
	
	public char[] getMessageBuffer()
	{
		return lockeybuf;
	}
	
	public int getMessageLength()
	{
		return locmessagelen;
	}

	public int putMessage(int maxsize, boolean cursor, boolean overtype, boolean specialSymbols)
	{
		int ch, keystate;
    	while (keyfifoplc != keyfifoend) {
			ch = keyfifo[keyfifoplc] & 0xFF;
			keystate = keyfifo[(keyfifoplc + 1) & (KEYFIFOSIZ - 1)];
			keyfifoplc = (byte) ((keyfifoplc + 2) & (KEYFIFOSIZ - 1));
			if (keystate != 0) {
				if (ch == Keys.ESCAPE) { 
                	keystatus[Keys.ESCAPE]=0;
                    locmessagelen=0;
                    return -1;
                }
				if (ch == Keys.BACKSPACE) { 
                	keystatus[Keys.BACKSPACE]=0;
                	if (locmessagelen == 0)
                        break;
          
                	locmessagelen--;
                	lockeybuf[locmessagelen]=0;
                }
				if (ch == Keys.ENTER) {
					keystatus[Keys.ENTER] = 0;
					return 1;
				}
				if (locmessagelen < (maxsize-1) && ch < 128) {
                    ch=gdxscantoasc[ch];
                    if (ch != 0) {
                    	if (specialSymbols || (!specialSymbols && (isalpha((char)ch) || isdigit((char)ch) || ch == ' '))) {
                    		lockeybuf[locmessagelen++]=(char)ch;
                    	}
                    }
            	}
			}
    	}
    	if(cursor) {
	    	if ( (totalclock & 0x20) != 0 ) {
	    		lockeybuf[locmessagelen]= '_';
	    	} else lockeybuf[locmessagelen] = 0;
    	}
    	return 0;
	}
	
	public boolean putMessage(MessageCallback callback, boolean cursor)
	{
		int ch, keystate;
    	while (keyfifoplc != keyfifoend) {
    		ch = keyfifo[keyfifoplc] & 0xFF;
    		keystate = keyfifo[(keyfifoplc + 1) & (KEYFIFOSIZ - 1)];
			keyfifoplc = (byte) ((keyfifoplc + 2) & (KEYFIFOSIZ - 1));
			if (keystate != 0) {
				return callback.run(ch);
			}
    	}
    	if(cursor) {
	    	if ( (totalclock & 0x20) != 0 ) {
	    		lockeybuf[locmessagelen]= '_';
	    	} else lockeybuf[locmessagelen] = 0;
    	}
    	return false;
	}
	
	
}