package com.infoMenu.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Небольшая обёртка над цветовыми кодами.
 * В конфиге поддерживаются обычные коды вида {@code &a}, {@code &7} и т.д.
 */
public final class Text {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    /** Переводит {@code &}-коды в {@code §}-коды (для старых String-API). */
    public static String legacy(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    /** Переводит строку с {@code &}-кодами в Adventure-компонент. */
    public static Component component(String text) {
        return SERIALIZER.deserialize(text == null ? "" : text);
    }

    /** То же самое, но для списка строк (лор предмета). */
    public static List<Component> components(List<String> lines) {
        List<Component> result = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                result.add(component(line));
            }
        }
        return result;
    }

    /**
     * Форматирует число: целые значения выводятся без дробной части (20),
     * дробные — с одним знаком после запятой (13.5).
     */
    public static String number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(Math.round(value * 10.0) / 10.0);
    }

    /** ZOMBIE_PIGMAN -> Zombie Pigman */
    public static String pretty(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String[] parts = name.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
