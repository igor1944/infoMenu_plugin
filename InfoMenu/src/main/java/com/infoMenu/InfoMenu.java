package com.infoMenu;

import com.infoMenu.command.InfoMenuCommand;
import com.infoMenu.config.PluginSettings;
import com.infoMenu.display.DamageTracker;
import com.infoMenu.display.HealthDisplayTask;
import com.infoMenu.menu.InfoMenuListener;
import com.infoMenu.user.UserStore;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * InfoMenu — показывает здоровье игрока (или моба), на которого вы смотрите,
 * и сколько урона вы ему нанесли. Все настройки доступны в config.yml
 * и в игровом меню по команде /infomenu.
 */
public final class InfoMenu extends JavaPlugin {

    private PluginSettings settings;
    private UserStore userStore;
    private DamageTracker damageTracker;
    private HealthDisplayTask displayTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.settings = new PluginSettings();
        this.settings.load(getConfig());

        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().warning("Не удалось создать папку " + dataFolder.getPath());
        }
        this.userStore = new UserStore(settings, new File(dataFolder, "users.yml"), getLogger());
        this.userStore.load();

        this.damageTracker = new DamageTracker(settings);

        Bukkit.getPluginManager().registerEvents(this.damageTracker, this);
        Bukkit.getPluginManager().registerEvents(new InfoMenuListener(this), this);

        PluginCommand command = getCommand("infomenu");
        if (command != null) {
            InfoMenuCommand executor = new InfoMenuCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("Команда /infomenu не описана в plugin.yml — команда не зарегистрирована.");
        }

        startTask();

        if (!settings.isEnabled()) {
            getLogger().warning("Плагин выключен в config.yml (enabled: false). Включите его или поставьте true.");
        }
        getLogger().info("InfoMenu включён. Способ вывода по умолчанию: " + settings.getDefaultMode().name()
                + ", обновление каждые " + settings.getRefreshInterval() + " тиков.");
    }

    @Override
    public void onDisable() {
        stopTask();
        if (userStore != null) {
            userStore.save();
        }
    }

    /** Запускает задачу показа здоровья с интервалом из конфига. */
    private void startTask() {
        stopTask();
        if (!settings.isEnabled()) {
            return;
        }
        this.displayTask = new HealthDisplayTask(this);
        int interval = settings.getRefreshInterval();
        this.displayTask.runTaskTimer(this, interval, interval);
    }

    private void stopTask() {
        if (displayTask != null) {
            displayTask.cancel();
            displayTask.removeAllBars();
            displayTask = null;
        }
    }

    /** Перезагрузка конфига по команде /infomenu reload. */
    public void reloadPlugin() {
        reloadConfig();
        settings.load(getConfig());
        damageTracker.clear();
        startTask();
    }

    public PluginSettings getSettings() {
        return settings;
    }

    public UserStore getUserStore() {
        return userStore;
    }

    public DamageTracker getDamageTracker() {
        return damageTracker;
    }

    public HealthDisplayTask getDisplayTask() {
        return displayTask;
    }
}
