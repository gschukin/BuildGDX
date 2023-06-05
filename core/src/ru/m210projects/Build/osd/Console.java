package ru.m210projects.Build.osd;

import com.badlogic.gdx.Input;
import org.jetbrains.annotations.NotNull;
import ru.m210projects.Build.Pattern.BuildFactory;
import ru.m210projects.Build.Types.collections.MapList;
import ru.m210projects.Build.Types.collections.MapNode;
import ru.m210projects.Build.osd.commands.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.m210projects.Build.Engine.MAXPALOOKUPS;
import static ru.m210projects.Build.Engine.getInput;
import static ru.m210projects.Build.Input.KeyInput.gdxscantoasc;
import static ru.m210projects.Build.Input.KeyInput.gdxscantoascwithshift;
import static ru.m210projects.Build.Input.Keymap.KEY_CAPSLOCK;

public class Console {

    private static final int MAX_LINES = 512;
    private static final OsdCommand UNKNOWN_COMMAND = new UnknownCommand();
    public static Console out;
    final Map<String, OsdCommand> osdVars;
    final OsdFunc func;
    // TODO сделать OSDString сегментами (сегмент цвета и кусок текста)
    private final MapList<OsdString> osdTextList = new MapList<>();
    private final OsdCommandPrompt prompt;
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

    private Console(@NotNull OsdFunc func) {
        this.osdVars = new HashMap<>();
        this.prompt = new OsdCommandPrompt(this);
        this.func = func;

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
        registerCommand(new OsdValueRange("osdtextshade", "osdtextshade: sets the shade of the OSD text", 0, 7) {
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

    public static Console init(BuildFactory factory) {
        Console.out = new Console(factory.getOsdFunc());
        return out;
    }

    // FIXME get rid of...parameter keycode. make it as listener?
    public void handleevents() {
        for (int i = 0; i < 4; i++) {
//                      Console.out.setCaptureKey(cfg.primarykeys[consolekey], 0);
//						Console.out.setCaptureKey(cfg.secondkeys[consolekey], 1);
//						Console.out.setCaptureKey(cfg.mousekeys[consolekey], 2);
//						Console.out.setCaptureKey(cfg.gpadkeys[consolekey], 3);
            if (getInput().keyStatusOnce(68)) {
                toggle();
                return;
            }
        }

        if (!isCaptured()) {
            return;
        }

        if (getInput().keyStatusOnce(Input.Keys.ESCAPE)) {
            onClose();
        }

        if (getInput().keyStatusOnce(Input.Keys.PAGE_UP)) {
            onPageUp();
        }

        if (getInput().keyStatusOnce(Input.Keys.PAGE_DOWN)) {
            onPageDown();
        }

        if (getInput().keyStatusOnce(Input.Keys.HOME)) {
            if (prompt.isCtrlPressed()) {
                onFirstPage();
            } else {
                prompt.onFirstPosition();
            }
        }

        if (getInput().keyStatusOnce(Input.Keys.END)) {
            if (prompt.isCtrlPressed()) {
                onLastPage();
            } else {
                prompt.onLastPosition();
            }
        }

        if (getInput().keyStatusOnce(Input.Keys.INSERT)) {
            prompt.toggleOverType();
        }

        if (getInput().keyStatusOnce(Input.Keys.LEFT)) {
            prompt.onLeft();
        }

        if (getInput().keyStatusOnce(Input.Keys.RIGHT)) {
            prompt.onRight();
        }

        if (getInput().keyStatusOnce(Input.Keys.UP)) {
            prompt.historyPrev();
        }

        if (getInput().keyStatusOnce(Input.Keys.DOWN)) {
            prompt.historyNext();
        }

        prompt.setShiftPressed(getInput().keyStatus(Input.Keys.SHIFT_LEFT) || getInput().keyStatus(Input.Keys.SHIFT_RIGHT));
        prompt.setCtrlPressed(getInput().keyStatus(Input.Keys.CONTROL_LEFT) || getInput().keyStatus(Input.Keys.CONTROL_RIGHT));
        if(getInput().keyStatusOnce(KEY_CAPSLOCK)) {
            prompt.setCapsLockPressed(!prompt.isCapsLockPressed());
        }

        if (getInput().keyStatusOnce(Input.Keys.DEL)) { //backspace
            prompt.onDelete();
        }

        if (getInput().keyStatusOnce(Input.Keys.ENTER)) {
            prompt.onEnter();
        }

        if (getInput().keyStatusOnce(Input.Keys.TAB)) {
            prompt.onTab();
        }

        getInput().putMessage(ch -> {
            if (ch < 128) {
                if (prompt.isShiftPressed()) {
                    ch = gdxscantoascwithshift[ch];
                } else {
                    ch = gdxscantoasc[ch];
                }

                if (ch != 0) {
                    prompt.append((char) ch);
                }
            }
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

                    println(String.format("\"%s\" ^%dis not a valid command or cvar", text, OsdColor.RED.getPal()), OsdColor.RED);
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

    void onClose() {
        osdScroll = -1;
        osdRowsCur--;
        prompt.captureInput(false);
        osdScrTime = func.getTicks();
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
                long j = (func.getTicks() - osdScrTime);
                while (j > -1) {
                    osdRowsCur++;
                    j -= 200 / osdRows;
                    if (osdRowsCur > osdRows - 1) {
                        break;
                    }
                }
            }
            if ((osdRowsCur > -1 && osdScroll == -1) || osdRowsCur > osdRows) {
                long j = (func.getTicks() - osdScrTime);
                while (j > -1) {
                    osdRowsCur--;
                    j -= 200 / osdRows;
                    if (osdRowsCur < 1) {
                        break;
                    }
                }
            }
            osdScrTime = func.getTicks();
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
        prompt.draw(func);
    }

    public void toggle() {
        osdScroll = -osdScroll;
        if (osdRowsCur == -1) {
            osdScroll = 1;
        } else if (osdRowsCur == osdRows) {
            osdScroll = -1;
        }
        osdRowsCur += osdScroll;
        prompt.captureInput(osdScroll == 1);
        getInput().initMessageInput(null);
        osdScrTime = func.getTicks();
    }

    public boolean isCaptured() {
        return prompt.isCaptured();
    }

    public void print(String text, OsdColor color) {
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
                String number = "";
                if (chp + 1 >= text.length()) {
                    continue;
                }

                char num1 = text.charAt(chp + 1);
                if (Character.isDigit(num1)) {
                    number += num1;
                    char num2 = ' ';
                    if (++chp + 1 < text.length()) {
                        num2 = text.charAt(chp + 1);
                    }

                    if (!Character.isDigit(num2)) {
                        pal = Integer.parseInt(number, 10);
                        continue;
                    }

                    chp++;
                    number += num2;
                    pal = Integer.parseInt(number, 10);
                    continue;
                }

                if (num1 == 'S' || num1 == 's') {
                    chp++;
                    char num = text.charAt(++chp);
                    if (Character.isDigit(num)) {
                        number += num;
                        s = Integer.parseInt(number, 10);
                        continue;
                    }
                }

                if (num1 == 'O' || num1 == 'o') {
                    pal = osdTextPal.getPal();
                    s = osdTextShade;
                    chp++;
                    continue;
                }
            }

            osdString.insert(osdCharacterPos++, text.charAt(chp), pal, s);
        } while (++chp < text.length());
    }

    public void print(String text) {
        print(text, osdTextPal);
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
            last = new MapNode<OsdString>(osdTextList.getSize()) {
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
            msg.append("\n");
            print(msg.toString());
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
}
