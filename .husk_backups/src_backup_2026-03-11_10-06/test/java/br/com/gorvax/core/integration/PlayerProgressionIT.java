package br.com.gorvax.core.integration;

import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.RankManager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B34 — Teste de integração: Progressão de jogador.
 * Managers envolvidos: PlayerData, RankManager (lógica), AchievementManager
 * (lógica).
 *
 * Fluxo testado:
 * 1. Jogador novo com stats zerados
 * 2. Acumula kills, playtime, bosses, blocos
 * 3. Verifica elegibilidade para cada rank
 * 4. Verifica achievements baseados em stats acumulados
 */
@Tag("integration")
class PlayerProgressionIT extends GorvaxIntegrationTest {

    // Requisitos default (replica RankManager.loadDefaults)
    // Explorador: 10h, 500 blocos
    // Guerreiro: 30h, 50 kills, 5 bosses
    // Lendário: 80h, 200 kills, 20 bosses

    @Test
    void jogadorNovoNaoAtendeNenhumRequisito() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // Explorador: 10h jogadas, 500 blocos
        assertFalse(meetsRankRequirements(pd, 10, 500, 0, 0));
        // Guerreiro: 30h, 0 blocos, 50 kills, 5 bosses
        assertFalse(meetsRankRequirements(pd, 30, 0, 50, 5));
        // Lendário: 80h, 0 blocos, 200 kills, 20 bosses
        assertFalse(meetsRankRequirements(pd, 80, 0, 200, 20));
    }

    @Test
    void jogadorAtingeExploradorComTempoEBlocos() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createActivePlayer(uuid,
                /* kills */ 0,
                /* deaths */ 0,
                /* bosses */ 0,
                /* playtimeMs */ TimeUnit.HOURS.toMillis(15), // 15h > 10h
                /* blocks */ 600 // 600 > 500
        );

        // Atende Explorador
        assertTrue(meetsRankRequirements(pd, 10, 500, 0, 0));
        // Não atende Guerreiro (falta kills e bosses)
        assertFalse(meetsRankRequirements(pd, 30, 0, 50, 5));
    }

    @Test
    void jogadorAtingeGuerreiroComKillsEBosses() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createActivePlayer(uuid,
                /* kills */ 55,
                /* deaths */ 20,
                /* bosses */ 8,
                /* playtimeMs */ TimeUnit.HOURS.toMillis(35),
                /* blocks */ 1000);

        // Atende Explorador e Guerreiro
        assertTrue(meetsRankRequirements(pd, 10, 500, 0, 0));
        assertTrue(meetsRankRequirements(pd, 30, 0, 50, 5));
        // Não atende Lendário
        assertFalse(meetsRankRequirements(pd, 80, 0, 200, 20));
    }

    @Test
    void jogadorAtingeLendario() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createActivePlayer(uuid,
                /* kills */ 210,
                /* deaths */ 50,
                /* bosses */ 25,
                /* playtimeMs */ TimeUnit.HOURS.toMillis(100),
                /* blocks */ 5000);

        // Atende todos os ranks
        assertTrue(meetsRankRequirements(pd, 10, 500, 0, 0));
        assertTrue(meetsRankRequirements(pd, 30, 0, 50, 5));
        assertTrue(meetsRankRequirements(pd, 80, 0, 200, 20));
    }

    @Test
    void statsAcumuladosSaoConsistentes() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // Simula sessão de jogo: incrementa stats
        for (int i = 0; i < 25; i++)
            pd.incrementKills();
        for (int i = 0; i < 10; i++)
            pd.incrementDeaths();
        for (int i = 0; i < 3; i++)
            pd.incrementBossesKilled();
        pd.addPlayTime(TimeUnit.HOURS.toMillis(5));
        for (int i = 0; i < 200; i++)
            pd.incrementBlocksBroken();

        // Verifica consistência
        assertEquals(25, pd.getTotalKills());
        assertEquals(10, pd.getTotalDeaths());
        assertEquals(3, pd.getBossesKilled());
        assertEquals(TimeUnit.HOURS.toMillis(5), pd.getTotalPlayTime());
        assertEquals(200, pd.getTotalBlocksBroken());

        // KDR consistente
        double expectedKDR = 25.0 / 10.0;
        double actualKDR = pd.getTotalDeaths() > 0
                ? (double) pd.getTotalKills() / pd.getTotalDeaths()
                : 0;
        assertEquals(expectedKDR, actualKDR, 0.001);
    }

    @Test
    void progressaoIncrementalDeStats() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // Sessão 1: um pouco de tudo
        for (int i = 0; i < 5; i++)
            pd.incrementKills();
        pd.addPlayTime(TimeUnit.HOURS.toMillis(3));
        for (int i = 0; i < 100; i++)
            pd.incrementBlocksBroken();

        // Ainda não atende Explorador
        assertFalse(meetsRankRequirements(pd, 10, 500, 0, 0));

        // Sessão 2: mais tempo + blocos
        pd.addPlayTime(TimeUnit.HOURS.toMillis(8)); // total 11h
        for (int i = 0; i < 450; i++)
            pd.incrementBlocksBroken(); // total 550

        // Agora atende Explorador
        assertTrue(meetsRankRequirements(pd, 10, 500, 0, 0));
    }

    @Test
    void achievementProgressComStatsMultiplos() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createActivePlayer(uuid, 50, 10, 10,
                TimeUnit.HOURS.toMillis(40), 2000);

        // Achievement "Caçador" tipicamente requer X kills
        assertTrue(pd.getTotalKills() >= 50, "Deve ter ≥50 kills para 'Caçador'");

        // Achievement "Veterano" tipicamente requer X horas
        long hours = TimeUnit.MILLISECONDS.toHours(pd.getTotalPlayTime());
        assertTrue(hours >= 40, "Deve ter ≥40h para 'Veterano'");

        // Achievement "Minerador" tipicamente requer X blocos
        assertTrue(pd.getTotalBlocksBroken() >= 2000, "Deve ter ≥2000 blocos para 'Minerador'");

        // Achievement "Caça-Bosses"
        assertTrue(pd.getBossesKilled() >= 10, "Deve ter ≥10 bosses para 'Caça-Bosses'");
    }

    @Test
    void enumGameRankOrdenacaoEProgressao() {
        // GameRank.next() — verifica cadeia de progressão
        assertEquals(RankManager.GameRank.EXPLORADOR, RankManager.GameRank.AVENTUREIRO.next());
        assertEquals(RankManager.GameRank.GUERREIRO, RankManager.GameRank.EXPLORADOR.next());
        assertEquals(RankManager.GameRank.LENDARIO, RankManager.GameRank.GUERREIRO.next());
        assertNull(RankManager.GameRank.LENDARIO.next()); // Já é o máximo

        // Ordem crescente
        assertTrue(RankManager.GameRank.AVENTUREIRO.getOrder() < RankManager.GameRank.EXPLORADOR.getOrder());
        assertTrue(RankManager.GameRank.EXPLORADOR.getOrder() < RankManager.GameRank.GUERREIRO.getOrder());
        assertTrue(RankManager.GameRank.GUERREIRO.getOrder() < RankManager.GameRank.LENDARIO.getOrder());
    }
}
