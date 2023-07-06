/*
 *  Wall structure code originally written by Ken Silverman
 *	Ken Silverman's official web site: http://www.advsys.net/ken
 *
 *  See the included license file "BUILDLIC.TXT" for license info.
 *
 *  This file has been modified by Alexander Makarov-[M210] (m210-2007@mail.ru)
 */

package ru.m210projects.Build.Types;

import ru.m210projects.Build.filehandle.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Wall {
    private static final int sizeof = 32;
    private static final ByteBuffer buffer = ByteBuffer.allocate(getSizeof()).order(ByteOrder.LITTLE_ENDIAN);

    private int x;
    private int y; //8
    private short point2;
    private short nextwall;
    private short nextsector;
    private short cstat; //8
    private short picnum;
    private short overpicnum; //4
    private byte shade; //1
    private short pal;
    private short xrepeat;
    private short yrepeat;
    private short xpanning;
    private short ypanning; //5
    private short lotag;
    private short hitag;
    private short extra; //6
    private Wall wall2;

    public Wall() {
    }

    public Wall(byte[] data) throws IOException {
        readObject(new ByteArrayInputStream(data));
    }

    public Wall(InputStream is) throws IOException {
        readObject(is);
    }

    public static int getSizeof() {
        return sizeof;
    }

    public Wall readObject(InputStream is) throws IOException {
        setX(StreamUtils.readInt(is));
        setY(StreamUtils.readInt(is));
        setPoint2(StreamUtils.readShort(is));
        setNextwall(StreamUtils.readShort(is));
        setNextsector(StreamUtils.readShort(is));
        setCstat(StreamUtils.readShort(is));
        setPicnum(StreamUtils.readShort(is));
        setOverpicnum(StreamUtils.readShort(is));
        setShade(is.read());
        setPal(is.read());
        setXrepeat(is.read());
        setYrepeat(is.read());
        setXpanning(is.read());
        setYpanning(is.read());
        setLotag(StreamUtils.readShort(is));
        setHitag(StreamUtils.readShort(is));
        setExtra(StreamUtils.readShort(is));
        return this;
    }

    public void writeObject(OutputStream os) throws IOException {
        StreamUtils.writeInt(os, getX());
        StreamUtils.writeInt(os, getY());
        StreamUtils.writeShort(os, getPoint2());
        StreamUtils.writeShort(os, getNextwall());
        StreamUtils.writeShort(os, getNextsector());
        StreamUtils.writeShort(os, getCstat());
        StreamUtils.writeShort(os, getPicnum());
        StreamUtils.writeShort(os, getOverpicnum());
        os.write(getShade());
        os.write(getPal());
        os.write(getXrepeat());
        os.write(getYrepeat());
        os.write(getXpanning());
        os.write(getYpanning());
        StreamUtils.writeShort(os, getLotag());
        StreamUtils.writeShort(os, getHitag());
        StreamUtils.writeShort(os, getExtra());
    }

    public void set(Wall src) {
        setX(src.getX());
        setY(src.getY());
        setPoint2(src.getPoint2());
        setNextwall(src.getNextwall());
        setNextsector(src.getNextsector());
        setCstat(src.getCstat());
        setPicnum(src.getPicnum());
        setOverpicnum(src.getOverpicnum());
        setShade(src.getShade());
        setPal(src.getPal());
        setXrepeat(src.getXrepeat());
        setYrepeat(src.getYrepeat());
        setXpanning(src.getXpanning());
        setYpanning(src.getYpanning());
        setLotag(src.getLotag());
        setHitag(src.getHitag());
        setExtra(src.getExtra());
    }

    public byte[] getBytes() {
        buffer.clear();

        buffer.putInt(this.getX());
        buffer.putInt(this.getY());
        buffer.putShort(this.getPoint2());
        buffer.putShort(this.getNextwall());
        buffer.putShort(this.getNextsector());
        buffer.putShort(this.getCstat());
        buffer.putShort(this.getPicnum());
        buffer.putShort(this.getOverpicnum());
        buffer.put(this.getShade());
        buffer.put((byte) this.getPal());
        buffer.put((byte) this.getXrepeat());
        buffer.put((byte) this.getYrepeat());
        buffer.put((byte) this.getXpanning());
        buffer.put((byte) this.getYpanning());
        buffer.putShort(this.getLotag());
        buffer.putShort(this.getHitag());
        buffer.putShort(this.getExtra());

        return buffer.array();
    }

    public boolean isSwapped() {
        return (getCstat() & 2) != 0;
    }

    public boolean isBottomAligned() {
        return (getCstat() & 4) != 0;
    }

    public boolean isXFlip() {
        return (getCstat() & 8) != 0;
    }

    public boolean isYFlip() {
        return (getCstat() & 256) != 0;
    }

    public boolean isMasked() {
        return (getCstat() & 16) != 0;
    }

    public boolean isOneWay() {
        return (getCstat() & 32) != 0;
    }

    public boolean isTransparent() {
        return (getCstat() & 128) != 0;
    }

    public boolean isTransparent2() {
        return (getCstat() & 512) != 0;
    }

    @Override
    public String toString() {
        String out = "x " + getX() + " \r\n";
        out += "y " + getY() + " \r\n";
        out += "point2 " + getPoint2() + " \r\n";
        out += "nextwall " + getNextwall() + " \r\n";
        out += "nextsector " + getNextsector() + " \r\n";
        out += "cstat " + getCstat() + " \r\n";
        out += "picnum " + getPicnum() + " \r\n";
        out += "overpicnum " + getOverpicnum() + " \r\n";
        out += "shade " + getShade() + " \r\n";
        out += "pal " + getPal() + " \r\n";
        out += "xrepeat " + getXrepeat() + " \r\n";
        out += "yrepeat " + getYrepeat() + " \r\n";
        out += "xpanning " + getXpanning() + " \r\n";
        out += "ypanning " + getYpanning() + " \r\n";
        out += "lotag " + getLotag() + " \r\n";
        out += "hitag " + getHitag() + " \r\n";
        out += "extra " + getExtra() + " \r\n";

        return out;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public short getPoint2() {
        return point2;
    }

    public void setPoint2(int point2) {
        this.point2 = (short) point2;
    }

    public short getNextwall() {
        return nextwall;
    }

    public void setNextwall(int nextwall) {
        this.nextwall = (short) nextwall;
    }

    public short getNextsector() {
        return nextsector;
    }

    public void setNextsector(int nextsector) {
        this.nextsector = (short) nextsector;
    }

    public short getCstat() {
        return cstat;
    }

    public void setCstat(int cstat) {
        this.cstat = (short) cstat;
    }

    public short getPicnum() {
        return picnum;
    }

    public void setPicnum(int picnum) {
        this.picnum = (short) picnum;
    }

    public short getOverpicnum() {
        return overpicnum;
    }

    public void setOverpicnum(int overpicnum) {
        this.overpicnum = (short) overpicnum;
    }

    public byte getShade() {
        return shade;
    }

    public void setShade(int shade) {
        this.shade = (byte) shade;
    }

    public short getPal() {
        return pal;
    }

    public void setPal(int pal) {
        this.pal = (short) (pal & 0xFF);
    }

    public short getXrepeat() {
        return xrepeat;
    }

    public void setXrepeat(int xrepeat) {
        this.xrepeat = (short) (xrepeat & 0xFF);
    }

    public short getYrepeat() {
        return yrepeat;
    }

    public void setYrepeat(int yrepeat) {
        this.yrepeat = (short) (yrepeat & 0xFF);
    }

    public short getXpanning() {
        return xpanning;
    }

    public void setXpanning(int xpanning) {
        this.xpanning = (short) (xpanning & 0xFF);
    }

    public short getYpanning() {
        return ypanning;
    }

    public void setYpanning(int ypanning) {
        this.ypanning = (short) (ypanning & 0xFF);
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

    /**
     * Get point2 wall object
     */
    public Wall getWall2() {
        return wall2;
    }

    /**
     * Set point2 wall object
     */
    public void setWall2(Wall wall2) {
        this.wall2 = wall2;
    }
}
