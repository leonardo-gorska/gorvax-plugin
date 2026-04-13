package br.com.gorvax.core.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.events.BossDeathEvent;
import br.com.gorvax.core.managers.CodexManager;

import java.util.Map;
import java.util.UUID;

/**
 * B28 — Listener de triggers automáticos do Códex.
 * Escuta eventos do jogo e desbloqueia entradas automaticamente.
 */
public class CodexListener implements Listener {

    private final GorvaxCore plugin;

    public CodexListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    // ==================== Boss Kill ====================

    /**
     * Ao derrotar um boss, desbloqueia a entrada do bestiário
     * para todos os participantes da luta (topDamagers).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossKill(BossDeathEvent event) {
        CodexManager mgr = plugin.getCodexManager();
        String bossId = event.getBossId();

        // Desbloqueia para todos os jogadores que participaram (topDamagers)
        for (Map.Entry<UUID, Double> damager : event.getTopDamagers()) {
            Player player = Bukkit.getPlayer(damager.getKey());
            if (player != null && player.isOnline()) {
                mgr.tryUnlockByTrigger(player, "BOSS_KILL", bossId);
            }
        }
    }

    // ==================== Outros Triggers ====================
    // BOOK_READ → chamado via CodexManager.tryUnlockByTrigger() no LoreListener
    // ITEM_OBTAIN → chamado via CodexManager.tryUnlockByTrigger() no
    // CustomItemListener
    // LOCATION_VISIT → chamado via CodexManager.tryUnlockByTrigger() no
    // StructureListener
}
