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

package ru.m210projects.Build.Pattern.MenuItems;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Pattern.MenuItems.MenuHandler.MenuOpt;
import ru.m210projects.Build.Settings.BuildConfig;
import ru.m210projects.Build.Types.ConvertType;
import ru.m210projects.Build.Types.Transparent;
import ru.m210projects.Build.Types.font.Font;
import ru.m210projects.Build.Types.font.TextAlign;
import ru.m210projects.Build.input.GameKey;
import ru.m210projects.Build.input.GameKeyListener;

public abstract class MenuVariants extends MenuTitle implements GameKeyListener {
    public MenuVariants(Engine draw, String text, Font font, int x, int y) {
        super(draw, text, font, x, y, -1);
        this.flags = 3 | 4;
    }

    @Override
    public void draw(MenuHandler handler) {
        if (text != null) {
            font.drawTextScaled(x, y - font.getSize() / 2, text, 1.0f, handler.getShade(this), pal, TextAlign.Center, Transparent.None, ConvertType.Normal, fontShadow);
        }

        handler.mPostDraw(this);
    }

    @Override
    public boolean callback(MenuHandler handler, MenuOpt opt) {
        return false;
    }

    public abstract void positive(MenuHandler handler);

    public void negative(MenuHandler handler) {
        handler.mMenuBack();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        switch (button) {
            case Input.Buttons.LEFT:
                positive(menuHandler);
                return true;
            case Input.Buttons.RIGHT:
                negative(menuHandler);
                return true;
        }
        return false;
    }

    @Override
    public boolean onGameKeyPressed(GameKey gameKey) {
        if (BuildConfig.MenuKeys.Menu_Enter.equals(gameKey)) {
            positive(menuHandler);
            return true;
        }

        if (BuildConfig.MenuKeys.Menu_Cancel.equals(gameKey) || BuildConfig.GameKeys.Menu_Toggle.equals(gameKey)) {
            negative(menuHandler);
            return true;
        }

        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Keys.Y) {
            positive(menuHandler);
            return true;
        }

        if (keycode == Keys.N) {
            negative(menuHandler);
            return true;
        }

        return false;
    }
}