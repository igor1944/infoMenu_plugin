package com.infoMenu.display;

import com.infoMenu.config.PluginSettings;
import com.infoMenu.util.Text;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Считает, сколько урона каждый игрок нанёс каждой цели.
 * Значения хранятся ограниченное время ({@code damage.memory-seconds} в конфиге),
 * поэтому «Урон: 12» означает урон за последние N секунд, а не за всё время.
 */
public final class DamageTracker implements Listener {

    /** Урон по одной цели от одного игрока. */
    private static final class Record {
        private double total;
        private long lastUpdate;

        Record(double total, long lastUpdate) {
            this.total = total;
            this.lastUpdate = lastUpdate;
        }
    }

    private final PluginSettings config;
    /** attacker -> (victim -> record) */
    private final Map<UUID, Map<UUID, Record>> damage = new ConcurrentHashMap<>();

    public DamageTracker(PluginSettings config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!config.isEnabled() || !config.isDamageAllowed()) {
            return;
        }

        Entity victim = event.getEntity();
        if (!(victim instanceof LivingEntity)) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        double amount = event.getFinalDamage();
        if (amount <= 0.0) {
            return;
        }

        double total = addDamage(attacker.getUniqueId(), victim.getUniqueId(), amount,
                System.currentTimeMillis());

        notify(attacker, (LivingEntity) victim, total);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        // Цель умерла — счётчик урона по ней больше не нужен.
        UUID victimId = event.getEntity().getUniqueId();
        for (Map<UUID, Record> byVictim : damage.values()) {
            byVictim.remove(victimId);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        damage.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Суммарный урон, нанесённый игроком {@code attacker} цели {@code victim}
     * за время, заданное в конфиге.
     */
    public double getDamage(Player attacker, Entity victim) {
        Map<UUID, Record> byVictim = damage.get(attacker.getUniqueId());
        if (byVictim == null) {
            return 0.0;
        }
        return damageOf(attacker.getUniqueId(), victim.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Прибавляет урон к счётчику «игрок -&gt; цель» и возвращает накопленное значение.
     * Если с прошлого удара прошло больше {@code damage.memory-seconds}, счётчик начинается заново.
     */
    public double addDamage(UUID attackerId, UUID victimId, double amount, long now) {
        Map<UUID, Record> byVictim = damage.computeIfAbsent(attackerId, key -> new ConcurrentHashMap<>());
        Record record = byVictim.compute(victimId,
                (key, old) -> (old == null || isExpired(old, now)) ? new Record(0.0, now) : old);
        record.total += amount;
        record.lastUpdate = now;
        return record.total;
    }

    /** Накопленный урон по цели; 0, если запись устарела или её нет. */
    public double damageOf(UUID attackerId, UUID victimId, long now) {
        Map<UUID, Record> byVictim = damage.get(attackerId);
        if (byVictim == null) {
            return 0.0;
        }
        Record record = byVictim.get(victimId);
        if (record == null || isExpired(record, now)) {
            return 0.0;
        }
        return record.total;
    }

    /** Полный сброс всех счётчиков (используется при /infomenu reload). */
    public void clear() {
        damage.clear();
    }

    /** Убирает устаревшие записи, чтобы карта не росла бесконечно. */
    public void prune() {
        prune(System.currentTimeMillis());
    }

    /** То же самое, но с явно заданным моментом времени. */
    public void prune(long now) {
        Iterator<Map.Entry<UUID, Map<UUID, Record>>> attackers = damage.entrySet().iterator();
        while (attackers.hasNext()) {
            Map<UUID, Record> byVictim = attackers.next().getValue();
            byVictim.values().removeIf(record -> isExpired(record, now));
            if (byVictim.isEmpty()) {
                attackers.remove();
            }
        }
    }

    private boolean isExpired(Record record, long now) {
        return now - record.lastUpdate > config.getDamageMemoryMillis();
    }

    /** Находит игрока-источника урона: удар рукой, луком, трезубцем и т.п. */
    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            ProjectileSource source = ((Projectile) damager).getShooter();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        if (damager instanceof Tameable) {
            // Урон от прирученного животного засчитываем владельцу.
            Object owner = ((Tameable) damager).getOwner();
            if (owner instanceof Player) {
                return (Player) owner;
            }
        }
        return null;
    }

    private void notify(Player attacker, LivingEntity victim, double total) {
        String message = config.getHitMessage();
        if (message != null && !message.isEmpty()) {
            double health = Math.max(0.0, victim.getHealth());
            attacker.sendMessage(Text.component(prefix(config) + message
                    .replace("%damage%", Text.number(total))
                    .replace("%target%", HealthFormatter.displayName(victim))
                    .replace("%health%", Text.number(health))
                    .replace("%max_health%", Text.number(HealthDisplayTask.maxHealth(victim)))));
        }

        String toVictim = config.getHitMessageSelf();
        if (toVictim != null && !toVictim.isEmpty() && victim instanceof Player) {
            ((Player) victim).sendMessage(Text.component(prefix(config) + toVictim
                    .replace("%damage%", Text.number(total))
                    .replace("%target%", attacker.getName())
                    .replace("%health%", Text.number(Math.max(0.0, victim.getHealth())))
                    .replace("%max_health%", Text.number(HealthDisplayTask.maxHealth(victim)))));
        }
    }

    private String prefix(PluginSettings config) {
        String prefix = config.getPrefix();
        return prefix == null ? "" : prefix;
    }
}
