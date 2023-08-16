package ru.m210projects.Build.Architecture.common;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public interface BuildApplication extends Application {

    RenderType getRenderType();

    default boolean changeApplication(RenderType type) {
        if(!isChangeRendererSupported()) {
            return false;
        }

        RenderChanger listener = (RenderChanger) Gdx.app.getApplicationListener();
        if(type != getRenderType()) {
            listener.changeApplication(type);
            return true;
        }

        return false;
    }

    default boolean isChangeRendererSupported() {
        return Gdx.app.getApplicationListener() instanceof RenderChanger;
    }

    void setFramesPerSecond(int fpslimit);

    boolean isActive();
}
