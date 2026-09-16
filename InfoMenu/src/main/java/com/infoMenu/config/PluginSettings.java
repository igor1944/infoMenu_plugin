package com.infoMenu.config;

import com.infoMenu.display.DisplayMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Типизированный доступ к config.yml.
 * Все значения читаются заново при вызове {@link #load()}, поэтому /infomenu reload
 * подхватывает изменения без перезапуска сервера.
 */
public final class PluginSettings {

    private static final List<String> DEFAULT_INFO_LORE = Arrays.asList(
            "&7Здесь можно настроить показ здоровья",
            "&7игрока, на которого вы смотрите,",
            "&7а также учёт нанесённого урона."
    );

    // основные
    private boolean enabled;
    private String prefix;

    // значения по умолчанию для новых игроков
    private DisplayMode defaultMode;
    private boolean defaultHearts;
    private boolean defaultDamage;
    private boolean defaultPlayersOnly;
    private int defaultMaxDistance;

    // задача
    private int refreshInterval;

    // формат строки
    private String format;

    // сердца
    private boolean heartsAllowed;
    private int heartCount;
    private String heartFull;
    private String heartHalf;
    private String heartEmpty;

    // цвета по проценту здоровья
    private double thresholdHigh;
    private double thresholdMedium;
    private String colorHigh;
    private String colorMedium;
    private String colorLow;

    // урон
    private boolean damageAllowed;
    private long damageMemoryMillis;
    private String damageLineFormat;
    private String hitMessage;
    private String hitMessageSelf;

    // босс-бар
    private BarStyle barStyle;
    private BarColor barColorHigh;
    private BarColor barColorMedium;
    private BarColor barColorLow;

    // меню
    private String menuTitle;
    private List<String> menuInfoLore;

    // сообщения
    private String msgNoPermission;
    private String msgReloaded;
    private String msgPlayerNotFound;
    private String msgHealthReport;
    private String msgDisplayOn;
    private String msgDisplayOff;

    /** Перечитывает настройки из переданной секции (обычно это config.yml). */
    public void load(ConfigurationSection config) {

        this.enabled = config.getBoolean("enabled", true);
        this.prefix = config.getString("prefix", "&8[&bInfoMenu&8] &r");

        this.defaultMode = DisplayMode.from(config.getString("display.mode"), DisplayMode.ACTION_BAR);
        this.defaultHearts = config.getBoolean("display.hearts.enabled", true);
        this.defaultDamage = config.getBoolean("damage.enabled", true);
        this.defaultPlayersOnly = config.getBoolean("display.players-only", true);
        this.defaultMaxDistance = clamp(config.getInt("display.max-distance", 32), 2, 128);

        this.refreshInterval = clamp(config.getInt("display.refresh-interval", 10), 1, 200);

        this.format = config.getString("display.format",
                "&f%target% &7| %color%%health%&c❤&7/&f%max_health% &8(%percent%%)%damage_line%");

        this.heartsAllowed = config.getBoolean("display.hearts.enabled", true);
        this.heartCount = clamp(config.getInt("display.hearts.count", 10), 1, 40);
        this.heartFull = config.getString("display.hearts.full", "%color%❤");
        this.heartHalf = config.getString("display.hearts.half", "&6❤");
        this.heartEmpty = config.getString("display.hearts.empty", "&8❤");

        this.thresholdHigh = config.getDouble("display.health-color.high-threshold", 66.0);
        this.thresholdMedium = config.getDouble("display.health-color.medium-threshold", 33.0);
        this.colorHigh = config.getString("display.health-color.high", "&a");
        this.colorMedium = config.getString("display.health-color.medium", "&e");
        this.colorLow = config.getString("display.health-color.low", "&c");

        this.damageAllowed = config.getBoolean("damage.enabled", true);
        this.damageMemoryMillis = Math.max(1, config.getInt("damage.memory-seconds", 10)) * 1000L;
        this.damageLineFormat = config.getString("damage.line-format", " &8| &7Урон: &c%damage%");
        this.hitMessage = config.getString("damage.hit-message", "");
        this.hitMessageSelf = config.getString("damage.hit-message-to-victim", "");

        this.barStyle = parseBarStyle(config.getString("boss-bar.style"), BarStyle.SOLID);
        this.barColorHigh = parseBarColor(config.getString("boss-bar.color-high"), BarColor.GREEN);
        this.barColorMedium = parseBarColor(config.getString("boss-bar.color-medium"), BarColor.YELLOW);
        this.barColorLow = parseBarColor(config.getString("boss-bar.color-low"), BarColor.RED);

        this.menuTitle = config.getString("menu.title", "&8Настройки InfoMenu");
        this.menuInfoLore = colored(config.getStringList("menu.info-lore"), DEFAULT_INFO_LORE);

        this.msgNoPermission = config.getString("messages.no-permission",
                "&cУ вас нет прав на это действие.");
        this.msgReloaded = config.getString("messages.reloaded",
                "&aКонфигурация InfoMenu перезагружена.");
        this.msgPlayerNotFound = config.getString("messages.player-not-found",
                "&cИгрок &f%player% &cне найден.");
        this.msgHealthReport = config.getString("messages.health-report",
                "&7Здоровье &f%player%&7: %color%%health%&c❤&7/&f%max_health% &8(%percent%%)");
        this.msgDisplayOn = config.getString("messages.display-on",
                "&aПоказ здоровья включён.");
        this.msgDisplayOff = config.getString("messages.display-off",
                "&7Показ здоровья выключен.");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<String> colored(List<String> fromConfig, List<String> fallback) {
        if (fromConfig == null || fromConfig.isEmpty()) {
            return new ArrayList<>(fallback);
        }
        return new ArrayList<>(fromConfig);
    }

    private static BarStyle parseBarStyle(String raw, BarStyle fallback) {
        if (raw != null) {
            try {
                return BarStyle.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // оставляем значение по умолчанию
            }
        }
        return fallback;
    }

    private static BarColor parseBarColor(String raw, BarColor fallback) {
        if (raw != null) {
            try {
                return BarColor.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // оставляем значение по умолчанию
            }
        }
        return fallback;
    }

    // ===== геттеры =====

    public boolean isEnabled() {
        return enabled;
    }

    public String getPrefix() {
        return prefix;
    }

    public DisplayMode getDefaultMode() {
        return defaultMode;
    }

    public boolean isDefaultHearts() {
        return defaultHearts;
    }

    public boolean isDefaultDamage() {
        return defaultDamage;
    }

    public boolean isDefaultPlayersOnly() {
        return defaultPlayersOnly;
    }

    public int getDefaultMaxDistance() {
        return defaultMaxDistance;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public String getFormat() {
        return format;
    }

    public boolean isHeartsAllowed() {
        return heartsAllowed;
    }

    public int getHeartCount() {
        return heartCount;
    }

    public String getHeartFull() {
        return heartFull;
    }

    public String getHeartHalf() {
        return heartHalf;
    }

    public String getHeartEmpty() {
        return heartEmpty;
    }

    /** Цвет текста в зависимости от процента здоровья. */
    public String colorForPercent(double percent) {
        if (percent >= thresholdHigh) {
            return colorHigh;
        }
        if (percent >= thresholdMedium) {
            return colorMedium;
        }
        return colorLow;
    }

    /** Цвет босс-бара в зависимости от процента здоровья. */
    public BarColor barColorForPercent(double percent) {
        if (percent >= thresholdHigh) {
            return barColorHigh;
        }
        if (percent >= thresholdMedium) {
            return barColorMedium;
        }
        return barColorLow;
    }

    public boolean isDamageAllowed() {
        return damageAllowed;
    }

    public long getDamageMemoryMillis() {
        return damageMemoryMillis;
    }

    public String getDamageLineFormat() {
        return damageLineFormat;
    }

    public String getHitMessage() {
        return hitMessage;
    }

    public String getHitMessageSelf() {
        return hitMessageSelf;
    }

    public BarStyle getBarStyle() {
        return barStyle;
    }

    public String getMenuTitle() {
        return menuTitle;
    }

    public List<String> getMenuInfoLore() {
        return menuInfoLore;
    }

    public String getMsgNoPermission() {
        return msgNoPermission;
    }

    public String getMsgReloaded() {
        return msgReloaded;
    }

    public String getMsgPlayerNotFound() {
        return msgPlayerNotFound;
    }

    public String getMsgHealthReport() {
        return msgHealthReport;
    }

    public String getMsgDisplayOn() {
        return msgDisplayOn;
    }

    public String getMsgDisplayOff() {
        return msgDisplayOff;
    }
}
