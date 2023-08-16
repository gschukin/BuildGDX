package ru.m210projects.Build.input;

import com.badlogic.gdx.InputProcessor;

public interface GameKeyListener extends InputProcessor {

    boolean onGameKeyPressed(GameKey gameKey);

    default void anyKeyPressed() {
    }

    default boolean keyRepeat(int keycode) {
        return false;
    }

    @Override
    default boolean keyDown(int keycode) {
        return false;
    }

    @Override
    default boolean keyUp(int keycode) {
        return false;
    }

    @Override
    default boolean keyTyped(char character) {
        return false;
    }

    @Override
    default boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    default boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    default boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    default boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    default boolean scrolled(int amount) {
        return false;
    }

}
