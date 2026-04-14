package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * B18 — Gerencia o sistema de Reputação / Karma.
 * Karma é um valor int por jogador que reflete seu comportamento.
 * Ações positivas aumentam karma, ações negativas diminuem.
 * O karma afeta descontos no mercado, preço de claims e revela jogadores
 * procurados.
 */
public class ReputationManager {

    private final GorvaxCore plugin;

    // ===== Configurações carregadas do config.yml =====
    private int killPenalty;
    private int bossReward;
    private int marketReward;
    private int questReward;
    private double heroDiscount; // % desconto no mercado para Heróis
    private double heroClaimDiscount; // % desconto em claims para Heróis
    private int bountyThreshold; // Karma para bounty automática
    private int revealIntervalMinutes; // Intervalo de reveal para Procurados
    private boolean enabled;

    // Limites dos ranks de karma
    private static final int HERO_THRESHOLD = 100;
    private static final int GOOD_THRESHOLD = 50;
    private static final int VILLAIN_THRESHOLD = -50;
    private static final int WANTED_THRESHOLD = -100;

    public ReputationManager(GorvaxCore plugin) {
        this.plugin = plugin;
        reload();
        startRevealTask();
    }

    /**
     * Carrega/recarrega configurações do config.yml.
     */
    public void reload() {
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("karma.enabled", true);
        this.killPenalty = config.getInt("karma.kill_penalty", 3);
        this.bossReward = config.getInt("karma.boss_reward", 5);
        this.marketReward = config.getInt("karma.market_reward", 1);
        this.questReward = config.getInt("karma.quest_reward", 2);
        this.heroDiscount = config.getDouble("karma.hero_discount", 5.0);
        this.heroClaimDiscount = config.getDouble("karma.hero_claim_discount", 5.0);
        this.bountyThreshold = config.getInt("karma.bounty_threshold", -100);
        this.revealIntervalMinutes = config.getInt("karma.reveal_interval_minutes", 5);
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ===== Enumeração de Ranks de Karma =====

    public enum KarmaRank {
        HEROI("§a✦ Herói", "§a", HERO_THRESHOLD),
        BOM("§2☘ Bom", "§2", GOOD_THRESHOLD),
        NEUTRO("§7⚖ Neutro", "§7", 0),
        VILAO("§c☠ Vilão", "§c", VILLAIN_THRESHOLD),
        PROCURADO("§4💀 Procurado", "§4", WANTED_THRESHOLD);

        private final String label;
        private final String color;
        private final int threshold;

        KarmaRank(String label, String color, int threshold) {
            this.label = label;
            this.color = color;
            this.threshold = threshold;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }

        public int getThreshold() {
            return threshold;
        }
    }

    /**
     * Retorna o rank de karma do jogador baseado no valor.
     */
    public KarmaRank getKarmaRank(int karma) {
        if (karma >= HERO_THRESHOLD)
            return KarmaRank.HEROI;
        if (karma >= GOOD_THRESHOLD)
            return KarmaRank.BOM;
        if (karma > VILLAIN_THRESHOLD)
            return KarmaRank.NEUTRO;
        if (karma > WANTED_THRESHOLD)
            return KarmaRank.VILAO;
        return KarmaRank.PROCURADO;
    }

    /**
     * Retorna a cor associada ao karma.
     */
    public String getKarmaColor(int karma) {
        return getKarmaRank(karma).getColor();
    }

    /**
     * Retorna o label legível do rank.
     */
    public String getKarmaLabel(int karma) {
        return getKarmaRank(karma).getLabel();
    }

    /**
     * Modifica o karma de um jogador, notifica e salva.
     * 
     * @param player Jogador
     * @param amount Valor a adicionar (negativo para remover)
     * @param reason Motivo (chave de mensagem) — usado para notificação
     */
    public void modifyKarma(Player player, int amount, String reason) {
        if (!enabled)
            return;

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        KarmaRank oldRank = getKarmaRank(pd.getKarma());

        pd.addKarma(amount);
        plugin.getPlayerDataManager().saveData(player.getUniqueId());

        KarmaRank newRank = getKarmaRank(pd.getKarma());

        // Notificar mudança de karma
        var msg = plugin.getMessageManager();
        String sign = amount > 0 ? "§a+" : "§c";
        player.sendMessage(msg.get("karma.changed", sign + amount, reason, pd.getKarma()));

        // Notificar se mudou de rank
        if (oldRank != newRank) {
            player.sendMessage(msg.get("karma.rank_changed", newRank.getLabel()));

            // Se atingiu Procurado, broadcast
            if (newRank == KarmaRank.PROCURADO && oldRank != KarmaRank.PROCURADO) {
                Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                        msg.get("karma.now_wanted", player.getName())));

                // Bounty automática via BountyManager
                if (plugin.getBountyManager() != null) {
                    double autoBounty = plugin.getConfig().getDouble("karma.auto_bounty_value", 500.0);
                    plugin.getBountyManager().placeBounty(null, player.getUniqueId(), player.getName(), autoBounty);
                    player.sendMessage(msg.get("karma.auto_bounty", String.format("%.0f", autoBounty)));
                }
            }
        }
    }

    /**
     * Retorna o percentual de desconto no mercado baseado no karma.
     * Karma >= 100 (Herói): desconto configurável (padrão 5%)
     */
    public double getMarketDiscount(int karma) {
        if (!enabled)
            return 0.0;
        if (karma >= HERO_THRESHOLD)
            return heroDiscount;
        return 0.0;
    }

    /**
     * Retorna o percentual de desconto em claims baseado no karma.
     */
    public double getClaimDiscount(int karma) {
        if (!enabled)
            return 0.0;
        if (karma >= HERO_THRESHOLD)
            return heroClaimDiscount;
        return 0.0;
    }

    /**
     * Retorna o multiplicador de preço para jogadores com karma negativo.
     * Vilão: +10% | Procurado: +25%
     */
    public double getPriceMultiplier(int karma) {
        if (!enabled)
            return 1.0;
        if (karma <= WANTED_THRESHOLD)
            return 1.25;
        if (karma <= VILLAIN_THRESHOLD)
            return 1.10;
        return 1.0;
    }

    // ===== Getters de configuração =====

    public int getKillPenalty() {
        return killPenalty;
    }

    public int getBossReward() {
        return bossReward;
    }

    public int getMarketReward() {
        return marketReward;
    }

    public int getQuestReward() {
        return questReward;
    }

    /**
     * Task periódica: revela localização de jogadores "Procurados" no broadcast.
     */
    private void startRevealTask() {
        long ticks = revealIntervalMinutes * 60L * 20L; // Minutos → ticks
        if (ticks <= 0)
            ticks = 6000L; // Fallback 5 minutos

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled)
                return;

            var msg = plugin.getMessageManager();
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
                if (pd.getKarma() <= WANTED_THRESHOLD) {
                    int x = player.getLocation().getBlockX();
                    int z = player.getLocation().getBlockZ();
                    String world = player.getWorld().getName();
                    Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                            msg.get("karma.reveal_location",
                                    player.getName(), world, x, z)));
                }
            }
        }, ticks, ticks);
    }
}
