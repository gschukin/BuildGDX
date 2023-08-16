package ru.m210projects.Build.Pattern.MenuItems;

public interface ScrollableMenuItem {

    boolean onMoveSlider(MenuHandler handler, int scaledX, int scaledY);

    boolean onLockSlider(MenuHandler handler, int scaledX, int scaledY);

    void onUnlockSlider();

}
