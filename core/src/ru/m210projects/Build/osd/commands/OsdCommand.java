package ru.m210projects.Build.osd.commands;


import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.osd.CommandResponse;
import ru.m210projects.Build.osd.Console;

public abstract class OsdCommand {

    private final String name;
    private final String description;
    protected Console parent;

    public OsdCommand(@NotNull String name, @NotNull String description) {
        this.name = name;
        this.description = description;
    }

    public OsdCommand(@NotNull String name) {
        this.name = name;
        this.description = "";
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public abstract CommandResponse execute(String[] argv);

    public void setParent(Console parent) {
        this.parent = parent;
    }
}
