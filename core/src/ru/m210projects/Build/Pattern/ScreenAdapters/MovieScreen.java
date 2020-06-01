package ru.m210projects.Build.Pattern.ScreenAdapters;

import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Input.Keymap.ANYKEY;

import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.BuildFont;
import ru.m210projects.Build.Pattern.ScreenAdapters.SkippableAdapter;
import ru.m210projects.Build.Settings.BuildSettings;

public abstract class MovieScreen extends SkippableAdapter {

	public static interface MovieFile {

		int getFrames();

		float getRate();

		byte[] getFrame(int num);

		byte[] getPalette();

		short getWidth();

		short getHeight();

		void close();

	}

	protected final int TILE_MOVIE;

	protected Runnable callback;
	protected int gCutsClock;
	protected long LastMS;
	protected MovieFile mvfil;
	protected int frame;
	protected long mvtime;
	protected byte[] opalookup;

	protected int nFlags = 2 | 8 | 64, nScale = 65536, nPosX = 160, nPosY = 100;

	public MovieScreen(BuildGame game, int nTile) {
		super(game);

		TILE_MOVIE = nTile;
		opalookup = new byte[palookup[0].length];
		System.arraycopy(palookup[0], 0, opalookup, 0, opalookup.length);
	}

	protected abstract MovieFile GetFile(String file);

	protected abstract void StopAllSounds();

	protected abstract byte[] DoDrawFrame(int num);

	protected abstract BuildFont GetFont();

	protected abstract void DrawEscText(BuildFont font, int pal);

	@Override
	public void show() {
		if (game.pMenu.gShowMenu)
			game.pMenu.mClose();

		StopAllSounds();
		engine.sampletimer();
		LastMS = engine.getticks();
		gCutsClock = totalclock = 0;
	}

	@Override
	public void hide() {
		engine.setbrightness(BuildSettings.paletteGamma.get(), palette, 2);
	}

	public MovieScreen setCallback(Runnable callback) {
		this.callback = callback;
		this.setSkipping(callback);
		return this;
	}

	protected boolean open(String fn) {
		if (mvfil != null)
			return false;

		mvfil = GetFile(fn);
		if (mvfil == null)
			return false;

		tilesizx[TILE_MOVIE] = mvfil.getWidth();
		tilesizy[TILE_MOVIE] = mvfil.getHeight();
		waloff[TILE_MOVIE] = null;

		float kt = tilesizy[TILE_MOVIE] / (float) tilesizx[TILE_MOVIE];
		float kv = xdim / (float) ydim;

		float scale = 1.0f;
		if (kv >= kt) {
			scale = (ydim / (float) tilesizx[TILE_MOVIE]);
			scale /= (ydim / (float) 200);
		}
		else {
			scale = (xdim / (float) tilesizy[TILE_MOVIE]);
			scale /= ((4 * ydim) / (float) (3 * 320));
		}
		nScale = (int) (scale * 65536);

//        if(3 * xdim / 4 <= ydim)
//        	nScale = divscale(310, tilesizy[TILE_MOVIE], 16);
//        else 
//        	nScale = divscale(190, tilesizx[TILE_MOVIE], 16);
//        nPosX = 160;
//        nPosY = 100;

//        float kt = tilesizy[TILE_MOVIE] / (float) tilesizx[TILE_MOVIE];
//        float kv = xdim / (float) ydim;
//
//        float scale = 1.0f;
//        if(kv >= kt)
//        	scale = (ydim / (float) tilesizx[TILE_MOVIE]);
//        else scale = (xdim / (float) tilesizy[TILE_MOVIE]);
//  
//        nScale = (int) (scale * 65536.0f);
//        nPosX = (xdim / 2) - (int) (tilesizy[TILE_MOVIE] / 2.0f * scale);
//        nPosY = (ydim / 2) - (int) (tilesizx[TILE_MOVIE] / 2.0f * scale);

		for (int i = 0; i < MAXPALOOKUPS; i++)
			palookup[0][i] = (byte) i;

		changepalette(mvfil.getPalette());

		frame = 0;
		mvtime = 0;
		LastMS = -1;

		return true;
	}

	public boolean isInited() {
		return mvfil != null;
	}

	protected void changepalette(byte[] pal) {
		if (pal == null)
			return;

//		engine.setbrightness(BuildSettings.paletteGamma.get(), pal, 2);
		engine.changepalette(pal);

		int white = -1;
		int k = 0;
		for (int i = 0; i < 256; i += 3) {
			int j = (pal[3 * i] & 0xFF) + (pal[3 * i + 1] & 0xFF) + (pal[3 * i + 2] & 0xFF);
			if (j > k) {
				k = j;
				white = i;
			}
		}

		if (white == -1)
			return;

		int palnum = MAXPALOOKUPS - RESERVEDPALS - 1;
		byte[] remapbuf = new byte[768];
		for (int i = 0; i < 768; i++)
			remapbuf[i] = (byte) white;
		engine.makepalookup(palnum, remapbuf, 0, 1, 0, 1);

		for (int i = 0; i < 256; i++) {
			int tile = GetFont().getTile(i);
			if (tile >= 0)
				engine.invalidatetile(tile, palnum, -1);
		}
	}

	protected boolean play() {
		if (mvfil != null) {
			if (LastMS == -1)
				LastMS = engine.getticks();

			long ms = engine.getticks();
			long dt = ms - LastMS;
			mvtime += dt;
			float tick = mvfil.getRate();
			if (mvtime >= tick) {
				if (frame < mvfil.getFrames()) {
					waloff[TILE_MOVIE] = DoDrawFrame(frame);
					engine.invalidatetile(TILE_MOVIE, 0, -1); // JBF 20031228

					frame++;
				} else
					return false;
				mvtime -= tick;
			}
			LastMS = ms;

			if (tilesizx[TILE_MOVIE] <= 0)
				return false;

			if (waloff[TILE_MOVIE] != null)
				engine.rotatesprite(nPosX << 16, nPosY << 16, nScale, 512, TILE_MOVIE, 0, 0, nFlags, 0, 0, xdim - 1,
						ydim - 1);
			return true;
		}
		return false;
	}

	@Override
	public void skip() {
		close();
		super.skip();
	}

	protected void callback() {
		close();
		if (callback != null) {
			BuildGdx.app.postRunnable(callback);
			callback = null;
		}
	}

	@Override
	public void draw(float delta) {
		if (!play() && skipCallback != null)
			callback();

		if (game.pInput.ctrlKeyStatus(ANYKEY))
			gCutsClock = totalclock;

		if (totalclock - gCutsClock < 200 && escSkip) // 2 sec
			DrawEscText(GetFont(), MAXPALOOKUPS - RESERVEDPALS - 1);
	}

	protected void close() {
		if (mvfil != null) {
			System.arraycopy(opalookup, 0, palookup[0], 0, opalookup.length);
			mvfil.close();
		}

		mvfil = null;
		LastMS = -1;
		frame = 0;
	}
}