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

package ru.m210projects.Build;

import java.util.Locale;

public class Strhandler {
	
	private static Locale usLocal = Locale.US;
	public static String toLowerCase(String text)
	{
		if(text != null)
			return text.toLowerCase(usLocal); //Turkish language issue
		return null;
	}

	//String handler
	
	static char[] tmp_buffer = new char[80];
	public static char[] toCharArray(String... text)
	{
		buildString(tmp_buffer, 0, text);
		
		return tmp_buffer;
	}
	
	public static int buildString(char[] buffer, int boffset, String... text)
	{
		int pos = boffset;
		for(int i = 0; i < text.length && pos < buffer.length; i++)
		{
			if(text[i] != null) {
				text[i].getChars(0, Math.min(text[i].length(), buffer.length), buffer, pos);
				pos += text[i].length();
			}
		}
		if(pos< buffer.length)
			buffer[pos] = 0;
		
		return pos;
	}
	
	public static int buildString(char[] buffer, int boffset, String text, int number)
	{
		int pos = boffset;
		text.getChars(0, Math.min(text.length(), buffer.length), buffer, pos);
		pos += text.length();
		
		int lnum = Bitoa(number, tmp_buffer);
		System.arraycopy(tmp_buffer, 0, buffer, pos, lnum);
		
		pos += lnum;
		if(pos < buffer.length)
			buffer[pos] = 0;
		
		return pos;
	}
	
	public static int buildString(char[] buffer, int boffset, String text, int number, int symbols)
	{
		int pos = boffset;
		text.getChars(0, Math.min(text.length(), buffer.length), buffer, pos);
		pos += text.length();
		
		int lnum = Bitoa(number, tmp_buffer, symbols);
		System.arraycopy(tmp_buffer, 0, buffer, pos, lnum);
		
		pos += lnum;
		if(pos < buffer.length)
			buffer[pos] = 0;
		
		return pos;
	}
	
	public static int indexOf(String target, byte[] source, int fromIndex) {
        if (fromIndex >= source.length) 
            return (target.length() == 0 ? source.length : -1);

        char first = target.charAt(0);
        int max = (source.length - target.length());

        for (int i = fromIndex; i <= max; i++) {
            if (source[i] != first) 
                while (++i <= max && source[i] != first);
            
            if (i <= max) {
                int j = i + 1;
                int end = j + target.length() - 1;
                for (int k = 1; j < end && source[j] == target.charAt(k); j++, k++);
                if (j == end) 
                    return i;
                
            }
        }
        return -1;
    }
	
		public static int Bsprintf(char[] b, int slen, int num, int align) {
			Bitoa(num, tmp_buffer);
			int len = Bstrlen(tmp_buffer);
			if( align == 0) {
				for(int i = 0; i < len && i < slen; i++) 
					b[i] = tmp_buffer[i];
			} else if( align == 1 ) {
				int dx = (slen - 1) - (len - 1);
				for(int i = slen - 1; i >= 0; i--) {
					if(i-dx >= 0)
						b[i] = tmp_buffer[i-dx];
					else b[i] = ' ';
				}
			}
			return slen;
		}
		
			public static String Bstrtoken(String s, char delim, String ptrptr, int chop)
			{
				String p, start = null;

			    if (ptrptr == null) return null;

			    if (s != null) p = s;
			    else p = ptrptr;

			    int pos = 0;
				while(pos < p.length() && p.charAt(pos) != ' ') pos++;
				if(pos == p.length()) {
					ptrptr = null;
				} else {
					int strpos = 0;
					if(chop != 0)
						strpos = pos+1;
					ptrptr = p.substring(strpos, p.length());
				}
				start = p.substring(0, pos);

			    return start;
			}
			
			public static int Bstrtoken(String s, char delim, int offset)
			{
			    if (s == null) return -1;
			    
			    int pos = offset;
				while(pos < s.length() && s.charAt(pos) != ' ') pos++;

			    return pos;
			}
			
			public static int Bstrchr(String str1, char sym) {
				for(int i = 0; i < str1.length(); i++)
					if(str1.charAt(i) == sym)
						return i;
				return 0;
			}
			public static int Bstrrchr(String str1, char sym) {
				int pos = 0;
				for(int i = 0; i < str1.length(); i++)
					if(str1.charAt(i) == sym)
						pos = i;
				return pos;
			}
			
			public static int Bstrcasecmp(String str1, String str2) {
				return str1.compareToIgnoreCase(str2);
			}
			
			public static int Bitoa(int n, char[] buffer) {
				int i = 0;
				boolean isneg = n < 0;
				
				long n1 = isneg ? -n:n;
				
				while(n1 !=0) {
					buffer[i++] = (char) (n1%10+'0');
					n1=n1/10;
				}
				if(isneg)
					buffer[i++] = '-';
				if(i < buffer.length)
					buffer[i] = '\0';
				
				for(int t = 0; t < i/2; t++) {
					buffer[t] ^= buffer[i-t-1];
					buffer[i-t-1] ^= buffer[t];
					buffer[t] ^= buffer[i-t-1];
				}
				
				if(n == 0) {
					buffer[i++] = '0';
					if(i < buffer.length)
						buffer[i] = '\0';
				}
				return i;
			}
			
			public static int Bitoa(int n, char[] buffer, int numsymbols) {
				int i = 0;
				boolean isneg = n < 0;
				
				long n1 = isneg ? -n:n;
				
				while(n1 !=0) {
					buffer[i++] = (char) (n1%10+'0');
					n1=n1/10;
				}
				int num = i;
				for(i = num; i < numsymbols; i++) {
					buffer[i] = '0';
				}
				
				if(isneg)
					buffer[i++] = '-';
				if(i < buffer.length)
					buffer[i] = '\0';
				
				for(int t = 0; t < i/2; t++) {
					buffer[t] ^= buffer[i-t-1];
					buffer[i-t-1] ^= buffer[t];
					buffer[t] ^= buffer[i-t-1];
				}
				
				if(n == 0) {
					for(i = 0; i < numsymbols; i++) {
						buffer[i] = '0';
					}
					if(i < buffer.length)
						buffer[i] = '\0';
				}
				return i;
			}

			public static int Bstrtol(String val, Object tmp, int arg) {
				return Integer.parseInt(val, arg);
			}
			
			public static int Bstrcpy(char[] dest, char[] src, int offset) {
				int c = 0;
				while(c < src.length && src[c] != '\0') {
					dest[offset + c] = src[c++];
				}
				if(offset + c < dest.length)
					dest[offset + c] = '\0';
				return offset + c;
			}
			
			public static int Bstrcpy(char[] dest, char[] src, int offset, int maxLen) {
				int c = 0;
				while(c < src.length && c < maxLen && src[c] != '\0') {
					dest[offset + c] = src[c++];
				}
				if(offset + c < dest.length)
					dest[offset + c] = '\0';
				return offset + c;
			}
			
			public static int Bstrcpy(char[] dest, String src) {
				int i = 0;
				while(i < src.length()) {
					dest[i] = src.charAt(i++);
				}
				if(i < dest.length)
					dest[i] = '\0';
				return i;
			}
			
			public static int Bstrcat(char[] dest, String src) {
				int len = Bstrlen(dest);
				int i = 0;
				while(i < src.length()) {
					dest[len++] = src.charAt(i++);
				}
				if(len < dest.length)
					dest[len] = '\0';
				return len;
			}
			
//			public static int Bstrcmp(String txt1, String txt2) {
//				if(txt1 == null || txt2 == null)
//					return -1;
//				return txt1.compareToIgnoreCase(txt2);
//			}
			
			public static int Bstrcmp(char[] txt1, int offset1, char[] txt2, int offset2) {
				int i = 0;
				if(txt1 == null || txt2 == null)
					return -1;
				
				int len = Math.max(txt1.length - offset1, txt2.length - offset2);
				while(i < len) {
					char ch1 = 0, ch2 = 0;
					if(offset1 + i < txt1.length) ch1 = txt1[offset1 + i];
					if(offset2 + i < txt2.length) ch2 = txt2[offset2 + i];
					if(ch1 != ch2)
						return -1;
					if(ch1 == ch2 && ch1 == 0)
						return 0;
					i++;
				}
				return 0;
			}
			
			public static int Bstrcmp(char[] txt1, char[] txt2) {
				int i = 0;
				if(txt1 == null || txt2 == null)
					return -1;
				
				int len = Math.max(txt1.length, txt2.length);
				while(i < len) {
					char ch1 = 0, ch2 = 0;
					if(i < txt1.length) ch1 = txt1[i];
					if(i < txt2.length) ch2 = txt2[i];
					if(ch1 != ch2)
						return -1;
					if(ch1 == ch2 && ch1 == 0)
						return 0;
					i++;
				}
				return 0;
			}		
			
			public static boolean isdigit(char ch) { 
				return ch>='0' && ch<='9'; 
			}
			
			public static boolean isalpha(char ch) {
				return Character.isLetter(ch);
			}

			public static int Bstrlen(char[] src) {
				int len = 0;
				while(len < src.length && src[len] != '\0') {
					len++;
				}
				return len;
			}
}
