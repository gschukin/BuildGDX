package ru.m210projects.Build.osd;

import com.badlogic.gdx.Input;
import ru.m210projects.Build.StringUtils;
import ru.m210projects.Build.Types.collections.MapList;
import ru.m210projects.Build.Types.collections.MapNode;
import ru.m210projects.Build.osd.commands.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.m210projects.Build.Engine.MAXPALOOKUPS;
import static ru.m210projects.Build.Engine.getInputController;
import static ru.m210projects.Build.Input.KeyInput.gdxscantoasc;
import static ru.m210projects.Build.Input.KeyInput.gdxscantoascwithshift;
import static ru.m210projects.Build.Input.Keymap.KEY_CAPSLOCK;

public class Console {

    private static final int MAX_LINES = 512;
    private static final OsdCommand UNKNOWN_COMMAND = new UnknownCommand();
    public static Console out = new Console();

    final Map<String, OsdCommand> osdVars;
    OsdFunc func;
    // TODO сделать OSDString сегментами (сегмент цвета и кусок текста)
    private final MapList<OsdString> osdTextList = new MapList<>();
    protected final OsdCommandPrompt prompt;

    protected final CommandFinder finder;
    /**
     * width of onscreen display in text columns
     */
    int osdCols = 65;
    int osdTextScale = 65536;
    /**
     * visible row count when moving (from 0 to osdRow)
     */
    int osdRowsCur = -1;
    private boolean osdDraw = false;
    /**
     * position next character will be written at
     */
    private int osdCharacterPos = 0;
    /**
     * topmost visible line number
     */
    private MapNode<OsdString> osdHead;
    /**
     * # lines of the buffer that are visible
     */
    private int osdRows = 20;
    /**
     * maximum number of lines which can fit on the screen
     */
    private int osdMaxRows = 20;
    private int osdScroll = 0;
    private long osdScrTime = 0;
    private OsdColor osdTextPal = OsdColor.DEFAULT;
    private int osdTextShade = 0;
    private ConsoleLogger logger;

    public Console() {
        this.osdVars = new HashMap<>();
        this.prompt = new OsdCommandPrompt(new OsdConsolePromptUI(this));
        prompt.registerDefaultCommands(this);
        registerDefaultCommands();
        finder = new CommandFinder(osdVars);

        prompt.setActionListener(input -> {
            if (!input.isEmpty()) {
                dispatch(input);
            }
            setFirstLine();
            finder.reset();
        });
    }

    protected void registerDefaultCommands() {
        registerCommand(new OsdCallback("osd_color_test", "", args -> {
            int len = MAXPALOOKUPS;

            if (args.length != 0 && StringUtils.isNumeric(args[0])) {
                len = Integer.parseInt(args[0]);
            }

            for (int i = 0; i < len; i++) {
                Console.out.println(String.format("pal%d: ^%dThe quick brown fox jumps over the lazy dog", i, i));
            }
            return CommandResponse.SILENT_RESPONSE;
        }));

        registerCommand(new OsdCommand("help", "lists all registered functions, cvars and aliases") {
            @Override
            public CommandResponse execute(String[] argv) {
                List<String> commands = osdVars.keySet().stream().filter(e -> !e.equals(getName())).sorted().collect(Collectors.toList());
                if (commands.size() > 0) {
                    printCommandList(commands, "Symbol listing:", String.format("Found %d symbols", commands.size()));
                    return CommandResponse.SILENT_RESPONSE;
                }
                return CommandResponse.DESCRIPTION_RESPONSE;
            }
        });
        registerCommand(new OsdValueRange("osdtextpal", "osdtextpal: sets the palette of the OSD text", 0, MAXPALOOKUPS - 1) {
            @Override
            public float getValue() {
                return osdTextPal.getPal();
            }

            @Override
            protected void setCheckedValue(float value) {
                osdTextPal = OsdColor.findColor((int) value);
            }
        });
        registerCommand(new OsdValueRange("osdtextshade", "osdtextshade: sets the shade of the OSD text", 0, 255) {
            @Override
            public float getValue() {
                return osdTextShade;
            }

            @Override
            protected void setCheckedValue(float value) {
                osdTextShade = (int) value;
            }
        });
        registerCommand(new OsdValue("osdtextscale", "osdtextscale: sets the OSD text scale", value -> value >= 0.5f) {
            @Override
            public float getValue() {
                return osdTextScale / 65536.0f;
            }

            @Override
            protected void setCheckedValue(float value) {
                setOsdTextScale(value);
            }
        });
        registerCommand(new OsdValue("osdrows", "osdrows: sets the number of visible lines of the OSD", value -> (value >= 0) && (value <= osdMaxRows)) {
            @Override
            public float getValue() {
                return osdRows;
            }

            @Override
            protected void setCheckedValue(float value) {
                osdRows = (int) value;
                stopMoving();
            }
        });
    }

    public void setFunc(OsdFunc func) {
        this.func = func;
    }

    // FIXME get rid of...parameter keycode. make it as listener?
    public void handleevents() {
        for (int i = 0; i < 4; i++) {
//                      Console.out.setCaptureKey(cfg.primarykeys[consolekey], 0);
//						Console.out.setCaptureKey(cfg.secondkeys[consolekey], 1);
//						Console.out.setCaptureKey(cfg.mousekeys[consolekey], 2);
//						Console.out.setCaptureKey(cfg.gpadkeys[consolekey], 3);
            if (getInputController().keyStatusOnce(Input.Keys.GRAVE)) {
                onToggle();
                return;
            }
        }

        prompt.setShiftPressed(getInputController().keyStatus(Input.Keys.SHIFT_LEFT) || getInputController().keyStatus(Input.Keys.SHIFT_RIGHT));
        prompt.setCtrlPressed(getInputController().keyStatus(Input.Keys.CONTROL_LEFT) || getInputController().keyStatus(Input.Keys.CONTROL_RIGHT));
        getInputController().putMessage(ch -> {
            onKeyPressed(ch);
            return 0;
        }, false);
    }

    public void registerCommand(OsdCommand cmd) {
        cmd.setParent(this);

        String name = cmd.getName();
        if (name.isEmpty()) {
            throw new RegisterException("Can't register null command");
        }

        if (Character.isDigit(name.charAt(0))) {
            throw new RegisterException(String.format("first character of command name \"%s\" must not be a numeral", name));
        }

        for (char ch : name.toCharArray()) {
            if (!Character.isLetterOrDigit(ch) && ch != '_') {
                throw new RegisterException(String.format("Illegal character in command name \"%s\"", name));
            }
        }

        if (osdVars.containsKey(name)) {
            throw new RegisterException(String.format("Command name \"%s\" is already registered", name));
        }

        osdVars.put(name, cmd);
    }

    public float getValue(String cmd) {
        OsdCommand command = osdVars.getOrDefault(cmd, UNKNOWN_COMMAND);
        if (command instanceof OsdValue) {
            return ((OsdValue) command).getValue();
        }
        return Float.NaN;
    }

    public boolean setValue(String cmd, float value) {
        OsdCommand command = osdVars.getOrDefault(cmd, UNKNOWN_COMMAND);
        if (command instanceof OsdValue) {
            return ((OsdValue) command).setValue(value);
        }
        return false;
    }

    public OsdCommandPrompt getPrompt() {
        return prompt;
    }

    void dispatch(String text) {
        try {
            if (text.isEmpty()) {
                return;
            }

            String[] argv = text.split(" ");
            OsdCommand command = UNKNOWN_COMMAND;
            if (argv.length > 0) {
                command = osdVars.getOrDefault(argv[0], UNKNOWN_COMMAND);
                argv = Arrays.copyOfRange(argv, 1, argv.length);
            }

            switch (command.execute(argv)) {
                case OUT_OF_RANGE:
                    println(String.format("\"%s\" value out of range", command.getName()));
                    break;
                case BAD_ARGUMENT_RESPONSE:
                    println(String.format("\"%s\" wrong value \"%s\"", command.getName(), text.substring(command.getName().length()).trim()));
                    break;
                case SILENT_RESPONSE:
                    return;
                case OK_RESPONSE:
                    println(text);
                    break;
                case DESCRIPTION_RESPONSE:
                    println(command.getDescription());
                    break;
                case UNKNOWN_RESPONSE:
                    if (func.textHandler(text)) {
                        return;
                    }

                    println(String.format("\"%s^O\" is not a valid command or cvar", text), OsdColor.RED);
                    break;
            }
        } catch (Exception e) {
            println(e.toString(), OsdColor.RED);

            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append("\t").append(element.toString());
                sb.append("\r\n");
            }
            logger.write(sb.toString());
        }
    }

    public boolean isMoving() {
        return (osdRowsCur != -1 && osdRowsCur != osdRows);
    }

    public boolean isShowing() {
        return osdDraw;
    }

    public void revalidate() {
        osdCols = func.getcolumnwidth(osdTextScale);
        osdMaxRows = func.getrowheight(osdTextScale) - 2;
        if (osdRows > osdMaxRows) {
            osdRows = osdMaxRows;
        }
        prompt.onResize();
        osdCharacterPos = 0;
        setFirstLine();
    }

    public void draw() {
        if (osdRowsCur == 0) {
            osdDraw = !osdDraw;
        }

        if (osdRowsCur == osdRows || osdRowsCur == osdMaxRows) {
            osdScroll = 0;
        } else {
            if ((osdRowsCur < osdRows && osdScroll == 1) || osdRowsCur < -1) {
                long j = (getTicks() - osdScrTime);
                while (j > -1) {
                    osdRowsCur++;
                    j -= 200 / osdRows;
                    if (osdRowsCur > osdRows - 1) {
                        break;
                    }
                }
            }

            if ((osdRowsCur > -1 && osdScroll == -1) || osdRowsCur > osdRows) {
                long j = (getTicks() - osdScrTime);
                while (j > -1) {
                    osdRowsCur--;
                    j -= 200 / osdRows;
                    if (osdRowsCur < 1) {
                        break;
                    }
                }
            }
            osdScrTime = getTicks();
        }

        if (!osdDraw || osdRowsCur <= 0) {
            return;
        }

        func.clearbg(osdCols, (int) ((osdRowsCur + 1) * osdTextScale / 65536.0f));
        MapNode<OsdString> node = osdHead;
        int row = osdRowsCur - 2;
        for (; node != null && row > 0; node = node.getNext()) {
            row -= func.drawosdstr(0, row, node.get(), osdCols, osdTextShade, osdTextPal, osdTextScale);
        }
        prompt.draw();
    }

    public boolean isCaptured() {
        return prompt.isCaptured();
    }

    public void print(String text, OsdColor color) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (color == osdTextPal || color == OsdColor.DEFAULT) {
            System.out.println(text);
        } else {
            System.out.printf("%s%s%s\n", color, text, OsdColor.RESET);
        }

        if (logger != null) {
            logger.write(text);
        }

        OsdString osdString = newLine().get();

        int chp = 0;
        int s = osdTextShade;
        int pal = color.getPal();

        do {
            if (text.charAt(chp) == '\n') {
                osdCharacterPos = 0;
                osdString = newLine().get();
                continue;
            }

            if (text.charAt(chp) == '\r') {
                osdCharacterPos = 0;
                continue;
            }

            if (text.charAt(chp) == '\t') {
                for (int i = 0; i < 2; i++) {
                    osdString.insert(osdCharacterPos++, ' ', pal, s);
                }
                continue;
            }

            if (text.charAt(chp) == '^') {
                StringBuilder number = new StringBuilder();
                int pos = chp + 1;
                if (pos >= text.length()) {
                    continue;
                }

                char num1 = text.charAt(pos++);
                if (Character.isDigit(num1)) {
                    number.append(num1);
                    while(pos < text.length() && Character.isDigit(text.charAt(pos)) && number.length() < 3) {
                        number.append(text.charAt(pos));
                        pos++;
                    }

                    if (number.length() > 0) {
                        pal = Integer.parseInt(number.toString(), 10);
                        chp = pos - 1;
                        continue;
                    }
                } else if (num1 == 'S' || num1 == 's') {
                    chp++;
                    char num = text.charAt(++chp);
                    if (Character.isDigit(num)) {
                        number.append(num);
                        s = Integer.parseInt(number.toString(), 10);
                        continue;
                    }
                } else if (num1 == 'O' || num1 == 'o') {
                    pal = color.getPal();
                    s = osdTextShade;
                    chp++;
                    continue;
                }
            }

            osdString.insert(osdCharacterPos++, text.charAt(chp), pal, s);
        } while (++chp < text.length());
    }

    public void println(String text) {
        print(text, osdTextPal);
        osdCharacterPos = 0;
    }

    public void println(String text, OsdColor color) {
        print(text, color);
        osdCharacterPos = 0;
    }

    private MapNode<OsdString> newLine() {
        MapNode<OsdString> last;
        if (osdTextList.getSize() < MAX_LINES) {
            last = new MapNode<>(osdTextList.getSize()) {
                final OsdString value = new OsdString();
                @Override
                public OsdString get() {
                    return value;
                }
            };
        } else {
            last = osdTextList.removeLast();
            last.get().clear();
        }
        osdTextList.addFirst(last);
        onPageDown();
        return last;
    }

    void printCommandList(List<String> list, String header, String footer) {
        if (list.size() > 0) {
            int maxLength = 0;
            for (String s : list) {
                maxLength = Math.max(maxLength, s.length());
            }
            maxLength += 3;

            println(header, OsdColor.RED);
            StringBuilder msg = new StringBuilder();
            int currentLength = 0;
            for (String s : list) {
                msg.append("  ");
                msg.append(s);
                currentLength += maxLength + 2;
                //FIXME msg.append(" ".repeat(Math.max(0, maxLength - s.length())));

                if (currentLength > osdCols) {
                    msg.append("\n");
                    currentLength = 0;
                }
            }
            println(msg.toString());
            println(footer, OsdColor.RED);
        }
    }

    public void setOsdTextScale(float value) {
        value = Math.max(0.5f, value);
        boolean isFullscreen = osdRowsCur == osdMaxRows;
        osdTextScale = (int) (value * 65536.0f);
        revalidate();
        if (isFullscreen) {
            setFullscreen(true);
        } else {
            stopMoving();
        }
    }

    public void setFullscreen(boolean fullscreen) {
        if (fullscreen) {
            osdRowsCur = osdMaxRows;
        } else {
            osdRowsCur = -1;
        }

        osdDraw = fullscreen;
        func.showOsd(fullscreen);
    }

    private void stopMoving() {
        if (osdRowsCur != -1) {
            osdRowsCur = osdRows;
        }
    }

    public boolean isOnLastLine() {
        return osdHead == osdTextList.getLast();
    }

    public boolean isOnFirstLine() {
        return osdHead == osdTextList.getFirst();
    }

    public void setFirstLine() {
        osdHead = osdTextList.getFirst();
    }

    public void setLogger(ConsoleLogger logger) {
        this.logger = logger;
    }

    public ConsoleLogger getLogger() {
        return logger;
    }

    private long getTicks() {
        return System.currentTimeMillis();
    }

    // Controller

    public void onToggle() {
        osdScroll = -osdScroll;
        if (osdRowsCur == -1) {
            osdScroll = 1;
        } else if (osdRowsCur == osdRows) {
            osdScroll = -1;
        }
        osdRowsCur += osdScroll;
        prompt.captureInput(osdScroll == 1);
        func.showOsd(osdScroll == 1);
        getInputController().initMessageInput(null);
        osdScrTime = getTicks();
    }

    void onClose() {
        osdScroll = -1;
        osdRowsCur--;
        prompt.captureInput(false);
        osdScrTime = getTicks();
    }

    void onPageUp() {
        if (!isOnLastLine()) {
            if (osdHead != null && osdHead.getNext() != null) {
                osdHead = osdHead.getNext();
            }
        }
    }

    void onPageDown() {
        if (!isOnFirstLine()) {
            if (osdHead != null && osdHead.getPrev() != null) {
                osdHead = osdHead.getPrev();
            }
        }
    }

    void onFirstPage() {
        osdHead = osdTextList.getLast();
    }

    void onLastPage() {
        osdHead = osdTextList.getFirst();
    }

    void onTabPressed() {
        String input = prompt.getTextInput();
        if (!finder.isListPresent()) {
            List<String> commands = finder.getCommands(input);
            if (commands.size() > 1) {
                printCommandList(commands, String.format("Found %d possible completions for \"%s\"", commands.size(), input), "Press TAB again to cycle through matches");
            } else if (commands.size() == 1) {
                prompt.setTextInput(commands.get(0));
            }
        } else {
            prompt.setTextInput(finder.getNextTabCommand());
        }
    }

    void onKeyPressed(int keyId) {
        if (!isCaptured()) {
            return;
        }

        switch (keyId) {
            case Input.Keys.GRAVE: // FIXME: from config
                onToggle();
                return;
            case Input.Keys.ESCAPE:
                onClose();
                return;
            case Input.Keys.TAB:
                onTabPressed();
                return;
            case Input.Keys.ENTER:
                onEnterPressed();
                return;
            case Input.Keys.DEL: //backspace
                onDeletePressed();
                return;
            case KEY_CAPSLOCK:
                onCapsLockPressed();
                return;
            case Input.Keys.DOWN:
                onNextHistory();
                return;
            case Input.Keys.UP:
                onPrevHistory();
                return;
            case Input.Keys.RIGHT:
                onRightPressed();
                return;
            case Input.Keys.LEFT:
                onLeftPressed();
                return;
            case Input.Keys.INSERT:
                onToggleOverType();
                return;
            case Input.Keys.END:
                if (prompt.isCtrlPressed()) {
                    onLastPage();
                } else {
                    onLastPosition();
                }
                return;
            case Input.Keys.HOME:
                if (prompt.isCtrlPressed()) {
                    onFirstPage();
                } else {
                    onFirstPosition();
                }
                return;
            case Input.Keys.PAGE_UP:
                onPageUp();
                return;
            case Input.Keys.PAGE_DOWN:
                onPageDown();
                return;
            default:
                if (keyId < 128) {
                    if (prompt.isShiftPressed()) {
                        keyId = gdxscantoascwithshift[keyId];
                    } else {
                        keyId = gdxscantoasc[keyId];
                    }

                    if (keyId != 0) {
                        prompt.append((char) keyId);
                        finder.reset();
                    }
                }
                break;
        }
    }

    void onFirstPosition() {
        prompt.onFirstPosition();
    }

    void onLastPosition() {
        prompt.onLastPosition();
    }

    void onToggleOverType() {
        prompt.toggleOverType();
    }

    void onLeftPressed() {
        prompt.onLeft();
    }

    void onRightPressed() {
        prompt.onRight();
    }

    void onPrevHistory() {
        prompt.historyPrev();
    }

    void onNextHistory() {
        prompt.historyNext();
    }

    void onCapsLockPressed() {
        prompt.setCapsLockPressed(!prompt.isCapsLockPressed());
    }

    void onDeletePressed() {
        prompt.onDelete();
    }

    void onEnterPressed() {
        prompt.onEnter();
    }
}
