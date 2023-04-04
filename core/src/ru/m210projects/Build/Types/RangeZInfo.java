package ru.m210projects.Build.Types;

public class RangeZInfo {

    private int ceilz = Integer.MIN_VALUE;
    private int ceilhit = -1;
    private int florz = Integer.MAX_VALUE;
    private int florhit = -1;

    public void init() {
        this.ceilz = Integer.MIN_VALUE;
        this.ceilhit = -1;
        this.florz = Integer.MAX_VALUE;
        this.florhit = -1;
    }

    public int getCeilz() {
        return ceilz;
    }

    public void setCeilz(int ceilz) {
        this.ceilz = ceilz;
    }

    public int getCeilhit() {
        return ceilhit;
    }

    public void setCeilhit(int ceilhit) {
        this.ceilhit = ceilhit;
    }

    public int getFlorz() {
        return florz;
    }

    public void setFlorz(int florz) {
        this.florz = florz;
    }

    public int getFlorhit() {
        return florhit;
    }

    public void setFlorhit(int florhit) {
        this.florhit = florhit;
    }
}
