package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * B5 — GUI de recompensas diárias (27 slots / 3 linhas).
 * Mostra o progresso de 7 dias com itens coloridos por estado.
 */
public class DailyRewardGUI implements Listener {

    private final GorvaxCore plugin;
    private final DailyRewardManager rewardManager;

    public DailyRewardGUI(GorvaxCore plugin, DailyRewardManager rewardManager) {
        this.plugin = plugin;
        this.rewardManager = rewardManager;
    }

    /**
     * Abre a GUI de recompensas diárias para o jogador.
     */
    public void open(Player player) {
        MessageManager msg = plugin.getMessageManager();
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        long now = System.currentTimeMillis();
        long lastReward = pd.getLastDailyReward();
        long hoursSinceReward = (lastReward > 0) ? (now - lastReward) / (1000 * 60 * 60) : Long.MAX_VALUE;
        int minHours = plugin.getConfig().getInt("daily_rewards.min_hours", 20);
        int maxHours = plugin.getConfig().getInt("daily_rewards.max_hours", 48);

        // Se passou mais de maxHours desde o último resgate, streak já foi resetada
        int currentStreak = pd.getLoginStreak();
        if (lastReward > 0 && hoursSinceReward > maxHours) {
            currentStreak = 0;
        }

        boolean canClaim = hoursSinceReward >= minHours;

        String guiTitle = msg.get("daily.gui_title");
        Inventory inv = Bukkit.createInventory(new DailyRewardHolder(), 27,
                LegacyComponentSerializer.legacySection().deserialize(guiTitle));

        // Preencher bordas com vidro preto
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Slots 10-16: os 7 dias (centralizado na segunda linha)
        for (int day = 1; day <= 7; day++) {
            int slot = 9 + day; // Slots 10, 11, 12, 13, 14, 15, 16
            String rewardDesc = rewardManager.getRewardDescription(day);

            if (day <= currentStreak) {
                // Dia já resgatado
                String name = msg.get("daily.gui_completed", String.valueOf(day));
                String lore = msg.get("daily.gui_reward_lore", rewardDesc);
                inv.setItem(slot, createLoreItem(Material.LIME_STAINED_GLASS_PANE, name, lore));

            } else if (day == currentStreak + 1 && canClaim) {
                // Dia atual — disponível para resgate
                String name = msg.get("daily.gui_current", String.valueOf(day));
                String lore = msg.get("daily.gui_reward_lore", rewardDesc);
                ItemStack item = createLoreItem(Material.CHEST, name, lore);

                // Adicionar enchant glow
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }

                inv.setItem(slot, item);

            } else {
                // Dia futuro
                String name = msg.get("daily.gui_future", String.valueOf(day));
                String lore = msg.get("daily.gui_reward_lore", rewardDesc);
                inv.setItem(slot, createLoreItem(Material.GRAY_STAINED_GLASS_PANE, name, lore));
            }
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // --- Listener para cliques na GUI ---

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DailyRewardHolder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Só o baú (CHEST) é clicável — é o dia atual disponível
        if (clicked.getType() == Material.CHEST) {
            player.closeInventory();
            rewardManager.claimReward(player);
        }
    }

    // --- Utilitários ---

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * B39 — Cria item com suporte a lore multi-line (split por \n).
     */
    private ItemStack createLoreItem(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLine.split("\n")) {
                lore.add(LegacyComponentSerializer.legacySection().deserialize(line));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * InventoryHolder customizado para identificar esta GUI nos eventos de clique.
     */
    public static class DailyRewardHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null; // Não usado — apenas marcador
        }
    }
}
