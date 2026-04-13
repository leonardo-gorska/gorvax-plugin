package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.boss.model.WorldBoss;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B6 — Gerenciador de Scoreboard dinâmico e contextual.
 * Controla a sidebar de cada jogador com informações que mudam conforme o
 * contexto:
 * padrão (reino/saldo), claim (info do terreno) ou boss (HP/ranking).
 */
public class ScoreboardManager {

    private final GorvaxCore plugin;
    private final Map<UUID, Boolean> hudEnabled = new ConcurrentHashMap<>();
    private final Map<UUID, Scoreboard> playerScoreboards = new ConcurrentHashMap<>();
    private BukkitTask updateTask;
    private int updateInterval;
    private boolean globalEnabled;
    private boolean defaultEnabled;

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

    public ScoreboardManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
        startUpdateTask();
        plugin.getLogger().info("§a[ScoreboardManager] Sistema de Scoreboard inicializado!");
    }

    // --- CONFIGURAÇÃO ---

    private void loadConfig() {
        this.globalEnabled = plugin.getConfig().getBoolean("scoreboard.enabled", true);
        this.updateInterval = plugin.getConfig().getInt("scoreboard.update_interval", 40);
        this.defaultEnabled = plugin.getConfig().getBoolean("scoreboard.default_enabled", true);
    }

    public void reload() {
        loadConfig();
        if (updateTask != null) {
            updateTask.cancel();
        }
        if (globalEnabled) {
            startUpdateTask();
            // Re-aplicar para todos os jogadores online
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isHudEnabled(p)) {
                    createScoreboard(p);
                }
            }
        } else {
            // Desligado globalmente — remover de todos
            for (Player p : Bukkit.getOnlinePlayers()) {
                removeScoreboard(p);
            }
        }
    }

    // --- TASK DE ATUALIZAÇÃO ---

    private void startUpdateTask() {
        if (!globalEnabled)
            return;

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isHudEnabled(p) && playerScoreboards.containsKey(p.getUniqueId())) {
                    updateScoreboard(p);
                }
            }
        }, 20L, updateInterval);
    }

    // --- CRIAÇÃO DA SCOREBOARD ---

    public void createScoreboard(Player player) {
        if (!globalEnabled || !isHudEnabled(player))
            return;

        org.bukkit.scoreboard.ScoreboardManager sbManager = Bukkit.getScoreboardManager();
        if (sbManager == null)
            return;

        Scoreboard scoreboard = sbManager.getNewScoreboard();
        playerScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
        updateScoreboard(player);
    }

    // --- ATUALIZAÇÃO DA SCOREBOARD ---

    public void updateScoreboard(Player player) {
        if (!globalEnabled)
            return;

        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null)
            return;

        // Determinar contexto
        List<String> lines = buildLines(player);

        String title = plugin.getMessageManager().get("scoreboard.title");
        // Recria o objective para limpar linhas antigas
        Objective old = scoreboard.getObjective("gorvax_hud");
        if (old != null)
            old.unregister();

        Objective objective = scoreboard.registerNewObjective("gorvax_hud", Criteria.DUMMY,
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .deserialize(title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Aplicar linhas (de cima para baixo)
        int score = lines.size();
        for (String line : lines) {
            // Evitar linhas duplicadas adicionando espaços invisíveis
            String unique = ensureUnique(scoreboard, line);
            objective.getScore(unique).setScore(score--);
        }
    }

    private List<String> buildLines(Player player) {
        // 1. Verificar se há boss ativo perto
        if (hasBossContext(player)) {
            return buildBossLines(player);
        }

        // 2. Verificar se está em um claim
        Claim claim = plugin.getClaimManager().getClaimAt(player.getLocation());
        if (claim != null) {
            return buildClaimLines(player, claim);
        }

        // 3. Padrão
        return buildDefaultLines(player);
    }

    // --- LINHAS PADRÃO ---

    private List<String> buildDefaultLines(Player player) {
        List<String> lines = new ArrayList<>();
        var msg = plugin.getMessageManager();

        lines.add(msg.get("scoreboard.separator"));

        // Reino
        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
        if (kingdom != null) {
            String nome = plugin.getKingdomManager().getNome(kingdom.getId());
            String rank = plugin.getKingdomManager().getKingdomRank(kingdom.getId());
            lines.add(msg.get("scoreboard.kingdom_label"));
            lines.add(msg.get("scoreboard.kingdom_value", rank, nome != null ? nome : "---"));
        } else {
            lines.add(msg.get("scoreboard.kingdom_label"));
            lines.add(msg.get("scoreboard.no_kingdom"));
        }

        lines.add(" ");

        // Blocos
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        int blocks = data != null ? data.getClaimBlocks() : 0;
        lines.add(msg.get("scoreboard.blocks_label"));
        lines.add(msg.get("scoreboard.blocks_value", blocks));

        lines.add("  ");

        // Saldo
        Economy econ = GorvaxCore.getEconomy();
        double balance = econ != null ? econ.getBalance(player) : 0;
        lines.add(msg.get("scoreboard.balance_label"));
        lines.add(msg.get("scoreboard.balance_value", MONEY_FORMAT.format(balance)));

        lines.add("   ");

        // Online
        int online = Bukkit.getOnlinePlayers().size();
        lines.add(msg.get("scoreboard.online_label", online));

        lines.add(msg.get("scoreboard.separator_bottom"));

        return lines;
    }

    // --- LINHAS DE CLAIM ---

    private List<String> buildClaimLines(Player player, Claim claim) {
        List<String> lines = new ArrayList<>();
        var msg = plugin.getMessageManager();

        lines.add(msg.get("scoreboard.separator"));

        String nomeReino = plugin.getKingdomManager().getNome(claim.getId());
        if (nomeReino != null) {
            // Reino
            String rank = plugin.getKingdomManager().getKingdomRank(claim.getId());
            lines.add(msg.get("scoreboard.claim_kingdom", rank, nomeReino));
        } else {
            // Terreno privado
            String ownerName = plugin.getClaimManager().getOwnerName(claim.getOwner());
            lines.add(msg.get("scoreboard.claim_private", ownerName));
        }

        lines.add(" ");

        // Sub-lote
        SubPlot subPlot = claim.getSubPlotAt(player.getLocation());
        if (subPlot != null) {
            if (subPlot.getOwner() != null) {
                String plotOwner = plugin.getClaimManager().getOwnerName(subPlot.getOwner());
                lines.add(msg.get("scoreboard.subplot_owner", plotOwner));
            } else if (subPlot.isForSale()) {
                lines.add(msg.get("scoreboard.subplot_sale", (int) subPlot.getPrice()));
            } else if (subPlot.isForRent() && subPlot.getRenter() == null) {
                lines.add(msg.get("scoreboard.subplot_rent", (int) subPlot.getRentPrice()));
            } else {
                lines.add(msg.get("scoreboard.subplot_vacant"));
            }
        }

        lines.add("  ");

        // Saldo (sempre visível)
        Economy econ = GorvaxCore.getEconomy();
        double balance = econ != null ? econ.getBalance(player) : 0;
        lines.add(msg.get("scoreboard.balance_label"));
        lines.add(msg.get("scoreboard.balance_value", MONEY_FORMAT.format(balance)));

        lines.add("   ");
        lines.add(msg.get("scoreboard.online_label", Bukkit.getOnlinePlayers().size()));
        lines.add(msg.get("scoreboard.separator_bottom"));

        return lines;
    }

    // --- LINHAS DE BOSS ---

    private boolean hasBossContext(Player player) {
        if (plugin.getBossManager() == null)
            return false;
        Map<UUID, WorldBoss> bosses = plugin.getBossManager().getActiveBosses();
        if (bosses.isEmpty())
            return false;

        // Verifica se o jogador está a 80 blocos de algum boss
        for (WorldBoss boss : bosses.values()) {
            if (boss.isAlive() && boss.getEntity() != null
                    && boss.getEntity().getWorld().equals(player.getWorld())
                    && boss.getEntity().getLocation().distance(player.getLocation()) <= 80) {
                return true;
            }
        }
        return false;
    }

    private List<String> buildBossLines(Player player) {
        List<String> lines = new ArrayList<>();
        var msg = plugin.getMessageManager();

        lines.add(msg.get("scoreboard.separator"));
        lines.add(msg.get("scoreboard.boss_header"));
        lines.add(" ");

        Map<UUID, WorldBoss> bosses = plugin.getBossManager().getActiveBosses();
        for (WorldBoss boss : bosses.values()) {
            if (!boss.isAlive() || boss.getEntity() == null)
                continue;
            if (!boss.getEntity().getWorld().equals(player.getWorld()))
                continue;
            if (boss.getEntity().getLocation().distance(player.getLocation()) > 80)
                continue;

            double hp = boss.getEntity().getHealth();
            double maxHp = boss.getEntity().getAttribute(
                    Attribute.GENERIC_MAX_HEALTH).getValue();
            int hpPercent = (int) ((hp / maxHp) * 100);

            lines.add(msg.get("scoreboard.boss_name", boss.getName()));
            lines.add(msg.get("scoreboard.boss_hp", hpPercent, buildHpBar(hp, maxHp)));

            // Dano do jogador
            Map<UUID, Double> damageMap = boss.getDamageDealt();
            double playerDamage = damageMap.getOrDefault(player.getUniqueId(), 0.0);
            if (playerDamage > 0) {
                lines.add(msg.get("scoreboard.boss_your_damage", (int) playerDamage));
            }

            // Ranking do jogador
            List<Map.Entry<UUID, Double>> sorted = damageMap.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                    .toList();
            int rank = 1;
            for (Map.Entry<UUID, Double> entry : sorted) {
                if (entry.getKey().equals(player.getUniqueId())) {
                    lines.add(msg.get("scoreboard.boss_rank", rank, sorted.size()));
                    break;
                }
                rank++;
            }

            lines.add("  ");
        }

        lines.add(msg.get("scoreboard.separator_bottom"));
        return lines;
    }

    private String buildHpBar(double hp, double maxHp) {
        int bars = 10;
        int filled = (int) ((hp / maxHp) * bars);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? "§a█" : "§7█");
        }
        return sb.toString();
    }

    // --- REMOÇÃO ---

    public void removeScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager sbManager = Bukkit.getScoreboardManager();
        if (sbManager != null) {
            player.setScoreboard(sbManager.getMainScoreboard());
        }
        playerScoreboards.remove(player.getUniqueId());
    }

    // --- TOGGLE ---

    public void toggleHud(Player player) {
        UUID uuid = player.getUniqueId();
        boolean current = hudEnabled.getOrDefault(uuid, defaultEnabled);
        hudEnabled.put(uuid, !current);

        if (!current) {
            // Estava off, ligar
            createScoreboard(player);
            plugin.getMessageManager().send(player, "scoreboard.hud_enabled");
        } else {
            // Estava on, desligar
            removeScoreboard(player);
            plugin.getMessageManager().send(player, "scoreboard.hud_disabled");
        }
    }

    public boolean isHudEnabled(Player player) {
        return hudEnabled.getOrDefault(player.getUniqueId(), defaultEnabled);
    }

    // --- CLEANUP ---

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        playerScoreboards.remove(uuid);
        hudEnabled.remove(uuid);
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        playerScoreboards.clear();
        hudEnabled.clear();
    }

    // --- UTIL ---

    /**
     * Garante que cada linha no scoreboard seja única adicionando
     * caracteres de reset invisíveis se necessário.
     */
    private String ensureUnique(Scoreboard sb, String line) {
        String result = line;
        while (sb.getEntries().contains(result)) {
            result = result + "§r";
        }
        // Limitar tamanho (scoreboard tem limite de 40 chars no 1.13+)
        if (result.length() > 40) {
            result = result.substring(0, 40);
        }
        return result;
    }
}
