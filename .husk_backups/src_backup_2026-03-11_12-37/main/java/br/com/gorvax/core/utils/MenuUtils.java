package br.com.gorvax.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitários compartilhados para criação de itens de menu.
 * Elimina duplicação entre MainMenuGUI e TeleportHubGUI.
 */
public final class MenuUtils {

    private MenuUtils() {
        // Utility class
    }

    /**
     * Cria um item de menu simples (sem lore).
     */
    public static ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * B39 — Cria item de menu com suporte a lore multi-line.
     * Separa linhas usando '\n' na string de lore do messages.yml.
     */
    public static ItemStack createMenuItem(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            List<Component> lore = new ArrayList<>();
            // B39 — Suporte multi-line: split por \n
            for (String line : loreLine.split("\n")) {
                lore.add(LegacyComponentSerializer.legacySection().deserialize(line));
            }
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}
