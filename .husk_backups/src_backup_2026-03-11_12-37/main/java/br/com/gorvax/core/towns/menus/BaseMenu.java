package br.com.gorvax.core.towns.menus;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.InputManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Classe base para todos os menus do sistema de reinos.
 * Contém helpers compartilhados para criação de itens, skulls e bordas.
 * B4.2 — Suporte a menus adaptativos para Bedrock (telas menores).
 */
public abstract class BaseMenu {

    protected final GorvaxCore plugin;

    protected BaseMenu(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * B4.2 — Retorna o tamanho do menu adaptado à plataforma do jogador.
     * Bedrock recebe menus menores (27 slots / 3 rows) para telas pequenas.
     *
     * @param player      Jogador que vai abrir o menu
     * @param javaSize    Tamanho para Java Edition (ex: 54)
     * @param bedrockSize Tamanho para Bedrock (ex: 27)
     * @return Tamanho adequado
     */
    protected int getMenuSize(Player player, int javaSize, int bedrockSize) {
        if (InputManager.isBedrockPlayer(player)) {
            return bedrockSize;
        }
        return javaSize;
    }

    /**
     * B4.2 — Retorna os slots de conteúdo (excluindo bordas) para um menu.
     * Para 27 slots: slots 10-16 (7 slots centrais)
     * Para 54 slots: slots centrais das 4 linhas do meio (28 slots)
     */
    protected int[] getContentSlots(int menuSize) {
        if (menuSize <= 27) {
            return new int[] { 10, 11, 12, 13, 14, 15, 16 };
        }
        // 54 slots: 4 linhas centrais
        return new int[] {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
    }

    /**
     * B4.2 — Verifica se o jogador é Bedrock.
     */
    protected boolean isBedrock(Player player) {
        return InputManager.isBedrockPlayer(player);
    }

    /**
     * Obtém o nome de um jogador pelo UUID.
     */
    protected String getPlayerName(UUID uuid) {
        return plugin.getPlayerName(uuid);
    }

    /**
     * Cria um ItemStack com nome, lore e metadados PDC para ação/dados.
     */
    protected ItemStack createItem(Material mat, String name, String action, String data, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines)
                lore.add(LegacyComponentSerializer.legacySection().deserialize(line));
            meta.lore(lore);

            // Esconde todos os tooltips vanilla (dano, armadura, encantamentos, etc.)
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_DYE, ItemFlag.HIDE_ARMOR_TRIM);

            if (action != null) {
                NamespacedKey key = new NamespacedKey(plugin, "gorvax_action");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
            }
            if (data != null) {
                NamespacedKey key = new NamespacedKey(plugin, "gorvax_data");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, data);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Cria uma cabeça de jogador (skull) com PDC para ação/dados.
     */
    protected ItemStack createSkull(UUID owner, String name, String action, String data, String... loreLines) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines)
                lore.add(LegacyComponentSerializer.legacySection().deserialize(line));
            meta.lore(lore);

            // Esconde todos os tooltips vanilla
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_DYE);

            if (action != null) {
                NamespacedKey key = new NamespacedKey(plugin, "gorvax_action");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
            }
            if (data != null) {
                NamespacedKey key = new NamespacedKey(plugin, "gorvax_data");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, data);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Preenche as bordas do inventário com painéis de vidro cinza.
     */
    protected void fillBorders(Inventory gui, String kingdomId) {
        int size = gui.getSize();
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", "MENU_BORDER", kingdomId);

        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, glass);
            }
        }
    }

    /**
     * Holder customizado para inventários do sistema de reinos.
     * Identifica o reino, o tipo de menu e a página atual.
     */
    public static class KingdomHolder implements org.bukkit.inventory.InventoryHolder {
        private final String kingdomId;
        private final String menuType;
        private int page = 0;

        public KingdomHolder(String kingdomId, String menuType) {
            this.kingdomId = kingdomId;
            this.menuType = menuType;
        }

        public KingdomHolder(String kingdomId, String menuType, int page) {
            this.kingdomId = kingdomId;
            this.menuType = menuType;
            this.page = page;
        }

        public String getKingdomId() {
            return kingdomId;
        }

        public String getMenuType() {
            return menuType;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
