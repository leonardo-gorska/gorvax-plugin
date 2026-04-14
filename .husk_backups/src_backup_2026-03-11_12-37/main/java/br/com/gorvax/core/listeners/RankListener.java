package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.RankManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryView;

/**
 * Listener para:
 * - Verificar rank ao logar
 * - Tratar cliques na GUI de ranks e kits (rank + VIP)
 */
public class RankListener implements Listener {

    private final GorvaxCore plugin;

    public RankListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Ao logar, verificar se o jogador pode subir de rank.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        RankManager rm = plugin.getRankManager();
        if (rm == null || !rm.isEnabled())
            return;

        // Verificar após 5 segundos (dar tempo de carregar dados)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            rm.checkAndPromote(player);
        }, 100L); // 5 seg
    }

    /**
     * Tratar cliques nas GUIs de rank e kit.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        InventoryView view = event.getView();
        String title = LegacyComponentSerializer.legacySection().serialize(view.title());

        // GUI de Progressão de Ranks
        if (title.contains("Progressão de Ranks")) {
            event.setCancelled(true);

            int slot = event.getRawSlot();
            RankManager rm = plugin.getRankManager();
            if (rm == null)
                return;

            // Slots de kit de rank: 37, 39, 41, 43
            RankManager.GameRank[] ranks = RankManager.GameRank.values();
            int[] kitSlots = { 37, 39, 41, 43 };
            for (int i = 0; i < kitSlots.length && i < ranks.length; i++) {
                if (slot == kitSlots[i]) {
                    rm.giveKit(player, ranks[i]);
                    player.closeInventory();
                    return;
                }
            }
            return;
        }

        // GUI de Kits (rank + VIP unificado)
        if (title.contains("Seus Kits")) {
            event.setCancelled(true);

            int slot = event.getRawSlot();
            RankManager rm = plugin.getRankManager();
            if (rm == null)
                return;

            // Rank kit slots: 10, 12, 14, 16 (linha 2)
            RankManager.GameRank[] ranks = RankManager.GameRank.values();
            int[] rankSlots = { 10, 12, 14, 16 };
            for (int i = 0; i < rankSlots.length && i < ranks.length; i++) {
                if (slot == rankSlots[i]) {
                    rm.giveKit(player, ranks[i]);
                    player.closeInventory();
                    return;
                }
            }

            // VIP kit slots: 29, 31, 33 (linha 4)
            String[] vipTiers = rm.getVipTiers();
            int[] vipSlots = { 29, 31, 33 };
            for (int i = 0; i < vipSlots.length && i < vipTiers.length; i++) {
                if (slot == vipSlots[i]) {
                    rm.giveVipKit(player, vipTiers[i]);
                    player.closeInventory();
                    return;
                }
            }
        }
    }
}
