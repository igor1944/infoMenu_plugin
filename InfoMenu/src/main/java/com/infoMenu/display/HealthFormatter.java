package com.infoMenu.display;

import com.infoMenu.config.PluginSettings;
import com.infoMenu.user.UserSettings;
import com.infoMenu.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Собирает строку с информацией о цели: имя, здоровье, полоска сердец и нанесённый урон.
 * Поддерживаемые плейсхолдеры:
 * <pre>
 *   %target% / %name%  — имя игрока или название существа
 *   %entity%           — тип существа (Player, Zombie, ...)
 *   %health%           — текущее здоровье
 *   %max_health%       — максимальное здоровье
 *   %percent%          — процент здоровья (0-100)
 *   %color%            — цвет, зависящий от процента здоровья
 *   %hearts%           — полоска из сердец
 *   %damage%           — суммарный урон, нанесённый вами этой цели
 *   %damage_line%      — готовый кусок строки с уроном (пусто, если урона не было)
 * </pre>
 */
public final class HealthFormatter {

    private final PluginSettings config;

    public HealthFormatter(PluginSettings config) {
        this.config = config;
    }

    public String buildLine(LivingEntity target, double health, double maxHealth,
                            double damage, UserSettings settings) {

        double percent = maxHealth > 0.0 ? Math.max(0.0, Math.min(100.0, health / maxHealth * 100.0)) : 0.0;
        String color = config.colorForPercent(percent);
        String name = displayName(target);

        String hearts = "";
        if (config.isHeartsAllowed() && settings.isHeartsEnabled()) {
            hearts = hearts(percent, color);
        }

        String damageLine = "";
        if (config.isDamageAllowed() && settings.isDamageEnabled() && damage > 0.0) {
            damageLine = config.getDamageLineFormat()
                    .replace("%damage%", Text.number(damage))
                    .replace("%target%", name)
                    .replace("%color%", color);
        }

        return config.getFormat()
                .replace("%target%", name)
                .replace("%name%", name)
                .replace("%entity%", Text.pretty(target.getType().name()))
                .replace("%max_health%", Text.number(maxHealth))
                .replace("%health%", Text.number(health))
                .replace("%percent%", String.valueOf((int) Math.round(percent)))
                .replace("%color%", color)
                .replace("%hearts%", hearts)
                .replace("%damage%", Text.number(damage))
                .replace("%damage_line%", damageLine);
    }

    /** Короткий отчёт для команды /infomenu health &lt;игрок&gt;. */
    public String buildReport(Player target, double health, double maxHealth) {
        double percent = maxHealth > 0.0 ? Math.max(0.0, Math.min(100.0, health / maxHealth * 100.0)) : 0.0;
        return config.getMsgHealthReport()
                .replace("%player%", target.getName())
                .replace("%max_health%", Text.number(maxHealth))
                .replace("%health%", Text.number(health))
                .replace("%percent%", String.valueOf((int) Math.round(percent)))
                .replace("%color%", config.colorForPercent(percent));
    }

    /** Полоска сердец, например: ❤❤❤❤❤❤❤♥☆☆ */
    public String hearts(double percent, String color) {
        int count = config.getHeartCount();
        double exact = Math.max(0.0, Math.min(100.0, percent)) / 100.0 * count;
        int full = (int) Math.floor(exact);
        boolean half = (exact - full) >= 0.5;

        String fullHeart = config.getHeartFull().replace("%color%", color);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < full; i++) {
            builder.append(fullHeart);
        }
        if (half && full < count) {
            builder.append(config.getHeartHalf());
            full++;
        }
        for (int i = full; i < count; i++) {
            builder.append(config.getHeartEmpty());
        }
        return builder.toString();
    }

    /** Имя цели: ник игрока, кастомное имя существа или просто тип. */
    public static String displayName(Entity entity) {
        if (entity instanceof Player) {
            return ((Player) entity).getName();
        }
        Component custom = entity.customName();
        if (custom != null) {
            String plain = PlainTextComponentSerializer.plainText().serialize(custom);
            if (!plain.isEmpty()) {
                return plain;
            }
        }
        return Text.pretty(entity.getType().name());
    }
}
