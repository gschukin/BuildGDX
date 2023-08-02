package ru.m210projects.Build.osd;

public class OsdConsolePromptUI implements OsdCommandPromptUI {

    private final Console parent;

    public OsdConsolePromptUI(Console parent) {
        this.parent = parent;
    }

    @Override
    public void draw() {
        OsdFunc func = parent.func;
        OsdCommandPrompt prompt = parent.prompt;

        int osdEditShade = prompt.osdEditShade;
        int osdPromptShade = prompt.osdPromptShade;
        OsdColor osdPromptPal = prompt.osdPromptPal;
        OsdColor osdEditPal = prompt.osdEditPal;
        OsdColor osdVersionPal = prompt.osdVersionPal;

        int ypos = parent.osdRowsCur;
        int textScale = parent.osdTextScale;

        int shade = osdPromptShade;
        if (shade == 0) {
            shade = parent.func.getPulseShade(4);
        }

        if (parent.isOnLastLine()) {
            func.drawchar(0, ypos, '~', shade, osdPromptPal, textScale);
        } else if (!parent.isOnFirstLine()) {
            func.drawchar(0, ypos, '^', shade, osdPromptPal, textScale);
        }

        int offset = 0;
        if (prompt.isCapsLockPressed()) {
            if (parent.isOnFirstLine()) {
                offset = 1;
            }
            func.drawchar(offset, ypos, 'C', shade, osdPromptPal, textScale);
        }

        if (prompt.isShiftPressed()) {
            offset = 1;
            if ((prompt.isCapsLockPressed() && parent.isOnFirstLine())) {
                offset = 2;
            }
            func.drawchar(offset, ypos, 'H', shade, osdPromptPal, textScale);
        }

        offset = 0;
        if (prompt.isCapsLockPressed() && prompt.isShiftPressed() && parent.isOnFirstLine()) {
            offset = 1;
        }
        func.drawchar(2 + offset, ypos, '>', shade, osdPromptPal, textScale);

        String inputText = prompt.getTextInput();
        int len = Math.min(parent.osdCols - 1 - 3 - offset, inputText.length());
        for (int x = len - 1; x >= 0; x--) {
            func.drawchar(3 + x + offset, ypos, inputText.charAt(x), osdEditShade << 1, osdEditPal, textScale);
        }

        offset += 3 + prompt.osdEditCursor;

        func.drawcursor(offset, ypos, prompt.isOsdOverType(), textScale);

        String osdVersionText = prompt.osdVersionText;
        if (osdVersionText != null) {
            int xpos = parent.osdCols - osdVersionText.length() + 2;
            func.drawstr(parent.osdCols - osdVersionText.length() + 2, ypos - ((offset >= xpos) ? 1 : 0),
                    osdVersionText, func.getPulseShade(4), osdVersionPal, textScale);
        }
    }
}
