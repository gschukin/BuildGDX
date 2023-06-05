package ru.m210projects.Build.osd.commands;

import org.jetbrains.annotations.NotNull;

public class OsdValueRange extends OsdValue {

    protected float value;

    public OsdValueRange(@NotNull String name, @NotNull String description, float min, float max) {
       this(name, description, 0, min, max);
    }

    public OsdValueRange(@NotNull String name, @NotNull String description, float value, float min, float max) {
        super(name, description, v -> (v >= min) && (v <= max));
        setCheckedValue(value);
    }

    @Override
    public float getValue() {
        return value;
    }

    @Override
    protected void setCheckedValue(float value) {
        this.value = value;
    }
}
