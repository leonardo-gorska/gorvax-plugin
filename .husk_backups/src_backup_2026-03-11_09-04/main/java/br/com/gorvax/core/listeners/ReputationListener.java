package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.ReputationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * B18 — Listener que aplica mudanças de karma baseado em ações dos jogadores.
 * - Kill PvP: -killPenalty karma
 * - Boss kill: integrado via BossManager (chamada direta ao ReputationManager)
 * - Venda no mercado: integrado via MarketManager
 * - Quest completada: integrado via QuestManager
 */
public class ReputationListener implements Listener {

    private final GorvaxCore plugin;

    public ReputationListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Ao matar outro jogador em PvP, o killer perde karma.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKill(PlayerDeathEvent event) {
        ReputationManager rep = plugin.getReputationManager();
        if (rep == null || !rep.isEnabled())
            return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.equals(victim))
            return;

        // Killer perde karma por matar jogador
        rep.modifyKarma(killer, -rep.getKillPenalty(), "matar jogador");

        // Vítima não ganha/perde karma ao morrer por PvP
    }
}
