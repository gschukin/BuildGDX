package ru.m210projects.Build.desktop.backend.awt;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl.*;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.*;
import ru.m210projects.Build.Architecture.common.BuildApplication;
import ru.m210projects.Build.desktop.audio.LwjglAudio;
import ru.m210projects.Build.Architecture.common.RenderType;

import javax.swing.*;
import java.io.File;

public class AWTApplication implements Application, BuildApplication {

    public static boolean windowDecorated = true;
    public static HdpiMode hdpiMode;
    private final LwjglApplicationConfiguration config;
    final Array<AWTWindow> windows = new Array<>();
    private volatile AWTWindow currentWindow;
    private LwjglAudio audio;
    private final Files files;
    private final Net net;
    private final ObjectMap<String, Preferences> preferences = new ObjectMap<>();
    private final LwjglClipboard clipboard;
    private int logLevel = LOG_INFO;
    private ApplicationLogger applicationLogger;
    private volatile boolean running = true;
    private final Array<Runnable> runnables = new Array<>();
    private final Array<Runnable> executedRunnables = new Array<>();
    private final Array<LifecycleListener> lifecycleListeners = new Array<>();

    public AWTApplication(ApplicationListener listener, LwjglApplicationConfiguration config) {
        this.config = config;
        this.running = true;
        LwjglNativesLoader.load();
        this.setApplicationLogger(new LwjglApplicationLogger());
        if (config.title == null) {
            config.title = listener.getClass().getSimpleName();
        }

        Gdx.app = this;
        if (!LwjglApplicationConfiguration.disableAudio) {
            try {
                this.audio = createAudio(config);
            } catch (Throwable t) {
                log("AWTApplication", "Couldn't initialize audio, disabling audio", t);
                LwjglApplicationConfiguration.disableAudio = true;
            }
        }
        Gdx.audio = audio;
        this.files = Gdx.files = createFiles();
        this.net = Gdx.net = new LwjglNet(config);
        this.clipboard = new LwjglClipboard();

        AWTWindow window = createWindow(config, listener, null);
        windows.add(window);
        try {
            loop();
            cleanupWindows();
        } catch (Throwable t) {
            if (t instanceof RuntimeException)
                throw (RuntimeException)t;
            else
                throw new GdxRuntimeException(t);
        } finally {
            cleanup();
        }
    }

    protected void loop () {
        Array<AWTWindow> closedWindows = new Array<>();
        while (running && windows.size > 0) {
            if (!LwjglApplicationConfiguration.disableAudio) {
                audio.update();
            }

            boolean haveWindowsRendered = false;
            closedWindows.clear();
            int targetFramerate = -2;
            for (AWTWindow window : windows) {
                window.makeCurrent();
                currentWindow = window;
                if (targetFramerate == -2) targetFramerate = window.getConfig().foregroundFPS;
                synchronized (lifecycleListeners) {
                    haveWindowsRendered |= window.update();
                }
                if (window.shouldClose()) {
                    closedWindows.add(window);
                }
            }

            boolean shouldRequestRendering;
            synchronized (runnables) {
                shouldRequestRendering = runnables.size > 0;
                executedRunnables.clear();
                executedRunnables.addAll(runnables);
                runnables.clear();
            }
            for (Runnable runnable : executedRunnables) {
                runnable.run();
            }
            if (shouldRequestRendering) {
                // Must follow Runnables execution so changes done by Runnables are reflected
                // in the following render.
                for (AWTWindow window : windows) {
                    if (!window.getGraphics().isContinuousRendering()) {
                        window.requestRendering();
                    }
                }
            }

            for (AWTWindow closedWindow : closedWindows) {
                if (windows.size == 1) {
                    // Lifecycle listener methods have to be called before ApplicationListener methods. The
                    // application will be disposed when _all_ windows have been disposed, which is the case,
                    // when there is only 1 window left, which is in the process of being disposed.
                    for (int i = lifecycleListeners.size - 1; i >= 0; i--) {
                        LifecycleListener l = lifecycleListeners.get(i);
                        l.pause();
                        l.dispose();
                    }
                    lifecycleListeners.clear();
                }
                closedWindow.dispose();

                windows.removeValue(closedWindow, false);
            }

            if (!haveWindowsRendered) {
                // Sleep a few milliseconds in case no rendering was requested
                // with continuous rendering disabled.
                try {
                    Thread.sleep(1000 / config.backgroundFPS);
                } catch (InterruptedException ignore) {
                }
            } else if (targetFramerate > 0) {
                AWTSync.sync(targetFramerate); // sleep as needed to meet the target framerate
            }
        }
    }

//    public AWTWindow newWindow (ApplicationListener listener, LwjglApplicationConfiguration config) {
////        LwjglApplicationConfiguration appConfig = LwjglApplicationConfiguration.copy(this.config);
////        appConfig.setWindowConfiguration(config);
//        if (config.title == null) config.title = listener.getClass().getSimpleName();
//        return createWindow(config, listener, windows.get(0).getWindowHandle());
//    }

    private AWTWindow createWindow(LwjglApplicationConfiguration config, ApplicationListener listener, final JFrame sharedContext) {
        final AWTWindow window = new AWTWindow(listener, config, this);
        if (sharedContext == null) {
            // the main window is created immediately
            createWindow(window, config, sharedContext);
        } else {
            // creation of additional windows is deferred to avoid GL context trouble
            postRunnable(() -> {
                createWindow(window, config, sharedContext);
                windows.add(window);
            });
        }
        return window;
    }

    protected void createWindow (AWTWindow window, LwjglApplicationConfiguration config, JFrame sharedContext) {
        window.create();
        window.setVisible(true);
    }

    protected void cleanupWindows () {
        synchronized (lifecycleListeners) {
            for (LifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.pause();
                lifecycleListener.dispose();
            }
        }
        for (AWTWindow window : windows) {
            window.dispose();
        }
        windows.clear();
        running = false;
    }

    protected void cleanup () {
        if(running) {
            for (AWTWindow window : windows) {
                window.getGraphics().dispose();
            }
            windows.clear();
        }
        audio.dispose();
    }

    @Override
    public ApplicationListener getApplicationListener () {
        return currentWindow.getListener();
    }

    @Override
    public Graphics getGraphics () {
        return currentWindow.getGraphics();
    }

    @Override
    public Audio getAudio () {
        return audio;
    }

    @Override
    public Input getInput () {
        return currentWindow.getInput();
    }

    @Override
    public Files getFiles() {
        return files;
    }

    @Override
    public Net getNet() {
        return net;
    }

    @Override
    public void debug (String tag, String message) {
        if (logLevel >= LOG_DEBUG) getApplicationLogger().debug(tag, message);
    }

    @Override
    public void debug (String tag, String message, Throwable exception) {
        if (logLevel >= LOG_DEBUG) getApplicationLogger().debug(tag, message, exception);
    }

    @Override
    public void log (String tag, String message) {
        if (logLevel >= LOG_INFO) getApplicationLogger().log(tag, message);
    }

    @Override
    public void log (String tag, String message, Throwable exception) {
        if (logLevel >= LOG_INFO) getApplicationLogger().log(tag, message, exception);
    }

    @Override
    public void error (String tag, String message) {
        if (logLevel >= LOG_ERROR) getApplicationLogger().error(tag, message);
    }

    @Override
    public void error (String tag, String message, Throwable exception) {
        if (logLevel >= LOG_ERROR) getApplicationLogger().error(tag, message, exception);
    }

    @Override
    public void setLogLevel (int logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public int getLogLevel () {
        return logLevel;
    }

    @Override
    public void setApplicationLogger (ApplicationLogger applicationLogger) {
        this.applicationLogger = applicationLogger;
    }

    @Override
    public ApplicationLogger getApplicationLogger () {
        return applicationLogger;
    }

    @Override
    public ApplicationType getType () {
        return ApplicationType.Desktop;
    }

    @Override
    public int getVersion () {
        return 0;
    }

    @Override
    public long getJavaHeap () {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    @Override
    public long getNativeHeap () {
        return getJavaHeap();
    }

    @Override
    public Preferences getPreferences (String name) {
        if (preferences.containsKey(name)) {
            return preferences.get(name);
        } else {
            Preferences prefs = new LwjglPreferences(
                    new LwjglFileHandle(new File(config.preferencesDirectory, name), config.preferencesFileType));
            preferences.put(name, prefs);
            return prefs;
        }
    }

    @Override
    public Clipboard getClipboard () {
        return clipboard;
    }

    @Override
    public void postRunnable (Runnable runnable) {
        synchronized (runnables) {
            runnables.add(runnable);
        }
    }

    @Override
    public void exit () {
        running = false;
    }

    @Override
    public void addLifecycleListener (LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.add(listener);
        }
    }

    @Override
    public void removeLifecycleListener (LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.removeValue(listener, true);
        }
    }

    public LwjglAudio createAudio(LwjglApplicationConfiguration config) {
        return new LwjglAudio(config.audioDeviceSimultaneousSources, config.audioDeviceBufferCount,
                config.audioDeviceBufferSize);
    }

    protected Files createFiles () {
        return new LwjglFiles();
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SOFTWARE;
    }

    @Override
    public void setFramesPerSecond(int fpslimit) {

    }

    @Override
    public boolean isActive() {
        return false;
    }
}
