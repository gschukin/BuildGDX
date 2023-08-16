package ru.m210projects.Build.Architecture.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

import java.util.*;
import java.util.stream.Collectors;

public class ResolutionUtils {

    public static LinkedHashMap<String, List<Graphics.DisplayMode>> getDisplayModes() {
        return Arrays
                .stream(Gdx.graphics.getDisplayModes())
                .sorted(Comparator.comparingInt(a -> a.width))
                .collect(Collectors.groupingBy(e -> e.width + "x" + e.height, LinkedHashMap::new, Collectors.toList()));
    }

    public static Graphics.DisplayMode getDisplayMode(String resolution) {
        return Arrays.stream(Gdx.graphics.getDisplayModes())
                .filter(e -> (e.width + "x" + e.height).equals(resolution))
                .max(Comparator.comparingInt(a -> a.refreshRate)).orElse(null);
    }

}
