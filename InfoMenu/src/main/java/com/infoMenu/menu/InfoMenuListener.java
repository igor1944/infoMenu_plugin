package com.infoMenu.menu;

import com.infoMenu.InfoMenu;
import com.infoMenu.user.UserSettings;
import com.infoMenu.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Обработка кликов в меню настроек и уборка за вышедшими игроками.
 */
public final class InfoMenuListener implements Listener {

    private final InfoMenu plugin;

    public InfoMenuListener(InfoMenu plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Босс-бар и личные настройки больше не нужны в памяти.
        if (plugin.getDisplayTask() != null) {
            plugin.getDisplayTask().removeBar(event.getPlayer().getUniqueId());
        }
        plugin.getUserStore().forget(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SettingsMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof SettingsMenu)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        SettingsMenu menu = (SettingsMenu) holder;

        if (!player.getUniqueId().equals(menu.getOwner().getUniqueId())) {
            player.closeInventory();
            return;
        }
        // Клики по собственному инвентарю (или мимо окна) настройки не меняют.
        if (event.getClickedInventory() != event.getInventory() || event.getCurrentItem() == null) {
            return;
        }

        UserSettings user = plugin.getUserStore().get(player.getUniqueId());

        switch (event.getSlot()) {
            case SettingsMenu.SLOT_DISPLAY:
                user.setDisplayEnabled(!user.isDisplayEnabled());
                player.sendMessage(Text.component(prefix()
                        + (user.isDisplayEnabled()
                        ? plugin.getSettings().getMsgDisplayOn()
                        : plugin.getSettings().getMsgDisplayOff())));
                break;
            case SettingsMenu.SLOT_MODE:
                menu.cycleMode();
                break;
            case SettingsMenu.SLOT_HEARTS:
                user.setHeartsEnabled(!user.isHeartsEnabled());
                break;
            case SettingsMenu.SLOT_DAMAGE:
                if (!plugin.getSettings().isDamageAllowed()) {
                    player.sendMessage(Text.component(prefix() + "&cУчёт урона выключен в config.yml."));
                } else {
                    user.setDamageEnabled(!user.isDamageEnabled());
                }
                break;
            case SettingsMenu.SLOT_DISTANCE:
                menu.cycleDistance();
                break;
            case SettingsMenu.SLOT_TARGETS:
                user.setPlayersOnly(!user.isPlayersOnly());
                break;
            case SettingsMenu.SLOT_RESET:
                plugin.getUserStore().reset(player.getUniqueId());
                player.sendMessage(Text.component(prefix() + "&aНастройки сброшены к значениям по умолчанию."));
                break;
            case SettingsMenu.SLOT_CLOSE:
                player.closeInventory();
                return;
            default:
                return;
        }

        plugin.getUserStore().touch(player.getUniqueId());
        menu.refresh();
    }

    private String prefix() {
        String prefix = plugin.getSettings().getPrefix();
        return prefix == null ? "" : prefix;
    }
}
