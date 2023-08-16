package ru.m210projects.Build.Architecture.common;

public abstract class AudioResampler {
    private final String name;

    public AudioResampler(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void setToSource(int sourceId);

}
