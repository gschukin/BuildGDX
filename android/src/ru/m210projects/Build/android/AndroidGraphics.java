package ru.m210projects.Build.android;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidGL20;
import com.badlogic.gdx.backends.android.AndroidGL30;
import com.badlogic.gdx.backends.android.surfaceview.GLSurfaceView20;
import com.badlogic.gdx.backends.android.surfaceview.GLSurfaceView20API18;
import com.badlogic.gdx.backends.android.surfaceview.GLSurfaceViewAPI18;
import com.badlogic.gdx.backends.android.surfaceview.GdxEglConfigChooser;
import com.badlogic.gdx.backends.android.surfaceview.ResolutionStrategy;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.math.WindowedMean;

import android.app.Activity;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import android.opengl.GLSurfaceView.Renderer;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import ru.m210projects.Build.Architecture.BuildConfiguration;
import ru.m210projects.Build.Architecture.BuildFrame.FrameStatus;
import ru.m210projects.Build.Architecture.BuildFrame.FrameType;
import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Architecture.BuildGraphics;

public class AndroidGraphics extends BuildGraphics implements Renderer {

	static {
//		GdxNativesLoader.load(); XXX
	}

	private final int r = 5, g = 6, b = 5, a = 0; // 16bit

	protected final Activity app;
	protected final AndroidFrame frame;
	protected final ResolutionStrategy resolutionStrategy;
	protected View view;
	private int rate;

	private BufferFormat bufferFormat = new BufferFormat(5, 6, 5, 0, 16, 0, 0, false);

	static volatile boolean enforceContinuousRendering = false;
	int width;
	int height;
	int safeInsetLeft, safeInsetTop, safeInsetBottom, safeInsetRight;
	EGLContext eglContext;
	GLVersion glVersion;
	String extensions;
	protected WindowedMean mean = new WindowedMean(5);

	private boolean wasResized = false;

	private float ppiX = 0;
	private float ppiY = 0;
	private float ppcX = 0;
	private float ppcY = 0;
	private float density = 1;

	volatile boolean created = false;
	volatile boolean running = false;
//	volatile boolean pause = false;
//	volatile boolean resume = false;
//	volatile boolean destroy = false;

	public AndroidGraphics(AndroidFrame frame, ResolutionStrategy resolutionStrategy) {
		this.frame = frame;
		this.app = frame.activity;
		this.resolutionStrategy = resolutionStrategy;
	}

	@Override
	protected void init() throws Exception {
		GLSurfaceView view = new GLSurfaceView(app); //XXX
		view.setRenderer(this);
		
//		view = createGLSurfaceView(app, resolutionStrategy);

		preserveEGLContextOnPause();

		view.setFocusable(true);
		view.setFocusableInTouchMode(true);

		try {
			app.requestWindowFeature(Window.FEATURE_NO_TITLE);
		} catch (Exception ex) {
//			log("AndroidApplication", "Content already displayed, cannot request FEATURE_NO_TITLE", ex);
		}

		app.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		app.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		app.setContentView(view, createLayoutParams());
	}

	protected FrameLayout.LayoutParams createLayoutParams() {
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;
		return layoutParams;
	}

	protected void preserveEGLContextOnPause() {
		int sdkVersion = android.os.Build.VERSION.SDK_INT;
		if (sdkVersion >= 11 && view instanceof GLSurfaceView20)
			((GLSurfaceView20) view).setPreserveEGLContextOnPause(true);
		if (view instanceof GLSurfaceView20API18)
			((GLSurfaceView20API18) view).setPreserveEGLContextOnPause(true);
	}

	protected View createGLSurfaceView(Activity application, ResolutionStrategy resolutionStrategy) {
		BuildConfiguration config = frame.getConfig();

		EGLConfigChooser configChooser = getEglConfigChooser(config);
//		int sdkVersion = android.os.Build.VERSION.SDK_INT;
//		if (sdkVersion <= 10 && config.useGLSurfaceView20API18) {
//			GLSurfaceView20API18 view = new GLSurfaceView20API18(application, resolutionStrategy);
//			if (configChooser != null)
//				view.setEGLConfigChooser(configChooser);
//			else
//				view.setEGLConfigChooser(r, g, b, a, config.depth, config.stencil);
//			view.setRenderer(this);
//			return view;
//		} 
//		else 
		{
			GLSurfaceView20 view = new GLSurfaceView20(application, resolutionStrategy, config.useGL30 ? 3 : 2);
			if (configChooser != null)
				view.setEGLConfigChooser(configChooser);
			else
				view.setEGLConfigChooser(r, g, b, a, config.depth, config.stencil);
			view.setRenderer(this);
			return view;
		}
	}

	protected EGLConfigChooser getEglConfigChooser(BuildConfiguration config) {
		int samples = 0;
		return new GdxEglConfigChooser(r, g, b, a, config.depth, config.stencil, samples);
	}

	@Override
	public GraphicsType getType() {
		return GraphicsType.AndroidGL;
	}

	@Override
	protected int getRefreshRate() {
		return rate;
	}

	@Override
	protected boolean isDirty() {
		if (view != null) {
			if (view instanceof GLSurfaceViewAPI18)
				return ((GLSurfaceViewAPI18) view).isDirty();
			if (view instanceof GLSurfaceView)
				return ((GLSurfaceView) view).isDirty();
		}
		return false;
	}

	@Override
	protected void sync(int fps) {
	}

	@Override
	protected void update() {
	}

	@Override
	protected void updateSize(int width, int height) {
		if (BuildGdx.gl != null)
			BuildGdx.gl.glViewport(0, 0, width, height);
	}

	@Override
	protected boolean wasResized() {
		boolean out = wasResized;
		wasResized = false;
		return out;
	}

	@Override
	protected int getX() {
		return 0;
	}

	@Override
	protected int getY() {
		return 0;
	}

	@Override
	protected boolean isCloseRequested() {
		return false;
	}

	@Override
	protected void dispose() {
	}

	@Override
	protected boolean isActive() {
		if (view != null) {
			if (view instanceof GLSurfaceViewAPI18)
				return ((GLSurfaceViewAPI18) view).isShown();
			if (view instanceof GLSurfaceView)
				return ((GLSurfaceView) view).isShown();
		}
		return false;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setFramesPerSecond(int fps) {
	}

	@Override
	public FrameType getFrameType() {
		return FrameType.GL;
	}

	@Override
	public GLVersion getGLVersion() {
		return glVersion;
	}

	@Override
	public boolean supportsDisplayModeChange() {
		return false;
	}

	@Override
	public boolean setFullscreenMode(DisplayMode displayMode) {
		return false;
	}

	private class AndroidMonitor extends Monitor {
		public AndroidMonitor(int virtualX, int virtualY, String name) {
			super(virtualX, virtualY, name);
		}
	}

	@Override
	public Monitor getPrimaryMonitor() {
		return new AndroidMonitor(0, 0, "Primary Monitor");
	}

	@Override
	public Monitor getMonitor() {
		return getPrimaryMonitor();
	}

	@Override
	public Monitor[] getMonitors() {
		return new Monitor[] { getPrimaryMonitor() };
	}

	@Override
	public DisplayMode[] getDisplayModes(Monitor monitor) {
		return getDisplayModes();
	}

	@Override
	public DisplayMode getDisplayMode(Monitor monitor) {
		return getDisplayMode();
	}

	@Override
	public DisplayMode[] getDisplayModes() {
		return new DisplayMode[] { getDisplayMode() };
	}

	@Override
	public DisplayMode getDisplayMode() {
		DisplayMetrics metrics = new DisplayMetrics();
		app.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return new AndroidDisplayMode(metrics.widthPixels, metrics.heightPixels, 0, 0);
	}

	private class AndroidDisplayMode extends DisplayMode {
		protected AndroidDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
			super(width, height, refreshRate, bitsPerPixel);
		}
	}

	@Override
	public DisplayMode getDesktopDisplayMode() {
		return getDisplayMode();
	}

	@Override
	public boolean setWindowedMode(int width, int height) {
		return false;
	}

	@Override
	public void setTitle(String title) {
	}

	@Override
	public void setUndecorated(boolean undecorated) {
		final int mask = (undecorated) ? 1 : 0;
		app.getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, mask);
	}

	@Override
	public void setResizable(boolean resizable) {
	}

	@Override
	public void setVSync(boolean vsync) {
	}

	@Override
	public BufferFormat getBufferFormat() {
		return bufferFormat;
	}

	@Override
	public boolean supportsExtension(String extension) {
		if (extensions == null)
			extensions = BuildGdx.gl.glGetString(GL10.GL_EXTENSIONS);
		return extensions.contains(extension);
	}

	@Override
	public void setContinuousRendering(boolean isContinuous) {
		if (view != null) {
			// ignore setContinuousRendering(false) while pausing
			this.isContinuous = enforceContinuousRendering || isContinuous;
			int renderMode = this.isContinuous ? GLSurfaceView.RENDERMODE_CONTINUOUSLY
					: GLSurfaceView.RENDERMODE_WHEN_DIRTY;
			if (view instanceof GLSurfaceViewAPI18)
				((GLSurfaceViewAPI18) view).setRenderMode(renderMode);
			if (view instanceof GLSurfaceView)
				((GLSurfaceView) view).setRenderMode(renderMode);
			mean.clear();
		}
	}

	@Override
	public void requestRendering() {
		if (view != null) {
			if (view instanceof GLSurfaceViewAPI18)
				((GLSurfaceViewAPI18) view).requestRender();
			if (view instanceof GLSurfaceView)
				((GLSurfaceView) view).requestRender();
		}
	}

	@Override
	public boolean isFullscreen() {
		return true;
	}

	@Override
	public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) {
		return null;
	}

	@Override
	public void setCursor(Cursor cursor) {
	}

	@Override
	public void setSystemCursor(SystemCursor systemCursor) {
	}

	@Override
	public Object extra(Option opt, Object... obj) {
		return null;
	}

	@Override
	public float getDensity() {
		return density;
	}

	@Override
	public float getPpiX() {
		return ppiX;
	}

	@Override
	public float getPpiY() {
		return ppiY;
	}

	@Override
	public float getPpcX() {
		return ppcX;
	}

	@Override
	public float getPpcY() {
		return ppcY;
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		eglContext = ((EGL10) EGLContext.getEGL()).eglGetCurrentContext();
		setupGL();
		updatePpi();
		updateSafeAreaInsets();

		Display display = app.getWindowManager().getDefaultDisplay();
		Point outSize = new Point();
		display.getSize(outSize);
		this.width = outSize.x;
		this.height = outSize.y;
		this.mean = new WindowedMean(5);

		Gdx.gl = BuildGdx.gl = getGL10();
		Gdx.gl20 = BuildGdx.gl20 = getGL20();
		Gdx.gl30 = BuildGdx.gl30 = getGL30();
	}

	protected void updateSafeAreaInsets() {
		safeInsetLeft = 0;
		safeInsetTop = 0;
		safeInsetRight = 0;
		safeInsetBottom = 0;
	}

	/**
	 * This instantiates the GL10, GL11 and GL20 instances. Includes the check for
	 * certain devices that pretend to support GL11 but fuck up vertex buffer
	 * objects. This includes the pixelflinger which segfaults when buffers are
	 * deleted as well as the Motorola CLIQ and the Samsung Behold II.
	 *
	 * @param gl
	 */
	protected void setupGL() {
		BuildConfiguration config = frame.getConfig();
		gl10 = new AndroidGL10();

		String versionString = "OpenGL ES 3.2 v1.r19p0-01rel0.###other-sha0123456789ABCDEF0###"; //gl10.glGetString(GL10.GL_VERSION); XXX
		String vendorString = gl10.glGetString(GL10.GL_VENDOR);
		String rendererString = gl10.glGetString(GL10.GL_RENDERER);
		glVersion = new GLVersion(Application.ApplicationType.Android, versionString, vendorString, rendererString);

		if (config.useGL30 && glVersion.getMajorVersion() > 2) {
			if (gl30 != null)
				return;
			gl20 = gl30 = new AndroidGL30();
		} else {
			if (gl20 != null)
				return;
			gl20 = new AndroidGL20();
		}
	}

	protected void updatePpi() {
		DisplayMetrics metrics = new DisplayMetrics();
		app.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		ppiX = metrics.xdpi;
		ppiY = metrics.ydpi;
		ppcX = metrics.xdpi / 2.54f;
		ppcY = metrics.ydpi / 2.54f;
		density = metrics.density;
	}

	Object synch = new Object();

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		this.width = width;
		this.height = height;
		updatePpi();
		updateSafeAreaInsets();
		if (created == false) {
			BuildGdx.app.getApplicationListener().create();
			created = true;
			synchronized (this) {
				running = true;
			}
		}

		synchronized (synch) {
			resize = true;
		}
	}
	
	@Override
	public void onDrawFrame(GL10 unused) {
		BuildGdx.input.processMessages();
		ApplicationListener listener = BuildGdx.app.getApplicationListener();
		BuildConfiguration config = frame.getConfig();

		FrameStatus status = null;
		synchronized (synch) {
			status = frame.getStatus();
		}

		switch (status) {
		default:
			return;
		case Closed:
			destroyLoop();
			break;
		case Running:
			break;
		case Pause:
			listener.pause();
			break;
		case Resume:
			listener.resume();
			break;
		case Changed:
			listener.resize(config.width, config.height);
			break;
		}

		boolean shouldRender = false;
		if (BuildGdx.app.executeRunnables())
			shouldRender = true;

		// If one of the runnables set running to false, for example after an exit().
		if (!running) {
			destroyLoop();
			return;
		}

		if (frame.process(shouldRender)) {
			listener.render();
			frame.update();
		}
	}

	private void destroyLoop() {
		ApplicationListener listener = BuildGdx.app.getApplicationListener();

		if (BuildGdx.input != null)
			BuildGdx.input.setCursorCatched(false);
		if (listener != null) {
			listener.pause();
			listener.dispose();
		}
		if (BuildGdx.audio != null)
			BuildGdx.audio.dispose();
		if (BuildGdx.message != null)
			BuildGdx.message.dispose();
		frame.dispose();
		app.finish();
	}
}
