package ru.m210projects.Build.Types.collections;

import ru.m210projects.Build.Types.Sprite;

import java.util.List;

public class SpriteMap extends LinkedMap<Sprite> {

    public SpriteMap(int listCount, List<Sprite> spriteList, int spriteCount, ValueSetter<Sprite> valueSetter) {
        super(listCount, spriteList, spriteCount, valueSetter);
    }

    @Override
    protected Sprite getInstance() {
        return new Sprite();
    }

}
