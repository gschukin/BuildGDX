/*
 *  Sprite structure code originally written by Ken Silverman
 *	Ken Silverman's official web site: http://www.advsys.net/ken
 *
 *  See the included license file "BUILDLIC.TXT" for license info.
 *
 *  This file has been modified by Alexander Makarov-[M210] (m210-2007@mail.ru)
 */

package ru.m210projects.Build.Types;

import static ru.m210projects.Build.Engine.MAXTILES;
import static ru.m210projects.Build.Engine.MAXSECTORS;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SPRITE {
	public static final int sizeof = 44;
	private static final ByteBuffer buffer = ByteBuffer.allocate(sizeof).order( ByteOrder.LITTLE_ENDIAN);
	
	public int x, y, z; //12
	public short cstat = 0, picnum; //4
	public byte shade; //1
	public short pal, detail; //3
	public int clipdist = 32;
	public short xrepeat = 32, yrepeat = 32; //2
	public short xoffset, yoffset; //2
	public short sectnum, statnum; //4
	public short ang, owner = -1, xvel, yvel, zvel; //10
	public short lotag, hitag;
	/**
	 * An index to {@link ru.m210projects.Blood.DB#xsprite} array linking
	 * this {@link SPRITE} with its corresponding {@link XSPRITE}.
	 */
	public short extra = -1;

	public SPRITE() { }
	
	public void init(byte[] data) {
		ByteBuffer bb = ByteBuffer.wrap(data);
    	bb.order( ByteOrder.LITTLE_ENDIAN);
    	buildSprite(bb);
	}
	
	public void buildSprite(ByteBuffer bb)
	{
		x = bb.getInt();
    	y = bb.getInt();
    	z = bb.getInt();
    	cstat = bb.getShort();
    	picnum = bb.getShort();
    	if(picnum < 0 || picnum >= MAXTILES) picnum = 0;
    	shade = bb.get();
    	pal = (short) (bb.get() & 0xFF);
    	clipdist = bb.get() & 0xFF;
    	detail = bb.get();
    	xrepeat = (short) (bb.get() & 0xFF);
    	yrepeat = (short) (bb.get() & 0xFF);
    	xoffset = bb.get();
    	yoffset = bb.get();
    	sectnum = bb.getShort();
    	if(sectnum < 0 || sectnum >= MAXSECTORS) sectnum = 0;
    	statnum = bb.getShort();
    	ang = bb.getShort();
    	owner = bb.getShort();
    	xvel = bb.getShort();
    	yvel = bb.getShort();
    	zvel = bb.getShort();
    	lotag = bb.getShort();
    	hitag = bb.getShort();
    	extra = bb.getShort();
	}
	
	public byte[] getBytes()
	{
		buffer.clear();
		
		buffer.putInt(this.x);
    	buffer.putInt(this.y);
    	buffer.putInt(this.z);
    	buffer.putShort(this.cstat);
    	buffer.putShort(this.picnum);
    	buffer.put(this.shade);
    	buffer.put((byte)this.pal);
    	buffer.put((byte) this.clipdist);
    	buffer.put((byte)this.detail);
    	buffer.put((byte)this.xrepeat);
    	buffer.put((byte)this.yrepeat);
    	buffer.put((byte)this.xoffset);
    	buffer.put((byte)this.yoffset);
    	buffer.putShort(this.sectnum);
    	buffer.putShort(this.statnum);
    	buffer.putShort(this.ang);
    	buffer.putShort(this.owner);
    	buffer.putShort(this.xvel);
    	buffer.putShort(this.yvel);
    	buffer.putShort(this.zvel);
    	buffer.putShort(this.lotag);
    	buffer.putShort(this.hitag);
    	buffer.putShort(this.extra);
		
		return buffer.array();
	}
	
	public String toString()
	{
		String out = "x " + x + " \r\n";
		out += "y " + y + " \r\n";
		out += "z " + z + " \r\n";
		out += "cstat " + cstat + " \r\n";
		out += "picnum " + picnum + " \r\n";
		out += "shade " + shade + " \r\n";
		out += "pal " + pal + " \r\n";
		out += "clipdist " + clipdist + " \r\n";
		out += "detail " + detail + " \r\n";
		out += "xrepeat " + xrepeat + " \r\n";
		out += "yrepeat " + yrepeat + " \r\n";
		out += "xoffset " + xoffset + " \r\n";
		out += "yoffset " + yoffset + " \r\n";
		out += "sectnum " + sectnum + " \r\n";
		out += "statnum " + statnum + " \r\n";
		out += "ang " + ang + " \r\n";
		out += "owner " + owner + " \r\n";
		out += "xvel " + xvel + " \r\n";
		out += "yvel " + yvel + " \r\n";
		out += "zvel " + zvel + " \r\n";
		out += "type " + lotag + " \r\n";
		out += "flags " + hitag + " \r\n";
		out += "extra " + extra + " \r\n";
    	
		return out;
	}
	
	public void reset()
	{
		reset((byte)0);
		this.clipdist = 32;
		this.xrepeat = this.yrepeat = 32;
		this.owner = -1;
		this.extra = -1;
	}
	
	public void reset(byte var) {
		this.x = var;
		this.y = var;
		this.z = var;
		this.cstat = var;
		this.picnum = var;
		this.shade = var;
		this.pal = var;
    	
		this.clipdist = var;
		this.detail = var;
		this.xrepeat = var;
		this.yrepeat = var;
		this.xoffset = var;
		this.yoffset = var;
		this.sectnum = var;
		this.statnum = var;
		this.ang = var;
		this.owner = var;
		this.xvel = var;
		this.yvel = var;
		this.zvel = var;
		this.lotag = var;
		this.hitag = var;
		this.extra = var;
	}
	
	public void set(SPRITE src) {
		this.x = src.x;
		this.y = src.y;
		this.z = src.z;
		this.cstat = src.cstat;
		this.picnum = src.picnum;
		this.shade = src.shade;
		this.pal = src.pal;
    	
		this.clipdist = src.clipdist;
		this.detail = src.detail;
		this.xrepeat = src.xrepeat;
		this.yrepeat = src.yrepeat;
		this.xoffset = src.xoffset;
		this.yoffset = src.yoffset;
		this.sectnum = src.sectnum;
		this.statnum = src.statnum;
		this.ang = src.ang;
		this.owner = src.owner;
		this.xvel = src.xvel;
		this.yvel = src.yvel;
		this.zvel = src.zvel;
		this.lotag = src.lotag;
		this.hitag = src.hitag;
		this.extra = src.extra;
	}
}


