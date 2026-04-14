package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * B6 — Listener para criação/remoção da scoreboard no join/quit.
 */
public class ScoreboardListener implements Listener {

    private final GorvaxCore plugin;

    public ScoreboardListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getScoreboardManager() == null) return;

        // Delay de 1 segundo para garantir que o jogador já carregou seus dados
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                plugin.getScoreboardManager().createScoreboard(event.getPlayer());
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getScoreboardManager() == null) return;
        plugin.getScoreboardManager().cleanup(event.getPlayer());
    }
}
