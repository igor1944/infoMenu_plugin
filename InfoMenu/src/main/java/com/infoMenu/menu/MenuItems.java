package com.infoMenu.menu;

import com.infoMenu.util.Text;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Помощник для сборки предметов в меню настроек.
 */
public final class MenuItems {

    private MenuItems() {
    }

    public static ItemStack item(Material material, String name, List<String> lore) {
        return item(material, 1, name, lore);
    }

    public static ItemStack item(Material material, int amount, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.component(name));
            List<String> lines = lore == null ? new ArrayList<>() : lore;
            meta.lore(Text.components(lines));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Предмет, чей лор заканчивается подсказкой «ЛКМ — изменить». */
    public static ItemStack toggle(Material material, String name, boolean value, List<String> description) {
        List<String> lore = new ArrayList<>(description);
        lore.add("");
        lore.add(value ? "&a✔ Включено" : "&c✖ Выключено");
        lore.add("&7Левый клик — изменить");
        return item(material, name, lore);
    }

    public static ItemStack head(OfflinePlayer player, String name, List<String> lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta skull = (SkullMeta) meta;
            skull.setOwningPlayer(player);
            skull.displayName(Text.component(name));
            skull.lore(Text.components(lore));
            stack.setItemMeta(skull);
        }
        return stack;
    }

    public static ItemStack filler() {
        return item(Material.GRAY_STAINED_GLASS_PANE, "&7", Collections.emptyList());
    }
}
