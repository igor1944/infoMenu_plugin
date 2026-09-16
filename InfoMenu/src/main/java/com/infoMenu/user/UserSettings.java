package com.infoMenu.user;

import com.infoMenu.display.DisplayMode;

/**
 * Личные настройки игрока. Значения по умолчанию берутся из config.yml,
 * но каждый игрок может изменить их в меню ({@code /infomenu}).
 */
public final class UserSettings {

    private boolean displayEnabled = true;
    private DisplayMode mode = DisplayMode.ACTION_BAR;
    private boolean heartsEnabled = true;
    private boolean damageEnabled = true;
    private boolean playersOnly = true;
    private int maxDistance = 32;

    public boolean isDisplayEnabled() {
        return displayEnabled;
    }

    public void setDisplayEnabled(boolean displayEnabled) {
        this.displayEnabled = displayEnabled;
    }

    public DisplayMode getMode() {
        return mode;
    }

    public void setMode(DisplayMode mode) {
        this.mode = mode == null ? DisplayMode.NONE : mode;
    }

    public boolean isHeartsEnabled() {
        return heartsEnabled;
    }

    public void setHeartsEnabled(boolean heartsEnabled) {
        this.heartsEnabled = heartsEnabled;
    }

    public boolean isDamageEnabled() {
        return damageEnabled;
    }

    public void setDamageEnabled(boolean damageEnabled) {
        this.damageEnabled = damageEnabled;
    }

    public boolean isPlayersOnly() {
        return playersOnly;
    }

    public void setPlayersOnly(boolean playersOnly) {
        this.playersOnly = playersOnly;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = Math.max(2, Math.min(128, maxDistance));
    }
}
