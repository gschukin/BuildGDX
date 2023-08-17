package ru.m210projects.Build.desktop.backend.awt;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AWTWindow extends WindowAdapter implements Disposable {

    private JFrame windowHandle;
    final ApplicationListener listener;
    final Application application;
    private final LwjglApplicationConfiguration config;
    private AWTGraphics graphics;
    private AWTInput input;
    private boolean listenerInitialized = false;
    private final Array<Runnable> runnables = new Array<Runnable>();
    private final Array<Runnable> executedRunnables = new Array<Runnable>();
    boolean iconified = false;
    boolean focused = false;
    boolean activated = false;
    boolean closing = false;
    private boolean requestRendering = false;

    @Override
    public void windowIconified(WindowEvent e) {
        iconified = true;
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        iconified = false;
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        focused = true;
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        focused = false;
    }

    @Override
    public void windowActivated(WindowEvent e) {
        activated = true;
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        activated = false;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        closing = true;
    }

    public AWTWindow(ApplicationListener listener, LwjglApplicationConfiguration config, Application application) {
        this.listener = listener;
        this.config = config;
        this.application = application;
    }

    /**
     * @return the {@link ApplicationListener} associated with this window
     **/
    public ApplicationListener getListener() {
        return listener;
    }

    boolean isListenerInitialized() {
        return listenerInitialized;
    }

    void initializeListener() {
        if (!listenerInitialized) {
            listener.create();
            listener.resize(graphics.getWidth(), graphics.getHeight());
            listenerInitialized = true;
        }
    }

    public void swapBuffers() {
        graphics.repaint();

        if (config.vSyncEnabled) {
            Toolkit.getDefaultToolkit().sync();
        }
    }

    void makeCurrent() {
        Gdx.graphics = graphics;
        Gdx.input = input;

        if (activated) {
//            windowHandle.toFront();
//            windowHandle.requestFocus();
        }
    }

    @Override
    public void dispose() {
        windowHandle.removeWindowListener(this);
        windowHandle.removeWindowFocusListener(this);
        listener.pause();
        listener.dispose();
        graphics.dispose();
        input.dispose();
        windowHandle.dispose();
    }

    public boolean update() {
        if (!listenerInitialized) {
            initializeListener();
        }
        synchronized (runnables) {
            executedRunnables.addAll(runnables);
            runnables.clear();
        }
        for (Runnable runnable : executedRunnables) {
            runnable.run();
        }
        boolean shouldRender = executedRunnables.size > 0 || graphics.isContinuousRendering();
        executedRunnables.clear();

        if (!iconified) {
            input.update();
        }

        synchronized (this) {
            shouldRender |= requestRendering && !iconified;
            requestRendering = false;
        }

        if (shouldRender) {
            graphics.update();
            listener.render();
            swapBuffers();
        }

        if (!iconified) {
            input.prepareNext();
        }

        return shouldRender;
    }

    public boolean shouldClose() {
        return closing;
    }

    void requestRendering() {
        synchronized (this) {
            this.requestRendering = true;
        }
    }

    public void create() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = ge.getDefaultScreenDevice();

        windowHandle = new JFrame(device.getDefaultConfiguration());
        windowHandle.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        windowHandle.setTitle(config.title);
        windowHandle.setPreferredSize(new Dimension(config.width, config.height));
        windowHandle.setMinimumSize(new Dimension(config.width, config.height));
        windowHandle.setMaximumSize(new Dimension(config.width, config.height));
        windowHandle.setVisible(false);
        windowHandle.setResizable(config.resizable);
        windowHandle.setUndecorated(!AWTApplication.windowDecorated);

        if (!config.fullscreen) {
            if (config.x == -1 && config.y == -1) {

            } else {
                windowHandle.setLocation(config.x, config.y);
            }
        } else {

        }

        // FIXME: 09.10.2022
        // set icon
        // set fullscreen

        this.graphics = new AWTGraphics(this);
        this.input = new AWTInput(this);

        windowHandle.addWindowListener(this);
        windowHandle.addWindowFocusListener(this);

        windowHandle.setLocationRelativeTo(null);
        windowHandle.pack();
    }

    public JFrame getWindowHandle() {
        return windowHandle;
    }

    public void setVisible(boolean initialVisible) {
        windowHandle.setVisible(initialVisible);
    }

    public void setPosition(int x, int y) {
        windowHandle.setLocation(x, y);
    }

    public int getPositionX() {
        return windowHandle.getX();
    }

    public int getPositionY() {
        return windowHandle.getY();
    }

    public AWTInput getInput() {
        return input;
    }

    public AWTGraphics getGraphics() {
        return graphics;
    }

    public LwjglApplicationConfiguration getConfig() {
        return config;
    }
}
