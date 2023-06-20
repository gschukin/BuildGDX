package ru.m210projects.Build.osd;

public enum OsdColor {

    DEFAULT(""),
    RESET("\u001B[0m"),
    RED("\u001B[91m"),
    BLUE("\u001B[94m"),
    GREEN("\u001B[92m"),
    YELLOW("\u001B[93m"),
    BROWN("\u001B[33m"),
    WHITE("\u001B[97m"),
//    PURPLE("\u001B[95m"),
//    CYAN("\u001B[96m"),
    GREY("\u001B[90m");

    private int pal;
    private final String ansi;

    OsdColor(String ansi) {
        this.ansi = ansi;
        this.pal = -1;
    }

    public static OsdColor findColor(int pal) {
        OsdColor[] values = OsdColor.values();
        for(OsdColor color : values) {
            if(color.pal == pal) {
                return color;
            }
        }
        return OsdColor.DEFAULT;
    }

    public int getPal() {
        return pal;
    }

    public void setPal(int pal) {
        this.pal = pal;
    }

    @Override
    public String toString() {
        return ansi;
    }
}
