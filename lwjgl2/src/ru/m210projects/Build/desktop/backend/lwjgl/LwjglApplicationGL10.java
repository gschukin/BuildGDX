package ru.m210projects.Build.desktop.backend.lwjgl;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import ru.m210projects.Build.Architecture.common.BuildApplication;
import ru.m210projects.Build.Architecture.common.RenderType;
import ru.m210projects.Build.desktop.audio.LwjglAudio;

public class LwjglApplicationGL10 extends LwjglApplication implements BuildApplication {

    private final LwjglApplicationConfiguration config;

    public LwjglApplicationGL10(ApplicationListener listener, LwjglApplicationConfiguration config) {
        super(listener, config);
        this.config = config;
        postRunnable(() -> Gdx.gl = new LwjglGL10());
    }

    @Override
    public void setFramesPerSecond(int fps) {
        config.foregroundFPS = fps;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    public Audio createAudio(LwjglApplicationConfiguration config) {
        return new LwjglAudio(config.audioDeviceSimultaneousSources, config.audioDeviceBufferCount,
                config.audioDeviceBufferSize);
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.GL;
    }
}
