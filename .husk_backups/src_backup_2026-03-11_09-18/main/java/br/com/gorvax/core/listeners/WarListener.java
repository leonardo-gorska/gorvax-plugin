package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.towns.managers.KingdomManager;
import br.com.gorvax.core.towns.managers.WarManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * B15 — Listener para eventos de combate durante guerras ativas.
 * Registra kills como pontos no sistema de guerra.
 */
public class WarListener implements Listener {

    private final GorvaxCore plugin;

    public WarListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Intercepta mortes de jogadores para registrar pontos de guerra.
     * Se a vítima e o killer pertencem a reinos em guerra ativa, registra ponto para o killer.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();

        // Sem killer (morte natural/mob) — ignorar
        if (killer == null) return;

        KingdomManager km = plugin.getKingdomManager();
        WarManager wm = plugin.getWarManager();
        if (wm == null) return;

        // Identificar reinos de ambos
        Claim victimKingdom = km.getKingdom(victim.getUniqueId());
        Claim killerKingdom = km.getKingdom(killer.getUniqueId());

        if (victimKingdom == null || killerKingdom == null) return;
        if (victimKingdom.getId().equals(killerKingdom.getId())) return; // Mesmo reino

        // Verificar se estão em guerra ativa
        WarManager.War war = wm.getWarBetween(victimKingdom.getId(), killerKingdom.getId());
        if (war == null || war.getState() != WarManager.WarState.ACTIVE) return;

        // Registrar kill
        wm.addKill(killerKingdom.getId(), victimKingdom.getId());

        // Broadcast do kill
        var msg = plugin.getMessageManager();
        String killerKingdomName = km.getNome(killerKingdom.getId());
        int pointsGained = plugin.getConfig().getInt("war.points_per_kill", 1);

        msg.broadcast("war.kill_point",
                killer.getName(),
                victim.getName(),
                String.valueOf(pointsGained),
                killerKingdomName);
    }
}
