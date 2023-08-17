//This file is part of BuildGDX.
//Copyright (C) 2017-2018  Alexander Makarov-[M210] (m210-2007@mail.ru)
//
//BuildGDX is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//BuildGDX is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.Pattern.ScreenAdapters;

import static ru.m210projects.Build.Net.Mmulti.uninitmultiplayer;
import static ru.m210projects.Build.Strhandler.toLowerCase;
import static ru.m210projects.Build.filehandle.CacheResourceMap.CachePriority.NORMAL;

import java.io.*;
import java.nio.file.Path;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;

import ru.m210projects.Build.Audio.BuildAudio;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Architecture.BuildMessage.MessageType;
import ru.m210projects.Build.Audio.BuildAudio.Driver;
import ru.m210projects.Build.Pattern.BuildGame;
import ru.m210projects.Build.Pattern.Tools.Interpolation;
import ru.m210projects.Build.Pattern.Tools.SaveManager;
import ru.m210projects.Build.Render.Renderer.RenderType;
import ru.m210projects.Build.Settings.BuildConfig;
import ru.m210projects.Build.Settings.BuildSettings;
import ru.m210projects.Build.Settings.GLSettings;
import ru.m210projects.Build.Types.MemLog;
import ru.m210projects.Build.Pattern.BuildFactory;
import ru.m210projects.Build.desktop.backend.lwjgl.LwjglGL10;
import ru.m210projects.Build.filehandle.Entry;
import ru.m210projects.Build.filehandle.Group;
import ru.m210projects.Build.filehandle.fs.Directory;
import ru.m210projects.Build.osd.CommandResponse;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.ConsoleLogger;
import ru.m210projects.Build.osd.OsdColor;
import ru.m210projects.Build.osd.commands.OsdCommand;

public class InitScreen extends ScreenAdapter {

	private int frames;
	private Engine engine;
	private final BuildFactory factory;
	private Thread thread;
	private final BuildGame game;
	private boolean gameInitialized;
	private boolean disposing;

	@Override
	public void show() {
		frames = 0;
		Console.out.setFullscreen(true);
		gameInitialized = false;
		disposing = false;
	}

	@Override
	public void hide() {
		Console.out.setFullscreen(false);
	}

	private void ConsoleInit() {
		Console.out.registerCommand(new OsdCommand("memusage", "mem usage / total") {
			@Override
			public CommandResponse execute(String[] argv) {
				Console.out.println("Memory used: " + MemLog.used() + " / " + MemLog.total() + " mb");
				return CommandResponse.SILENT_RESPONSE;
			}
		});
		
		Console.out.registerCommand(new OsdCommand("net_bufferjitter", "net_bufferjitter") {
			@Override
			public CommandResponse execute(String[] argv) {
				Console.out.println("bufferjitter: " + game.pNet.bufferJitter);
				return CommandResponse.SILENT_RESPONSE;
			}
		});

		Console.out.registerCommand(new OsdCommand("deb_filelist", "deb_filelist") {
			@Override
			public CommandResponse execute(String[] argv) {
				for (Group g : BuildGdx.cache.getGroups()) {
					Console.out.println(String.format("group: \"%s\" priority: %s", g.getName(), BuildGdx.cache.getPriority(g)), OsdColor.BLUE);
					for (Entry res : g.getEntries()) {
						String descr;
						if (res.isDirectory()) {
							descr = "directory";
						} else {
							descr = "file";
						}
						Console.out.println(String.format("\t    %s: \"%s\"", descr, res));
					}
				}
				return CommandResponse.SILENT_RESPONSE;
			}
		});

		Console.out.registerCommand(new OsdCommand("quit", "") {
			@Override
			public CommandResponse execute(String[] argv) {
				game.gExit = true;
				return CommandResponse.SILENT_RESPONSE;
			}
		});
	}

	public InitScreen(final BuildGame game) {
		this.game = game;
		Gdx.gl = new LwjglGL10(); // FIXME:
		BuildGdx.audio = new BuildAudio();

		BuildConfig cfg = game.pCfg;
		factory = game.getFactory();

		Directory gameDirectory;
		Directory userDirectory;
		try {
			gameDirectory = new Directory(cfg.gamePath);
			userDirectory = new Directory(cfg.cfgPath);
		} catch (IOException e) {
			BuildGdx.message.show("Game Resources initialization failed!",
					String.format("%s at\r\n %s", e, e.getStackTrace()[0]),
					MessageType.Info);
			e.printStackTrace();
			System.exit(1);
			return;
		}

		game.initCache(gameDirectory, userDirectory);

		try {
			Path path = userDirectory.getPath().resolve(toLowerCase(game.appname + ".log"));
			Console.out.setLogger(new ConsoleLogger(path));
		} catch (IOException e) {
			e.printStackTrace();
		}

		Console.out.println("BUILD engine by Ken Silverman (http://www.advsys.net/ken) \r\n" + game.appname + " "
				+ game.sversion + "(BuildGdx v" + Engine.version + ") by [M210�] (http://m210.duke4.net)");

		Console.out.println("Current date " + game.date.getLaunchDate());

		String osver = System.getProperty("os.version");
		String jrever = System.getProperty("java.version");

		Console.out.println("Running on " + game.OS + " (version " + osver + ")");
		Console.out.println("\t with JRE version: " + jrever + "\r\n");

		Console.out.println("Initializing resource archives");

		for (String res : factory.resources) {
			Entry entry = gameDirectory.getEntry(res);
			if (entry.exists() && !entry.isDirectory()) {
				BuildGdx.cache.addGroup(entry, NORMAL);
			}
		}

		game.pInt = new Interpolation();
		game.pSavemgr = new SaveManager();

		try {
			Console.out.println("Initializing Build 3D engine");
			this.engine = game.pEngine = factory.engine();
		} catch (Exception e) {
			BuildGdx.message.show("Build Engine Initialization Error!",
					String.format("There was a problem initialising the Build engine: \r\n %s at\r\n %s", e, e.getStackTrace()[0]),
					MessageType.Info);
			e.printStackTrace();
			System.exit(1);
			return;
		}

		if (engine.loadpics() == 0) {
			BuildGdx.message.show("Build Engine Initialization Error!",
					"ART files not found " + gameDirectory.getPath().resolve(engine.getTileManager().getTilesPath()),
					MessageType.Info);
			System.exit(1);
			return;
		}

		game.pFonts = factory.fonts();
		Console.out.setFunc(factory.getOsdFunc());

		BuildSettings.init(engine, cfg);
		GLSettings.init(engine, cfg);

		if(!engine.setrendermode(factory.renderer(cfg.renderType))) {
			engine.setrendermode(factory.renderer(RenderType.Software));
			cfg.renderType = RenderType.Software;
		}

		if (!engine.setgamemode(cfg.fullscreen, cfg.ScreenWidth, cfg.ScreenHeight)) {
			cfg.fullscreen = 0;
		}

		Console.out.revalidate();

		if(cfg.autoloadFolder) {
			Entry entry = gameDirectory.getEntry("autoload");
			if (!entry.exists()) {
				File f = new File(gameDirectory.getPath().resolve("autoload").toString());
				if(!f.exists() && !f.mkdirs() && !f.isDirectory()) {
					Console.out.println("Can't create autoload folder", OsdColor.RED);
				}
			}
		}

		Gdx.input.setInputProcessor(game.getProcessor());

		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					BuildConfig cfg = game.pCfg;
					cfg.InitKeymap();
					if (!cfg.isInited) {
						cfg.isInited = cfg.InitConfig(!cfg.isExist());
					}

					game.pNet = factory.net();
//					game.pInput = factory.input(BuildGdx.controllers.init());
					game.pSlider = factory.slider();
					game.pMenu = factory.menus();
					game.baseDef = factory.getBaseDef(engine);

					uninitmultiplayer();

					cfg.snddrv = BuildGdx.audio.checkNum(Driver.Sound, cfg.snddrv);
					cfg.middrv = BuildGdx.audio.checkNum(Driver.Music, cfg.middrv);

					BuildGdx.audio.setDriver(Driver.Sound, cfg.snddrv);
					BuildGdx.audio.setDriver(Driver.Music, cfg.middrv);

					BuildSettings.usenewaspect.set(cfg.widescreen == 1);
					BuildSettings.fov.set(cfg.gFov);
					BuildSettings.fpsLimit.set(cfg.fpslimit);

//					BuildGdx.threads = new ThreadProcessor();

					gameInitialized = game.init();

					ConsoleInit();
				} catch (OutOfMemoryError me) {
					System.gc();

					me.printStackTrace();
					String message = "Memory used: [ " + MemLog.used() + " / " + MemLog.total()
							+ " mb ] \r\nPlease, increase the java's heap size.";
					Console.out.println(message, OsdColor.RED);
					BuildGdx.message.show("OutOfMemory!", message, MessageType.Info);
					System.exit(1);
				} catch (FileNotFoundException fe) {
					fe.printStackTrace();

					String message = fe.getMessage();
					Console.out.println(message, OsdColor.RED);
					BuildGdx.message.show("File not found!", message, MessageType.Info);
					System.exit(1);
				} catch (Throwable e) {
					if (!disposing) {
						game.ThrowError("InitScreen error", e);
						System.exit(1);
					}
				}
			}
		});
		thread.setName("InitEngine thread");
		thread.setDaemon(true); // to make the thread as background process and kill it if the app was closed
	}

	public void start() {
		if (thread != null) {
			thread.start();
		}
	}

	@Override
	public void dispose() {
		synchronized (Engine.lock) {
			disposing = true;
		}
	}

	@Override
	public void render(float delta) {
		synchronized (Engine.lock) {
			if (!disposing && engine.getrender().isInited()) { // don't draw anything after disposed
				engine.clearview(0);

//				engine.rotatesprite(0, 0, 65536, 0, factory.getInitTile(), -128, 0, 10 | 16);

				factory.drawInitScreen();

				if (frames++ > 3) {
					if (!thread.isAlive()) {
						if (gameInitialized) {
							game.show();
						} else {
							game.GameMessage("InitScreen unknown error!");
							Gdx.app.exit();
						}
					}
				}
				engine.nextpage();
			}
		}
	}
}
