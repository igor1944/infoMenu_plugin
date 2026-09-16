package com.infoMenu.user;

import com.infoMenu.config.PluginSettings;
import com.infoMenu.display.DisplayMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит личные настройки игроков и сохраняет их в plugins/InfoMenu/users.yml,
 * чтобы после перезапуска сервера ничего не сбрасывалось.
 */
public final class UserStore {

    private final PluginSettings defaults;
    private final File file;
    private final Logger logger;
    private final Map<UUID, UserSettings> settings = new ConcurrentHashMap<>();

    public UserStore(PluginSettings defaults, File file, Logger logger) {
        this.defaults = defaults;
        this.file = file;
        this.logger = logger;
    }

    /** Настройки игрока; если их ещё нет — создаёт из значений по умолчанию. */
    public UserSettings get(UUID playerId) {
        return settings.computeIfAbsent(playerId, key -> defaults());
    }

    /** Создаёт настройки со значениями из config.yml. */
    public UserSettings defaults() {
        UserSettings result = new UserSettings();
        result.setMode(defaults.getDefaultMode());
        result.setHeartsEnabled(defaults.isDefaultHearts());
        result.setDamageEnabled(defaults.isDefaultDamage());
        result.setPlayersOnly(defaults.isDefaultPlayersOnly());
        result.setMaxDistance(defaults.getDefaultMaxDistance());
        return result;
    }

    /** Сбрасывает настройки игрока к значениям из конфига. */
    public UserSettings reset(UUID playerId) {
        UserSettings fresh = defaults();
        settings.put(playerId, fresh);
        return fresh;
    }

    public void load() {
        loadFrom(file);
    }

    /** Читает настройки из указанного файла (удобно для проверок и тестов). */
    public void loadFrom(File source) {
        settings.clear();
        if (source == null || !source.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(source);
        for (String key : yaml.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                logger.warning("Пропущен неизвестный раздел в users.yml: " + key);
                continue;
            }
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            UserSettings user = defaults();
            user.setDisplayEnabled(section.getBoolean("display", true));
            user.setMode(DisplayMode.from(section.getString("mode"), user.getMode()));
            user.setHeartsEnabled(section.getBoolean("hearts", user.isHeartsEnabled()));
            user.setDamageEnabled(section.getBoolean("damage", user.isDamageEnabled()));
            user.setPlayersOnly(section.getBoolean("players-only", user.isPlayersOnly()));
            user.setMaxDistance(section.getInt("distance", user.getMaxDistance()));
            settings.put(playerId, user);
        }
        logger.info("Загружено настроек игроков: " + settings.size());
    }

    public void save() {
        saveTo(file);
    }

    /** Сохраняет настройки в указанный файл. */
    public void saveTo(File target) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, UserSettings> entry : settings.entrySet()) {
            UserSettings user = entry.getValue();
            String path = entry.getKey().toString();
            yaml.set(path + ".display", user.isDisplayEnabled());
            yaml.set(path + ".mode", user.getMode().name());
            yaml.set(path + ".hearts", user.isHeartsEnabled());
            yaml.set(path + ".damage", user.isDamageEnabled());
            yaml.set(path + ".players-only", user.isPlayersOnly());
            yaml.set(path + ".distance", user.getMaxDistance());
        }
        try {
            yaml.save(target);
        } catch (IOException ex) {
            logger.severe("Не удалось сохранить users.yml: " + ex.getMessage());
        }
    }

    /** Запоминает игрока, чтобы его настройки попали в users.yml. */
    public void touch(UUID playerId) {
        get(playerId);
    }

    public void forget(UUID playerId) {
        settings.remove(playerId);
    }
}
