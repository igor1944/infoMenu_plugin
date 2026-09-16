package com.infoMenu.display;

/**
 * Куда выводить информацию о здоровье цели.
 */
public enum DisplayMode {

    /** Полоса действий над хотбаром. */
    ACTION_BAR("Action Bar"),
    /** Полоса босса сверху экрана. */
    BOSS_BAR("Boss Bar"),
    /** Ничего не показывать. */
    NONE("Выключено");

    private final String label;

    DisplayMode(String label) {
        this.label = label;
    }

    /** Человекочитаемое название для меню. */
    public String getLabel() {
        return label;
    }

    /** Следующий режим по кругу (используется в меню настроек). */
    public DisplayMode next() {
        DisplayMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /** Безопасный разбор значения из конфига. */
    public static DisplayMode from(String raw, DisplayMode fallback) {
        if (raw != null) {
            for (DisplayMode mode : values()) {
                if (mode.name().equalsIgnoreCase(raw.trim())) {
                    return mode;
                }
            }
        }
        return fallback;
    }
}
