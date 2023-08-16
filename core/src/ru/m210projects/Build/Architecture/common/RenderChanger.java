package ru.m210projects.Build.Architecture.common;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;

public abstract class RenderChanger implements ApplicationListener {

    protected ApplicationListener listener;
    private boolean changing = false;

    public RenderChanger(ApplicationListener listener) {
        this.listener = listener;
    }

    protected abstract void startApplication(RenderType type);

    @Override
    public void create() {
        BuildGdx.app = (BuildApplication) Gdx.app;

        if (changing) {
            if (listener instanceof RenderListener) {
                ((RenderListener) listener).createRenderer();
            }
            changing = false;
            return;
        }

        listener.create();
    }

    @Override
    public void resize(int width, int height) {
        listener.resize(width, height);
    }

    @Override
    public void render() {
        listener.render();
    }

    @Override
    public void pause() {
        listener.pause();
    }

    @Override
    public void resume() {
        listener.resume();
    }

    @Override
    public void dispose() {
        if (changing) {
            if (listener instanceof RenderListener) {
                ((RenderListener) listener).disposeRenderer();
            }
            return;
        }

        listener.dispose();
    }

    protected void changeApplication(RenderType type) {
        changing = true;

        Gdx.app.exit();

        final Thread oldThread = Thread.currentThread();

        new Thread(() -> {
            try {
                oldThread.join();
            } catch (InterruptedException ignore) {
            }

            try {
                startApplication(type);
            } catch (Exception e) {
                e.printStackTrace();
                startApplication(RenderType.SOFTWARE);
            }
        }).start();
    }
}
