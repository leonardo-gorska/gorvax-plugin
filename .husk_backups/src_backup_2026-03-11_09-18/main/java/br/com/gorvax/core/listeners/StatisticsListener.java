package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B3 — Listener de estatísticas do jogador.
 * Rastreia blocos, kills, mortes e tempo de jogo automaticamente.
 * Os dados são modificados em memória; o PlayerDataManager salva via dirty flag async.
 */
public class StatisticsListener implements Listener {

    private final GorvaxCore plugin;
    // Mapa de sessão: armazena o timestamp de login para calcular playtime no quit
    private final Map<UUID, Long> sessionStartMap = new ConcurrentHashMap<>();

    public StatisticsListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        PlayerData pd = plugin.getPlayerDataManager().getData(event.getPlayer().getUniqueId());
        pd.incrementBlocksBroken();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        PlayerData pd = plugin.getPlayerDataManager().getData(event.getPlayer().getUniqueId());
        pd.incrementBlocksPlaced();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        PlayerData victimData = plugin.getPlayerDataManager().getData(victim.getUniqueId());
        victimData.incrementDeaths();

        // Se foi um PvP kill, incrementar kills do matador
        Player killer = victim.getKiller();
        if (killer != null) {
            PlayerData killerData = plugin.getPlayerDataManager().getData(killer.getUniqueId());
            killerData.incrementKills();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);
        pd.setLastLogin(now);

        // Primeiro login: registrar data
        if (pd.getFirstJoin() == 0L) {
            pd.setFirstJoin(now);
        }

        // Iniciar tracking de sessão para playtime
        sessionStartMap.put(uuid, now);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Long sessionStart = sessionStartMap.remove(uuid);

        if (sessionStart != null) {
            long sessionDuration = System.currentTimeMillis() - sessionStart;
            if (sessionDuration > 0) {
                PlayerData pd = plugin.getPlayerDataManager().getData(uuid);
                pd.addPlayTime(sessionDuration);
                // Marcar dados como dirty para persistência
                plugin.getPlayerDataManager().saveData(uuid);
            }
        }
    }
}
