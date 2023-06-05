package ru.m210projects.Build.osd.commands;

import ru.m210projects.Build.osd.CommandResponse;

public class UnknownCommand extends OsdCommand {

    public UnknownCommand() {
        super("Unknown");
    }

    public CommandResponse execute(String[] argv) {
        return CommandResponse.UNKNOWN_RESPONSE;
    }
}
