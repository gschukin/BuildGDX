package ru.m210projects.Build.Pattern;

import ru.m210projects.Build.Types.font.Font;

public abstract class FontHandler {

	protected Font[] fonts;

	public FontHandler(int nFonts) {
		fonts = new Font[nFonts];
	}
	
	protected abstract Font init(int i);
	
	public Font getFont(int i) {
		if(i < 0 || i >= fonts.length) {
			throw new IllegalArgumentException("Wrong font number");
		}
		
		if(fonts[i] == null) {
			fonts[i] = init(i);
		}

		return fonts[i];
	}

}
