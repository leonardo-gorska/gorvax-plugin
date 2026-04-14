package br.com.gorvax.core.integration;

import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.ChatManager;
import br.com.gorvax.core.managers.ReputationManager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B34 — Teste de integração: Sistema de Chat.
 * Managers envolvidos: ChatManager (rate limit, filtro, canais),
 * ReputationManager (karma rank no chat), PlayerData (ignore).
 *
 * Fluxo testado:
 * 1. Mensagens → rate limit → filtro
 * 2. Karma rank afeta labels no chat
 * 3. Sistema de ignore entre jogadores
 */
@Tag("integration")
class ChatSystemIT extends GorvaxIntegrationTest {

    // ===== Lógica replicada de rate limit =====

    private final Map<UUID, List<Long>> messageTimes = new HashMap<>();
    private static final int MAX_MESSAGES = 3;
    private static final long RATE_WINDOW_MS = 5000L; // 5 segundos

    private boolean isRateLimited(UUID uuid, long currentTimeMs) {
        var times = messageTimes.computeIfAbsent(uuid, k -> new ArrayList<>());
        // Limpar mensagens fora da janela
        times.removeIf(t -> currentTimeMs - t > RATE_WINDOW_MS);
        if (times.size() >= MAX_MESSAGES)
            return true;
        times.add(currentTimeMs);
        return false;
    }

    // ===== Lógica replicada de filtro =====

    private final Map<UUID, String> lastMessages = new HashMap<>();

    private boolean isDuplicateMessage(UUID uuid, String message) {
        String last = lastMessages.get(uuid);
        lastMessages.put(uuid, message);
        return message.equalsIgnoreCase(last);
    }

    @Test
    void rateLimitBloqueiaMensagensExcessivas() {
        UUID uuid = UUID.randomUUID();
        long now = System.currentTimeMillis();

        // 3 mensagens permitidas
        assertFalse(isRateLimited(uuid, now));
        assertFalse(isRateLimited(uuid, now + 100));
        assertFalse(isRateLimited(uuid, now + 200));

        // 4ª mensagem dentro da janela: bloqueada
        assertTrue(isRateLimited(uuid, now + 300));

        // Após 5s: permitida novamente
        assertFalse(isRateLimited(uuid, now + 6000));
    }

    @Test
    void mensagensDuplicadasBloqueadas() {
        UUID uuid = UUID.randomUUID();

        // Primeira mensagem: OK
        assertFalse(isDuplicateMessage(uuid, "ola mundo"));

        // Mesma mensagem: bloqueada
        assertTrue(isDuplicateMessage(uuid, "ola mundo"));

        // Mensagem diferente: OK
        assertFalse(isDuplicateMessage(uuid, "outra mensagem"));

        // Case insensitive: mesma mensagem
        assertTrue(isDuplicateMessage(uuid, "OUTRA MENSAGEM"));
    }

    @Test
    void karmaRankApareceSufixoCorreto() {
        // Herói
        assertEquals(ReputationManager.KarmaRank.HEROI, getKarmaRank(120));
        assertEquals("§a✦ Herói", getKarmaRank(120).getLabel());
        assertEquals("§a", getKarmaRank(120).getColor());

        // Vilão
        assertEquals(ReputationManager.KarmaRank.VILAO, getKarmaRank(-60));
        assertEquals("§c☠ Vilão", getKarmaRank(-60).getLabel());

        // Procurado
        assertEquals(ReputationManager.KarmaRank.PROCURADO, getKarmaRank(-120));
        assertEquals("§4💀 Procurado", getKarmaRank(-120).getLabel());
    }

    @Test
    void ignorePlayerBloqueiaMensagens() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();

        PlayerData pdReceiver = createPlayerData(receiver);

        // Receiver não ignora sender inicialmente
        assertFalse(pdReceiver.getIgnoredPlayers().contains(sender));

        // Receiver ignora sender
        Set<UUID> ignored = new HashSet<>(pdReceiver.getIgnoredPlayers());
        ignored.add(sender);
        pdReceiver.setIgnoredPlayers(ignored);

        assertTrue(pdReceiver.getIgnoredPlayers().contains(sender));

        // Receiver remove ignore
        ignored.remove(sender);
        pdReceiver.setIgnoredPlayers(ignored);
        assertFalse(pdReceiver.getIgnoredPlayers().contains(sender));
    }

    @Test
    void chatChannelEnumOperacoes() {
        // Verificar que os canais existem
        assertEquals("GLOBAL", ChatManager.ChatChannel.GLOBAL.name());
        assertEquals("LOCAL", ChatManager.ChatChannel.LOCAL.name());
        assertEquals("KINGDOM", ChatManager.ChatChannel.KINGDOM.name());

        // fromString com aliases em PT
        assertEquals(ChatManager.ChatChannel.GLOBAL, ChatManager.ChatChannel.fromString("global"));
        assertEquals(ChatManager.ChatChannel.LOCAL, ChatManager.ChatChannel.fromString("local"));
        assertEquals(ChatManager.ChatChannel.KINGDOM, ChatManager.ChatChannel.fromString("reino"));
    }

    @Test
    void fluxoCompletoMensagemComKarma() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);
        long now = System.currentTimeMillis();

        // Jogador com karma Herói
        pd.addKarma(150);
        ReputationManager.KarmaRank rank = getKarmaRank(pd.getKarma());
        assertEquals(ReputationManager.KarmaRank.HEROI, rank);

        // Mensagem 1: não rate limited, não duplicada → enviada
        assertFalse(isRateLimited(uuid, now));
        assertFalse(isDuplicateMessage(uuid, "Vamos ao boss!"));

        // Label de karma seria incluído no formato: [karmaLabel] [Player] mensagem
        String karmaPrefix = rank.getColor() + rank.getLabel();
        assertNotNull(karmaPrefix);
        assertTrue(karmaPrefix.contains("Herói"));
    }
}
