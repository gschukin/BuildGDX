package ru.m210projects.Build.desktop.audio;

import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;
import ru.m210projects.Build.Architecture.common.AudioResampler;
import ru.m210projects.Build.Architecture.common.BuildAudio;

public class LwjglAudio extends OpenALAudio implements BuildAudio {

    private final String name;

    public LwjglAudio(int simultaneousSources, int deviceBufferCount, int deviceBufferSize) {
        super(simultaneousSources, deviceBufferCount, deviceBufferSize);

        ALCcontext context = ALC10.alcGetCurrentContext();
        ALCdevice device = ALC10.alcGetContextsDevice(context);

        String version = AL10.alGetString(AL10.AL_VERSION);
        this.name = ALC10.alcGetString(device, ALC10.ALC_DEVICE_SPECIFIER) + " " + version;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEFXSupport() {
        return false;
    }

    @Override
    public AudioResampler getSoftResampler(int num) {
        return null;
    }

    @Override
    public int getNumResamplers() {
        return 0;
    }
}
