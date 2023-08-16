//This file is part of BuildGDX.
//Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
//
//BuildGDX is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//BuildGDX is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.Pattern.ScreenAdapters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import ru.m210projects.Build.Architecture.BuildFrame.FrameType;
import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Architecture.BuildGraphics.Option;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Settings.BuildConfig;
import ru.m210projects.Build.Settings.BuildConfig.GameKeys;
import ru.m210projects.Build.input.GameKey;
import ru.m210projects.Build.input.GameKeyListener;

public abstract class SkippableAdapter extends ScreenAdapter implements GameKeyListener {

    protected BuildGame game;
    protected Engine engine;
    protected boolean escSkip;
    protected Runnable skipCallback;

    public SkippableAdapter(BuildGame game) {
        this.game = game;
        this.engine = game.pEngine;
    }

    public SkippableAdapter setSkipping(Runnable skipCallback) {
        this.skipCallback = skipCallback;
        return this;
    }

    public SkippableAdapter escSkipping(boolean escSkip) {
        this.escSkip = escSkip;
        return this;
    }

    public abstract void draw(float delta);

    public void skip() {
        if (skipCallback != null) {
            Gdx.app.postRunnable(skipCallback);
            skipCallback = null;
        }
    }

    @Override
    public final void render(float delta) {
        engine.clearview(0);
        engine.getTimer().update();

        draw(delta);

        engine.nextpage();
    }

    private boolean onSkipPressed(GameKey gameKey) {
        if (!escSkip || GameKeys.Menu_Toggle.equals(gameKey)) {
            skip();
            return true;
        }
        return false;
    }

    @Override
    public void pause() {
//        if (BuildGdx.graphics.getFrameType() == FrameType.GL) {
//            BuildGdx.graphics.extra(Option.GLDefConfiguration);
//        }
    }

    @Override
    public void resume() {
        game.updateColorCorrection();
    }

    @Override
    public boolean onGameKeyPressed(GameKey gameKey) {
        return onSkipPressed(gameKey);
    }

    @Override
    public boolean keyDown(int keycode) {
        return onSkipPressed(BuildConfig.MenuKeys.Menu_Cancel);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return onSkipPressed(BuildConfig.MenuKeys.Menu_Cancel);
    }
}
