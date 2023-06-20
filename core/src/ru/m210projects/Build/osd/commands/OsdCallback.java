package ru.m210projects.Build.osd.commands;

import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.osd.CommandResponse;

public class OsdCallback extends OsdCommand {

    private final OsdRunnable callback;

    public OsdCallback(@NotNull String name, @NotNull String description, OsdRunnable callback) {
        super(name, description);
        this.callback = callback;
    }

    public CommandResponse execute(String[] argv) {
        return callback.execute(argv);
    }

    public interface OsdRunnable {
        CommandResponse execute(String[] argv);
    }

    public interface OsdSilentRunnable extends OsdRunnable {
        void run(String[] argv);

        default CommandResponse execute(String[] argv) {
            run(argv);
            return CommandResponse.SILENT_RESPONSE;
        }
    }
}
