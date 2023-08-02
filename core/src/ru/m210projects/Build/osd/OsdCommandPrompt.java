package ru.m210projects.Build.osd;

import ru.m210projects.Build.osd.commands.OsdValueRange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static ru.m210projects.Build.Engine.MAXPALOOKUPS;

public class OsdCommandPrompt {

    private static final int OSD_EDIT_LENGTH = 511;
    private final ConsoleHistory inputHistory;
    private final StringBuilder osdEditBuf = new StringBuilder(OSD_EDIT_LENGTH);
    private final OsdCommandPromptUI ui;
    //    private final Console parent;
    protected String osdVersionText;
    protected int osdVersionShade;
    protected OsdColor osdVersionPal = OsdColor.DEFAULT;
    protected OsdColor osdPromptPal = OsdColor.DEFAULT;
    protected int osdPromptShade;
    protected OsdColor osdEditPal = OsdColor.DEFAULT;
    protected int osdEditShade;
    protected int osdEditCursor = 0; // position of cursor in edit buffer
    private boolean osdCaptureInput = false;
    private boolean osdShift = false;
    private boolean osdCtrl = false;
    private boolean osdCapsLock = false;
    private boolean osdOverType = false;
    private ActionListener actionListener;

    public OsdCommandPrompt(OsdCommandPromptUI ui) {
        this.inputHistory = new ConsoleHistory(32);
        this.ui = ui;
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    protected void registerDefaultCommands(Console parent) {
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
            String input = osdEditBuf.toString();
            inputHistory.add(input);
        }
        actionListener.onEnter(getTextInput());
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

    public void append(char ch) {
        if (osdEditBuf.length() < OSD_EDIT_LENGTH) {
            if (osdEditCursor < osdEditBuf.length()) {
                if (osdOverType) {
                    osdEditBuf.deleteCharAt(osdEditCursor);
                }
                osdEditBuf.insert(osdEditCursor, ch);
            } else {
                osdEditBuf.append(ch);
            }
            osdEditCursor++;
        }
    }

    public String getTextInput() {
        return osdEditBuf.toString();
    }

    public void setTextInput(String text) {
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

    public boolean isOsdOverType() {
        return osdOverType;
    }

    public void onResize() {
    }

    public void draw() {
        ui.draw();
    }

    public boolean isCaptured() {
        return osdCaptureInput;
    }

    public boolean isCapsLockPressed() {
        return osdCapsLock;
    }

    public void setCapsLockPressed(boolean osdCapsLock) {
        this.osdCapsLock = osdCapsLock;
    }

    public boolean isShiftPressed() {
        return osdShift;
    }

    public void setShiftPressed(boolean osdShift) {
        this.osdShift = osdShift;
    }

    public boolean isCtrlPressed() {
        return osdCtrl;
    }

    public void setCtrlPressed(boolean osdCtrl) {
        this.osdCtrl = osdCtrl;
    }

    public interface ActionListener {
        void onEnter(String input);
    }
}
