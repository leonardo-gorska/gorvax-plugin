package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.utils.BlueMapHook;
import br.com.gorvax.core.utils.DynmapHook;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * B16 — Gerenciador de integração com mapas web (Dynmap / BlueMap).
 * Auto-detecta qual plugin de mapa está disponível e cria marcadores
 * para reinos, outposts e claims no mapa web.
 */
public class WebMapManager {

    private final GorvaxCore plugin;
    private DynmapHook dynmapHook;
    private BlueMapHook blueMapHook;
    private BukkitTask updateTask;
    private boolean dynmapActive = false;
    private boolean blueMapActive = false;

    public WebMapManager(GorvaxCore plugin) {
        this.plugin = plugin;

        if (!plugin.getConfig().getBoolean("webmap.enabled", true)) {
            plugin.getLogger().info("[GorvaxCore] Integração com mapa web desativada no config.yml.");
            return;
        }

        // Tentar Dynmap primeiro
        if (Bukkit.getPluginManager().getPlugin("dynmap") != null) {
            try {
                this.dynmapHook = new DynmapHook(plugin.getLogger());
                if (dynmapHook.isAvailable()) {
                    dynmapActive = true;
                    plugin.getLogger().info("[GorvaxCore] Dynmap detectado! Reinos visíveis no mapa web.");
                }
            } catch (Throwable e) {
                plugin.getLogger().warning("[GorvaxCore] Falha ao conectar com Dynmap: " + e.getMessage());
            }
        }

        // Tentar BlueMap se Dynmap não estiver disponível
        if (!dynmapActive) {
            try {
                Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
                this.blueMapHook = new BlueMapHook(plugin.getLogger());
                // BlueMap usa callback assíncrono, então pode ficar disponível depois
                blueMapActive = true;
                plugin.getLogger().info("[GorvaxCore] BlueMap detectado! Reinos serão visíveis no mapa web quando pronto.");
            } catch (ClassNotFoundException ignored) {
                // BlueMap não disponível
            } catch (Throwable e) {
                plugin.getLogger().warning("[GorvaxCore] Falha ao conectar com BlueMap: " + e.getMessage());
            }
        }

        if (!dynmapActive && !blueMapActive) {
            plugin.getLogger().info("[GorvaxCore] Nenhum plugin de mapa web encontrado. Integração desativada.");
            return;
        }

        // Iniciar task periódica de atualização
        long interval = plugin.getConfig().getLong("webmap.update_interval", 1200L);
        this.updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateMarkers, 100L, interval);
    }

    /**
     * Atualiza os marcadores no mapa web com os claims atuais.
     * Chamado periodicamente pela task e sob demanda (criar/deletar claims).
     */
    public void updateMarkers() {
        List<Claim> claims = plugin.getClaimManager().getClaims();
        var km = plugin.getKingdomManager();

        if (dynmapActive && dynmapHook != null && dynmapHook.isAvailable()) {
            dynmapHook.updateMarkers(claims, km);
        }
        if (blueMapActive && blueMapHook != null && blueMapHook.isAvailable()) {
            blueMapHook.updateMarkers(claims, km);
        }
    }

    /**
     * Força uma atualização imediata dos marcadores.
     * Chamado quando claims são criados ou deletados.
     */
    public void forceUpdate() {
        // Executar async para não bloquear a main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::updateMarkers);
    }

    /**
     * Limpa os marcadores e cancela a task periódica.
     * Chamado no onDisable.
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        if (dynmapActive && dynmapHook != null) {
            dynmapHook.clearMarkers();
        }
        if (blueMapActive && blueMapHook != null) {
            blueMapHook.clearMarkers();
        }
    }

    /**
     * Recarrega as configurações e reinicia a task.
     * Chamado no /gorvax reload.
     */
    public void reload() {
        shutdown();
        // Reinicializar
        if (plugin.getConfig().getBoolean("webmap.enabled", true)) {
            long interval = plugin.getConfig().getLong("webmap.update_interval", 1200L);
            this.updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateMarkers, 20L, interval);
        }
    }

    public boolean isDynmapActive() {
        return dynmapActive;
    }

    public boolean isBlueMapActive() {
        return blueMapActive;
    }
}
