package ru.m210projects.Build.osd;

import ru.m210projects.Build.osd.commands.OsdValueRange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static ru.m210projects.Build.Engine.MAXPALOOKUPS;

public class OsdCommandPrompt {

    private final Console parent;
    private String osdVersionText;
    private int osdVersionShade;
    private OsdColor osdVersionPal = OsdColor.DEFAULT;
    private OsdColor osdPromptPal = OsdColor.DEFAULT;
    private int osdPromptShade;
    private OsdColor osdEditPal = OsdColor.DEFAULT;
    private int osdEditShade;
    private final ConsoleHistory inputHistory;
    private final CommandFinder finder;
    private static final int OSD_EDIT_LENGTH = 511;
    private final StringBuilder osdEditBuf = new StringBuilder(OSD_EDIT_LENGTH);
    private int osdEditCursor = 0; // position of cursor in edit buffer
    private boolean osdCaptureInput = false;
    private boolean osdShift = false;
    private boolean osdCtrl = false;
    private boolean osdCapsLock = false;
    private boolean osdOverType = false;

    public OsdCommandPrompt(Console parent) {
        this.parent = parent;
        this.finder = new CommandFinder(parent.osdVars);
        this.inputHistory = new ConsoleHistory(32);

        parent.registerCommand(new OsdValueRange("osdpromptshade", "osdpromptshade: sets the shade of the OSD prompt", 0, 7) {
            @Override
            public float getValue() {
                return osdPromptShade;
            }

            @Override
            protected void setCheckedValue(float value) {
                osdPromptShade = (int) value;
            }
        });
        parent.registerCommand(new OsdValueRange("osdpromptpal", "osdpromptpal: sets the palette of the OSD prompt", 0, MAXPALOOKUPS - 1) {
            @Override
            public float getValue() {
                return osdPromptPal.getPal();
            }

            @Override
            protected void setCheckedValue(float value) {
                osdPromptPal = OsdColor.findColor((int) value);
            }
        });
        parent.registerCommand(new OsdValueRange("osdeditshade", "osdeditshade: sets the shade of the OSD input text", 0, 7) {
            @Override
            public float getValue() {
                return osdEditShade;
            }

            @Override
            protected void setCheckedValue(float value) {
                osdEditShade = (int) value;
            }
        });
        parent.registerCommand(new OsdValueRange("osdeditpal", "osdeditpal: sets the palette of the OSD input text", 0, MAXPALOOKUPS - 1) {
            @Override
            public float getValue() {
                return osdEditPal.getPal();
            }

            @Override
            protected void setCheckedValue(float value) {
                osdEditPal = OsdColor.findColor((int) value);
            }
        });
    }

    public void captureInput(boolean capture) {
        this.osdCaptureInput = capture;
        parent.func.showOsd(capture);
    }

    public void onFirstPosition() {
        osdEditCursor = 0;
    }

    public void onLastPosition() {
        osdEditCursor = osdEditBuf.length();
    }

    public void setVersion(String version, OsdColor pal, int shade) {
        this.osdVersionText = version;
        this.osdVersionShade = shade;
        this.osdVersionPal = pal;
    }

    public void onLeft() {
        if (osdEditCursor > 0) {
            if (isCtrlPressed()) {
                while (osdEditCursor > 0) {
                    if (!Character.isSpaceChar(osdEditBuf.charAt(osdEditCursor - 1))) {
                        break;
                    }
                    osdEditCursor--;
                }
                while (osdEditCursor > 0) {
                    if (Character.isSpaceChar(osdEditBuf.charAt(osdEditCursor - 1))) {
                        break;
                    }
                    osdEditCursor--;
                }
            } else {
                osdEditCursor--;
            }
        }
    }

    public void onRight() {
        if (osdEditCursor < osdEditBuf.length()) {
            if (isCtrlPressed()) {
                while (osdEditCursor < osdEditBuf.length()) {
                    if (Character.isSpaceChar(osdEditBuf.charAt(osdEditCursor))) {
                        break;
                    }
                    osdEditCursor++;
                }
                while (osdEditCursor < osdEditBuf.length()) {
                    if (!Character.isSpaceChar(osdEditBuf.charAt(osdEditCursor))) {
                        break;
                    }
                    osdEditCursor++;
                }
            } else osdEditCursor++;
        }
    }

    public void onDelete() {
        if (osdEditCursor == 0 || osdEditBuf.length() == 0) {
            return;
        }

        if (osdEditCursor <= osdEditBuf.length()) {
            osdEditBuf.deleteCharAt(--osdEditCursor);
        }
    }

    public void handleInput() {
        try {
            while (System.in.available() != 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                handleInput(reader.readLine());
            }
        } catch (IOException ignore) {
        }
    }

    public void handleInput(String text) {
        osdEditBuf.append(text);
        onEnter();
    }

    public void onEnter() {
        if (!isEmpty()) {
            String input = osdEditBuf.toString().toLowerCase();
            inputHistory.add(input);
            parent.dispatch(input);
        }
        parent.setFirstLine();
        finder.reset();
        clear();
    }

    public void historyPrev() {
        if (inputHistory.hasPrev()) {
            applyHistory(inputHistory.prev());
        }
    }

    public void historyNext() {
        String history = inputHistory.next();
        if (history.isEmpty()) {
            osdEditBuf.setLength(0);
            osdEditCursor = 0;
            return;
        }

        applyHistory(history);
    }

    private void applyHistory(String history) {
        osdEditBuf.setLength(0);
        osdEditBuf.append(history);
        osdEditCursor = osdEditBuf.length();
    }

    public void onTab() {
        handleTab(osdEditBuf.toString());
    }

    public void append(char ch) {
        finder.reset();
        if (osdEditBuf.length() < OSD_EDIT_LENGTH) {
            if (osdEditCursor < osdEditBuf.length()) {
                if(osdOverType) {
                    osdEditBuf.deleteCharAt(osdEditCursor);
                    osdEditBuf.insert(osdEditCursor, ch);
                    osdEditCursor++;
                } else {
                    osdEditBuf.insert(osdEditCursor, ch);
                    osdEditCursor++;
                }
            } else {
                osdEditBuf.append(ch);
                osdEditCursor++;
            }
        }
    }

    private void handleTab(String input) {
        if (!finder.isListPresent()) {
            List<String> commands = finder.getCommands(input);
            if (commands.size() > 1) {
                parent.printCommandList(commands, String.format("Found %d possible completions for \"%s\"", commands.size(), input), "Press TAB again to cycle through matches");
            } else if (commands.size() == 1) {
                fillEditBuf(commands.get(0));
            }
        } else {
            fillEditBuf(finder.getNextTabCommand());
        }
    }

    private void fillEditBuf(String text) {
        clear();
        osdEditBuf.append(text);
        onLastPosition();
    }

    public boolean isEmpty() {
        return osdEditBuf.length() == 0;
    }

    public void clear() {
        osdEditBuf.setLength(0);
        osdEditCursor = 0;
    }

    /**
     * Toggles when insert button is pressed
     */
    public void toggleOverType() {
        osdOverType = !osdOverType;
    }

    public void onResize() {
    }

    public void draw(OsdFunc func) {
        int ypos = parent.osdRowsCur;
        int textScale = parent.osdTextScale;

        int shade = osdPromptShade;
        if (shade == 0) {
            shade = func.getPulseShade(4);
        }

        if (parent.isOnLastLine()) {
            func.drawchar(0, ypos, '~', shade, osdPromptPal, textScale);
        } else if (!parent.isOnFirstLine()) {
            func.drawchar(0, ypos, '^', shade, osdPromptPal, textScale);
        }

        int offset = 0;
        if (osdCapsLock) {
            if (parent.isOnFirstLine()) {
                offset = 1;
            }
            func.drawchar(offset, ypos, 'C', shade, osdPromptPal, textScale);
        }

        if (osdShift) {
            offset = 1;
            if ((osdCapsLock && parent.isOnFirstLine())) {
                offset = 2;
            }
            func.drawchar(offset, ypos, 'H', shade, osdPromptPal, textScale);
        }

        offset = 0;
        if (osdCapsLock && osdShift && parent.isOnFirstLine()) {
            offset = 1;
        }
        func.drawchar(2 + offset, ypos, '>', shade, osdPromptPal, textScale);

        int len = Math.min(parent.osdCols - 1 - 3 - offset, osdEditBuf.length());
        for (int x = len - 1; x >= 0; x--) {
            func.drawchar(3 + x + offset, ypos, osdEditBuf.charAt(x), osdEditShade << 1, osdEditPal, textScale);
        }

        offset += 3 + osdEditCursor;

        func.drawcursor(offset, ypos, osdOverType, textScale);

        if (osdVersionText != null) {
            int xpos = parent.osdCols - osdVersionText.length() + 2;
            shade = osdVersionShade;
            if (shade == 0) {
                shade = func.getPulseShade(4);
            }

            func.drawstr(parent.osdCols - osdVersionText.length() + 2, ypos - ((offset >= xpos) ? 1 : 0),
                    osdVersionText, shade, osdVersionPal, textScale);
        }
    }

    public boolean isCaptured() {
        return osdCaptureInput;
    }

    public void setShiftPressed(boolean osdShift) {
        this.osdShift = osdShift;
    }

    public void setCtrlPressed(boolean osdCtrl) {
        this.osdCtrl = osdCtrl;
    }

    public void setCapsLockPressed(boolean osdCapsLock) {
        this.osdCapsLock = osdCapsLock;
    }

    public boolean isCapsLockPressed() {
        return osdCapsLock;
    }

    public boolean isShiftPressed() {
        return osdShift;
    }

    public boolean isCtrlPressed() {
        return osdCtrl;
    }
}
