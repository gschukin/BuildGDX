package ru.m210projects.Build.Pattern.MenuItems;

import ru.m210projects.Build.osd.OsdCommandPrompt;

public class MenuPrompt extends OsdCommandPrompt {
    public MenuPrompt(int editLength, int historyDepth) {
        super(editLength, historyDepth);
    }

    @Override
    public void historyNext() {
        if (!inputHistory.hasNext()) {
            return;
        }
        super.historyNext();
    }

    @Override
    public void onEnter() {
        if (!isEmpty()) {
            String input = getTextInput();
            inputHistory.add(input);
            actionListener.onEnter(input);
        }
    }

    @Override
    public void setCaptureInput(boolean capture) {
        super.setCaptureInput(capture);
        if(capture) {
            inputHistory.prev(); // trig history
        }
    }
}
