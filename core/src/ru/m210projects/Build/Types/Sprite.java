/*
 *  Sprite structure code originally written by Ken Silverman
 *	Ken Silverman's official web site: http://www.advsys.net/ken
 *
 *  See the included license file "BUILDLIC.TXT" for license info.
 *
 *  This file has been modified by Alexander Makarov-[M210] (m210-2007@mail.ru)
 */

package ru.m210projects.Build.Types;

import static ru.m210projects.Build.Engine.MAXSECTORS;
import static ru.m210projects.Build.Gameutils.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import ru.m210projects.Build.FileHandle.Resource;

public class Sprite {
	public static final int sizeof = 44;
	private static final ByteBuffer buffer = ByteBuffer.allocate(sizeof).order( ByteOrder.LITTLE_ENDIAN);

	public int x, y, z; //12
	public short cstat = 0, picnum; //4
	public byte shade; //1
	public short pal, detail; //3
	public int clipdist = 32;
	public short xrepeat = 32, yrepeat = 32; //2
	public short xoffset, yoffset; //2
	protected short sectnum;
	protected short statnum; //4
	public short ang, owner = -1, xvel, yvel, zvel; //10
	public short lotag, hitag;
	public short extra = -1;

	public void buildSprite(Resource bb)
	{
		x = bb.readInt();
    	y = bb.readInt();
    	z = bb.readInt();
    	cstat = bb.readShort();
    	picnum = bb.readShort();
    	if(!isValidTile(picnum)) picnum = 0;
    	shade = bb.readByte();
    	pal = (short) (bb.readByte() & 0xFF);
    	clipdist = bb.readByte() & 0xFF;
    	detail = bb.readByte();
    	xrepeat = (short) (bb.readByte() & 0xFF);
    	yrepeat = (short) (bb.readByte() & 0xFF);
    	xoffset = bb.readByte();
    	yoffset = bb.readByte();
    	sectnum = bb.readShort();
//    	if(sectnum < 0 || sectnum >= MAXSECTORS)
//			sectnum = 0;
    	statnum = bb.readShort();
    	ang = bb.readShort();
    	owner = bb.readShort();
    	xvel = bb.readShort();
    	yvel = bb.readShort();
    	zvel = bb.readShort();
    	lotag = bb.readShort();
    	hitag = bb.readShort();
    	extra = bb.readShort();
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
    	buffer.putShort(this.getSectnum());
    	buffer.putShort(this.getStatnum());
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

	@Override
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
		out += "sectnum " + getSectnum() + " \r\n";
		out += "statnum " + getStatnum() + " \r\n";
		out += "ang " + ang + " \r\n";
		out += "owner " + owner + " \r\n";
		out += "xvel " + xvel + " \r\n";
		out += "yvel " + yvel + " \r\n";
		out += "zvel " + zvel + " \r\n";
		out += "lotag " + lotag + " \r\n";
		out += "hitag " + hitag + " \r\n";
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

	public void set(Sprite src) {
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

	public short getSectnum() {
		return sectnum;
	}

	public void setSectnum(int sectnum) {
		this.sectnum = (short) sectnum;
	}

	public short getStatnum() {
		return statnum;
	}

	public void setStatnum(int statnum) {
		this.statnum = (short) statnum;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Sprite)) return false;
		Sprite sprite = (Sprite) o;

		boolean[] b = new boolean[23];
		b[0] = x == sprite.x;
		b[1] = y == sprite.y;
		b[2] = z == sprite.z;
		b[3] = cstat == sprite.cstat;
		b[4] = picnum == sprite.picnum;
		b[5] = shade == sprite.shade;
		b[6] = pal == sprite.pal;
		b[7] = detail == sprite.detail;
		b[8] = clipdist == sprite.clipdist;
		b[9] = xrepeat == sprite.xrepeat;
		b[10] = yrepeat == sprite.yrepeat;
		b[11] = xoffset == sprite.xoffset;
		b[12] = yoffset == sprite.yoffset;
		b[13] = sectnum == sprite.sectnum;
		b[14] = statnum == sprite.statnum;
		b[15] = ang == sprite.ang;
		b[16] = owner == sprite.owner;
		b[17] = xvel == sprite.xvel;
		b[18] = yvel == sprite.yvel;
		b[19] = zvel == sprite.zvel;
		b[20] = lotag == sprite.lotag;
		b[21] = hitag == sprite.hitag;
		b[22] = extra == sprite.extra;

		boolean result = true;
		for (int i = 0; i < 23; i++) {
			if (!b[i]) {
				System.err.print("Unsync in ");
				switch (i) {
					case 0: System.err.println("x: " + x + " != " + sprite.x); break;
					case 1: System.err.println("y: " + y + " != " + sprite.y); break;
					case 2: System.err.println("z: " + z + " != " + sprite.z); break;
					case 3: System.err.println("cstat: " + cstat + " != " + sprite.cstat); break;
					case 4: System.err.println("picnum: " + picnum + " != " + sprite.picnum); break;
					case 5: System.err.println("shade: " + shade + " != " + sprite.shade); break;
					case 6: System.err.println("pal: " + pal + " != " + sprite.pal); break;
					case 7: System.err.println("detail: " + detail + " != " + sprite.detail); break;
					case 8: System.err.println("clipdist: " + clipdist + " != " + sprite.clipdist); break;
					case 9: System.err.println("xrepeat: " + xrepeat + " != " + sprite.xrepeat); break;
					case 10: System.err.println("yrepeat: " + yrepeat + " != " + sprite.yrepeat); break;
					case 11: System.err.println("xoffset: " + xoffset + " != " + sprite.xoffset); break;
					case 12: System.err.println("yoffset: " + yoffset + " != " + sprite.yoffset); break;
					case 13: System.err.println("sectnum: " + sectnum + " != " + sprite.sectnum); break;
					case 14: System.err.println("statnum: " + statnum + " != " + sprite.statnum); break;
					case 15: System.err.println("ang: " + ang + " != " + sprite.ang); break;
					case 16: System.err.println("owner: " + owner + " != " + sprite.owner); break;
					case 17: System.err.println("xvel: " + xvel + " != " + sprite.xvel); break;
					case 18: System.err.println("yvel: " + yvel + " != " + sprite.yvel); break;
					case 19: System.err.println("zvel: " + zvel + " != " + sprite.zvel); break;
					case 20: System.err.println("lotag: " + lotag + " != " + sprite.lotag); break;
					case 21: System.err.println("hitag: " + hitag + " != " + sprite.hitag); break;
					case 22: System.err.println("extra: " + extra + " != " + sprite.extra); break;
				}
				result = false;
			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, z, cstat, picnum, shade, pal, detail, clipdist, xrepeat, yrepeat, xoffset, yoffset, sectnum, statnum, ang, owner, xvel, yvel, zvel, lotag, hitag, extra);
	}
}


