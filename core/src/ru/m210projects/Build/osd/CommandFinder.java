package ru.m210projects.Build.osd;

import ru.m210projects.Build.osd.commands.OsdCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandFinder {

    private int osdTabCommandIndex;
    private List<String> osdTabCommandList;
    private final Map<String, OsdCommand> osdVars;

    public CommandFinder(Map<String, OsdCommand> osdVars) {
        this.osdVars = osdVars;
    }

    /**
     * Resets command list.
     * Should be called when any other key is pressed.
     */
    public void reset() {
        osdTabCommandList = null;
    }

    public String getNextTabCommand() {
        String msg = osdTabCommandList.get(osdTabCommandIndex++);
        if(osdTabCommandIndex >= osdTabCommandList.size()) {
            osdTabCommandIndex = 0;
        }
        return msg;
    }

    public boolean isListPresent() {
        return osdTabCommandList != null;
    }

    /**
     * @param inputText reference text to search command
     * Updates osdTabCommandList if command count more then 1
     */
    public List<String> getCommands(String inputText) {
        if(inputText.isEmpty()) {
            return new ArrayList<>(1);
        }

        List<String> commands;
        commands = osdVars.keySet().stream().filter(e -> e.startsWith(inputText)).sorted().collect(Collectors.toList());
        if(commands.size() > 1) {
            osdTabCommandList = commands;
            osdTabCommandIndex = 0;
        }
        return commands;
    }
}
