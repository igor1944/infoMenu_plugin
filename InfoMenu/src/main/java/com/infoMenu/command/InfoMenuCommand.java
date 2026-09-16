package com.infoMenu.command;

import com.infoMenu.InfoMenu;
import com.infoMenu.display.HealthDisplayTask;
import com.infoMenu.display.HealthFormatter;
import com.infoMenu.menu.SettingsMenu;
import com.infoMenu.user.UserSettings;
import com.infoMenu.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Команда /infomenu:
 * <ul>
 *   <li>без аргументов — открывает меню настроек;</li>
 *   <li>toggle — быстро включить/выключить показ здоровья;</li>
 *   <li>health &lt;игрок&gt; — посмотреть здоровье игрока;</li>
 *   <li>reset — сбросить свои настройки;</li>
 *   <li>reload — перезагрузить конфиг (только для администрации).</li>
 * </ul>
 */
public final class InfoMenuCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("toggle", "health", "reset", "reload");

    private final InfoMenu plugin;

    public InfoMenuCommand(InfoMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Text.component(prefix()
                        + "&cИз консоли меню недоступно. Используйте &f/infomenu health <игрок>&c."));
                return true;
            }
            openMenu((Player) sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload":
                reload(sender);
                return true;
            case "health":
            case "hp":
                health(sender, args);
                return true;
            case "toggle":
                toggle(sender);
                return true;
            case "reset":
                reset(sender);
                return true;
            default:
                help(sender);
                return true;
        }
    }

    private void openMenu(Player player) {
        if (!player.hasPermission("infomenu.use")) {
            player.sendMessage(Text.component(prefix() + plugin.getSettings().getMsgNoPermission()));
            return;
        }
        plugin.getUserStore().touch(player.getUniqueId());
        SettingsMenu menu = new SettingsMenu(plugin, player);
        player.openInventory(menu.getInventory());
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("infomenu.admin")) {
            sender.sendMessage(Text.component(prefix() + plugin.getSettings().getMsgNoPermission()));
            return;
        }
        plugin.reloadPlugin();
        sender.sendMessage(Text.component(prefix() + plugin.getSettings().getMsgReloaded()));
    }

    private void health(CommandSender sender, String[] args) {
        if (!sender.hasPermission("infomenu.use")) {
            sender.sendMessage(Text.component(prefix() + plugin.getSettings().getMsgNoPermission()));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Text.component(prefix() + "&7Использование: &f/infomenu health <игрок>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Text.component(prefix() + plugin.getSettings().getMsgPlayerNotFound()
                    .replace("%player%", args[1])));
            return;
        }
        double health = Math.max(0.0, target.getHealth());
        double maxHealth = HealthDisplayTask.maxHealth(target);
        sender.sendMessage(Text.component(prefix()
                + new HealthFormatter(plugin.getSettings()).buildReport(target, health, maxHealth)));
    }

    private void toggle(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Text.component(prefix() + "&cЭта подкоманда доступна только игрокам."));
            return;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("infomenu.use")) {
            player.sendMessage(Text.component(prefix() + plugin.getSettings().getMsgNoPermission()));
            return;
        }
        UserSettings user = plugin.getUserStore().get(player.getUniqueId());
        user.setDisplayEnabled(!user.isDisplayEnabled());
        player.sendMessage(Text.component(prefix()
                + (user.isDisplayEnabled()
                ? plugin.getSettings().getMsgDisplayOn()
                : plugin.getSettings().getMsgDisplayOff())));
    }

    private void reset(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Text.component(prefix() + "&cЭта подкоманда доступна только игрокам."));
            return;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("infomenu.use")) {
            player.sendMessage(Text.component(prefix() + plugin.getSettings().getMsgNoPermission()));
            return;
        }
        plugin.getUserStore().reset(player.getUniqueId());
        player.sendMessage(Text.component(prefix() + "&aНастройки сброшены к значениям по умолчанию."));
    }

    private void help(CommandSender sender) {
        sender.sendMessage(Text.component(prefix() + "&7Неизвестная подкоманда. Доступно:"));
        sender.sendMessage(Text.component("&f/infomenu &7— открыть меню настроек"));
        sender.sendMessage(Text.component("&f/infomenu toggle &7— включить/выключить показ здоровья"));
        sender.sendMessage(Text.component("&f/infomenu health <игрок> &7— здоровье игрока"));
        sender.sendMessage(Text.component("&f/infomenu reset &7— сбросить свои настройки"));
        if (sender.hasPermission("infomenu.admin")) {
            sender.sendMessage(Text.component("&f/infomenu reload &7— перезагрузить конфиг"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String token = args[0].toLowerCase(Locale.ROOT);
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(token)) {
                    if (sub.equals("reload") && !sender.hasPermission("infomenu.admin")) {
                        continue;
                    }
                    result.add(sub);
                }
            }
            return result;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("health") || args[0].equalsIgnoreCase("hp"))) {
            String token = args[1].toLowerCase(Locale.ROOT);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(token)) {
                    result.add(online.getName());
                }
            }
        }
        return result;
    }

    private String prefix() {
        String prefix = plugin.getSettings().getPrefix();
        return prefix == null ? "" : prefix;
    }
}
