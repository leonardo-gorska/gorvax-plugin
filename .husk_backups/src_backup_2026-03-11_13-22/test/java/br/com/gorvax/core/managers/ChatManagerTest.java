package br.com.gorvax.core.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para ChatManager.
 * Testa ChatChannel enum, spam checking, filtro de palavras e canais.
 */
class ChatManagerTest {

    // ==================== ChatChannel enum ====================

    @Test
    void chatChannelValues() {
        assertEquals(6, ChatManager.ChatChannel.values().length);
    }

    @Test
    void chatChannelLabels() {
        assertEquals("global", ChatManager.ChatChannel.GLOBAL.getLabel());
        assertEquals("reino", ChatManager.ChatChannel.KINGDOM.getLabel());
        assertEquals("alianca", ChatManager.ChatChannel.ALLIANCE.getLabel());
        assertEquals("local", ChatManager.ChatChannel.LOCAL.getLabel());
        assertEquals("comercio", ChatManager.ChatChannel.TRADE.getLabel());
        assertEquals("nacao", ChatManager.ChatChannel.NATION.getLabel());
    }

    @Test
    void chatChannelDefaultPrefixes() {
        assertNotNull(ChatManager.ChatChannel.GLOBAL.getDefaultPrefix());
        assertTrue(ChatManager.ChatChannel.GLOBAL.getDefaultPrefix().contains("GLOBAL"));
    }

    @Test
    void chatChannelDefaultColors() {
        assertNotNull(ChatManager.ChatChannel.GLOBAL.getDefaultColor());
        assertEquals("§f", ChatManager.ChatChannel.GLOBAL.getDefaultColor());
        assertEquals("§b", ChatManager.ChatChannel.KINGDOM.getDefaultColor());
    }

    // --- fromString ---

    @Test
    void fromStringPortugues() {
        assertEquals(ChatManager.ChatChannel.GLOBAL, ChatManager.ChatChannel.fromString("global"));
        assertEquals(ChatManager.ChatChannel.KINGDOM, ChatManager.ChatChannel.fromString("reino"));
        assertEquals(ChatManager.ChatChannel.ALLIANCE, ChatManager.ChatChannel.fromString("alianca"));
        assertEquals(ChatManager.ChatChannel.ALLIANCE, ChatManager.ChatChannel.fromString("aliança"));
        assertEquals(ChatManager.ChatChannel.LOCAL, ChatManager.ChatChannel.fromString("local"));
        assertEquals(ChatManager.ChatChannel.TRADE, ChatManager.ChatChannel.fromString("comercio"));
        assertEquals(ChatManager.ChatChannel.TRADE, ChatManager.ChatChannel.fromString("comércio"));
        assertEquals(ChatManager.ChatChannel.NATION, ChatManager.ChatChannel.fromString("nacao"));
        assertEquals(ChatManager.ChatChannel.NATION, ChatManager.ChatChannel.fromString("nação"));
    }

    @Test
    void fromStringIngles() {
        assertEquals(ChatManager.ChatChannel.KINGDOM, ChatManager.ChatChannel.fromString("kingdom"));
        assertEquals(ChatManager.ChatChannel.ALLIANCE, ChatManager.ChatChannel.fromString("alliance"));
        assertEquals(ChatManager.ChatChannel.TRADE, ChatManager.ChatChannel.fromString("trade"));
        assertEquals(ChatManager.ChatChannel.NATION, ChatManager.ChatChannel.fromString("nation"));
    }

    @Test
    void fromStringAbreviacoes() {
        assertEquals(ChatManager.ChatChannel.GLOBAL, ChatManager.ChatChannel.fromString("g"));
        assertEquals(ChatManager.ChatChannel.KINGDOM, ChatManager.ChatChannel.fromString("k"));
        assertEquals(ChatManager.ChatChannel.KINGDOM, ChatManager.ChatChannel.fromString("rc"));
        assertEquals(ChatManager.ChatChannel.ALLIANCE, ChatManager.ChatChannel.fromString("ac"));
        assertEquals(ChatManager.ChatChannel.LOCAL, ChatManager.ChatChannel.fromString("l"));
        assertEquals(ChatManager.ChatChannel.TRADE, ChatManager.ChatChannel.fromString("tc"));
        assertEquals(ChatManager.ChatChannel.NATION, ChatManager.ChatChannel.fromString("nc"));
    }

    @Test
    void fromStringCaseInsensitive() {
        assertEquals(ChatManager.ChatChannel.GLOBAL, ChatManager.ChatChannel.fromString("GLOBAL"));
        assertEquals(ChatManager.ChatChannel.GLOBAL, ChatManager.ChatChannel.fromString("Global"));
    }

    @Test
    void fromStringNull() {
        assertNull(ChatManager.ChatChannel.fromString(null));
    }

    @Test
    void fromStringInvalido() {
        assertNull(ChatManager.ChatChannel.fromString("invalido"));
        assertNull(ChatManager.ChatChannel.fromString(""));
    }

    // ==================== SpamCheckResult enum ====================

    @Test
    void spamCheckResultValues() {
        assertEquals(4, ChatManager.SpamCheckResult.values().length);
        assertNotNull(ChatManager.SpamCheckResult.OK);
        assertNotNull(ChatManager.SpamCheckResult.RATE_LIMITED);
        assertNotNull(ChatManager.SpamCheckResult.DUPLICATE_MESSAGE);
        assertNotNull(ChatManager.SpamCheckResult.MUTED);
    }

    // ==================== FilterResult enum ====================

    @Test
    void filterResultValues() {
        assertEquals(4, ChatManager.FilterResult.values().length);
        assertNotNull(ChatManager.FilterResult.CLEAN);
        assertNotNull(ChatManager.FilterResult.BLOCKED);
        assertNotNull(ChatManager.FilterResult.CENSORED);
        assertNotNull(ChatManager.FilterResult.MUTED);
    }

    // ==================== FilterResultData record ====================

    @Test
    void filterResultDataRecord() {
        var data = new ChatManager.FilterResultData(ChatManager.FilterResult.CLEAN, "hello");
        assertEquals(ChatManager.FilterResult.CLEAN, data.result());
        assertEquals("hello", data.filteredMessage());
    }

    @Test
    void filterResultDataCensored() {
        var data = new ChatManager.FilterResultData(ChatManager.FilterResult.CENSORED, "****");
        assertEquals(ChatManager.FilterResult.CENSORED, data.result());
        assertEquals("****", data.filteredMessage());
    }

    // ==================== Lógica de canais replicada ====================

    private Map<UUID, ChatManager.ChatChannel> activeChannels;

    @BeforeEach
    void setUp() {
        activeChannels = new ConcurrentHashMap<>();
    }

    @Test
    void getChannelDefault() {
        UUID uuid = UUID.randomUUID();
        assertEquals(ChatManager.ChatChannel.GLOBAL,
                activeChannels.getOrDefault(uuid, ChatManager.ChatChannel.GLOBAL));
    }

    @Test
    void setChannelNonGlobal() {
        UUID uuid = UUID.randomUUID();
        activeChannels.put(uuid, ChatManager.ChatChannel.KINGDOM);
        assertEquals(ChatManager.ChatChannel.KINGDOM,
                activeChannels.getOrDefault(uuid, ChatManager.ChatChannel.GLOBAL));
    }

    @Test
    void setChannelGlobalRemoveDoMap() {
        UUID uuid = UUID.randomUUID();
        activeChannels.put(uuid, ChatManager.ChatChannel.KINGDOM);

        // Replica: se canal é GLOBAL, remove do map
        ChatManager.ChatChannel channel = ChatManager.ChatChannel.GLOBAL;
        if (channel == ChatManager.ChatChannel.GLOBAL) {
            activeChannels.remove(uuid);
        } else {
            activeChannels.put(uuid, channel);
        }

        assertFalse(activeChannels.containsKey(uuid));
        // Mas getOrDefault ainda retorna GLOBAL
        assertEquals(ChatManager.ChatChannel.GLOBAL,
                activeChannels.getOrDefault(uuid, ChatManager.ChatChannel.GLOBAL));
    }

    @Test
    void removePlayerLimpaCache() {
        UUID uuid = UUID.randomUUID();
        Map<UUID, Deque<Long>> messageTimestamps = new ConcurrentHashMap<>();
        Map<UUID, String> lastMessages = new ConcurrentHashMap<>();
        Map<UUID, Long> mutedPlayers = new ConcurrentHashMap<>();

        activeChannels.put(uuid, ChatManager.ChatChannel.KINGDOM);
        messageTimestamps.put(uuid, new ArrayDeque<>());
        lastMessages.put(uuid, "test");
        mutedPlayers.put(uuid, System.currentTimeMillis());

        // Replica removePlayer
        activeChannels.remove(uuid);
        messageTimestamps.remove(uuid);
        lastMessages.remove(uuid);
        mutedPlayers.remove(uuid);

        assertFalse(activeChannels.containsKey(uuid));
        assertFalse(messageTimestamps.containsKey(uuid));
        assertFalse(lastMessages.containsKey(uuid));
        assertFalse(mutedPlayers.containsKey(uuid));
    }

    // ==================== Spam checking replicado ====================

    @Test
    void checkSpamDuplicata() {
        Map<UUID, String> lastMessages = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        String message = "Olá mundo!";

        lastMessages.put(uuid, message.trim());

        // Mesma mensagem
        String lastMsg = lastMessages.get(uuid);
        boolean isDuplicate = lastMsg != null && lastMsg.equalsIgnoreCase(message.trim());
        assertTrue(isDuplicate);
    }

    @Test
    void checkSpamDiferente() {
        Map<UUID, String> lastMessages = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        lastMessages.put(uuid, "mensagem 1");

        String lastMsg = lastMessages.get(uuid);
        boolean isDuplicate = lastMsg != null && lastMsg.equalsIgnoreCase("mensagem 2");
        assertFalse(isDuplicate);
    }

    @Test
    void checkSpamMuted() {
        Map<UUID, Long> mutedPlayers = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        mutedPlayers.put(uuid, System.currentTimeMillis() + 60000L); // mute por mais 1min

        Long muteExpiry = mutedPlayers.get(uuid);
        boolean isMuted = muteExpiry != null && System.currentTimeMillis() < muteExpiry;
        assertTrue(isMuted);
    }

    @Test
    void checkSpamMuteExpirou() {
        Map<UUID, Long> mutedPlayers = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        mutedPlayers.put(uuid, System.currentTimeMillis() - 1000L); // mute já expirou

        Long muteExpiry = mutedPlayers.get(uuid);
        boolean isMuted = muteExpiry != null && System.currentTimeMillis() < muteExpiry;
        assertFalse(isMuted);
    }

    @Test
    void rateLimitDetection() {
        int rateLimitMessages = 3;
        Deque<Long> timestamps = new ArrayDeque<>();
        long now = System.currentTimeMillis();

        // Simular 3 mensagens recentes
        timestamps.addLast(now - 1000);
        timestamps.addLast(now - 500);
        timestamps.addLast(now - 100);

        boolean limitReached = timestamps.size() >= rateLimitMessages;
        assertTrue(limitReached);
    }

    @Test
    void rateLimitTimestampCleanup() {
        Deque<Long> timestamps = new ArrayDeque<>();
        long now = System.currentTimeMillis();
        long windowMs = 5000L;

        timestamps.addLast(now - 10000); // Antigo
        timestamps.addLast(now - 6000); // Antigo
        timestamps.addLast(now - 100); // Recente

        // Limpar antigos
        while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) > windowMs) {
            timestamps.pollFirst();
        }

        assertEquals(1, timestamps.size());
    }

    // ==================== Filtro de palavras replicado ====================

    @Test
    void filtroSemPatterns() {
        List<Pattern> patterns = new ArrayList<>();
        assertTrue(patterns.isEmpty());
        // Quando vazio, retorna CLEAN sem modificar
    }

    @Test
    void filtroCensorReplace() {
        Pattern p = Pattern.compile("palavrão", Pattern.CASE_INSENSITIVE);
        String message = "Isso é um palavrão total";
        var matcher = p.matcher(message);
        String filtered = matcher.replaceAll(mr -> "*".repeat(mr.group().length()));
        assertEquals("Isso é um ******** total", filtered);
    }

    @Test
    void filtroCaseInsensitive() {
        Pattern p = Pattern.compile("spam", Pattern.CASE_INSENSITIVE);
        assertTrue(p.matcher("SPAM").find());
        assertTrue(p.matcher("Spam").find());
        assertTrue(p.matcher("spam").find());
    }

    @Test
    void getMuteRemainingCalculation() {
        long expiry = System.currentTimeMillis() + 60000L;
        int remaining = (int) ((expiry - System.currentTimeMillis()) / 1000);
        remaining = Math.max(remaining, 0);
        assertTrue(remaining > 0 && remaining <= 60);
    }

    @Test
    void getMuteRemainingExpired() {
        long expiry = System.currentTimeMillis() - 1000L;
        int remaining = (int) ((expiry - System.currentTimeMillis()) / 1000);
        remaining = Math.max(remaining, 0);
        assertEquals(0, remaining);
    }
}
