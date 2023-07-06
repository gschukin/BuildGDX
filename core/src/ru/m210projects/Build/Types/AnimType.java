package ru.m210projects.Build.Types;

public enum AnimType {
    OSCIL(1 << 6), FORWARD(2 << 6), BACKWARD(3 << 6), NONE(0);

    private final int bit;

    AnimType(int bit) {
        this.bit = bit;
    }

    public int getBit() {
        return bit;
    }

    public static AnimType findAnimType(int flags) {
        switch (flags & 192) {
            case 64:
                return AnimType.OSCIL;
            case 128:
                return AnimType.FORWARD;
            case 192:
                return AnimType.BACKWARD;
        }
        return AnimType.NONE;
    }
}
