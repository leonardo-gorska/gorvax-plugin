package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B37 — Manager central do Hub de Teleportes.
 * Gerencia bússola, cooldowns, warmups e lógica de teleporte.
 */
public class TeleportHubManager {

    private final GorvaxCore plugin;
    private final NamespacedKey compassKey;

    // Cooldowns: tipo → (UUID → timestamp de expiração)
    private final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

    // Warmups ativos: UUID → BukkitTask (para cancelar ao mover)
    private final Map<UUID, BukkitTask> activeWarmups = new ConcurrentHashMap<>();
    // Warmups: UUID → Location original (para detectar movimento)
    private final Map<UUID, Location> warmupLocations = new ConcurrentHashMap<>();

    // Configurações
    private boolean enabled;
    private boolean compassGiveOnJoin;
    private int compassSlot;
    private boolean combatCheck;

    // Cooldowns em segundos por tipo
    private int cooldownSpawn;
    private int cooldownRtp;
    private int cooldownKingdomHome;
    private int cooldownKingdomVisit;

    // Warmups em segundos por tipo
    private int warmupSpawn;
    private int warmupRtp;
    private int warmupKingdomHome;

    public TeleportHubManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.compassKey = new NamespacedKey(plugin, "teleport_compass");
        loadConfig();
    }

    /**
     * Carrega/recarrega configurações da seção teleport_hub do config.yml.
     */
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        this.enabled = config.getBoolean("teleport_hub.enabled", true);
        this.compassGiveOnJoin = config.getBoolean("teleport_hub.compass_give_on_join", true);
        this.compassSlot = config.getInt("teleport_hub.compass_slot", 8);
        this.combatCheck = config.getBoolean("teleport_hub.combat_check", true);

        this.cooldownSpawn = config.getInt("teleport_hub.cooldowns.spawn", 30);
        this.cooldownRtp = config.getInt("teleport_hub.cooldowns.rtp", 300);
        this.cooldownKingdomHome = config.getInt("teleport_hub.cooldowns.kingdom_home", 30);
        this.cooldownKingdomVisit = config.getInt("teleport_hub.cooldowns.kingdom_visit", 60);

        this.warmupSpawn = config.getInt("teleport_hub.warmups.spawn", 5);
        this.warmupRtp = config.getInt("teleport_hub.warmups.rtp", 3);
        this.warmupKingdomHome = config.getInt("teleport_hub.warmups.kingdom_home", 3);
    }

    /**
     * Recarrega configurações (para /gorvax reload).
     */
    public void reload() {
        loadConfig();
    }

    // =========================================================================
    // BÚSSOLA
    // =========================================================================

    /**
     * Cria o ItemStack da bússola do Hub de Teleportes.
     */
    public ItemStack createCompass() {
        MessageManager msg = plugin.getMessageManager();
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection()
                    .deserialize(msg.get("teleport_hub.compass_name")));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection()
                    .deserialize(msg.get("teleport_hub.compass_hint")));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);
            compass.setItemMeta(meta);
        }
        return compass;
    }

    /**
     * Verifica se um ItemStack é a bússola do Hub.
     */
    public boolean isCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(compassKey, PersistentDataType.BYTE);
    }

    /**
     * Dá a bússola ao jogador no slot configurado.
     * Não dá se o jogador já tem uma.
     */
    public void giveCompass(Player player) {
        if (!enabled || !compassGiveOnJoin) return;

        // Verificar se já possui
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCompass(item)) return;
        }

        // Dar no slot configurado
        ItemStack existing = player.getInventory().getItem(compassSlot);
        if (existing != null && existing.getType() != Material.AIR) {
            // Slot ocupado — dar no primeiro slot livre
            player.getInventory().addItem(createCompass());
        } else {
            player.getInventory().setItem(compassSlot, createCompass());
        }
    }

    // =========================================================================
    // COOLDOWNS
    // =========================================================================

    /**
     * Retorna o cooldown em segundos para um tipo de teleporte.
     */
    public int getCooldownSeconds(String type) {
        return switch (type) {
            case "spawn" -> cooldownSpawn;
            case "rtp" -> cooldownRtp;
            case "kingdom_home" -> cooldownKingdomHome;
            case "kingdom_visit" -> cooldownKingdomVisit;
            default -> 0;
        };
    }

    /**
     * Verifica se o jogador está em cooldown para um tipo de teleporte.
     */
    public boolean isOnCooldown(UUID uuid, String type) {
        Map<UUID, Long> map = cooldowns.get(type);
        if (map == null) return false;
        Long expireAt = map.get(uuid);
        if (expireAt == null) return false;
        if (System.currentTimeMillis() >= expireAt) {
            map.remove(uuid);
            return false;
        }
        return true;
    }

    /**
     * Retorna os segundos restantes de cooldown.
     */
    public int getRemainingCooldown(UUID uuid, String type) {
        Map<UUID, Long> map = cooldowns.get(type);
        if (map == null) return 0;
        Long expireAt = map.get(uuid);
        if (expireAt == null) return 0;
        long remaining = expireAt - System.currentTimeMillis();
        if (remaining <= 0) {
            map.remove(uuid);
            return 0;
        }
        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Define o cooldown de um tipo de teleporte.
     */
    public void setCooldown(UUID uuid, String type) {
        int seconds = getCooldownSeconds(type);
        if (seconds <= 0) return;
        cooldowns.computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                .put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }

    // =========================================================================
    // WARMUPS
    // =========================================================================

    /**
     * Retorna os segundos de warmup para um tipo de teleporte.
     */
    public int getWarmupSeconds(String type) {
        return switch (type) {
            case "spawn" -> warmupSpawn;
            case "rtp" -> warmupRtp;
            case "kingdom_home" -> warmupKingdomHome;
            default -> 0;
        };
    }

    /**
     * Verifica se o jogador está em warmup.
     */
    public boolean isInWarmup(UUID uuid) {
        return activeWarmups.containsKey(uuid);
    }

    /**
     * Inicia um warmup de teleporte. Após X segundos, executa a ação de TP.
     * Cancelado se o jogador mover.
     */
    public void startWarmup(Player player, String type, Runnable onComplete) {
        UUID uuid = player.getUniqueId();
        int seconds = getWarmupSeconds(type);
        MessageManager msg = plugin.getMessageManager();

        if (seconds <= 0) {
            // Sem warmup — executar imediatamente
            onComplete.run();
            setCooldown(uuid, type);
            return;
        }

        // Cancelar warmup anterior se existir
        cancelWarmup(uuid);

        // Salvar posição atual
        warmupLocations.put(uuid, player.getLocation().clone());

        // Enviar mensagem de warmup
        msg.send(player, "teleport_hub.warmup", seconds);

        // Agendar teleporte após warmup
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                activeWarmups.remove(uuid);
                warmupLocations.remove(uuid);
                if (player.isOnline()) {
                    onComplete.run();
                    setCooldown(uuid, type);
                }
            }
        }.runTaskLater(plugin, seconds * 20L);

        activeWarmups.put(uuid, task);
    }

    /**
     * Cancela o warmup de um jogador (chamado ao mover).
     */
    public void cancelWarmup(UUID uuid) {
        BukkitTask task = activeWarmups.remove(uuid);
        if (task != null) {
            task.cancel();
            warmupLocations.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getMessageManager().send(player, "teleport_hub.warmup_cancelled");
            }
        }
    }

    /**
     * Verifica se o jogador se moveu da posição de warmup (chamado pelo listener).
     */
    public void checkWarmupMovement(Player player) {
        UUID uuid = player.getUniqueId();
        Location origin = warmupLocations.get(uuid);
        if (origin == null) return;

        Location current = player.getLocation();
        // Verificar se moveu mais de 0.5 blocos (ignorar rotação da câmera)
        if (origin.getWorld() != null && current.getWorld() != null
                && origin.getWorld().equals(current.getWorld())
                && origin.distanceSquared(current) > 0.25) {
            cancelWarmup(uuid);
        }
    }

    // =========================================================================
    // TELEPORTES
    // =========================================================================

    /**
     * Valida se o jogador pode teleportar (combat check + cooldown).
     * Retorna true se pode, false se bloqueado (com mensagem).
     */
    public boolean canTeleport(Player player, String type) {
        MessageManager msg = plugin.getMessageManager();

        // Combat check
        if (combatCheck && plugin.getCombatManager() != null
                && plugin.getCombatManager().isInCombat(player.getUniqueId())) {
            msg.send(player, "teleport_hub.combat_blocked");
            return false;
        }

        // Cooldown check (admins bypassam)
        if (!player.hasPermission("gorvax.admin") && isOnCooldown(player.getUniqueId(), type)) {
            int remaining = getRemainingCooldown(player.getUniqueId(), type);
            msg.send(player, "teleport_hub.cooldown", remaining);
            return false;
        }

        // Warmup já ativo
        if (isInWarmup(player.getUniqueId())) {
            return false;
        }

        return true;
    }

    /**
     * Teleporta o jogador ao spawn do servidor (com warmup).
     */
    public void teleportToSpawn(Player player) {
        if (!canTeleport(player, "spawn")) return;

        startWarmup(player, "spawn", () -> {
            Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation().add(0.5, 0, 0.5);
            player.teleport(spawn);
            plugin.getMessageManager().send(player, "teleport_hub.spawn_tp");
        });
    }

    /**
     * Teleporta o jogador ao spawn do seu reino (com warmup).
     * Prioriza o spawn definido via /reino setspawn; fallback para centro do claim.
     */
    public void teleportToKingdom(Player player) {
        if (!canTeleport(player, "kingdom_home")) return;

        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
        if (kingdom == null) {
            plugin.getMessageManager().send(player, "teleport_hub.kingdom_none");
            return;
        }

        startWarmup(player, "kingdom_home", () -> {
            // B40-fix: Priorizar spawn customizado definido pelo Rei
            Location spawn = plugin.getKingdomManager().getSpawn(kingdom.getId());
            if (spawn != null) {
                player.teleport(spawn);
                plugin.getMessageManager().send(player, "teleport_hub.kingdom_tp");
                return;
            }

            // Fallback: centro geométrico do claim
            org.bukkit.World world = Bukkit.getWorld(kingdom.getWorldName());
            if (world == null) return;
            double cx = (kingdom.getMinX() + kingdom.getMaxX()) / 2.0;
            double cz = (kingdom.getMinZ() + kingdom.getMaxZ()) / 2.0;
            int y = world.getHighestBlockYAt((int) cx, (int) cz) + 1;
            Location loc = new Location(world, cx, y, cz);
            player.teleport(loc);
            plugin.getMessageManager().send(player, "teleport_hub.kingdom_tp");
        });
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isCompassGiveOnJoin() {
        return compassGiveOnJoin;
    }

    public int getCompassSlot() {
        return compassSlot;
    }

    public NamespacedKey getCompassKey() {
        return compassKey;
    }
}
