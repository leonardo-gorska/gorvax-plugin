package br.com.gorvax.core.boss.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.boss.model.WorldBoss;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * B11.2 — Gerenciador de Boss Raids (eventos cooperativos em ondas).
 * Spawna bosses em sequência; cada onda sobrevivida aumenta o multiplicador de
 * loot.
 */
public class BossRaidManager {

    private final GorvaxCore plugin;

    // Configuração
    private List<String> waveSequence = new ArrayList<>();
    private int minPlayers = 3;
    private int playerRadius = 100;
    private int waveCooldownSeconds = 30;
    private double lootMultiplierPerWave = 0.2;

    // Estado da raid
    private boolean raidActive = false;
    private int currentWave = 0;
    private int totalWaves = 0;
    private Location raidLocation;
    private BossBar raidBar;
    private UUID currentBossUUID;
    private BukkitTask cooldownTask;

    // Tracking de participantes
    private final Set<UUID> participants = Collections.synchronizedSet(new HashSet<>());
    private final Map<UUID, Double> totalDamageByPlayer = Collections.synchronizedMap(new HashMap<>());

    public BossRaidManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
        plugin.getLogger()
                .info("§a[BossRaidManager] Sistema de raids inicializado com " + waveSequence.size() + " ondas.");
    }

    /**
     * Carrega configuração de raids do config.yml.
     */
    private void loadConfig() {
        waveSequence = plugin.getConfig().getStringList("boss.raid.waves");
        minPlayers = plugin.getConfig().getInt("boss.raid.min_players", 3);
        playerRadius = plugin.getConfig().getInt("boss.raid.player_radius", 100);
        waveCooldownSeconds = plugin.getConfig().getInt("boss.raid.wave_cooldown", 30);
        lootMultiplierPerWave = plugin.getConfig().getDouble("boss.raid.loot_multiplier_per_wave", 0.2);
        totalWaves = waveSequence.size();
    }

    /**
     * Inicia uma raid na localização fornecida.
     * 
     * @return true se a raid foi iniciada, false se não foi possível
     */
    public boolean startRaid(Location location) {
        if (raidActive)
            return false;
        if (!plugin.getConfig().getBoolean("boss.raid.enabled", false))
            return false;
        if (waveSequence.isEmpty())
            return false;

        // Verificar número mínimo de jogadores no raio
        int nearbyPlayers = countNearbyPlayers(location);
        if (nearbyPlayers < minPlayers) {
            return false;
        }

        // B19 — Evento customizado: RaidStartEvent (cancelável)
        br.com.gorvax.core.events.RaidStartEvent raidEvent = new br.com.gorvax.core.events.RaidStartEvent(location);
        org.bukkit.Bukkit.getPluginManager().callEvent(raidEvent);
        if (raidEvent.isCancelled())
            return false;

        this.raidActive = true;
        this.currentWave = 0;
        this.raidLocation = location.clone();
        this.participants.clear();
        this.totalDamageByPlayer.clear();

        // Cria BossBar global para a raid
        raidBar = Bukkit.createBossBar(
                "§c§l⚔ BOSS RAID §8— §fPreparando...",
                BarColor.RED,
                BarStyle.SEGMENTED_6);
        raidBar.setVisible(true);

        // Adicionar todos os jogadores online à bossbar
        for (Player p : Bukkit.getOnlinePlayers()) {
            raidBar.addPlayer(p);
        }

        // Broadcast de início
        plugin.getMessageManager().broadcast("boss_raid.started", totalWaves);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 2.0f, 0.7f);
        }

        // B8 — Discord: alerta de raid iniciada
        plugin.getDiscordManager().sendRaidStartAlert(totalWaves, minPlayers);

        // Inicia primeira onda após 5 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> startNextWave(), 100L);

        return true;
    }

    /**
     * Inicia a próxima onda de bosses.
     */
    private void startNextWave() {
        if (!raidActive)
            return;

        currentWave++;
        if (currentWave > totalWaves) {
            endRaid(true);
            return;
        }

        // Verificar se ainda há jogadores suficientes
        int nearbyPlayers = countNearbyPlayers(raidLocation);
        if (nearbyPlayers < minPlayers) {
            endRaid(false);
            return;
        }

        String bossId = waveSequence.get(currentWave - 1);

        // Atualizar BossBar
        double progress = (double) (currentWave - 1) / totalWaves;
        raidBar.setTitle("§c§l⚔ BOSS RAID §8— §fOnda §e" + currentWave + "§f/§e" + totalWaves);
        raidBar.setProgress(Math.min(1.0, progress));
        raidBar.setColor(getBarColorForWave(currentWave));

        // Broadcast da onda
        String bossName = resolveBossDisplayName(bossId);
        plugin.getMessageManager().broadcast("boss_raid.wave_start", currentWave, totalWaves, bossName);

        // Sons épicos com intensidade crescente
        float pitch = 1.0f - (currentWave * 0.1f);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, Math.max(0.5f, pitch));
        }

        // Spawna o boss da onda
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getBossManager().spawnBoss(bossId, raidLocation);

            // Encontra o UUID do boss recém-spawnado
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                var activeBosses = plugin.getBossManager().getActiveBosses();
                for (Map.Entry<UUID, WorldBoss> entry : activeBosses.entrySet()) {
                    WorldBoss boss = entry.getValue();
                    if (boss.getEntity() != null && boss.getEntity().isValid()) {
                        currentBossUUID = entry.getKey();
                        break;
                    }
                }
            }, 5L);
        });

        // Registrar jogadores próximos como participantes
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getLocation().distance(raidLocation) <= playerRadius) {
                participants.add(p.getUniqueId());
            }
        }
    }

    /**
     * Chamado quando um boss da raid morre.
     * Verifica se é o boss da onda atual e avança.
     */
    public void onBossDeath(UUID bossUUID) {
        if (!raidActive)
            return;
        if (currentBossUUID == null || !currentBossUUID.equals(bossUUID))
            return;

        // Coletar dano dos participantes desta onda
        WorldBoss boss = plugin.getBossManager().getActiveBosses().get(bossUUID);
        if (boss != null) {
            Map<UUID, Double> waveDamage = boss.getDamageDealt();
            for (Map.Entry<UUID, Double> entry : waveDamage.entrySet()) {
                totalDamageByPlayer.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        currentBossUUID = null;

        // Onda completa!
        plugin.getMessageManager().broadcast("boss_raid.wave_complete", currentWave, totalWaves);

        // Se era a última onda → raid completa
        if (currentWave >= totalWaves) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> endRaid(true), 60L);
            return;
        }

        // Cooldown antes da próxima onda
        plugin.getMessageManager().broadcast("boss_raid.wave_cooldown", waveCooldownSeconds);

        raidBar.setTitle("§e§l⏳ PRÓXIMA ONDA §8— §f" + waveCooldownSeconds + "s");
        raidBar.setColor(BarColor.YELLOW);

        // Contagem regressiva visual
        final int[] countdown = { waveCooldownSeconds };
        cooldownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            countdown[0]--;
            if (countdown[0] <= 0) {
                cooldownTask.cancel();
                startNextWave();
            } else {
                double cd = (double) countdown[0] / waveCooldownSeconds;
                raidBar.setProgress(Math.max(0.0, Math.min(1.0, cd)));
                raidBar.setTitle("§e§l⏳ PRÓXIMA ONDA §8— §f" + countdown[0] + "s");

                // Som de tick nos últimos 5 segundos
                if (countdown[0] <= 5) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
                    }
                }
            }
        }, 20L, 20L);
    }

    /**
     * Finaliza a raid.
     * 
     * @param success true se todas as ondas foram completadas
     */
    public void endRaid(boolean success) {
        if (!raidActive)
            return;
        raidActive = false;

        if (cooldownTask != null) {
            cooldownTask.cancel();
            cooldownTask = null;
        }

        if (success) {
            // Raid vitoriosa! Distribuir loot com multiplicador
            double multiplier = 1.0 + (lootMultiplierPerWave * totalWaves);

            plugin.getMessageManager().broadcast("boss_raid.victory", totalWaves,
                    String.format("%.0f%%", multiplier * 100));

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
            }

            // Distribuir loot com multiplicador de raid
            distributeRaidLoot(multiplier);
        } else {
            // Raid falhou
            plugin.getMessageManager().broadcast("boss_raid.failed", currentWave, totalWaves);

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
            }
        }

        // Limpar BossBar
        if (raidBar != null) {
            raidBar.removeAll();
            raidBar.setVisible(false);
            raidBar = null;
        }

        // Limpar estado
        currentBossUUID = null;
        currentWave = 0;
        participants.clear();
        totalDamageByPlayer.clear();
    }

    /**
     * Distribui loot de raid para os participantes baseado no dano total.
     */
    private void distributeRaidLoot(double multiplier) {
        if (totalDamageByPlayer.isEmpty())
            return;

        // Ordenar por dano total
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(totalDamageByPlayer.entrySet());
        sorted.sort(Map.Entry.<UUID, Double>comparingByValue().reversed());

        int rank = 1;
        for (Map.Entry<UUID, Double> entry : sorted) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                // Gera loot com multiplicador de raid
                plugin.getBossManager().getLootManager().generateLoot(player, rank,
                        waveSequence.get(totalWaves - 1), multiplier, "§c⚔ §fRecompensa de Raid");

                player.sendMessage(plugin.getMessageManager().get("boss_raid.personal_reward",
                        rank, String.format("%.0f", entry.getValue()),
                        String.format("%.0f%%", multiplier * 100)));
            }
            rank++;
        }
    }

    /**
     * Conta jogadores num raio da localização.
     */
    private int countNearbyPlayers(Location loc) {
        if (loc == null || loc.getWorld() == null)
            return 0;
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distance(loc) <= playerRadius) {
                count++;
            }
        }
        return count;
    }

    /**
     * Cor da BossBar baseada no número da onda.
     */
    private BarColor getBarColorForWave(int wave) {
        return switch (wave) {
            case 1 -> BarColor.GREEN;
            case 2 -> BarColor.YELLOW;
            case 3 -> BarColor.PINK;
            case 4 -> BarColor.RED;
            default -> BarColor.PURPLE;
        };
    }

    /**
     * Resolve nome de exibição do boss.
     */
    private String resolveBossDisplayName(String bossId) {
        return switch (bossId.toLowerCase()) {
            case "rei_gorvax" -> "§6Rei Gorvax";
            case "indrax_abissal", "indrax" -> "§5Indrax Abissal";
            case "vulgathor" -> "§cVulgathor";
            case "xylos" -> "§dXylos Devorador";
            case "skulkor" -> "§7Skulkor";
            case "kaldur" -> "§bKaldur";
            case "zarith" -> "§aZar'ith";
            case "rei_indrax" -> "§4Ruptura Temporal";
            case "halloween_boss" -> "§5Ceifador das Sombras";
            case "natal_boss" -> "§bRei do Gelo Eterno";
            default -> "§eBoss Desconhecido";
        };
    }

    // ========== Getters e Estado ==========

    public boolean isRaidActive() {
        return raidActive;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getTotalWaves() {
        return totalWaves;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public Set<UUID> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }

    /**
     * Recarrega configuração.
     */
    public void reload() {
        loadConfig();
        plugin.getLogger().info("[BossRaidManager] Configuração recarregada: " + waveSequence.size() + " ondas.");
    }

    /**
     * Desliga o sistema de raids (usa no onDisable).
     */
    public void shutdown() {
        if (raidActive) {
            endRaid(false);
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("boss.raid.enabled", false) && !waveSequence.isEmpty();
    }
}
