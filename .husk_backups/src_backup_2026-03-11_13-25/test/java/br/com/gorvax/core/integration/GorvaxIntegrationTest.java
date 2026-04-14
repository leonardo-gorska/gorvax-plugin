package br.com.gorvax.core.integration;

import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.ReputationManager;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * B34 — Classe base para testes de integração do GorvaxCore.
 * Fornece helpers compartilhados para criação de dados de teste,
 * replicação de lógica cross-module e utilitários comuns.
 *
 * Os testes de integração validam fluxos end-to-end que cruzam
 * múltiplos managers, replicando a lógica pura sem dependências Bukkit.
 */
public abstract class GorvaxIntegrationTest {

    // ===== Helpers para criação de dados =====

    /**
     * Cria um PlayerData com valores default para testes.
     */
    protected PlayerData createPlayerData(UUID uuid) {
        return new PlayerData(uuid, 0);
    }

    /**
     * Cria um PlayerData com stats pré-definidos para simular um jogador ativo.
     * Usa os setters disponíveis no PlayerData.
     */
    protected PlayerData createActivePlayer(UUID uuid, int kills, int deaths,
            int bossesKilled, long playtimeMs, int blocksBroken) {
        PlayerData pd = new PlayerData(uuid, 0);
        for (int i = 0; i < kills; i++)
            pd.incrementKills();
        for (int i = 0; i < deaths; i++)
            pd.incrementDeaths();
        for (int i = 0; i < bossesKilled; i++)
            pd.incrementBossesKilled();
        pd.addPlayTime(playtimeMs);
        for (int i = 0; i < blocksBroken; i++)
            pd.incrementBlocksBroken();
        return pd;
    }

    // ===== Lógica replicada de ReputationManager =====

    protected static final int HERO_THRESHOLD = 100;
    protected static final int GOOD_THRESHOLD = 50;
    protected static final int VILLAIN_THRESHOLD = -50;
    protected static final int WANTED_THRESHOLD = -100;

    protected ReputationManager.KarmaRank getKarmaRank(int karma) {
        if (karma >= HERO_THRESHOLD)
            return ReputationManager.KarmaRank.HEROI;
        if (karma >= GOOD_THRESHOLD)
            return ReputationManager.KarmaRank.BOM;
        if (karma > VILLAIN_THRESHOLD)
            return ReputationManager.KarmaRank.NEUTRO;
        if (karma > WANTED_THRESHOLD)
            return ReputationManager.KarmaRank.VILAO;
        return ReputationManager.KarmaRank.PROCURADO;
    }

    protected double getMarketDiscount(int karma, double heroDiscount) {
        if (karma >= HERO_THRESHOLD)
            return heroDiscount;
        return 0.0;
    }

    protected double getPriceMultiplier(int karma) {
        if (karma <= WANTED_THRESHOLD)
            return 1.25;
        if (karma <= VILLAIN_THRESHOLD)
            return 1.10;
        return 1.0;
    }

    // ===== Lógica replicada de RankManager =====

    /**
     * Verifica se um PlayerData atende os requisitos de um rank.
     * Replica checkAndPromote() sem dependências Bukkit (ignora balance e kingdom
     * level).
     */
    protected boolean meetsRankRequirements(PlayerData pd,
            long playtimeHours, int blocksBroken, int kills, int bossesKilled) {
        long hoursPlayed = pd.getTotalPlayTime() / (1000L * 60L * 60L);
        if (hoursPlayed < playtimeHours)
            return false;
        if (pd.getTotalBlocksBroken() < blocksBroken)
            return false;
        if (pd.getTotalKills() < kills)
            return false;
        if (pd.getBossesKilled() < bossesKilled)
            return false;
        return true;
    }

    // ===== Lógica replicada de KingdomManager =====

    protected String getKingdomRank(int suditos) {
        if (suditos >= 50)
            return "§6§lImpério";
        if (suditos >= 20)
            return "§e§lReino";
        if (suditos >= 10)
            return "§b§lVila";
        return "§7Acampamento";
    }

    protected double getPassiveXpBonus(int suditos) {
        return (suditos / 5) * 0.02;
    }

    // ===== Lógica replicada de BattlePassManager =====

    /**
     * Calcula XP necessário para um nível do Battle Pass.
     * Fórmula: nível * 100 (ex: nível 5 = 500 XP)
     */
    protected int getXpForLevel(int level) {
        return level * 100;
    }

    // ===== Lógica replicada do CombatManager =====

    protected final Map<UUID, Long> combatTags = new ConcurrentHashMap<>();
    protected final Map<UUID, Integer> killStreaks = new ConcurrentHashMap<>();

    protected void tagPlayer(UUID uuid, long durationMs) {
        combatTags.put(uuid, System.currentTimeMillis() + durationMs);
    }

    protected boolean isInCombat(UUID uuid) {
        Long expiry = combatTags.get(uuid);
        if (expiry == null)
            return false;
        if (System.currentTimeMillis() > expiry) {
            combatTags.remove(uuid);
            return false;
        }
        return true;
    }

    protected void registerKill(UUID killer) {
        killStreaks.merge(killer, 1, Integer::sum);
    }

    protected void resetKillStreak(UUID uuid) {
        killStreaks.remove(uuid);
    }

    protected int getKillStreak(UUID uuid) {
        return killStreaks.getOrDefault(uuid, 0);
    }
}
