package ru.m210projects.Build.osd;

import ru.m210projects.Build.Architecture.BuildGdx;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static ru.m210projects.Build.Strhandler.toLowerCase;

public class ConsoleLogger {

    private final OutputStream out;
    private final Path path;
    private boolean closed;

    public ConsoleLogger(Path path) throws IOException {
        this.path = path;
        this.out = new BufferedOutputStream(Files.newOutputStream(path));
        this.closed = false;
    }

    public void write(String message) {
        if(closed) {
            return;
        }

        try {
            out.write(String.format("%s\r\n", message).getBytes());
        } catch (IOException ignored) {
        }
    }

    @Override
    public String toString() {
        try(InputStream is = Files.newInputStream(path)) {
            byte[] data = new byte[is.available()];
            int len = is.read(data);
            if (len > 0) {
                return new String(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Can't read the log file";
    }

    public void close() {
        if(closed) {
            return;
        }

        try {
            out.close();
            closed = true;
        } catch (Exception ignored) {
        }
    }

}
