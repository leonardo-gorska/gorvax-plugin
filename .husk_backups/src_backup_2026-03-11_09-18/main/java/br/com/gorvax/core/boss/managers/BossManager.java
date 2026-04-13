package br.com.gorvax.core.boss.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.boss.BossTask;
import br.com.gorvax.core.boss.model.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gerenciador central do sistema de Bosses do GorvaxCore.
 * Responsável por spawn, gerenciamento de bosses ativos, sistema de recompensas
 * e agendamento.
 */
public class BossManager {

    private final GorvaxCore plugin;
    private final ConfigManager configManager;
    private final LootManager lootManager;
    private final AtmosphereManager atmosphereManager;
    private final BossScheduleManager scheduleManager;
    private final BossRaidManager raidManager;
    private final Map<UUID, WorldBoss> activeBosses = new ConcurrentHashMap<>();
    private final Set<Location> temporaryBlocks = Collections.synchronizedSet(new HashSet<>());
    private final java.util.concurrent.locks.ReentrantLock bossDataLock = new java.util.concurrent.locks.ReentrantLock();
    private org.bukkit.configuration.file.YamlConfiguration bossData;
    private java.io.File bossDataFile;

    private long nextSpawnTime;
    private BukkitTask spawnTask;
    private BukkitTask updateTask;

    // Configurações de spawn automático
    // Intervalo aleatório entre 3 a 7 horas (em ms)
    private static final long MIN_SPAWN_INTERVAL = 3 * 3600000L;
    private static final long MAX_SPAWN_INTERVAL = 7 * 3600000L;
    private static final int MIN_PLAYERS_FOR_SPAWN = 1;
    private static final Random RANDOM = new Random();
    private static final double REI_INDRAX_CHANCE = 0.05; // 5% chance de spawnar ambos

    public BossManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.configManager = new ConfigManager(plugin);
        this.lootManager = new LootManager(plugin);
        this.atmosphereManager = new AtmosphereManager(plugin);
        this.scheduleManager = new BossScheduleManager(plugin);
        this.raidManager = new BossRaidManager(plugin);

        // Calcula próximo spawn (3 a 7 horas a partir de agora)
        this.nextSpawnTime = System.currentTimeMillis() + randomSpawnInterval();

        setupData();
        cleanupOrphanBlocks();

        // Inicia tarefas agendadas
        startTasks();

        plugin.getLogger().info("§a[BossManager] Sistema de Bosses inicializado!");
    }

    private void setupData() {
        bossDataFile = new java.io.File(plugin.getDataFolder(), "boss_data.yml");
        if (!bossDataFile.exists()) {
            try {
                bossDataFile.createNewFile();
            } catch (java.io.IOException e) {
                plugin.getLogger().severe("Erro ao criar boss_data.yml: " + e.getMessage());
            }
        }
        bossData = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(bossDataFile);
    }

    private void cleanupOrphanBlocks() {
        List<String> locStrings = bossData.getStringList("temporary_blocks");
        if (locStrings.isEmpty())
            return;

        int count = 0;
        for (String s : locStrings) {
            Location loc = stringToLocation(s);
            if (loc != null && loc.getBlock().getType() == Material.BEDROCK) {
                loc.getBlock().setType(Material.AIR);
                count++;
            }
        }
        bossData.set("temporary_blocks", new ArrayList<>());
        saveBossData();
        if (count > 0) {
            plugin.getLogger()
                    .info("§e[BossManager] Limpando " + count + " blocos de bedrock órfãos de sessões anteriores.");
        }
    }

    private void saveBossData() {
        bossDataLock.lock();
        try {
            bossData.save(bossDataFile);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Erro ao salvar boss_data.yml: " + e.getMessage());
        } finally {
            bossDataLock.unlock();
        }
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location stringToLocation(String s) {
        String[] parts = s.split(",");
        if (parts.length < 4)
            return null;
        World w = Bukkit.getWorld(parts[0]);
        if (w == null)
            return null;
        try {
            return new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Inicia as tarefas agendadas do sistema de Bosses.
     */
    private void startTasks() {
        // Task de atualização dos bosses ativos (tick a cada 20 ticks = 1 segundo)
        updateTask = new BossTask(plugin).runTaskTimer(plugin, 20L, 20L);

        // Task de spawn automático (verifica a cada minuto)
        spawnTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            if (now >= nextSpawnTime && Bukkit.getOnlinePlayers().size() >= MIN_PLAYERS_FOR_SPAWN) {
                spawnRandomBoss();
                nextSpawnTime = now + randomSpawnInterval();
            }
        }, 1200L, 1200L); // 1200 ticks = 1 minuto
    }

    /**
     * Spawna um boss aleatório em uma localização aleatória do mundo principal.
     */
    public void spawnRandomBoss() {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            plugin.getLogger().warning("§c[BossManager] Mundo 'world' não encontrado para spawn de boss!");
            return;
        }

        // Localização aleatória dentro de um raio de 10000 blocos do spawn
        Location spawn = world.getSpawnLocation();
        int x = spawn.getBlockX() + (RANDOM.nextInt(20000) - 10000);
        int z = spawn.getBlockZ() + (RANDOM.nextInt(20000) - 10000);
        int y = world.getHighestBlockYAt(x, z) + 1;

        Location bossLoc = new Location(world, x, y, z);
        spawnRandomBossAt(bossLoc);
    }

    /**
     * Spawna um boss aleatório em uma localização específica.
     * 5% de chance de spawnar Rei Gorvax + Indrax juntos (Ruptura Temporal).
     * Os outros 7 bosses têm chance igual.
     */
    public void spawnRandomBossAt(Location loc) {
        // --- CROSS-03: Claim Protection ---
        // Prevent bosses from spawning inside Kingdoms/Claims
        if (plugin.getClaimManager().getClaimAt(loc) != null) {
            plugin.getLogger().info("§e[BossManager] Spawn cancelado: Localização dentro de território protegido.");
            return;
        }

        // Primeiro: seleciona um boss aleatório entre todos (chance igual)
        String[] allBosses = { "rei_gorvax", "indrax_abissal", "zarith", "kaldur", "skulkor", "xylos",
                "vulgathor" };
        String selected = allBosses[RANDOM.nextInt(allBosses.length)];

        // Se sorteou Gorvax ou Indrax, há 5% de chance de virar spawn DUAL (Ruptura
        // Temporal)
        // Chance real do dual = (2/7) * 0.05 ≈ 1.4% por spawn
        if ((selected.equals("rei_gorvax") || selected.equals("indrax_abissal"))
                && RANDOM.nextDouble() < REI_INDRAX_CHANCE) {
            spawnDualBoss(loc);
        } else {
            spawnBoss(selected, loc);
        }
    }

    /**
     * Gera intervalo aleatório entre 3 a 7 horas (em ms).
     */
    private long randomSpawnInterval() {
        return MIN_SPAWN_INTERVAL + (long) (RANDOM.nextDouble() * (MAX_SPAWN_INTERVAL - MIN_SPAWN_INTERVAL));
    }

    /**
     * Spawna um boss específico em uma localização.
     * 
     * @param bossId ID do boss (rei_gorvax ou indrax_abissal)
     * @param loc    Localização do spawn
     */
    public void spawnBoss(String bossId, Location loc) {
        // Caso especial: spawna ambos os bosses juntos
        if (bossId.equalsIgnoreCase("rei_indrax")) {
            spawnDualBoss(loc);
            return;
        }

        WorldBoss boss;

        switch (bossId.toLowerCase()) {
            case "rei_gorvax":
                boss = new KingGorvax();
                break;
            case "indrax_abissal":
            case "indrax":
                boss = new IndraxAbissal();
                break;
            case "vulgathor":
                boss = new Vulgathor();
                break;
            case "xylos":
                boss = new XylosDevorador();
                break;
            case "skulkor":
                boss = new Skulkor();
                break;
            case "kaldur":
                boss = new Kaldur();
                break;
            case "zarith":
                boss = new Zarith();
                break;
            case "halloween_boss":
                boss = new HalloweenBoss();
                break;
            case "natal_boss":
                boss = new NatalBoss();
                break;
            default:
                plugin.getLogger().warning("§c[BossManager] Boss ID '" + bossId + "' não reconhecido!");
                return;
        }

        // B19 — Evento customizado: BossSpawnEvent (cancelável)
        br.com.gorvax.core.events.BossSpawnEvent spawnEvent = new br.com.gorvax.core.events.BossSpawnEvent(bossId, loc);
        Bukkit.getPluginManager().callEvent(spawnEvent);
        if (spawnEvent.isCancelled()) {
            plugin.getLogger().info("[BossManager] Spawn de '" + bossId + "' cancelado por evento.");
            return;
        }

        // Carrega configurações do boss — resolve alias
        String configKey = resolveConfigKey(bossId);
        String configPath = "bosses." + configKey;
        double hp = configManager.getSettings().getDouble(configPath + ".hp", 1000.0);
        String nome = configManager.getSettings().getString(configPath + ".nome", boss.getName());

        // Aplica configurações
        boss.setMaxHealth(hp);
        boss.setName(nome.replace("&", "§"));

        // Spawna o boss com tratamento de erro para evitar memory leak de chunks
        try {
            // Garante que o chunk está carregado
            loc.getChunk().setForceLoaded(true);
            boss.spawn(loc);

            // Registra no mapa de bosses ativos
            activeBosses.put(boss.getEntity().getUniqueId(), boss);

            // Notifica AtmosphereManager sobre spawn
            atmosphereManager.onBossSpawn(boss);

            // Broadcast da localização
            Bukkit.broadcast(Component.text(" "));
            plugin.getMessageManager().broadcast("boss_event.spawn_header");
            plugin.getMessageManager().broadcast("boss_event.spawn_name", boss.getName());
            plugin.getMessageManager().broadcast("boss_event.spawn_coords", loc.getBlockX(), loc.getBlockY(),
                    loc.getBlockZ());
            plugin.getMessageManager().broadcast("boss_event.spawn_world", loc.getWorld().getName());

            // Lore dialogue do boss ao spawnar (lido de boss_settings.yml)
            java.util.List<String> spawnDialogues = configManager.getSettings()
                    .getStringList(configPath + ".dialogues.spawn");
            if (!spawnDialogues.isEmpty()) {
                String dialogue = spawnDialogues.get(RANDOM.nextInt(spawnDialogues.size()))
                        .replace("&", "§");
                Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(dialogue));
            }

            Bukkit.broadcast(Component.text(" "));

            // B8 — Discord: alerta de boss spawn
            plugin.getDiscordManager().sendBossSpawnAlert(boss.getName(), loc.getWorld().getName(), (int) hp);

            plugin.getLogger().info("[BossManager] Boss '" + boss.getName() + "' spawnado em " +
                    loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        } catch (Exception e) {
            plugin.getLogger().severe("[BossManager] Erro ao spawnar boss: " + e.getMessage());
            e.printStackTrace();
            // Cleanup do chunk para evitar memory leak
            if (loc.getChunk().isForceLoaded()) {
                loc.getChunk().setForceLoaded(false);
            }
        }
    }

    /**
     * Sistema de recompensas baseado em dano causado.
     * Distribui loot para os top 5 jogadores e dá recompensa de participação para
     * os demais.
     */
    public void rewardPlayers(WorldBoss boss) {
        if (boss.getDamageDealt().isEmpty()) {
            plugin.getLogger().info("[BossManager] Boss '" + boss.getName() + "' morreu sem nenhum dano registrado.");
            return;
        }

        // Ordena jogadores por dano causado (maior para menor)
        List<Map.Entry<UUID, Double>> topDamagers = boss.getTopDamagers();

        // B19 — Evento customizado: BossDeathEvent
        Location deathLocation = boss.getEntity() != null ? boss.getEntity().getLocation() : null;
        Bukkit.getPluginManager().callEvent(
                new br.com.gorvax.core.events.BossDeathEvent(boss.getId(), deathLocation, topDamagers));

        // Localização para spawnar baús de recompensa
        Location deathLoc = deathLocation;

        // Guard: se a entidade já foi removida, não gerar loot físico
        if (deathLoc == null) {
            plugin.getLogger().warning("[BossManager] Boss morreu sem localização válida — loot não será gerado.");
            return;
        }

        // Plataforma temporária se o boss morrer no ar ou na água
        createLootPlatform(deathLoc);

        // Spawna baú físico de loot
        spawnLootChest(deathLoc);

        // Distribui recompensas
        for (int i = 0; i < topDamagers.size(); i++) {
            UUID uuid = topDamagers.get(i).getKey();
            double damage = topDamagers.get(i).getValue();

            Player p = Bukkit.getPlayer(uuid);
            if (p == null)
                continue; // Jogador offline

            // Progresso de quests: BOSS_PARTICIPATE (diárias/semanais + lore)
            if (plugin.getQuestManager() != null) {
                plugin.getQuestManager().addProgress(uuid,
                        br.com.gorvax.core.managers.QuestManager.QuestType.BOSS_PARTICIPATE, "ANY", 1);
                plugin.getQuestManager().addLoreProgress(uuid,
                        br.com.gorvax.core.managers.QuestManager.QuestType.BOSS_PARTICIPATE, "ANY", 1);
            }

            int rank = i + 1;

            // Top 5 recebem loot especial
            if (rank <= 5) {
                lootManager.generateLoot(p, rank, boss.getId());

                double moneyReward = calculateMoneyReward(rank);
                if (GorvaxCore.getEconomy() != null) {
                    GorvaxCore.getEconomy().depositPlayer(p, moneyReward);
                    p.sendMessage(plugin.getMessageManager().get("boss_event.reward_rank_money",
                            String.format("%.2f", moneyReward), rank));
                }
            } else {
                // Participação (rank > 5) - Loot básico de consolação
                lootManager.generateLoot(p, rank, boss.getId());

                double participationReward = 100.0;
                if (GorvaxCore.getEconomy() != null) {
                    GorvaxCore.getEconomy().depositPlayer(p, participationReward);
                    p.sendMessage(plugin.getMessageManager().get("boss_event.reward_participation_money",
                            String.format("%.2f", participationReward)));
                }
            }
        }

        // Spawna holograma de ranking em cima do baú
        spawnRankingHologram(deathLoc, boss);

        // Broadcast do ranking no chat (geral)
        broadcastRanking(boss, topDamagers);
    }

    /**
     * Remove um boss específico e limpa seus recursos (chunks, bossbar, etc).
     * Centraliza a restauração de clima e visibilidade.
     */
    public void removeBoss(WorldBoss boss) {
        if (boss == null)
            return;

        // 1. Remover a BossBar imediatamente para todos os jogadores
        boss.removeBossBar();

        // 2. Remover do mapa de bosses ativos ANTES de notificar AtmosphereManager
        if (boss.getEntity() != null) {
            activeBosses.remove(boss.getEntity().getUniqueId());
        }

        // 3. Notifica AtmosphereManager sobre morte/remoção
        atmosphereManager.onBossDeath(boss);

        // 4. Cleanup centralizado (habilidades, summons, blocos temporários, música)
        boss.cleanup();

        // 5. Descarrega chunk force-loaded
        if (boss.getEntity() != null) {
            Location loc = boss.getEntity().getLocation();
            if (loc.getChunk().isForceLoaded()) {
                loc.getChunk().setForceLoaded(false);
            }
        }
    }

    /**
     * Remove um boss pelo ID da entidade (útil para limpeza de Ghost-Bars).
     */
    public void removeBossById(UUID entityUUID) {
        WorldBoss boss = activeBosses.get(entityUUID);
        if (boss != null) {
            removeBoss(boss);
        }
    }

    /**
     * Resolve o ID do boss para a chave de configuração no boss_settings.yml.
     */
    private String resolveConfigKey(String bossId) {
        return switch (bossId.toLowerCase()) {
            case "indrax_abissal", "indrax" -> "indrax";
            case "rei_gorvax" -> "rei_gorvax";
            case "vulgathor" -> "vulgathor";
            case "xylos" -> "xylos";
            case "skulkor" -> "skulkor";
            case "kaldur" -> "kaldur";
            case "zarith" -> "zarith";
            case "halloween_boss" -> "halloween_boss";
            case "natal_boss" -> "natal_boss";
            default -> bossId;
        };
    }

    /**
     * Spawna ambos Rei Gorvax e Indrax Abissal juntos (Ruptura Temporal).
     * Eles aparecem 10 blocos separados. Apenas via comando /boss spawn rei_indrax
     * ou 5% de chance no spawn automático.
     */
    private void spawnDualBoss(Location loc) {
        Bukkit.broadcast(Component.text(" "));
        plugin.getMessageManager().broadcast("boss_event.double_spawn_header");
        plugin.getMessageManager().broadcast("boss_event.double_spawn_subtitle");
        Bukkit.broadcast(Component.text(" "));

        // Sons e efeitos visuais majestosos
        for (Player p : loc.getWorld().getPlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.3f);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.5f);
        }
        loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 2, 0), 200, 5.0, 3.0, 5.0, 0.5);
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 2, 0), 100, 3.0, 2.0, 3.0, 0.05);

        // Spawna ambos com 10 blocos de distância
        Location loc1 = loc.clone().add(5, 0, 0);
        Location loc2 = loc.clone().add(-5, 0, 0);

        // Garante Y seguro
        loc1.setY(loc1.getWorld().getHighestBlockYAt(loc1) + 1);
        loc2.setY(loc2.getWorld().getHighestBlockYAt(loc2) + 1);

        spawnBoss("rei_gorvax", loc1);
        spawnBoss("indrax_abissal", loc2);
    }

    /**
     * Calcula recompensa em dinheiro baseada no rank.
     */
    private double calculateMoneyReward(int rank) {
        return switch (rank) {
            case 1 -> 5000.0;
            case 2 -> 3000.0;
            case 3 -> 2000.0;
            case 4 -> 1500.0;
            case 5 -> 1000.0;
            default -> 100.0;
        };
    }

    /**
     * Cria plataforma temporária para segurar loot se o boss morrer no ar/água.
     */
    private void createLootPlatform(Location loc) {
        Location below = loc.clone().subtract(0, 1, 0);
        if (below.getBlock().getType() == Material.AIR || below.getBlock().isLiquid()) {
            // Cria plataforma 3x3 de bedrock temporária
            List<String> list = bossData.getStringList("temporary_blocks");
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location platLoc = below.clone().add(x, 0, z);
                    platLoc.getBlock().setType(Material.BEDROCK);
                    temporaryBlocks.add(platLoc);

                    // Persistência em arquivo para caso de crash
                    list.add(locationToString(platLoc));

                    // Remove a plataforma após 5 minutos
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (platLoc.getBlock().getType() == Material.BEDROCK) {
                            platLoc.getBlock().setType(Material.AIR);
                        }
                        temporaryBlocks.remove(platLoc);

                        // Remove do arquivo
                        List<String> current = bossData.getStringList("temporary_blocks");
                        current.remove(locationToString(platLoc));
                        bossData.set("temporary_blocks", current);
                        saveBossData();
                    }, 6000L);
                }
            }
            // Salva uma única vez após registrar todos os blocos
            bossData.set("temporary_blocks", list);
            saveBossData();
        }
    }

    /**
     * Spawna baú físico de loot na localização do boss.
     */
    private void spawnLootChest(Location loc) {
        loc.getBlock().setType(Material.CHEST);
        loc.getBlock().setMetadata("boss_loot_chest", new FixedMetadataValue(plugin, true));

        // Persistência com PDC (PersistentDataContainer)
        if (loc.getBlock().getState() instanceof org.bukkit.block.Chest chest) {
            org.bukkit.persistence.PersistentDataContainer data = chest.getPersistentDataContainer();
            data.set(new org.bukkit.NamespacedKey(plugin, "boss_loot_chest"),
                    org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            chest.update();
        }

        // Remove o baú após 5 minutos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (loc.getBlock().getType() == Material.CHEST && loc.getBlock().hasMetadata("boss_loot_chest")) {
                loc.getBlock().setType(Material.AIR);
                loc.getBlock().removeMetadata("boss_loot_chest", plugin);
            }
        }, 6000L);
    }

    /**
     * Faz broadcast do ranking de dano.
     */
    private void broadcastRanking(WorldBoss boss, List<Map.Entry<UUID, Double>> topDamagers) {
        Bukkit.broadcast(Component.text(" "));
        plugin.getMessageManager().broadcast("boss_event.death_header", boss.getName());
        plugin.getMessageManager().broadcast("boss_event.death_ranking_header");

        for (int i = 0; i < topDamagers.size(); i++) {
            UUID uuid = topDamagers.get(i).getKey();
            double damage = topDamagers.get(i).getValue();
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null)
                name = "Desconhecido";

            if (i < 5) {
                String medal = switch (i) {
                    case 0 -> "§6🥇";
                    case 1 -> "§7🥈";
                    case 2 -> "§c🥉";
                    default -> "§e#" + (i + 1);
                };
                Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("boss_event.death_ranking_entry_medal", medal,
                                name, String.format("%.0f", damage))));
            } else {
                // Participantes além do Top 5
                Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("boss_event.death_ranking_entry_extra", (i + 1),
                                name, String.format("%.0f", damage))));
            }
        }
        Bukkit.broadcast(Component.text(" "));

        // B8 — Discord: alerta de boss derrotado
        plugin.getDiscordManager().sendBossDeathAlert(boss.getName());
    }

    /**
     * Remove todos os bosses ativos (usado no shutdown do servidor).
     */
    public void removeAllBosses() {
        for (WorldBoss boss : new ArrayList<>(activeBosses.values())) {
            removeBoss(boss);
            if (boss.getEntity() != null) {
                boss.getEntity().remove();
            }
        }
        activeBosses.clear();
        atmosphereManager.restoreNormal(); // Forçar restauração imediata no shutdown

        // Limpa blocos temporários pendentes
        for (Location loc : temporaryBlocks) {
            if (loc.getBlock().getType() == Material.BEDROCK) {
                loc.getBlock().setType(Material.AIR);
            }
        }
        temporaryBlocks.clear();

        // Cancela tarefas agendadas
        if (updateTask != null) {
            updateTask.cancel();
        }
        if (spawnTask != null) {
            spawnTask.cancel();
        }

        // B11 — Desliga schedule e raid managers
        if (scheduleManager != null) {
            scheduleManager.shutdown();
        }
        if (raidManager != null) {
            raidManager.shutdown();
        }

        plugin.getLogger().info("§a[BossManager] Todos os bosses e blocos temporários foram removidos.");
    }

    // ==================== GETTERS ====================

    public Map<UUID, WorldBoss> getActiveBosses() {
        return activeBosses;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public AtmosphereManager getAtmosphereManager() {
        return atmosphereManager;
    }

    public BossScheduleManager getScheduleManager() {
        return scheduleManager;
    }

    public BossRaidManager getRaidManager() {
        return raidManager;
    }

    public long getNextSpawnTime() {
        return nextSpawnTime;
    }

    public void setNextSpawnTime(long time) {
        this.nextSpawnTime = time;
    }

    /**
     * Spawna holograma temporário de ranking sobre o loot.
     */
    private void spawnRankingHologram(Location loc, WorldBoss boss) {
        Location holoLoc = loc.clone().add(0, 2.8, 0);
        StringBuilder sb = new StringBuilder(
                plugin.getMessageManager().get("boss_event.ranking_hologram_title", boss.getName()) + "\n");

        List<Map.Entry<UUID, Double>> list = boss.getTopDamagers();
        for (int i = 0; i < Math.min(5, list.size()); i++) {
            UUID uuid = list.get(i).getKey();
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            double damage = list.get(i).getValue();
            sb.append(plugin.getMessageManager().get("boss_event.ranking_hologram_entry", (i + 1),
                    name != null ? name : "Desconhecido", (int) damage)).append("\n");
        }
        sb.append("\n").append(plugin.getMessageManager().get("boss_event.ranking_hologram_footer"));

        spawnHologram(holoLoc, sb.toString());
    }

    private void spawnHologram(Location loc, String text) {
        org.bukkit.entity.TextDisplay display = (org.bukkit.entity.TextDisplay) loc.getWorld()
                .spawn(loc.clone().add(0.5, 0, 0.5), org.bukkit.entity.TextDisplay.class);
        display.text(
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(text));
        display.setAlignment(org.bukkit.entity.TextDisplay.TextAlignment.CENTER);
        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        display.setMetadata("boss_hologram", new FixedMetadataValue(plugin, true));

        // Remove o holograma após 5 minutos (6000 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, display::remove, 6000L);
    }
}
