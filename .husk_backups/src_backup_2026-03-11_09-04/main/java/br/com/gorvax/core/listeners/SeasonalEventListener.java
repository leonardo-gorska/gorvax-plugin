package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.SeasonalEventManager;
import br.com.gorvax.core.managers.SeasonalEventManager.SeasonalEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * B17 — Listener de Eventos Sazonais.
 * Notifica jogadores sobre o evento ativo ao logar.
 */
public class SeasonalEventListener implements Listener {

    private final GorvaxCore plugin;

    public SeasonalEventListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SeasonalEventManager sem = plugin.getSeasonalEventManager();
        if (sem == null || !sem.isEventActive())
            return;

        SeasonalEvent activeEvent = sem.getActiveEvent();
        int daysRemaining = sem.getDaysRemaining();

        // Notificar com um pequeno atraso para não sobrecarregar o login
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline())
                return;

            plugin.getMessageManager().send(player, "seasonal_event.login_notify",
                    activeEvent.name(), String.valueOf(daysRemaining));

            // Mostrar multiplicadores ativos
            StringBuilder mults = new StringBuilder();
            if (activeEvent.xpMultiplier() != 1.0)
                mults.append("§bXP: ").append(String.format("%.1fx", activeEvent.xpMultiplier())).append("  ");
            if (activeEvent.lootMultiplier() != 1.0)
                mults.append("§dLoot: ").append(String.format("%.1fx", activeEvent.lootMultiplier())).append("  ");
            if (activeEvent.moneyMultiplier() != 1.0)
                mults.append("§aMoney: ").append(String.format("%.1fx", activeEvent.moneyMultiplier())).append("  ");

            if (!mults.isEmpty()) {
                plugin.getMessageManager().send(player, "seasonal_event.login_multipliers",
                        mults.toString().trim());
            }
        }, 60L); // 3 segundos depois do login
    }
}
