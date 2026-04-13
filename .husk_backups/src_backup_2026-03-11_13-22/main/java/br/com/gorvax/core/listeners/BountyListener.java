package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.BountyManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * B17.3 — Resolve bounties quando um jogador com bounty é morto por outro jogador.
 */
public class BountyListener implements Listener {

    private final GorvaxCore plugin;

    public BountyListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Apenas kills PvP
        if (killer == null) return;
        if (killer.equals(victim)) return;

        BountyManager bm = plugin.getBountyManager();
        if (bm == null || !bm.isEnabled()) return;

        // Verificar se a vítima tem bounty
        double bountyValue = bm.resolveBounty(victim.getUniqueId());
        if (bountyValue <= 0) return;

        // Pagar ao assassino
        Economy econ = GorvaxCore.getEconomy();
        econ.depositPlayer(killer, bountyValue);

        var msg = plugin.getMessageManager();

        // Notificar assassino
        msg.send(killer, "bounty.collected", victim.getName(), String.format("%.2f", bountyValue));
        killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Notificar vítima
        msg.send(victim, "bounty.head_claimed", killer.getName(), String.format("%.2f", bountyValue));

        // Broadcast global
        msg.broadcast("bounty.resolved_broadcast",
                killer.getName(), victim.getName(), String.format("%.2f", bountyValue));

        // Log de auditoria
        if (plugin.getAuditManager() != null) {
            plugin.getAuditManager().log(
                    br.com.gorvax.core.managers.AuditManager.AuditAction.MARKET_SELL,
                    killer.getUniqueId(), killer.getName(),
                    "Bounty resolvida: " + victim.getName() + " ($" + String.format("%.2f", bountyValue) + ")",
                    bountyValue);
        }
    }
}
