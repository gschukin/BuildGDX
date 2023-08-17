package ru.m210projects.Build.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.Vector2;
import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Architecture.common.BuildApplication;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.BuildNet;
import ru.m210projects.Build.input.keymap.Keymap;
import ru.m210projects.Build.osd.Console;

import static ru.m210projects.Build.RenderService.xdim;
import static ru.m210projects.Build.RenderService.ydim;
import static ru.m210projects.Build.Settings.BuildConfig.UNKNOWN_KEY;

public abstract class GameProcessor implements GameKeyListener {

    protected final BuildGame game;
    protected boolean[] keyState;
    protected int lastKeyPressed;
    float keyRepeatTimer;
    static public float keyRepeatInitialTime = 0.1f;
    static public float keyRepeatTime = 0.01f;
    protected final Vector2 mouseDelta;
    protected final Vector2 stick1;
    protected final Vector2 stick2;

    public GameProcessor(BuildGame game) {
        this.game = game;
        this.keyState = new boolean[game.pCfg.getKeyMap().length + 1];
        this.mouseDelta = new Vector2();
        this.stick1 = new Vector2();
        this.stick2 = new Vector2();
    }

    /**
     * @param input network empty input to fill by game
     */
    public abstract void fillInput(BuildNet.NetInput input);


    /**
     called by syncInput (in faketimehandler) by BuildGame (30 times per sec with network sync)
     */
    public void processInput(BuildNet.NetInput input) {
        if (lastKeyPressed != -1) {
            keyRepeatTimer -= Gdx.graphics.getDeltaTime();
            if (keyRepeatTimer < 0) {
                keyRepeatTimer = keyRepeatTime;
                GameKeyListener gameKeyListener = getGameKeyListener();
                if (gameKeyListener != null) {
                    gameKeyListener.anyKeyPressed();
                    gameKeyListener.keyRepeat(lastKeyPressed);
                }
            }
        }

        input.reset();
        fillInput(input);

        if (!Console.out.isShowing() && !game.pMenu.isShowing()) {
            resetMousePos();
        }
    }

    protected GameKeyListener getGameKeyListener() {
        if (Console.out.isShowing()) {
            return Console.out;
        }

        if (game.pMenu.isShowing()) {
            return game.pMenu;
        }

        Screen screen = game.getScreen();
        if (screen instanceof GameKeyListener) {
            return (GameKeyListener) screen;
        }

        return null;
    }

    public void resetMousePos() {
//        if (((BuildApplication) Gdx.app).isActive()) {
//            mouseDelta.set(0, 0);
//            Gdx.input.setCursorPosition(xdim / 2, ydim / 2);
//
//            // reset axis input
//        }
    }

    @Override
    public boolean onGameKeyPressed(GameKey gameKey) {
        if (gameKey.equals(UNKNOWN_KEY)) {
            return false;
        }

        keyState[gameKey.getNum()] = true;
        return false;
    }

    public void onGameKeyReleased(GameKey gameKey) {
        if (gameKey.equals(UNKNOWN_KEY)) {
            return;
        }

        keyState[gameKey.getNum()] = false;
    }

    @Override
    public boolean keyDown(int i) {
        if (lastKeyPressed == -1) {
            keyRepeatTimer = keyRepeatInitialTime;
            lastKeyPressed = i;
        }

        GameKey gameKey = game.pCfg.convertToGameKey(i);
        onGameKeyPressed(gameKey);

        GameKeyListener gameKeyListener = getGameKeyListener();
        if (gameKeyListener != null) {
            gameKeyListener.anyKeyPressed();
            if (gameKeyListener.onGameKeyPressed(gameKey)) {
                return true;
            }
            return gameKeyListener.keyDown(i);
        }
        return false;
    }

    @Override
    public boolean keyUp(int i) {
        lastKeyPressed = -1;
        onGameKeyReleased(game.pCfg.convertToGameKey(i));
        GameKeyListener gameKeyListener = getGameKeyListener();
        if (gameKeyListener != null) {
            return gameKeyListener.keyUp(i);
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        onGameKeyPressed(game.pCfg.convertToGameKey(Keymap.MOUSE_LBUTTON + button));
        GameKeyListener gameKeyListener = getGameKeyListener();
        if (gameKeyListener != null) {
            gameKeyListener.anyKeyPressed();
            return gameKeyListener.touchDown(screenX, screenY, pointer, button);
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        onGameKeyReleased(game.pCfg.convertToGameKey(Keymap.MOUSE_LBUTTON + button));
        GameKeyListener gameKeyListener = getGameKeyListener();
        if (gameKeyListener != null) {
            return gameKeyListener.touchUp(screenX, screenY, pointer, button);
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        mouseDelta.add(Gdx.input.getDeltaX(), Gdx.input.getDeltaY());
//        if (mouseDelta.x != 0) {
//            onGameKeyPressed(game.pCfg.convertToGameKey(0, (int) mouseDelta.x));
//        }
//
//        if (mouseDelta.y != 0) {
//            onGameKeyPressed(game.pCfg.convertToGameKey(1, (int) mouseDelta.y));
//        }

        GameKeyListener gameKeyListener = getGameKeyListener();
        if (gameKeyListener != null) {
            return gameKeyListener.mouseMoved(screenX, screenY);
        }
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        GameKeyListener gameKeyListener = getGameKeyListener();
        if (gameKeyListener != null) {
            gameKeyListener.anyKeyPressed();
            return gameKeyListener.scrolled(amount);
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        mouseMoved(screenX, screenY);
        GameKeyListener gameKeyListener = getGameKeyListener();
        if (gameKeyListener != null) {
            return gameKeyListener.touchDragged(screenX, screenY, pointer);
        }
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        GameKeyListener gameKeyListener = getGameKeyListener();
        if (gameKeyListener != null) {
            gameKeyListener.anyKeyPressed();
            return gameKeyListener.keyTyped(c);
        }
        return false;
    }

    protected boolean isGameKeyPressed(GameKey gameKey, boolean resetState) {
        boolean status = keyState[gameKey.getNum()];
        if (resetState) {
            keyState[gameKey.getNum()] = false;
        }
        return status;
    }

    public float ctrlGetMouseMove() {
        return mouseDelta.y * getCommonMouseSensitivity() * game.pCfg.gMouseMoveSpeed / 65536f;
    }

    public float ctrlGetMouseLook(boolean invert) {
        float value = mouseDelta.y * getCommonMouseSensitivity() * game.pCfg.gMouseLookSpeed / 65536f;
        return invert ? -value : value;
    }

    public float ctrlGetMouseTurn() {
        return mouseDelta.x * getCommonMouseSensitivity() * game.pCfg.gMouseTurnSpeed / 65536f;
    }

    public float ctrlGetMouseStrafe() {
        return mouseDelta.x * getCommonMouseSensitivity() * game.pCfg.gMouseStrafeSpeed / 2097152f;
    }

    public float getCommonMouseSensitivity() {
        return game.pCfg.gSensitivity / 65536.0f;
    }
}
