package ru.m210projects.Build.filehandle;

import java.io.IOException;
import java.io.InputStream;

public interface InputStreamProvider {

    InputStream newInputStream() throws IOException;

}
