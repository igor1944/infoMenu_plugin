package com.infoMenu.menu;

import com.infoMenu.InfoMenu;
import com.infoMenu.config.PluginSettings;
import com.infoMenu.display.DisplayMode;
import com.infoMenu.user.UserSettings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Arrays;
import java.util.Collections;

/**
 * Меню настроек плагина. Открывается командой {@code /infomenu}.
 * Реализует {@link InventoryHolder}, чтобы клики определялись по владельцу инвентаря,
 * а не по совпадению заголовка.
 */
public final class SettingsMenu implements InventoryHolder {

    public static final int SLOT_INFO = 4;
    public static final int SLOT_DISPLAY = 10;
    public static final int SLOT_MODE = 11;
    public static final int SLOT_HEARTS = 12;
    public static final int SLOT_DAMAGE = 13;
    public static final int SLOT_DISTANCE = 14;
    public static final int SLOT_TARGETS = 15;
    public static final int SLOT_RESET = 22;
    public static final int SLOT_CLOSE = 26;

    /** Возможные значения дистанции, между которыми переключается пункт меню. */
    private static final int[] DISTANCES = {8, 16, 32, 48, 64};

    private final InfoMenu plugin;
    private final Player owner;
    private final Inventory inventory;

    public SettingsMenu(InfoMenu plugin, Player owner) {
        this.plugin = plugin;
        this.owner = owner;
        this.inventory = Bukkit.createInventory(this, 27,
                com.infoMenu.util.Text.component(plugin.getSettings().getMenuTitle()));
        refresh();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getOwner() {
        return owner;
    }

    /** Перерисовывает меню после изменения настроек. */
    public void refresh() {
        PluginSettings config = plugin.getSettings();
        UserSettings user = plugin.getUserStore().get(owner.getUniqueId());

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, MenuItems.filler());
        }

        inventory.setItem(SLOT_INFO, MenuItems.head(owner, "&b&lInfoMenu", config.getMenuInfoLore()));

        inventory.setItem(SLOT_DISPLAY, MenuItems.toggle(Material.LIME_DYE, "&a&lПоказ здоровья",
                user.isDisplayEnabled(),
                Arrays.asList("&7Показывать здоровье цели,", "&7на которую вы смотрите.")));

        inventory.setItem(SLOT_MODE, MenuItems.item(Material.CLOCK, "&e&lСпособ вывода",
                Arrays.asList(
                        "&7Текущий: &f" + user.getMode().getLabel(),
                        "",
                        "&7ACTION_BAR — строка над хотбаром",
                        "&7BOSS_BAR — полоса сверху экрана",
                        "&7NONE — ничего не показывать",
                        "",
                        "&7Левый клик — следующий вариант")));

        inventory.setItem(SLOT_HEARTS, MenuItems.toggle(Material.RED_DYE, "&c&lПолоска сердец",
                user.isHeartsEnabled(),
                Arrays.asList("&7Добавляет в строку сердца,", "&7как на экране здоровья.")));

        boolean damageAllowed = config.isDamageAllowed() && user.isDamageEnabled();
        inventory.setItem(SLOT_DAMAGE, MenuItems.toggle(Material.IRON_SWORD, "&f&lНанесённый урон",
                damageAllowed,
                Arrays.asList(
                        "&7Показывать, сколько урона вы нанесли",
                        "&7цели за последние &f"
                                + (config.getDamageMemoryMillis() / 1000L) + " &7сек.",
                        config.isDamageAllowed()
                                ? "&7Левый клик — изменить"
                                : "&cУчёт урона выключен в config.yml")));

        inventory.setItem(SLOT_DISTANCE, MenuItems.item(Material.ENDER_EYE,
                "&d&lДальность: &f" + user.getMaxDistance() + " &7блоков",
                Arrays.asList(
                        "&7Как далеко можно смотреть,",
                        "&7чтобы видеть здоровье цели.",
                        "",
                        "&7Левый клик — увеличить")));

        inventory.setItem(SLOT_TARGETS, MenuItems.toggle(Material.ZOMBIE_HEAD, "&6&lТолько игроки",
                user.isPlayersOnly(),
                Arrays.asList(
                        "&7Включено — показывать только игроков.",
                        "&7Выключено — показывать и мобов.")));

        inventory.setItem(SLOT_RESET, MenuItems.item(Material.NETHER_STAR, "&b&lСбросить настройки",
                Arrays.asList(
                        "&7Вернуть значения из config.yml.",
                        "",
                        "&7Левый клик — сбросить")));

        inventory.setItem(SLOT_CLOSE, MenuItems.item(Material.BARRIER, "&c&lЗакрыть",
                Collections.singletonList("&7Закрыть меню")));
    }

    /** Переключает режим вывода и возвращает новый. */
    public DisplayMode cycleMode() {
        UserSettings user = plugin.getUserStore().get(owner.getUniqueId());
        user.setMode(user.getMode().next());
        return user.getMode();
    }

    /** Переключает дальность и возвращает новую. */
    public int cycleDistance() {
        UserSettings user = plugin.getUserStore().get(owner.getUniqueId());
        int current = user.getMaxDistance();
        int next = DISTANCES[DISTANCES.length - 1];
        for (int distance : DISTANCES) {
            if (distance > current) {
                next = distance;
                break;
            }
        }
        user.setMaxDistance(next);
        return next;
    }
}
