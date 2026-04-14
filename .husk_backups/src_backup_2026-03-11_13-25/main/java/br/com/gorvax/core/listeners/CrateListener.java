package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.CrateGUI;
import br.com.gorvax.core.managers.InputManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * B12 — Listener para interação com blocos de crate no spawn.
 * Ao clicar com botão direito em um Ender Chest (configurável),
 * abre a GUI de seleção de crates.
 */
public class CrateListener implements Listener {

    private final GorvaxCore plugin;

    public CrateListener(GorvaxCore plugin) {
        this.plugin = plugin;
        startParticleTask();
    }

    /**
     * Task repetitiva que spawna partículas em crates físicas
     * para indicar visualmente onde ficam.
     */
    private void startParticleTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (plugin.getCrateManager() == null)
                return;

            for (Location loc : plugin.getCrateManager().getPhysicalCrateLocations()) {
                if (loc.getWorld() == null)
                    continue;
                if (!loc.isWorldLoaded())
                    continue;
                boolean hasNearby = loc.getWorld().getPlayers().stream()
                        .anyMatch(p -> p.getLocation().distanceSquared(loc) < 2500);
                if (hasNearby) {
                    loc.getWorld().spawnParticle(Particle.FLAME,
                            loc.clone().add(0.5, 1.3, 0.5), 4, 0.2, 0.1, 0.2, 0.01);
                }
            }
        }, 40L, 40L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Block block = event.getClickedBlock();
        if (block == null)
            return;

        Player player = event.getPlayer();

        // Verificar se o bloco é um bloco de crate configurado
        String crateBlockStr = plugin.getConfig().getString("crates.crate_block", "ENDER_CHEST");
        Material crateBlock;
        try {
            crateBlock = Material.valueOf(crateBlockStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            crateBlock = Material.ENDER_CHEST;
        }

        if (block.getType() != crateBlock)
            return;

        // Verificar se está na região de spawn (coordenadas configuráveis)
        if (!isInCrateZone(block))
            return;

        // Cancelar abertura normal do ender chest
        event.setCancelled(true);

        // Verificar se é uma crate física com tipo específico por coordenada
        String physicalCrateType = plugin.getCrateManager().getCrateTypeAtLocation(block.getLocation());
        if (physicalCrateType != null) {
            // Abrir diretamente o tipo de crate associado à coordenada
            plugin.getCrateManager().openCrate(player, physicalCrateType);
            return;
        }

        // Fallback: abrir GUI de seleção de crates (Ender Chest genérica)
        CrateGUI crateGUI = plugin.getCrateGUI();
        if (InputManager.isBedrockPlayer(player)) {
            crateGUI.openBedrockForm(player);
        } else {
            crateGUI.openCrateSelection(player);
        }
    }

    /**
     * Verifica se o bloco está na zona de crate (raio configurável ao redor do
     * spawn).
     */
    private boolean isInCrateZone(Block block) {
        // Se não configurado, aceita em qualquer lugar
        if (!plugin.getConfig().getBoolean("crates.restrict_to_spawn", false)) {
            return true;
        }

        // Verificar se está no raio do spawn
        int radius = plugin.getConfig().getInt("crates.spawn_radius", 50);
        var spawnLoc = block.getWorld().getSpawnLocation();
        double distSq = block.getLocation().distanceSquared(spawnLoc);
        return distSq <= (double) radius * radius;
    }

}
