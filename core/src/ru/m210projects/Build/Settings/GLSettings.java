package ru.m210projects.Build.Settings;

import com.badlogic.gdx.graphics.Texture.TextureFilter;
import ru.m210projects.Build.Architecture.BuildGdx;
import ru.m210projects.Build.Architecture.BuildGraphics;
import ru.m210projects.Build.Engine;
import ru.m210projects.Build.Render.GLInfo;
import ru.m210projects.Build.Render.GLRenderer;
import ru.m210projects.Build.Render.Types.GLFilter;
import ru.m210projects.Build.Types.BuildVariable;
import ru.m210projects.Build.osd.Console;
import ru.m210projects.Build.osd.commands.OsdValueRange;

import static ru.m210projects.Build.Engine.pow2long;

public class GLSettings extends BuildSettings {

    public static GLFilter[] glfiltermodes = {
            new GLFilter("None", TextureFilter.Nearest, TextureFilter.Nearest), // 0
            new GLFilter("Bilinear", TextureFilter.Linear, TextureFilter.Linear), // 1
            new GLFilter("Trilinear", TextureFilter.MipMapLinearLinear, TextureFilter.Linear) // 2
    };

    public static BuildVariable<GLFilter> textureFilter;
    public static BuildVariable<Integer> textureAnisotropy;
    public static BuildVariable<Boolean> useHighTile;
    public static BuildVariable<Boolean> useModels;
    public static BuildVariable<Boolean> usePaletteShader;

    public static BuildVariable<Integer> gamma;

    public static BuildVariable<Boolean> animSmoothing;
    public static int detailMapping = 1;
    public static int glowMapping = 1;

    public static void init(final Engine engine, final BuildConfig cfg) {
        textureFilter = new BuildVariable<GLFilter>(
                cfg.glfilter < glfiltermodes.length ? glfiltermodes[cfg.glfilter] : glfiltermodes[0],
                "Changes the texture filtering settings") {
            @Override
            public void execute(GLFilter value) {
                BuildGdx.app.postRunnable(new Runnable() { // it must be started at GLthread
                    @Override
                    public void run() {
                        GLRenderer gl = engine.glrender();
                        if (gl != null) {
                            gl.gltexapplyprops();
                        }
                    }
                });

                for (int i = 0; i < glfiltermodes.length; i++) {
                    if (value.equals(glfiltermodes[i])) {
                        cfg.glfilter = i;
                        break;
                    }
                }
            }

            @Override
            public GLFilter check(Object value) {
                if (value instanceof GLFilter) {
                    return (GLFilter) value;
                }
                return null;
            }
        };

        textureAnisotropy = new BuildVariable<Integer>(1, "Changes the texture anisotropy settings") {
            @Override
            public void execute(final Integer value) {
                BuildGdx.app.postRunnable(new Runnable() { // it must be started at GLthread
                    @Override
                    public void run() {
                        GLRenderer gl = engine.glrender();
                        if (gl != null) {
                            gl.gltexapplyprops();
                        }
                        cfg.glanisotropy = value;
                    }
                });
            }

            @Override
            public Integer check(Object value) {
                if (value instanceof Integer) {
                    int anisotropy = (Integer) value;
                    if (GLInfo.maxanisotropy > 1.0) {
                        if (anisotropy <= 0 || anisotropy > GLInfo.maxanisotropy) {
                            anisotropy = (int) GLInfo.maxanisotropy;
                        }
                    }
                    return pow2long[checkAnisotropy(anisotropy)];
                }
                return null;
            }

            int checkAnisotropy(int anisotropy) {
                int anisotropysize = 0;
                for (int s = anisotropy; s > 1; s >>= 1) {
                    anisotropysize++;
                }
                return anisotropysize;
            }
        };
        textureAnisotropy.set(cfg.glanisotropy);

        Console.out.registerCommand(new OsdValueRange("r_texturemode",
                "r_texturemode: " + GLSettings.textureFilter.getDescription(), 0, 2) {

            @Override
            public String getDescription() {
                return "Current texturing mode is " + GLSettings.textureFilter.get().name;
            }

            @Override
            protected void setCheckedValue(float value) {
                this.value = value;
                GLSettings.textureFilter.set(glfiltermodes[(int) value]);
            }

        });

        Console.out.registerCommand(new OsdValueRange("r_detailmapping", "r_detailmapping: use detail textures", 1, 0, 1) {
            @Override
            public float getValue() {
                return detailMapping;
            }

            @Override
            protected void setCheckedValue(float value) {
                detailMapping = (int) value;
            }
        });

        Console.out.registerCommand(new OsdValueRange("r_glowmapping", "r_detailmapping: use detail textures", 1, 0, 1) {
            @Override
            public float getValue() {
                return glowMapping;
            }

            @Override
            protected void setCheckedValue(float value) {
                glowMapping = (int) value;
            }
        });

		useHighTile = new BooleanVar(true, "Use true color textures from high resolution pack") {
			@Override
			public void execute(Boolean value) {
				BuildGdx.app.postRunnable(new Runnable() { // it must be started at GLthread
					@Override
					public void run() {
						GLRenderer gl = engine.glrender();
						if (gl != null) {
							gl.gltexinvalidateall(GLRenderer.GLInvalidateFlag.All, GLRenderer.GLInvalidateFlag.Uninit);
						}
					}
				});
			}
		};
		useModels = new BooleanVar(true, "Use md2 / md3 models from high resolution pack");

		usePaletteShader = new BooleanVar(true, "Use palette emulation") {
			@Override
			public void execute(Boolean value) {
				GLRenderer gl = engine.glrender();
				if (gl != null) {
					gl.enableIndexedShader(value);
				}
				cfg.paletteEmulation = value;
			}
		};
		usePaletteShader.set(cfg.paletteEmulation);

//		OSDCOMMAND r_paletteshader = new OSDCOMMAND("r_paletteshader",
//				"r_paletteshader: " + GLSettings.usePaletteShader.get(), new OSDCVARFUNC() {
//					@Override
//					public CommandResponse execute(String[] argv) {
//						if (Console.osd_argc != 2) {
//							Console.out.println("r_paletteshader: " + GLSettings.usePaletteShader.get());
//							return;
//						}
//						try {
//							final int value = Integer.parseInt(osd_argv[1]);
//							BuildGdx.app.postRunnable(new Runnable() { // it must be started at GLthread
//								@Override
//								public void run() {
//									usePaletteShader.set(value == 1);
//									Console.out.println("r_paletteshader changed to " + GLSettings.usePaletteShader.get());
//								}
//							});
//						} catch (Exception e) {
//							Console.out.println("r_paletteshader: out of range");
//						}
//					}
//				});
//		r_paletteshader.setRange(0, 1);
//		Console.RegisterCvar(r_paletteshader);

		animSmoothing = new BooleanVar(true, "Use  model animation smoothing");

		gamma = new BuildVariable<Integer>((int) ((1 - cfg.fgamma) * 4096), "Global gamma") {
			@Override
			protected void execute(Integer value) {
				cfg.fgamma = (1 - (value / 4096.0f));
			}

			@Override
			protected Integer check(Object value) {
				if (value instanceof Integer) {
					try {
						float gamma = (Integer) value / 4096.0f;
						if (engine.glrender() == null || (Boolean) BuildGdx.graphics.extra(BuildGraphics.Option.GLSetConfiguration,
								//1 - gamma, cfg.fbrightness, cfg.fcontrast))
								1 - gamma, 0.0f, 1.0f)) {
							return (Integer) value;
						}
					} catch(Throwable ignored) {
					}
				}
				return null;
			}
		};
    }

}
