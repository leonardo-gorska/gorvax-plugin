package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B2 — Manager central do Sistema de Combate.
 * Gerencia Combat Tag, Kill Streaks, Spawn Protection e PvP Logger NPC.
 */
public class CombatManager {

    private final GorvaxCore plugin;

    // B2.1 — Combat Tag: mapa jogador → timestamp de expiração do combat tag
    private final Map<UUID, Long> combatTagMap = new ConcurrentHashMap<>();

    // B2.3 — Kill Streaks: mapa jogador → kills consecutivos na sessão atual
    private final Map<UUID, Integer> killStreakMap = new ConcurrentHashMap<>();

    // B2.4 — Spawn Protection: mapa jogador → timestamp de expiração da proteção
    private final Map<UUID, Long> spawnProtectionMap = new ConcurrentHashMap<>();

    // B2.2 — PvP Logger: mapa jogador deslogado → UUID da entidade NPC
    private final Map<UUID, UUID> loggerNPCMap = new ConcurrentHashMap<>();
    // Mapa reverso: UUID do NPC → UUID do jogador (para lookup rápido na morte do
    // NPC)
    private final Map<UUID, UUID> npcToPlayerMap = new ConcurrentHashMap<>();
    // Mapa: UUID do NPC → inventário do jogador (para drops)
    private final Map<UUID, ItemStack[]> npcInventoryMap = new ConcurrentHashMap<>();
    // Mapa: UUID do NPC → armadura do jogador
    private final Map<UUID, ItemStack[]> npcArmorMap = new ConcurrentHashMap<>();
    // Jogadores que morreram via NPC logger (para notificar no login)
    private final Set<UUID> killedByLogger = ConcurrentHashMap.newKeySet();

    // Configurações
    private int tagDuration;
    private List<String> blockedCommands;
    private boolean blockGuiInCombat;
    private boolean loggerEnabled;
    private int loggerDuration;
    private int spawnProtectionDuration;
    private int streakBroadcastInterval;
    private int streakAutoBountyAt;
    private double streakAutoBountyValue;
    private Map<Integer, String> streakTitles = new LinkedHashMap<>();

    private BukkitTask actionBarTask;

    public CombatManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
        startActionBarTask();
    }

    /**
     * Carrega/recarrega configurações do config.yml.
     */
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        this.tagDuration = config.getInt("combat.tag_duration", 15);
        this.blockedCommands = config.getStringList("combat.blocked_commands");
        if (blockedCommands.isEmpty()) {
            blockedCommands = List.of("home", "tpa", "spawn", "warp", "rtp", "back", "logout");
        }
        this.blockGuiInCombat = config.getBoolean("combat.block_gui_in_combat", true);
        this.loggerEnabled = config.getBoolean("combat.logger_enabled", true);
        this.loggerDuration = config.getInt("combat.logger_duration", 15);
        this.spawnProtectionDuration = config.getInt("combat.spawn_protection", 5);
        this.streakBroadcastInterval = config.getInt("combat.killstreak.broadcast_interval", 5);
        this.streakAutoBountyAt = config.getInt("combat.killstreak.auto_bounty_at", 10);
        this.streakAutoBountyValue = config.getDouble("combat.killstreak.auto_bounty_value", 500.0);

        // Carregar títulos de streak
        streakTitles.clear();
        if (config.isConfigurationSection("combat.killstreak.titles")) {
            for (String key : config.getConfigurationSection("combat.killstreak.titles").getKeys(false)) {
                try {
                    int threshold = Integer.parseInt(key);
                    String title = config.getString("combat.killstreak.titles." + key);
                    streakTitles.put(threshold, title);
                } catch (NumberFormatException ignored) {
                    // Ignorar chaves inválidas
                }
            }
        }
        if (streakTitles.isEmpty()) {
            streakTitles.put(5, "§c⚔ Imparável");
            streakTitles.put(10, "§6⚔ Lendário");
            streakTitles.put(20, "§4⚔ Deus da Guerra");
        }
    }

    /**
     * Recarrega configurações e reinicia a task.
     */
    public void reload() {
        loadConfig();
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        startActionBarTask();
    }

    // =========================================================================
    // B2.1 — COMBAT TAG
    // =========================================================================

    /**
     * Coloca um jogador em modo de combate.
     * Se já estiver em combate, apenas reseta o timer.
     */
    public void tagPlayer(UUID uuid) {
        long expireAt = System.currentTimeMillis() + (tagDuration * 1000L);
        boolean wasInCombat = combatTagMap.containsKey(uuid);
        combatTagMap.put(uuid, expireAt);

        if (!wasInCombat) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getMessageManager().send(player, "combat.tagged");
            }
        }
    }

    /**
     * Verifica se um jogador está em modo de combate.
     */
    public boolean isInCombat(UUID uuid) {
        Long expireAt = combatTagMap.get(uuid);
        if (expireAt == null)
            return false;
        if (System.currentTimeMillis() >= expireAt) {
            removeCombatTag(uuid);
            return false;
        }
        return true;
    }

    /**
     * Retorna os segundos restantes de combat tag, ou 0 se não está em combate.
     */
    public int getRemainingCombatTime(UUID uuid) {
        Long expireAt = combatTagMap.get(uuid);
        if (expireAt == null)
            return 0;
        long remaining = expireAt - System.currentTimeMillis();
        if (remaining <= 0) {
            removeCombatTag(uuid);
            return 0;
        }
        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Remove o combat tag de um jogador e notifica.
     */
    public void removeCombatTag(UUID uuid) {
        if (combatTagMap.remove(uuid) != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getMessageManager().send(player, "combat.tag_expired");
            }
        }
    }

    /**
     * Verifica se um comando está bloqueado durante combate.
     */
    public boolean isCommandBlocked(String command) {
        String cmd = command.toLowerCase().trim();
        // Remover / inicial se presente
        if (cmd.startsWith("/"))
            cmd = cmd.substring(1);
        // Pegar apenas o primeiro token (o comando base)
        String baseCmd = cmd.split("\\s+")[0];

        for (String blocked : blockedCommands) {
            if (baseCmd.equalsIgnoreCase(blocked)) {
                return true;
            }
        }

        // Bloquear também /reino spawn (variante de teleporte)
        if (baseCmd.equals("reino") && cmd.contains("spawn")) {
            return true;
        }

        return false;
    }

    /**
     * Retorna se GUIs devem ser bloqueadas em combate.
     */
    public boolean isGuiBlockedInCombat() {
        return blockGuiInCombat;
    }

    // =========================================================================
    // B2.3 — KILL STREAKS
    // =========================================================================

    /**
     * Retorna o kill streak atual de um jogador.
     */
    public int getKillStreak(UUID uuid) {
        return killStreakMap.getOrDefault(uuid, 0);
    }

    /**
     * Incrementa o kill streak e processa broadcasts, títulos e bounties.
     */
    public void incrementKillStreak(Player killer) {
        UUID uuid = killer.getUniqueId();
        int newStreak = killStreakMap.merge(uuid, 1, Integer::sum);

        // Atualizar maior streak no PlayerData
        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);
        if (newStreak > pd.getHighestKillStreak()) {
            pd.setHighestKillStreak(newStreak);
        }

        // Broadcast a cada N kills
        if (streakBroadcastInterval > 0 && newStreak % streakBroadcastInterval == 0) {
            plugin.getMessageManager().broadcast("combat.killstreak_broadcast",
                    killer.getName(), newStreak);

            // B8 — Discord: alerta de killstreak
            plugin.getDiscordManager().sendKillStreakAlert(killer.getName(), newStreak);
        }

        // Verificar título de streak
        String earnedTitle = null;
        for (Map.Entry<Integer, String> entry : streakTitles.entrySet()) {
            if (newStreak == entry.getKey()) {
                earnedTitle = entry.getValue();
                break;
            }
        }
        if (earnedTitle != null) {
            plugin.getMessageManager().send(killer, "combat.killstreak_title_earned", earnedTitle);
        }

        // Bounty automática ao atingir threshold (apenas uma vez por streak)
        if (streakAutoBountyAt > 0 && newStreak == streakAutoBountyAt) {
            BountyManager bountyManager = plugin.getBountyManager();
            if (bountyManager != null && bountyManager.isEnabled()) {
                // Bounty colocada pelo "sistema" usando UUID sintético
                UUID systemUUID = new UUID(0L, 0L);
                bountyManager.placeBounty(systemUUID, uuid, killer.getName(), streakAutoBountyValue);
                plugin.getMessageManager().broadcast("combat.killstreak_bounty",
                        String.format("%.0f", streakAutoBountyValue), killer.getName());
            }
        }

        // B19 — Evento customizado: KillStreakEvent
        org.bukkit.Bukkit.getPluginManager().callEvent(
                new br.com.gorvax.core.events.KillStreakEvent(killer, newStreak));
    }

    /**
     * Reseta o kill streak de um jogador (ao morrer).
     */
    public void resetKillStreak(UUID uuid) {
        Integer oldStreak = killStreakMap.remove(uuid);
        if (oldStreak != null && oldStreak > 0) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getMessageManager().send(player, "combat.killstreak_reset", oldStreak);
            }
        }
    }

    // =========================================================================
    // B2.4 — SPAWN PROTECTION
    // =========================================================================

    /**
     * Aplica proteção de spawn a um jogador.
     */
    public void applySpawnProtection(UUID uuid) {
        if (spawnProtectionDuration <= 0)
            return;
        long expireAt = System.currentTimeMillis() + (spawnProtectionDuration * 1000L);
        spawnProtectionMap.put(uuid, expireAt);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            plugin.getMessageManager().send(player, "combat.spawn_protection_active",
                    spawnProtectionDuration);
        }
    }

    /**
     * Verifica se o jogador tem proteção de spawn ativa.
     */
    public boolean isSpawnProtected(UUID uuid) {
        Long expireAt = spawnProtectionMap.get(uuid);
        if (expireAt == null)
            return false;
        if (System.currentTimeMillis() >= expireAt) {
            spawnProtectionMap.remove(uuid);
            return false;
        }
        return true;
    }

    /**
     * Remove proteção de spawn (quando o jogador ataca alguém).
     */
    public void removeSpawnProtection(UUID uuid) {
        if (spawnProtectionMap.remove(uuid) != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getMessageManager().send(player, "combat.spawn_protection_cancelled");
            }
        }
    }

    /**
     * Retorna segundos restantes de spawn protection.
     */
    public int getRemainingSpawnProtection(UUID uuid) {
        Long expireAt = spawnProtectionMap.get(uuid);
        if (expireAt == null)
            return 0;
        long remaining = expireAt - System.currentTimeMillis();
        if (remaining <= 0) {
            spawnProtectionMap.remove(uuid);
            return 0;
        }
        return (int) Math.ceil(remaining / 1000.0);
    }

    // =========================================================================
    // B2.2 — PVP LOGGER NPC
    // =========================================================================

    /**
     * Verifica se o sistema de logger está ativado.
     */
    public boolean isLoggerEnabled() {
        return loggerEnabled;
    }

    /**
     * Spawna um NPC Villager no local do jogador que deslogou em combate.
     * O NPC tem o equipamento e HP do jogador, e permanece por loggerDuration
     * segundos.
     */
    public void spawnLoggerNPC(Player player) {
        if (!loggerEnabled)
            return;

        Location loc = player.getLocation();
        UUID playerUUID = player.getUniqueId();

        // Salvar inventário e armadura do jogador
        ItemStack[] inventory = player.getInventory().getContents().clone();
        ItemStack[] armor = player.getInventory().getArmorContents().clone();
        double health = player.getHealth();

        // Spawnar o Villager NPC
        Villager npc = loc.getWorld().spawn(loc, Villager.class, villager -> {
            villager.customName(LegacyComponentSerializer.legacySection()
                    .deserialize("§c☠ " + player.getName() + " §7(Combat Logger)"));
            villager.setCustomNameVisible(true);
            villager.setAI(false); // NPC fica parado
            villager.setSilent(true);
            villager.setInvulnerable(false);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setVillagerType(Villager.Type.PLAINS);
            villager.setProfession(Villager.Profession.NONE);

            // Definir HP
            if (villager.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                villager.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(
                        Math.max(health, 1.0));
            }
            villager.setHealth(Math.max(health, 1.0));

            // Equipar com a armadura do jogador
            EntityEquipment equipment = villager.getEquipment();
            if (equipment != null) {
                equipment.setHelmet(armor.length > 3 ? armor[3] : null);
                equipment.setChestplate(armor.length > 2 ? armor[2] : null);
                equipment.setLeggings(armor.length > 1 ? armor[1] : null);
                equipment.setBoots(armor.length > 0 ? armor[0] : null);
                // Arma na mão
                equipment.setItemInMainHand(player.getInventory().getItemInMainHand());
                // Drop rates 0 (não dropar equipamento do NPC, sim o inventário salvo)
                equipment.setHelmetDropChance(0f);
                equipment.setChestplateDropChance(0f);
                equipment.setLeggingsDropChance(0f);
                equipment.setBootsDropChance(0f);
                equipment.setItemInMainHandDropChance(0f);
            }
        });

        UUID npcUUID = npc.getUniqueId();

        // Registrar nos mapas
        loggerNPCMap.put(playerUUID, npcUUID);
        npcToPlayerMap.put(npcUUID, playerUUID);
        npcInventoryMap.put(npcUUID, inventory);
        npcArmorMap.put(npcUUID, armor);

        // Broadcast
        plugin.getMessageManager().broadcast("combat.logger_spawned", player.getName());

        // Agendar remoção após loggerDuration segundos
        new BukkitRunnable() {
            @Override
            public void run() {
                removeLoggerNPC(npcUUID, false);
            }
        }.runTaskLater(plugin, loggerDuration * 20L);
    }

    /**
     * Remove um NPC de logger. Se killed=true, dropa o inventário no local.
     */
    public void removeLoggerNPC(UUID npcUUID, boolean killed) {
        UUID playerUUID = npcToPlayerMap.remove(npcUUID);
        if (playerUUID == null)
            return; // Já removido

        loggerNPCMap.remove(playerUUID);

        Entity entity = null;
        for (var world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e.getUniqueId().equals(npcUUID)) {
                    entity = e;
                    break;
                }
            }
            if (entity != null)
                break;
        }

        if (killed) {
            // Dropar inventário no local do NPC
            Location dropLoc = entity != null ? entity.getLocation() : null;
            if (dropLoc != null) {
                ItemStack[] inventory = npcInventoryMap.remove(npcUUID);
                ItemStack[] armor = npcArmorMap.remove(npcUUID);

                if (inventory != null) {
                    for (ItemStack item : inventory) {
                        if (item != null && item.getType() != Material.AIR) {
                            dropLoc.getWorld().dropItemNaturally(dropLoc, item);
                        }
                    }
                }
                if (armor != null) {
                    for (ItemStack item : armor) {
                        if (item != null && item.getType() != Material.AIR) {
                            dropLoc.getWorld().dropItemNaturally(dropLoc, item);
                        }
                    }
                }
            }
            // Marcar jogador como morto pelo logger
            killedByLogger.add(playerUUID);
        } else {
            // NPC expirou sem ser morto — limpar inventário
            npcInventoryMap.remove(npcUUID);
            npcArmorMap.remove(npcUUID);
        }

        // Remover a entidade do mundo
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }

    /**
     * Verifica se uma entidade é um NPC de combat logger.
     */
    public boolean isLoggerNPC(UUID entityUUID) {
        return npcToPlayerMap.containsKey(entityUUID);
    }

    /**
     * Retorna o UUID do jogador dono de um NPC logger.
     */
    public UUID getLoggerOwner(UUID npcUUID) {
        return npcToPlayerMap.get(npcUUID);
    }

    /**
     * Verifica se o jogador foi morto via NPC logger (e limpa a flag).
     */
    public boolean wasKilledByLogger(UUID playerUUID) {
        return killedByLogger.remove(playerUUID);
    }

    /**
     * Verifica se o jogador tem um NPC logger ativo.
     */
    public boolean hasActiveLogger(UUID playerUUID) {
        return loggerNPCMap.containsKey(playerUUID);
    }

    // =========================================================================
    // TASK PERIÓDICA — ActionBar de combate e spawn protection
    // =========================================================================

    private void startActionBarTask() {
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                // Combat Tag ActionBar
                Iterator<Map.Entry<UUID, Long>> it = combatTagMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, Long> entry = it.next();
                    UUID uuid = entry.getKey();
                    long expireAt = entry.getValue();

                    if (now >= expireAt) {
                        it.remove();
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            plugin.getMessageManager().send(player, "combat.tag_expired");
                        }
                    } else {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            int remaining = (int) Math.ceil((expireAt - now) / 1000.0);
                            plugin.getMessageManager().sendActionBar(player,
                                    "combat.actionbar_combat", remaining);
                        }
                    }
                }

                // Spawn Protection ActionBar
                Iterator<Map.Entry<UUID, Long>> spIt = spawnProtectionMap.entrySet().iterator();
                while (spIt.hasNext()) {
                    Map.Entry<UUID, Long> entry = spIt.next();
                    UUID uuid = entry.getKey();
                    long expireAt = entry.getValue();

                    if (now >= expireAt) {
                        spIt.remove();
                    } else {
                        // Só mostrar ActionBar se NÃO estiver em combate (combate tem prioridade)
                        if (!combatTagMap.containsKey(uuid)) {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null && player.isOnline()) {
                                int remaining = (int) Math.ceil((expireAt - now) / 1000.0);
                                plugin.getMessageManager().sendActionBar(player,
                                        "combat.actionbar_spawn_protection", remaining);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L); // A cada 10 ticks (0.5s) para actionbar suave
    }

    // =========================================================================
    // CLEANUP
    // =========================================================================

    /**
     * Limpa todos os NPCs de logger ativos. Chamado no onDisable.
     */
    public void cleanup() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }

        // Remover todos os NPCs de logger sem dropar inventário
        for (UUID npcUUID : new HashSet<>(npcToPlayerMap.keySet())) {
            removeLoggerNPC(npcUUID, false);
        }

        combatTagMap.clear();
        killStreakMap.clear();
        spawnProtectionMap.clear();
    }

    /**
     * Limpa os dados de um jogador que saiu (sem ser em combate).
     */
    public void cleanupPlayer(UUID uuid) {
        combatTagMap.remove(uuid);
        killStreakMap.remove(uuid);
        spawnProtectionMap.remove(uuid);
    }
}
