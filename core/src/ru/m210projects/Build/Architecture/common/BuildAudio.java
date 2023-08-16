package ru.m210projects.Build.Architecture.common;

import com.badlogic.gdx.Audio;

public interface BuildAudio extends Audio {

    String getName();
    boolean isEFXSupport();
    AudioResampler getSoftResampler(int num);
    int getNumResamplers();

}
