package ru.m210projects.Build.filehandle;

import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.OsdColor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public interface Entry {

    InputStream getInputStream() throws IOException;

    /**
     * @return full file name with extension
     */
    String getName();

    /**
     * @return file extension in upper case
     */
    String getExtension();
    long getSize();
    boolean exists();

    Group getParent();

    void setParent(Group parent);

    default boolean isExtension(String fmt) {
        return getExtension().equalsIgnoreCase(fmt);
    }
    
    default boolean isDirectory() {
        return false;
    }

    default byte[] getBytes() {
        long size = getSize();
        if(exists() && size > 0) {
//            long start = System.currentTimeMillis();
            try (InputStream is = getInputStream()) {
                return StreamUtils.readBytes(is, new byte[(int) size]);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            finally {
//                Console.out.println(String.format("file = %s read for %dms", this, System.currentTimeMillis() - start), OsdColor.RED);
//            }
        }
        return new byte[0];
    }
}
