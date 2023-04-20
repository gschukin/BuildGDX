/*
 *  Sector structure code originally written by Ken Silverman
 *	Ken Silverman's official web site: http://www.advsys.net/ken
 *
 *  See the included license file "BUILDLIC.TXT" for license info.
 *
 *  This file has been modified by Alexander Makarov-[M210] (m210-2007@mail.ru)
 */

package ru.m210projects.Build.Types;

import static ru.m210projects.Build.Engine.*;
import static ru.m210projects.Build.Gameutils.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

import ru.m210projects.Build.Engine;
import ru.m210projects.Build.FileHandle.DataResource;
import ru.m210projects.Build.FileHandle.Resource;

public class Sector {
	public static final int sizeof = 40;
	private static final ByteBuffer buffer = ByteBuffer.allocate(sizeof).order(ByteOrder.LITTLE_ENDIAN);

	private Wall[] walls = new Wall[0];

	public Wall[] getWalls() {
		return walls;
	}
	public void setWalls(Wall[] walls) {
		this.walls = walls;
	}

	public short wallptr;
	public short wallnum; // 4
	public int ceilingz;
	public int floorz; // 8
	public short ceilingstat;
	public short floorstat; // 4
	public short ceilingpicnum;
	public short ceilingheinum; // 4
	public byte ceilingshade; // 1

	public short ceilingpal;
	public short ceilingxpanning;
	public short ceilingypanning; // 3
	public short floorpicnum;
	public short floorheinum; // 4
	public byte floorshade; // 1
	public short floorpal;
	public short floorxpanning;
	public short floorypanning; // 3
	public short visibility;
	public short filler; // 2
	public short lotag;
	public short hitag;
	public short extra; // 6

	public Sector() {
	}

	public Sector(byte[] data) {
		buildSector(new DataResource(data));
	}

	public Sector(Resource data) {
		buildSector(data);
	}

	public int getEndWall() {
		return wallptr + wallnum - 1;
	}

	public boolean isParallaxCeiling() {
		return (getCeilingstat() & 1) != 0;
	}

	public boolean isCeilingSlope() {
		return (getCeilingstat() & 2) != 0;
	}

	public boolean isTexSwapedCeiling() {
		return (getCeilingstat() & 4) != 0;
	}

	public boolean isTexSmooshedCeiling() {
		return (getCeilingstat() & 8) != 0;
	}

	public boolean isTexXFlippedCeiling() {
		return (getCeilingstat() & 16) != 0;
	}

	public boolean isTexYFlippedCeiling() {
		return (getCeilingstat() & 32) != 0;
	}

	public boolean isRelativeTexCeiling() {
		return (getCeilingstat() & 64) != 0;
	}

	public boolean isMaskedTexCeiling() {
		return (getCeilingstat() & 128) != 0;
	}

	public boolean isTransparentCeiling() {
		return (getCeilingstat() & 256) != 0;
	}

	public boolean isTransparent2Ceiling() {
		return (getCeilingstat() & (128 | 256)) != 0;
	}

	public boolean isParallaxFloor() {
		return (getFloorstat() & 1) != 0;
	}

	public boolean isFloorSlope() {
		return (getFloorstat() & 2) != 0;
	}

	public boolean isTexSwapedFloor() {
		return (getFloorstat() & 4) != 0;
	}

	public boolean isTexSmooshedFloor() {
		return (getFloorstat() & 8) != 0;
	}

	public boolean isTexXFlippedFloor() {
		return (getFloorstat() & 16) != 0;
	}

	public boolean isTexYFlippedFloor() {
		return (getFloorstat() & 32) != 0;
	}

	public boolean isRelativeTexFloor() {
		return (getFloorstat() & 64) != 0;
	}

	public boolean isMaskedTexFloor() {
		return (getFloorstat() & 128) != 0;
	}

	public boolean isTransparentFloor() {
		return (getFloorstat() & 256) != 0;
	}

	public boolean isTransparent2Floor() {
		return (getFloorstat() & (128 | 256)) != 0;
	}

	public void buildSector(Resource bb) {
		setWallptr(bb.readShort());
		if (getWallptr() < 0 || getWallptr() >= MAXWALLS)
			setWallptr(0);
		setWallnum(bb.readShort());
		setCeilingz(bb.readInt());
		setFloorz(bb.readInt());
		setCeilingstat(bb.readShort());
		setFloorstat(bb.readShort());
		setCeilingpicnum(bb.readShort());
		if (!isValidTile(getCeilingpicnum()))
			setCeilingpicnum(0);
		setCeilingheinum(bb.readShort());
		setCeilingshade(bb.readByte());
		setCeilingpal((short) (bb.readByte() & 0xFF));
		setCeilingxpanning((short) (bb.readByte() & 0xFF));
		setCeilingypanning((short) (bb.readByte() & 0xFF));
		setFloorpicnum(bb.readShort());
		if (!isValidTile(getFloorpicnum()))
			setFloorpicnum(0);
		setFloorheinum(bb.readShort());
		setFloorshade(bb.readByte());
		setFloorpal((short) (bb.readByte() & 0xFF));
		setFloorxpanning((short) (bb.readByte() & 0xFF));
		setFloorypanning((short) (bb.readByte() & 0xFF));
		setVisibility((short) (bb.readByte() & 0xFF));
		setFiller(bb.readByte());
		setLotag(bb.readShort());
		setHitag(bb.readShort());
		setExtra(bb.readShort());
	}

	public void set(Sector src) {
		setWallptr(src.getWallptr());
		setWallnum(src.getWallnum());
		setCeilingz(src.getCeilingz());
		setFloorz(src.getFloorz());
		setCeilingstat(src.getCeilingstat());
		setFloorstat(src.getFloorstat());
		setCeilingpicnum(src.getCeilingpicnum());
		setCeilingheinum(src.getCeilingheinum());
		setCeilingshade(src.getCeilingshade());
		setCeilingpal(src.getCeilingpal());
		setCeilingxpanning(src.getCeilingxpanning());
		setCeilingypanning(src.getCeilingypanning());
		setFloorpicnum(src.getFloorpicnum());
		setFloorheinum(src.getFloorheinum());
		setFloorshade(src.getFloorshade());
		setFloorpal(src.getFloorpal());
		setFloorxpanning(src.getFloorxpanning());
		setFloorypanning(src.getFloorypanning());
		setVisibility(src.getVisibility());
		setFiller(src.getFiller());
		setLotag(src.getLotag());
		setHitag(src.getHitag());
		setExtra(src.getExtra());
	}

	public byte[] getBytes() {
		buffer.clear();

		buffer.putShort(this.getWallptr());
		buffer.putShort(this.getWallnum());
		buffer.putInt(this.getCeilingz());
		buffer.putInt(this.getFloorz());
		buffer.putShort(this.getCeilingstat());
		buffer.putShort(this.getFloorstat());
		buffer.putShort(this.getCeilingpicnum());
		buffer.putShort(this.getCeilingheinum());
		buffer.put(this.getCeilingshade());
		buffer.put((byte) this.getCeilingpal());
		buffer.put((byte) this.getCeilingxpanning());
		buffer.put((byte) this.getCeilingypanning());
		buffer.putShort(this.getFloorpicnum());
		buffer.putShort(this.getFloorheinum());
		buffer.put(this.getFloorshade());
		buffer.put((byte) this.getFloorpal());
		buffer.put((byte) this.getFloorxpanning());
		buffer.put((byte) this.getFloorypanning());
		buffer.put((byte) this.getVisibility());
		buffer.put((byte) this.getFiller());
		buffer.putShort(this.getLotag());
		buffer.putShort(this.getHitag());
		buffer.putShort(this.getExtra());

		return buffer.array();
	}

	@Override
	public String toString() {
		String out = "wallptr " + getWallptr() + " \r\n";
		out += "wallnum " + getWallnum() + " \r\n";
		out += "ceilingz " + getCeilingz() + " \r\n";
		out += "floorz " + getFloorz() + " \r\n";
		out += "ceilingstat " + getCeilingstat() + " \r\n";
		out += "floorstat " + getFloorstat() + " \r\n";
		out += "ceilingpicnum " + getCeilingpicnum() + " \r\n";
		out += "ceilingheinum " + getCeilingheinum() + " \r\n";
		out += "ceilingshade " + getCeilingshade() + " \r\n";
		out += "ceilingpal " + getCeilingpal() + " \r\n";
		out += "ceilingxpanning " + getCeilingxpanning() + " \r\n";
		out += "ceilingypanning " + getCeilingypanning() + " \r\n";
		out += "floorpicnum " + getFloorpicnum() + " \r\n";
		out += "floorheinum " + getFloorheinum() + " \r\n";
		out += "floorshade " + getFloorshade() + " \r\n";
		out += "floorpal " + getFloorpal() + " \r\n";
		out += "floorxpanning " + getFloorxpanning() + " \r\n";
		out += "floorypanning " + getFloorypanning() + " \r\n";
		out += "visibility " + getVisibility() + " \r\n";
		out += "filler " + getFiller() + " \r\n";
		out += "lotag " + getLotag() + " \r\n";
		out += "hitag " + getHitag() + " \r\n";
		out += "extra " + getExtra() + " \r\n";

		return out;
	}

	private static SectorIterator it;

	public short getWallptr() {
		return wallptr;
	}

	public void setWallptr(int wallptr) {
		this.wallptr = (short) wallptr;
	}

	public short getWallnum() {
		return wallnum;
	}

	public void setWallnum(int wallnum) {
		this.wallnum = (short) wallnum;
	}

	public int getCeilingz() {
		return ceilingz;
	}

	public void setCeilingz(int ceilingz) {
		this.ceilingz = ceilingz;
	}

	public int getFloorz() {
		return floorz;
	}

	public void setFloorz(int floorz) {
		this.floorz = floorz;
	}

	public short getCeilingstat() {
		return ceilingstat;
	}

	public void setCeilingstat(int ceilingstat) {
		this.ceilingstat = (short) ceilingstat;
	}

	public short getFloorstat() {
		return floorstat;
	}

	public void setFloorstat(int floorstat) {
		this.floorstat = (short) floorstat;
	}

	public short getCeilingpicnum() {
		return ceilingpicnum;
	}

	public void setCeilingpicnum(int ceilingpicnum) {
		this.ceilingpicnum = (short) ceilingpicnum;
	}

	public short getCeilingheinum() {
		return ceilingheinum;
	}

	public void setCeilingheinum(int ceilingheinum) {
		this.ceilingheinum = (short) ceilingheinum;
	}

	public byte getCeilingshade() {
		return ceilingshade;
	}

	public void setCeilingshade(int ceilingshade) {
		this.ceilingshade = (byte) ceilingshade;
	}

	public short getCeilingpal() {
		return ceilingpal;
	}

	public void setCeilingpal(int ceilingpal) {
		this.ceilingpal = (short) ceilingpal;
	}

	public short getCeilingxpanning() {
		return ceilingxpanning;
	}

	public void setCeilingxpanning(int ceilingxpanning) {
		this.ceilingxpanning = (short) ceilingxpanning;
	}

	public short getCeilingypanning() {
		return ceilingypanning;
	}

	public void setCeilingypanning(int ceilingypanning) {
		this.ceilingypanning = (short) ceilingypanning;
	}

	public short getFloorpicnum() {
		return floorpicnum;
	}

	public void setFloorpicnum(int floorpicnum) {
		this.floorpicnum = (short) floorpicnum;
	}

	public short getFloorheinum() {
		return floorheinum;
	}

	public void setFloorheinum(int floorheinum) {
		this.floorheinum = (short) floorheinum;
	}

	public byte getFloorshade() {
		return floorshade;
	}

	public void setFloorshade(int floorshade) {
		this.floorshade = (byte) floorshade;
	}

	public short getFloorpal() {
		return floorpal;
	}

	public void setFloorpal(int floorpal) {
		this.floorpal = (short) floorpal;
	}

	public short getFloorxpanning() {
		return floorxpanning;
	}

	public void setFloorxpanning(int floorxpanning) {
		this.floorxpanning = (short) floorxpanning;
	}

	public short getFloorypanning() {
		return floorypanning;
	}

	public void setFloorypanning(int floorypanning) {
		this.floorypanning = (short) floorypanning;
	}

	public short getVisibility() {
		return visibility;
	}

	public void setVisibility(int visibility) {
		this.visibility = (short) visibility;
	}

	public short getFiller() {
		return filler;
	}

	public void setFiller(int filler) {
		this.filler = (short) filler;
	}

	public short getLotag() {
		return lotag;
	}

	public void setLotag(int lotag) {
		this.lotag = (short) lotag;
	}

	public short getHitag() {
		return hitag;
	}

	public void setHitag(int hitag) {
		this.hitag = (short) hitag;
	}

	public short getExtra() {
		return extra;
	}

	public void setExtra(int extra) {
		this.extra = (short) extra;
	}

	public static final class SectorIterator implements Iterator<Wall> {
		private short i, startwall, endwall;

		@Override
		public boolean hasNext() {
			return startwall < endwall;
		}

		@Override
		public Wall next() {
			return Engine.getWall(nexti());
		}

		public short nexti() {
			return i = startwall++;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}

		public int getIndex() {
			return i;
		}

		protected SectorIterator init(short wallptr, short wallnum) {
			startwall = wallptr;
			endwall = (short) (wallnum + startwall);
			return this;
		}
	}

	public SectorIterator iterator() {
		if (it == null)
			it = new SectorIterator();
		return it.init(getWallptr(), getWallnum());
	}
}