package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.SubPlot;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VisualizationListener implements Listener {

    private final GorvaxCore plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    // Tracking for passive visualization (Shovel)
    private final Map<UUID, Set<Location>> passiveVisualizations = new ConcurrentHashMap<>();

    // Tracking for active visualization (Stick)
    private final Map<UUID, BukkitTask> activeInspectionTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Location>> activeInspectionBlocks = new ConcurrentHashMap<>();

    public VisualizationListener(GorvaxCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startPassiveVisualizationTask();
    }

    private void startPassiveVisualizationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    updatePassiveVisualization(p);
                }
            }
        }.runTaskTimer(plugin, 20L, 10L); // Verify every 10 ticks (0.5s)
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        // Reset Obrigatório: Limpar tudo ao trocar de item
        clearAllVisualizations(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clearAllVisualizations(e.getPlayer());
    }

    private void clearAllVisualizations(Player p) {
        UUID uuid = p.getUniqueId();

        // 1. Clear Passive (Shovel)
        if (passiveVisualizations.containsKey(uuid)) {
            Set<Location> blocks = passiveVisualizations.remove(uuid);
            if (blocks != null) {
                for (Location loc : blocks) {
                    if (p.isOnline()) {
                        p.sendBlockChange(loc, loc.getBlock().getBlockData());
                    }
                }
            }
        }

        // 2. Clear Active (Stick)
        if (activeInspectionTasks.containsKey(uuid)) {
            activeInspectionTasks.get(uuid).cancel();
            activeInspectionTasks.remove(uuid);
        }
        if (activeInspectionBlocks.containsKey(uuid)) {
            Set<Location> blocks = activeInspectionBlocks.remove(uuid);
            if (blocks != null) {
                for (Location loc : blocks) {
                    if (p.isOnline()) {
                        p.sendBlockChange(loc, loc.getBlock().getBlockData());
                    }
                }
            }
        }
    }

    private void updatePassiveVisualization(Player p) {
        Material hand = p.getInventory().getItemInMainHand().getType();
        boolean isHoldingShovel = (hand == Material.GOLDEN_SHOVEL || hand == Material.IRON_SHOVEL);

        // Se estiver segurando pá, cancela qualquer inspeção de graveto ativa
        // (Prioridade Absoluta)
        if (isHoldingShovel && activeInspectionTasks.containsKey(p.getUniqueId())) {
            clearAllVisualizations(p);
        }

        if (isHoldingShovel) {
            Set<Location> currentFrame = new HashSet<>();

            // Calculate blocks based on shovel type
            showNearbyClaims(p, currentFrame, hand);

            // Diff and update
            Set<Location> previousFrame = passiveVisualizations.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());

            // Remove blocks that are no longer in current frame
            for (Location loc : previousFrame) {
                if (!currentFrame.contains(loc) && p.isOnline()) {
                    p.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }

            // Note: New blocks are sent inside showNearbyClaims logic to ensure they appear
            passiveVisualizations.put(p.getUniqueId(), currentFrame);

        } else {
            // Not holding shovel, clear passive if exists
            if (passiveVisualizations.containsKey(p.getUniqueId())) {
                Set<Location> blocks = passiveVisualizations.remove(p.getUniqueId());
                for (Location loc : blocks) {
                    if (p.isOnline()) {
                        p.sendBlockChange(loc, loc.getBlock().getBlockData());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() == null || e.getItem().getType() != Material.STICK)
            return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR)
            return;

        Player p = e.getPlayer();

        // Se estiver segurando pá na outra mão (improvável mas possível), pá tem
        // prioridade, não ativa graveto
        Material off = p.getInventory().getItemInOffHand().getType();
        if (off == Material.GOLDEN_SHOVEL || off == Material.IRON_SHOVEL)
            return;

        if (cooldowns.containsKey(p.getUniqueId())) {
            if (System.currentTimeMillis() - cooldowns.get(p.getUniqueId()) < 2000)
                return; // 2s cooldown
        }
        cooldowns.put(p.getUniqueId(), System.currentTimeMillis());

        // Limpa visualizações anteriores para garantir
        clearAllVisualizations(p);

        p.sendMessage(plugin.getMessageManager().get("visualization.inspecting"));
        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);

        // B9 — Durção configurável
        int durationTicks = plugin.getConfig().getInt("visualization.inspection_duration", 15) * 20;
        int particleSpacing = plugin.getConfig().getInt("visualization.particle_spacing", 3);

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!p.isOnline() || ticks >= durationTicks) { // B9: Durção configurável
                    clearAllVisualizations(p);
                    this.cancel();
                    return;
                }

                // Se trocou para pá, o evento onItemHeld já limpou e cancelou esta task?
                // A limpeza remove do mapa, mas a task continua rodando até o cancel() aqui ou
                // lá.
                // Mas verificação de item na mão é garantida.
                if (p.getInventory().getItemInMainHand().getType() != Material.STICK) {
                    // Deixa o onItemHeld cuidar, mas se por acaso falhar:
                    clearAllVisualizations(p);
                    this.cancel();
                    return;
                }

                Set<Location> currentFrame = new HashSet<>();
                Set<Location> previousFrame = activeInspectionBlocks.computeIfAbsent(p.getUniqueId(),
                        k -> new HashSet<>());

                // CENÁRIO A: Clicando com Graveto (Inspeção)
                Location pLoc = p.getLocation();
                int radius = 100;

                for (Claim claim : plugin.getClaimManager().getClaimsNearby(pLoc, radius)) {
                    if (!claim.getWorldName().equals(pLoc.getWorld().getName()))
                        continue;

                    // B9.1 — Determinar tipo de partícula pela relação
                    Claim playerKingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
                    String playerKingdomId = playerKingdom != null ? playerKingdom.getId() : null;
                    List<String> alliances = playerKingdomId != null
                            ? plugin.getKingdomManager().getAlliances(playerKingdomId)
                            : new java.util.ArrayList<>();

                    // CENÁRIO A - Cores
                    // Reinos/Terras: Verde Claro (LIME_WOOL)
                    // Feudos: Verde Escuro (GREEN_WOOL)
                    // Outros Feudos (do meu reino): Amarelo (YELLOW_WOOL)
                    // Terra de Outros: Lã Rosa (PINK_WOOL)
                    // Reino/Feudos de Outros: Lã Cinza (GRAY_WOOL)

                    Material borderMat;
                    boolean isOwner = claim.getOwner().equals(p.getUniqueId());
                    boolean isTrusted = claim.hasPermission(p.getUniqueId(), Claim.TrustType.CONSTRUCAO);
                    boolean isResidentOrOwner = isOwner || isTrusted;

                    // B9.1 — Determinar partícula com base na relação
                    Particle particleType = Particle.END_ROD; // Padrão: Claim pessoal
                    Particle.DustOptions dustOptions = null;

                    if (isResidentOrOwner) {
                        borderMat = Material.LIME_WOOL; // Minha Terra/Reino
                        particleType = Particle.FLAME; // Reino próprio
                    } else if (claim.isKingdom()) {
                        if (playerKingdomId != null && alliances.contains(claim.getId())) {
                            borderMat = Material.LIGHT_BLUE_WOOL; // Aliado
                            particleType = Particle.HAPPY_VILLAGER;
                        } else if (playerKingdomId != null
                                && plugin.getKingdomManager().areEnemies(playerKingdomId, claim.getId())) {
                            borderMat = Material.RED_WOOL; // Inimigo
                            particleType = Particle.DUST;
                            dustOptions = new Particle.DustOptions(Color.RED, 1.0f);
                        } else {
                            borderMat = Material.PINK_WOOL; // Terra alheia
                        }
                    } else {
                        borderMat = Material.PINK_WOOL; // Terra alheia
                    }

                    // Main Border
                    drawBorder(p, claim.getMinX(), claim.getMinZ(), claim.getMaxX(), claim.getMaxZ(), borderMat,
                            currentFrame, previousFrame);

                    // B9.1 — Partículas na borda principal
                    spawnParticleBorder(p, claim.getMinX(), claim.getMinZ(), claim.getMaxX(), claim.getMaxZ(),
                            particleType, dustOptions, particleSpacing);

                    // SubPlots
                    for (SubPlot plot : claim.getSubPlots()) {
                        Material plotMat;
                        Particle plotParticle = Particle.END_ROD;
                        Particle.DustOptions plotDust = null;

                        if (isResidentOrOwner) {
                            plotMat = Material.GREEN_WOOL;
                        } else {
                            plotMat = Material.GRAY_WOOL;
                        }

                        // B9.1 — Partículas por tipo de sub-lote
                        if (plot.isForSale()) {
                            plotParticle = Particle.HAPPY_VILLAGER;
                        } else if (plot.isForRent()) {
                            plotParticle = Particle.DRIPPING_WATER;
                        }

                        drawBorder(p, plot.getMinX(), plot.getMinZ(), plot.getMaxX(), plot.getMaxZ(), plotMat,
                                currentFrame, previousFrame);
                        spawnParticleBorder(p, plot.getMinX(), plot.getMinZ(), plot.getMaxX(), plot.getMaxZ(),
                                plotParticle, plotDust, particleSpacing);
                    }
                }

                // Remove old blocks
                for (Location loc : previousFrame) {
                    if (!currentFrame.contains(loc) && p.isOnline()) {
                        p.sendBlockChange(loc, loc.getBlock().getBlockData());
                    }
                }

                activeInspectionBlocks.put(p.getUniqueId(), currentFrame);
                ticks += 20;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Execute every second

        activeInspectionTasks.put(p.getUniqueId(), task);
    }

    private void showNearbyClaims(Player p, Set<Location> currentFrame, Material hand) {
        Location pLoc = p.getLocation();
        int radius = 100;
        Set<Location> previousSet = passiveVisualizations.get(p.getUniqueId());

        for (Claim claim : plugin.getClaimManager().getClaimsNearby(pLoc, radius)) {
            if (!claim.getWorldName().equals(pLoc.getWorld().getName()))
                continue;

            boolean isOwner = claim.getOwner().equals(p.getUniqueId());
            boolean isVice = claim.hasPermission(p.getUniqueId(), Claim.TrustType.VICE);
            boolean isMine = isOwner || isVice;

            if (hand == Material.GOLDEN_SHOVEL) {
                // PÁ DE OURO: Mostra tudo, mas diferencia o que é meu do que é de outros.
                Material borderMat = isMine ? Material.YELLOW_WOOL : Material.PINK_WOOL;

                drawBorder(p, claim.getMinX(), claim.getMinZ(), claim.getMaxX(), claim.getMaxZ(), borderMat,
                        currentFrame, previousSet);

                // Quinas (Apenas se for o dono)
                if (isMine) {
                    sendLShapeCorner(p, claim.getMinX(), claim.getMinZ(), 1, 1, Material.OCHRE_FROGLIGHT,
                            Material.YELLOW_WOOL, currentFrame, previousSet);
                    sendLShapeCorner(p, claim.getMaxX(), claim.getMaxZ(), -1, -1, Material.OCHRE_FROGLIGHT,
                            Material.YELLOW_WOOL, currentFrame, previousSet);
                    sendLShapeCorner(p, claim.getMinX(), claim.getMaxZ(), 1, -1, Material.OCHRE_FROGLIGHT,
                            Material.YELLOW_WOOL, currentFrame, previousSet);
                    sendLShapeCorner(p, claim.getMaxX(), claim.getMinZ(), -1, 1, Material.OCHRE_FROGLIGHT,
                            Material.YELLOW_WOOL, currentFrame, previousSet);
                }

                // SubPlots
                for (SubPlot plot : claim.getSubPlots()) {
                    Material plotMat = Material.WHITE_WOOL;
                    if (isMine && claim.getOwner().equals(plot.getOwner())) {
                        plotMat = Material.PURPLE_WOOL;
                    } else if (plot.getOwner() != null && plot.getOwner().equals(p.getUniqueId())) {
                        plotMat = Material.PURPLE_WOOL;
                    }
                    drawBorder(p, plot.getMinX(), plot.getMinZ(), plot.getMaxX(), plot.getMaxZ(), plotMat, currentFrame,
                            previousSet);
                }

            } else if (hand == Material.IRON_SHOVEL) {
                // PÁ DE PRATA (FERRO): Foco em feudos. Regra: Borda principal alheia NÃO
                // aparece.
                if (!isMine) {
                    continue; // Pula terrenos que não são meus
                }

                // Minha Borda Principal
                drawBorder(p, claim.getMinX(), claim.getMinZ(), claim.getMaxX(), claim.getMaxZ(), Material.YELLOW_WOOL,
                        currentFrame, previousSet);

                // SubPlots
                for (SubPlot plot : claim.getSubPlots()) {
                    boolean isMyFeud = (plot.getOwner() != null && plot.getOwner().equals(p.getUniqueId()));
                    Material plotMat = isMyFeud ? Material.PURPLE_WOOL : Material.WHITE_WOOL;
                    Material cornerMat = isMyFeud ? Material.SHROOMLIGHT : Material.PEARLESCENT_FROGLIGHT;

                    drawBorder(p, plot.getMinX(), plot.getMinZ(), plot.getMaxX(), plot.getMaxZ(), plotMat, currentFrame,
                            previousSet);

                    if (isMine || isMyFeud) {
                        sendLShapeCorner(p, plot.getMinX(), plot.getMinZ(), 1, 1, cornerMat, plotMat, currentFrame,
                                previousSet);
                        sendLShapeCorner(p, plot.getMaxX(), plot.getMaxZ(), -1, -1, cornerMat, plotMat, currentFrame,
                                previousSet);
                        sendLShapeCorner(p, plot.getMinX(), plot.getMaxZ(), 1, -1, cornerMat, plotMat, currentFrame,
                                previousSet);
                        sendLShapeCorner(p, plot.getMaxX(), plot.getMinZ(), -1, 1, cornerMat, plotMat, currentFrame,
                                previousSet);
                    }
                }
            }
        }
    }

    private void sendLShapeCorner(Player p, int x, int z, int dirX, int dirZ, Material cornerMat, Material sideMat,
            Set<Location> currentFrame, Set<Location> previousSet) {
        org.bukkit.World world = p.getWorld();
        int y = world.getHighestBlockYAt(x, z);

        // 1. Central Corner (Luminoso)
        sendGhostBlock(p, new Location(world, x, y, z), cornerMat, currentFrame, previousSet);

        // 2. Adjacent Blocks (Lã da borda)
        int adj1_X = x + dirX;
        int adj1_Z = z;
        int y1 = world.getHighestBlockYAt(adj1_X, adj1_Z);
        sendGhostBlock(p, new Location(world, adj1_X, y1, adj1_Z), sideMat, currentFrame, previousSet);

        int adj2_X = x;
        int adj2_Z = z + dirZ;
        int y2 = world.getHighestBlockYAt(adj2_X, adj2_Z);
        sendGhostBlock(p, new Location(world, adj2_X, y2, adj2_Z), sideMat, currentFrame, previousSet);
    }

    private void drawBorder(Player p, int minX, int minZ, int maxX, int maxZ, Material mat,
            Set<Location> currentFrame, Set<Location> previousSet) {
        Location pLoc = p.getLocation();
        double pY = pLoc.getY();
        org.bukkit.World world = p.getWorld();

        if (pLoc.distanceSquared(new Location(world, (minX + maxX) / 2.0, pY, (minZ + maxZ) / 2.0)) > 10000)
            return;

        int step = 2;
        Map<Long, Integer> heightCache = new java.util.HashMap<>();

        // Draw along X axis
        for (int x = minX; x <= maxX; x += step) {
            sendGhostBlock(p, new Location(world, x, getCachedHeight(world, x, minZ, heightCache), minZ), mat,
                    currentFrame, previousSet);
            sendGhostBlock(p, new Location(world, x, getCachedHeight(world, x, maxZ, heightCache), maxZ), mat,
                    currentFrame, previousSet);
        }

        // Draw along Z axis
        for (int z = minZ; z <= maxZ; z += step) {
            sendGhostBlock(p, new Location(world, minX, getCachedHeight(world, minX, z, heightCache), z), mat,
                    currentFrame, previousSet);
            sendGhostBlock(p, new Location(world, maxX, getCachedHeight(world, maxX, z, heightCache), z), mat,
                    currentFrame, previousSet);
        }
    }

    // B9.1 — Partículas ao longo da borda do claim
    private void spawnParticleBorder(Player p, int minX, int minZ, int maxX, int maxZ,
            Particle particle, Particle.DustOptions dustOptions, int spacing) {
        Location pLoc = p.getLocation();
        double pY = pLoc.getY();
        org.bukkit.World world = p.getWorld();

        // Distância máxima para spawnar partículas (performance)
        if (pLoc.distanceSquared(new Location(world, (minX + maxX) / 2.0, pY, (minZ + maxZ) / 2.0)) > 10000)
            return;

        Map<Long, Integer> heightCache = new java.util.HashMap<>();

        // Eixo X
        for (int x = minX; x <= maxX; x += spacing) {
            spawnBorderParticle(p, world, x, minZ, particle, dustOptions, heightCache);
            spawnBorderParticle(p, world, x, maxZ, particle, dustOptions, heightCache);
        }

        // Eixo Z
        for (int z = minZ; z <= maxZ; z += spacing) {
            spawnBorderParticle(p, world, minX, z, particle, dustOptions, heightCache);
            spawnBorderParticle(p, world, maxX, z, particle, dustOptions, heightCache);
        }
    }

    private void spawnBorderParticle(Player p, org.bukkit.World world, int x, int z,
            Particle particle, Particle.DustOptions dustOptions, Map<Long, Integer> heightCache) {
        int y = getCachedHeight(world, x, z, heightCache) + 1; // +1 para ficar acima do bloco
        Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);

        if (dustOptions != null) {
            p.spawnParticle(particle, loc, 1, 0, 0, 0, 0, dustOptions);
        } else {
            p.spawnParticle(particle, loc, 1, 0, 0.2, 0, 0);
        }
    }

    private int getCachedHeight(org.bukkit.World world, int x, int z, Map<Long, Integer> cache) {
        long key = ((long) x << 32) | (z & 0xffffffffL);
        return cache.computeIfAbsent(key, k -> world.getHighestBlockYAt(x, z));
    }

    private void sendGhostBlock(Player p, Location loc, Material mat, Set<Location> currentFrame,
            Set<Location> previousSet) {
        // Optimization: avoid resending if existing
        if (previousSet != null && previousSet.contains(loc)) {
            // Already sent
        } else {
            p.sendBlockChange(loc, mat.createBlockData());
        }
        currentFrame.add(loc);
    }
}
