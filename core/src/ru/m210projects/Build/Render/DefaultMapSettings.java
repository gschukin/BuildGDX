package ru.m210projects.Build.Render;

import static ru.m210projects.Build.Engine.show2dsector;
import static ru.m210projects.Build.Gameutils.BClipRange;
import static ru.m210projects.Build.Pragmas.klabs;
import static ru.m210projects.Build.Pragmas.mulscale;
import static ru.m210projects.Build.RenderService.yxaspect;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Types.Sector;
import ru.m210projects.Build.Types.Sprite;
import ru.m210projects.Build.Types.Wall;

public class DefaultMapSettings implements IOverheadMapSettings {

	@Override
	public boolean isShowSprites(MapView view) {
		return false;
	}

	@Override
	public boolean isShowFloorSprites() { // in Polygon mode
		return false;
	}

	@Override
	public boolean isShowRedWalls() {
		return true;
	}

	@Override
	public boolean isShowAllPlayers() {
		return false;
	}

	@Override
	public boolean isSpriteVisible(MapView view, int index) {
		if (view == MapView.Polygons)
			return false;

		switch (Engine.getSprite(index).getCstat() & 48) {
		case 0:
			return true;
		case 16: // wall sprites
			return false;
		case 32: // floor sprites
			return false;
		}
		return true;
	}

	@Override
	public boolean isWallVisible(int w, int s) {
		Wall wal = Engine.getWall(w);
		Sector sec = Engine.getSector(s);
		if (wal.getNextsector() != 0) // red wall
			return (wal.getNextwall() <= w && ((Engine.getSector(wal.getNextsector()).getCeilingz() != sec.getCeilingz() //
					|| Engine.getSector(wal.getNextsector()).getFloorz() != sec.getFloorz() //
					|| ((wal.getCstat() | Engine.getWall(wal.getNextwall()).getCstat()) & (16 + 32)) != 0) //
					|| (!isFullMap() && (show2dsector[wal.getNextsector() >> 3] & 1 << (wal.getNextsector() & 7)) == 0)));
		return true;
	}

	@Override
	public int getWallColor(int w, int sec) {
		Wall wal = Engine.getWall(w);
//		if (Gameutils.isValidSector(wal.nextsector)) // red wall
//			return 31;
		return 31; // white wall
	}

	@Override
	public int getSpriteColor(int s) {
		Sprite spr = Engine.getSprite(s);
//		switch (spr.cstat & 48) {
//		case 0:
//			return 31;
//		case 16:
//			return 31;
//		case 32:
//			return 31;
//		}

		return 31;
	}

	@Override
	public int getPlayerSprite(int player) {
		return -1;
	}

	@Override
	public int getPlayerPicnum(int player) {
		int spr = getPlayerSprite(player);
		return spr != -1 ? Engine.getSprite(spr).getPicnum() : -1;
	}

	@Override
	public int getPlayerZoom(int player, int czoom) {
		Sprite pPlayer = Engine.getSprite(getPlayerSprite(player));
		int nZoom = mulscale(yxaspect,
				czoom * (klabs((Engine.getSector(pPlayer.getSectnum()).getFloorz() - pPlayer.getZ()) >> 8) + pPlayer.getYrepeat()), 16);
		return BClipRange(nZoom, 22000, 0x20000);
	}

	@Override
	public boolean isFullMap() {
		return false;
	}

	@Override
	public boolean isScrollMode() {
		return false;
	}

	@Override
	public int getViewPlayer() {
		return 0;
	}

	@Override
	public int getSpriteX(int spr) {
		return Engine.getSprite(spr).getX();
	}

	@Override
	public int getSpriteY(int spr) {
		return Engine.getSprite(spr).getY();
	}

	@Override
	public int getSpritePicnum(int spr) {
		return Engine.getSprite(spr).getPicnum();
	}

	@Override
	public int getWallX(int w) {
		return Engine.getWall(w).getX();
	}

	@Override
	public int getWallY(int w) {
		return Engine.getWall(w).getY();
	}
}
