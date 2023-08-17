package ru.m210projects.Build.desktop.backend.awt;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import ru.m210projects.Build.Architecture.common.SoftwareGraphics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.util.*;

public class AWTGraphics implements Graphics, SoftwareGraphics {

    protected AWTWindow window;

    private volatile int backBufferWidth;
    private volatile int backBufferHeight;
    private volatile int logicalWidth;
    private volatile int logicalHeight;
    private long lastFrameTime = -1;
    private float deltaTime;
    private boolean resetDeltaTime = false;
    private long frameId;
    private long frameCounterStart = 0;
    private int frames;
    private int fps;
    private int windowPosXBeforeFullscreen;
    private int windowPosYBeforeFullscreen;
    private int windowWidthBeforeFullscreen;
    private int windowHeightBeforeFullscreen;
    private BufferFormat bufferFormat;
    private volatile boolean isContinuous = true;

    private DisplayMode displayModeBeforeFullscreen;
    private boolean fullscreen;

    Raster raster;

    private final ComponentAdapter resizeCallback = new ComponentAdapter() {
        volatile boolean posted;

        @Override
        public void componentResized(ComponentEvent evt) {
            if (posted) return;
            posted = true;
            Gdx.app.postRunnable(() -> {
                posted = false;
                updateFramebufferInfo();
                if (!window.isListenerInitialized()) {
                    return;
                }
                window.makeCurrent();
                window.getListener().resize(getWidth(), getHeight());
                window.getListener().render();
                window.swapBuffers();
            });
        }
    };

    public AWTGraphics(AWTWindow window) {
        this.window = window;

        com.badlogic.gdx.graphics.Color color = window.getConfig().initialBackgroundColor;
        this.raster = new Raster(new Color(color.r, color.g, color.b, color.a));
        updateFramebufferInfo();
        window.getWindowHandle().addComponentListener(resizeCallback);
        window.getWindowHandle().add(raster);
    }

    void updateFramebufferInfo() {
        JFrame windowHandle = window.getWindowHandle();
        Dimension size = windowHandle.getPreferredSize();
        this.backBufferWidth = (int) size.getWidth();
        this.backBufferHeight = (int) size.getHeight();
        this.logicalWidth = (int) size.getWidth();
        this.logicalHeight = (int) size.getHeight();
        LwjglApplicationConfiguration config = window.getConfig();
        bufferFormat = new BufferFormat(config.r, config.g, config.b, config.a, config.depth, config.stencil, config.samples,
                false);
        raster.update(logicalWidth, logicalHeight);
    }

    public void repaint() {
        raster.repaint();
    }

    public void update() {
        long time = System.nanoTime();
        if (lastFrameTime == -1) lastFrameTime = time;
        if (resetDeltaTime) {
            resetDeltaTime = false;
            deltaTime = 0;
        } else
            deltaTime = (time - lastFrameTime) / 1000000000.0f;
        lastFrameTime = time;

        if (time - frameCounterStart >= 1000000000) {
            fps = frames;
            frames = 0;
            frameCounterStart = time;
        }
        frames++;
        frameId++;
    }


    @Override
    public int getWidth() {
        if (AWTApplication.hdpiMode == HdpiMode.Pixels) {
            return backBufferWidth;
        } else {
            return logicalWidth;
        }
    }

    @Override
    public int getHeight() {
        if (AWTApplication.hdpiMode == HdpiMode.Pixels) {
            return backBufferHeight;
        } else {
            return logicalHeight;
        }
    }

    @Override
    public int getBackBufferWidth() {
        return backBufferWidth;
    }

    @Override
    public int getBackBufferHeight() {
        return backBufferHeight;
    }


    public float getBackBufferScale() {
        return 0;
    }

    public int getLogicalWidth() {
        return logicalWidth;
    }

    public int getLogicalHeight() {
        return logicalHeight;
    }

    @Override
    public long getFrameId() {
        return frameId;
    }

    @Override
    public float getDeltaTime() {
        return deltaTime;
    }

    @Override
    public float getRawDeltaTime() {
        return 0;
    }

    @Override
    public int getFramesPerSecond() {
        return fps;
    }

    @Override
    public GraphicsType getType() {
        return GraphicsType.LWJGL3;
    }

    @Override
    public float getPpiX() {
        return getPpcX() * 2.54f;
    }

    @Override
    public float getPpiY() {
        return getPpcY() * 2.54f;
    }

    @Override
    public float getPpcX() {
        float pixelPerMm = Toolkit.getDefaultToolkit().getScreenResolution() / 2.54f;
        DisplayMode mode = getDisplayMode();
        int sizeX = (int) (mode.width / pixelPerMm * 10);
        return mode.width / (float) sizeX * 10;
    }

    @Override
    public float getPpcY() {
        float pixelPerMm = Toolkit.getDefaultToolkit().getScreenResolution() / 2.54f;
        DisplayMode mode = getDisplayMode();
        int sizeY = (int) (mode.height / pixelPerMm * 10);
        return mode.height / (float) sizeY * 10;
    }

    @Override
    public boolean supportsDisplayModeChange() {
        AWTMonitor monitor = (AWTMonitor) getMonitor();
        return monitor.getMonitorHandle().isDisplayChangeSupported();
    }

    @Override
    public Monitor getPrimaryMonitor() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        Rectangle bounds = gc.getBounds();
        return new AWTMonitor(gd, bounds.x, bounds.y, gd.getIDstring());
    }

    @Override
    public Monitor getMonitor() {
        Monitor[] monitors = getMonitors();
        Monitor result = monitors[0];

        JFrame windowHandle = window.getWindowHandle();

        int windowX = windowHandle.getX();
        int windowY = windowHandle.getY();
        int windowWidth = windowHandle.getWidth();
        int windowHeight = windowHandle.getHeight();
        int overlap;
        int bestOverlap = 0;

        for (Monitor monitor : monitors) {
            DisplayMode mode = getDisplayMode(monitor);

            overlap = Math.max(0,
                    Math.min(windowX + windowWidth, monitor.virtualX + mode.width) - Math.max(windowX, monitor.virtualX))
                    * Math.max(0, Math.min(windowY + windowHeight, monitor.virtualY + mode.height) - Math.max(windowY, monitor.virtualY));

            if (bestOverlap < overlap) {
                bestOverlap = overlap;
                result = monitor;
            }
        }
        return result;
    }

    @Override
    public Monitor[] getMonitors() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        Monitor[] monitors = new Monitor[gs.length];
        for (int i = 0; i < monitors.length; i++) {
            GraphicsDevice gd = gs[i];
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            monitors[i] = new AWTMonitor(gd, bounds.x, bounds.y, gd.getIDstring());
        }
        return monitors;
    }

    @Override
    public DisplayMode[] getDisplayModes() {
        return getDisplayModes(getMonitor());
    }

    @Override
    public DisplayMode[] getDisplayModes(Monitor monitor) {
        java.awt.DisplayMode[] videoModes = ((AWTMonitor) monitor).getMonitorHandle().getDisplayModes();
        Set<AWTDisplayMode> modes = new LinkedHashSet<>();
        for (java.awt.DisplayMode videoMode : videoModes) {
            modes.add(new AWTDisplayMode((AWTMonitor) monitor, videoMode.getWidth(),
                    videoMode.getHeight(), videoMode.getRefreshRate(), videoMode.getBitDepth()));
        }
        DisplayMode[] result = new DisplayMode[modes.size()];
        modes.toArray(result);
        Arrays.sort(result, Comparator.comparingInt((DisplayMode mode) -> mode.width)
                .thenComparingInt(mode -> mode.height)
                .thenComparingInt(mode -> mode.refreshRate));
        return result;
    }

    @Override
    public DisplayMode getDisplayMode() {
        return getDisplayMode(getMonitor());
    }

    @Override
    public DisplayMode getDisplayMode(Monitor monitor) {
        java.awt.DisplayMode mode = ((AWTMonitor) monitor).getMonitorHandle().getDisplayMode();
        return new AWTDisplayMode((AWTMonitor) monitor, mode.getWidth(), mode.getHeight(), mode.getRefreshRate(), mode.getBitDepth());
    }

    @Override
    public boolean setFullscreenMode(DisplayMode displayMode) {
        window.getInput().resetPollingStates();
        AWTDisplayMode newMode = (AWTDisplayMode) displayMode;
        if (!isFullscreen()) {
            // store window position so we can restore it when switching from fullscreen to windowed later
            storeCurrentWindowPositionAndDisplayMode();
        }

        GraphicsDevice device = newMode.getMonitor().getMonitorHandle();
        if (!device.isFullScreenSupported()) {
            return false;
        }

        JFrame windowHandle = window.getWindowHandle();
        windowHandle.dispose();

        setUndecorated(true);
        device.setFullScreenWindow(windowHandle);

        if (device.isDisplayChangeSupported()) {
            device.setDisplayMode(new java.awt.DisplayMode(newMode.width, newMode.height, newMode.bitsPerPixel, newMode.refreshRate));
        }

        updateFramebufferInfo();

        fullscreen = true;
        setVSync(window.getConfig().vSyncEnabled);

        return true;
    }

    private void storeCurrentWindowPositionAndDisplayMode() {
        windowPosXBeforeFullscreen = window.getPositionX();
        windowPosYBeforeFullscreen = window.getPositionY();
        windowWidthBeforeFullscreen = logicalWidth;
        windowHeightBeforeFullscreen = logicalHeight;
        displayModeBeforeFullscreen = getDisplayMode();
    }

    @Override
    public boolean setWindowedMode(int width, int height) {
        window.getInput().resetPollingStates();
        GraphicsDevice device = ((AWTMonitor) getMonitor()).getMonitorHandle();
        if (!isFullscreen()) {
            int newX = 0, newY = 0;
            boolean centerWindow = false;
            if (width != logicalWidth || height != logicalHeight) {
                centerWindow = true;
                GraphicsConfiguration gc = device.getDefaultConfiguration();
                Rectangle bounds = gc.getBounds();
                newX = Math.max(0, bounds.x + (bounds.width - width) / 2);
                newY = Math.max(0, bounds.y + (bounds.height - height) / 2);
            }
            int refreshRate = device.getDisplayMode().getRefreshRate();
            int bpp = device.getDisplayMode().getBitDepth();

            if (device.isDisplayChangeSupported()) {
                device.setDisplayMode(new java.awt.DisplayMode(width, height, bpp, refreshRate));
            }

            if (centerWindow) {
                window.setPosition(newX, newY); // on macOS the centering has to happen _after_ the new window size was set
            }
        } else {
            setUndecorated(AWTApplication.windowDecorated);
            device.setFullScreenWindow(null);

            if (displayModeBeforeFullscreen == null) {
                storeCurrentWindowPositionAndDisplayMode();
            }

            int x = windowPosXBeforeFullscreen;
            int y = windowPosYBeforeFullscreen;
            if (width != windowWidthBeforeFullscreen || height != windowHeightBeforeFullscreen) { // Center window
                GraphicsConfiguration gc = device.getDefaultConfiguration();
                Rectangle bounds = gc.getBounds();
                x = Math.max(0, bounds.x + (bounds.width - width) / 2);
                y = Math.max(0, bounds.y + (bounds.height - height) / 2);
            }

            if (device.isDisplayChangeSupported()) {
                device.setDisplayMode(new java.awt.DisplayMode(width, height,
                        device.getDisplayMode().getBitDepth(),
                        displayModeBeforeFullscreen.refreshRate));
            }
            window.setPosition(x, y);
        }

        updateFramebufferInfo();
        fullscreen = false;
        return true;
    }

    @Override
    public void setTitle(String title) {
        if (title == null) {
            title = "";
        }
        window.getWindowHandle().setTitle(title);
    }

    @Override
    public void setUndecorated(boolean undecorated) {
        AWTApplication.windowDecorated = !undecorated;

        JFrame windowHandle = window.getWindowHandle();
        if (windowHandle.isUndecorated() == undecorated) {
            return;
        }

        windowHandle.dispose();
        windowHandle.setUndecorated(undecorated);
        windowHandle.setVisible(true);
    }

    @Override
    public void setResizable(boolean resizable) {
        window.getConfig().resizable = (resizable);
        window.getWindowHandle().setResizable(resizable);
    }

    @Override
    public void setVSync(boolean vsync) {
        window.getConfig().vSyncEnabled = vsync;
    }

    /**
     * Sets the target framerate for the application, when using continuous rendering. Must be positive. The cpu sleeps as needed.
     * Use 0 to never sleep. If there are multiple windows, the value for the first window created is used for all. Default is 0.
     *
     * @param fps fps
     */

    public void setForegroundFPS(int fps) {
        window.getConfig().foregroundFPS = fps;
    }

    @Override
    public BufferFormat getBufferFormat() {
        return bufferFormat;
    }

    @Override
    public void setContinuousRendering(boolean isContinuous) {
        this.isContinuous = isContinuous;
    }

    @Override
    public boolean isContinuousRendering() {
        return isContinuous;
    }

    @Override
    public void requestRendering() {
        window.requestRendering();
    }

    @Override
    public boolean isFullscreen() {
        return fullscreen;
    }

    public void dispose() {
        window.getWindowHandle().removeComponentListener(resizeCallback);
        window.getWindowHandle().setVisible(false);
        window.getWindowHandle().dispose();
    }

    @Override
    public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) {
        // FIXME: 07.10.2022
        return null;
    }

    @Override
    public void setCursor(Cursor cursor) {
        // GLFW.glfwSetCursor(getWindow().getWindowHandle(), ((Lwjgl3Cursor)cursor).glfwCursor);
    }

    @Override
    public void setSystemCursor(Cursor.SystemCursor systemCursor) {
        // Lwjgl3Cursor.setSystemCursor(getWindow().getWindowHandle(), systemCursor);
    }

    // Unsupported

    @Override
    public float getDensity() {
        return 0;
    }

    @Override
    public boolean supportsExtension(String extension) {
        return false;
    }

    @Override
    public boolean isGL30Available() {
        return false;
    }

    @Override
    public GL20 getGL20() {
        return null;
    }

    @Override
    public GL30 getGL30() {
        return null;
    }

    @Override
    public void setGL20(GL20 gl20) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGL30(GL30 gl30) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GLVersion getGLVersion() {
        return null;
    }

    @Override
    public byte[] getRasterBuffer() {
        return raster.getRasterBuffer();
    }

    @Override
    public void changePalette(byte[] palette) {
        raster.changePalette(palette);
    }

    private static class AWTDisplayMode extends DisplayMode {
        final AWTMonitor monitorHandle;

        public AWTDisplayMode(AWTMonitor monitorHandle, int width, int height, int refreshRate, int bitsPerPixel) {
            super(width, height, refreshRate, bitsPerPixel);
            this.monitorHandle = monitorHandle;
        }

        public AWTMonitor getMonitor() {
            return monitorHandle;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AWTDisplayMode)) return false;
            AWTDisplayMode that = (AWTDisplayMode) o;
            return Objects.equals(monitorHandle, that.monitorHandle)
                    && width == that.width
                    && height == that.height
                    && refreshRate == that.refreshRate
                    && bitsPerPixel == that.bitsPerPixel;
        }

        @Override
        public int hashCode() {
            return Objects.hash(monitorHandle, width, height, refreshRate, bitsPerPixel);
        }
    }

    private static class AWTMonitor extends Monitor {
        private final GraphicsDevice monitorHandle;

        AWTMonitor(GraphicsDevice monitorHandle, int virtualX, int virtualY, String name) {
            super(virtualX, virtualY, name);
            this.monitorHandle = monitorHandle;
        }

        public GraphicsDevice getMonitorHandle() {
            return monitorHandle;
        }
    }

     static class Raster extends Canvas implements SoftwareGraphics {
        private BufferedImage display;
        private byte[] data;
        private IndexColorModel paletteModel;
        private int width, height;

        private Color background;

        public Raster(Color background) {
            this.background = background;
        }

        @Override
        public void update(java.awt.Graphics g) {
            paint(g);
        }

        @Override
        public void paint(java.awt.Graphics g) {
            g.drawImage(display, 0, 0, null);
        }

        @Override
        public byte[] getRasterBuffer() {
            return data;
        }

        @Override
        public void changePalette(byte[] palette) {
            paletteModel = new IndexColorModel(8, 256, palette, 0, false);
            if (display == null) {
                display = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, paletteModel);
                data = ((DataBufferByte) display.getRaster().getDataBuffer()).getData();
            } else {
                display = new BufferedImage(paletteModel, display.getRaster(), false, null);
            }
        }

        public void update(int width, int height) {
            if (this.width == width && this.height == height) {
                return;
            }

            this.width = width;
            this.height = height;

            Dimension size = new Dimension(width, height);
            setPreferredSize(size);
            setSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setBackground(background);

            if (paletteModel != null) {
                display = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, paletteModel);
                data = ((DataBufferByte) display.getRaster().getDataBuffer()).getData();
            }

            validate();
        }
    }


}
