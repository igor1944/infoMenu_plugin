package com.infoMenu.display;

import com.infoMenu.InfoMenu;
import com.infoMenu.config.PluginSettings;
import com.infoMenu.user.UserSettings;
import com.infoMenu.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Периодически смотрит, на кого направлен взгляд каждого игрока,
 * и показывает здоровье этой цели (и нанесённый ей урон).
 */
public final class HealthDisplayTask extends BukkitRunnable {

    private final InfoMenu plugin;
    private final HealthFormatter formatter;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public HealthDisplayTask(InfoMenu plugin) {
        this.plugin = plugin;
        this.formatter = new HealthFormatter(plugin.getSettings());
    }

    @Override
    public void run() {
        PluginSettings config = plugin.getSettings();
        if (!config.isEnabled()) {
            return;
        }

        // Раз в несколько запусков чистим устаревшие записи об уроне.
        plugin.getDamageTracker().prune();

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.isOnline() || viewer.isDead()) {
                removeBar(viewer);
                continue;
            }

            UserSettings settings = plugin.getUserStore().get(viewer.getUniqueId());
            if (!settings.isDisplayEnabled() || settings.getMode() == DisplayMode.NONE) {
                removeBar(viewer);
                continue;
            }

            LivingEntity target = findTarget(viewer, settings);
            if (target == null) {
                removeBar(viewer);
                continue;
            }

            show(viewer, target, settings);
        }
    }

    /** Ищет существо, на которое смотрит игрок, с учётом его личных настроек. */
    private LivingEntity findTarget(Player viewer, UserSettings settings) {
        Entity target;
        try {
            target = viewer.getTargetEntity(settings.getMaxDistance());
        } catch (IllegalStateException ex) {
            // Может произойти, если игрок находится в мире, который ещё не загружен.
            return null;
        }
        if (!(target instanceof LivingEntity)) {
            return null;
        }
        if (target.getUniqueId().equals(viewer.getUniqueId())) {
            return null;
        }
        if (settings.isPlayersOnly() && !(target instanceof Player)) {
            return null;
        }
        LivingEntity living = (LivingEntity) target;
        if (living.isDead()) {
            return null;
        }
        return living;
    }

    private void show(Player viewer, LivingEntity target, UserSettings settings) {
        double health = Math.max(0.0, target.getHealth());
        double maxHealth = maxHealth(target);
        double damage = plugin.getDamageTracker().getDamage(viewer, target);

        String line = formatter.buildLine(target, health, maxHealth, damage, settings);

        if (settings.getMode() == DisplayMode.BOSS_BAR) {
            updateBar(viewer, line, health, maxHealth);
        } else {
            removeBar(viewer);
            viewer.sendActionBar(Text.component(line));
        }
    }

    private void updateBar(Player viewer, String line, double health, double maxHealth) {
        double progress = maxHealth > 0.0 ? Math.max(0.0, Math.min(1.0, health / maxHealth)) : 0.0;
        double percent = progress * 100.0;
        PluginSettings config = plugin.getSettings();

        BossBar bar = bars.get(viewer.getUniqueId());
        if (bar == null) {
            BarColor color = config.barColorForPercent(percent);
            bar = Bukkit.createBossBar(Text.legacy(line), color, config.getBarStyle());
            bar.addPlayer(viewer);
            bars.put(viewer.getUniqueId(), bar);
        } else {
            bar.setTitle(Text.legacy(line));
            bar.setColor(config.barColorForPercent(percent));
        }
        bar.setProgress(progress);
    }

    private void removeBar(Player viewer) {
        BossBar bar = bars.remove(viewer.getUniqueId());
        if (bar != null) {
            bar.removePlayer(viewer);
            bar.removeAll();
        }
    }

    /** Убирает все босс-бары (выключается плагин, перезагружается конфиг, игрок вышел). */
    public void removeAllBars() {
        for (BossBar bar : bars.values()) {
            bar.removeAll();
        }
        bars.clear();
    }

    /** Убирает босс-бар конкретного игрока, например при выходе с сервера. */
    public void removeBar(UUID playerId) {
        BossBar bar = bars.remove(playerId);
        if (bar != null) {
            bar.removeAll();
        }
    }

    /**
     * Максимальное здоровье цели: берём из атрибута GENERIC_MAX_HEALTH,
     * а если атрибута нет — используем текущее здоровье.
     */
    public static double maxHealth(LivingEntity entity) {
        if (entity instanceof org.bukkit.attribute.Attributable) {
            AttributeInstance attribute = ((org.bukkit.attribute.Attributable) entity)
                    .getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attribute != null && attribute.getValue() > 0.0) {
                return attribute.getValue();
            }
        }
        return Math.max(1.0, entity.getHealth());
    }
}
