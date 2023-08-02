package ru.m210projects.Build.Types;

import com.badlogic.gdx.InputProcessor;
import ru.m210projects.Build.Settings.BuildConfig;

public class BuildInputController implements InputProcessor {

    private final BuildConfig config;

    public boolean onKeyPressed(BuildConfig.KeyType keyType) {

        System.out.println(keyType);
        return true;
    }

    public BuildInputController(BuildConfig config) {
        this.config = config;
    }

    @Override
    public boolean keyDown(int i) {
        return onKeyPressed(config.arrayPressedKey[i]);
    }

    @Override
    public boolean keyUp(int i) {
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean touchDown(int i, int i1, int i2, int button) {
        return false; //onKeyPressed(config.mousekeys[button]);
    }

    @Override
    public boolean touchUp(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchDragged(int i, int i1, int i2) {
        return false;
    }

    @Override
    public boolean mouseMoved(int i, int i1) {
        return false;
    }

    @Override
    public boolean scrolled(int i) {
        return false;
    }
}
