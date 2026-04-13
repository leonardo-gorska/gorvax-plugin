package br.com.gorvax.core.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para MailManager.
 * Testa lógica pura dos modelos e operações de correio.
 * Replica lógica interna para evitar dependências Bukkit.
 */
class MailManagerTest {

    // --- MailEntry model ---

    @Test
    void mailEntryFields() {
        UUID sender = UUID.randomUUID();
        long now = System.currentTimeMillis();
        var entry = new MailManager.MailEntry(sender, "Jogador1", "Olá!", now, false);

        assertEquals(sender, entry.senderUUID);
        assertEquals("Jogador1", entry.senderName);
        assertEquals("Olá!", entry.message);
        assertEquals(now, entry.timestamp);
        assertFalse(entry.read);
    }

    @Test
    void mailEntryFormattedDate() {
        long now = System.currentTimeMillis();
        var entry = new MailManager.MailEntry(UUID.randomUUID(), "X", "msg", now, false);
        String expected = new SimpleDateFormat("dd/MM HH:mm").format(new Date(now));
        assertEquals(expected, entry.getFormattedDate());
    }

    // --- sendMail lógica replicada ---

    private final int maxUnread = 20;
    private final int maxMessageLength = 200;
    private Map<UUID, List<MailManager.MailEntry>> mailbox;

    @BeforeEach
    void setUp() {
        mailbox = new HashMap<>();
    }

    /**
     * Replica a lógica de sendMail para testar sem Bukkit.
     */
    private boolean sendMail(UUID senderUUID, String senderName, UUID targetUUID, String message) {
        if (message == null || message.trim().isEmpty())
            return false;
        if (message.length() > maxMessageLength)
            return false;

        List<MailManager.MailEntry> entries = mailbox.computeIfAbsent(targetUUID, k -> new CopyOnWriteArrayList<>());
        long unreadCount = entries.stream().filter(e -> !e.read).count();
        if (unreadCount >= maxUnread)
            return false;

        String sanitized = message.replace("%", "%%");
        entries.add(new MailManager.MailEntry(senderUUID, senderName, sanitized, System.currentTimeMillis(), false));
        return true;
    }

    @Test
    void sendMailNullMessage() {
        assertFalse(sendMail(UUID.randomUUID(), "A", UUID.randomUUID(), null));
    }

    @Test
    void sendMailEmptyMessage() {
        assertFalse(sendMail(UUID.randomUUID(), "A", UUID.randomUUID(), ""));
        assertFalse(sendMail(UUID.randomUUID(), "A", UUID.randomUUID(), "   "));
    }

    @Test
    void sendMailTooLong() {
        String longMsg = "a".repeat(201);
        assertFalse(sendMail(UUID.randomUUID(), "A", UUID.randomUUID(), longMsg));
    }

    @Test
    void sendMailExactMaxLength() {
        String exactMsg = "b".repeat(200);
        assertTrue(sendMail(UUID.randomUUID(), "A", UUID.randomUUID(), exactMsg));
    }

    @Test
    void sendMailSuccess() {
        UUID target = UUID.randomUUID();
        assertTrue(sendMail(UUID.randomUUID(), "Sender", target, "Olá mundo!"));
        assertEquals(1, mailbox.get(target).size());
    }

    @Test
    void sendMailSanitizaPercent() {
        UUID target = UUID.randomUUID();
        sendMail(UUID.randomUUID(), "A", target, "100% legal");
        assertEquals("100%% legal", mailbox.get(target).get(0).message);
    }

    @Test
    void sendMailCaixaCheia() {
        UUID target = UUID.randomUUID();
        for (int i = 0; i < 20; i++) {
            assertTrue(sendMail(UUID.randomUUID(), "A", target, "msg" + i));
        }
        // 21ª mensagem deve falhar
        assertFalse(sendMail(UUID.randomUUID(), "A", target, "extra"));
    }

    @Test
    void sendMailCaixaCheiaComLidas() {
        UUID target = UUID.randomUUID();
        // Enviar 20 mensagens
        for (int i = 0; i < 20; i++) {
            sendMail(UUID.randomUUID(), "A", target, "msg" + i);
        }
        // Marcar 5 como lidas
        mailbox.get(target).subList(0, 5).forEach(e -> e.read = true);
        // Agora só há 15 não lidas, deve conseguir enviar
        assertTrue(sendMail(UUID.randomUUID(), "A", target, "nova"));
    }

    // --- getMail (ordenação) ---

    private List<MailManager.MailEntry> getMail(UUID playerUUID) {
        List<MailManager.MailEntry> entries = mailbox.get(playerUUID);
        if (entries == null || entries.isEmpty())
            return Collections.emptyList();
        List<MailManager.MailEntry> sorted = new ArrayList<>(entries);
        sorted.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return sorted;
    }

    @Test
    void getMailVazio() {
        assertTrue(getMail(UUID.randomUUID()).isEmpty());
    }

    @Test
    void getMailOrdenadoDesc() {
        UUID target = UUID.randomUUID();
        List<MailManager.MailEntry> entries = new CopyOnWriteArrayList<>();
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "A", "antiga", 1000L, false));
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "B", "nova", 3000L, false));
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "C", "meio", 2000L, false));
        mailbox.put(target, entries);

        List<MailManager.MailEntry> result = getMail(target);
        assertEquals("nova", result.get(0).message);
        assertEquals("meio", result.get(1).message);
        assertEquals("antiga", result.get(2).message);
    }

    // --- getUnreadCount ---

    private int getUnreadCount(UUID playerUUID) {
        List<MailManager.MailEntry> entries = mailbox.get(playerUUID);
        if (entries == null)
            return 0;
        return (int) entries.stream().filter(e -> !e.read).count();
    }

    @Test
    void getUnreadNenhum() {
        assertEquals(0, getUnreadCount(UUID.randomUUID()));
    }

    @Test
    void getUnreadMisto() {
        UUID target = UUID.randomUUID();
        List<MailManager.MailEntry> entries = new CopyOnWriteArrayList<>();
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "A", "m1", 1000L, false));
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "B", "m2", 2000L, true));
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "C", "m3", 3000L, false));
        mailbox.put(target, entries);

        assertEquals(2, getUnreadCount(target));
    }

    // --- markAllRead ---

    @Test
    void markAllRead() {
        UUID target = UUID.randomUUID();
        List<MailManager.MailEntry> entries = new CopyOnWriteArrayList<>();
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "A", "m1", 1000L, false));
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "B", "m2", 2000L, false));
        mailbox.put(target, entries);

        entries.forEach(e -> e.read = true); // replica markAllRead
        assertEquals(0, getUnreadCount(target));
    }

    @Test
    void markAllReadSemCartas() {
        // Não deve lançar exceção
        UUID target = UUID.randomUUID();
        List<MailManager.MailEntry> entries = mailbox.get(target);
        if (entries != null)
            entries.forEach(e -> e.read = true);
        // OK se não lançou exceção
    }

    // --- deleteMail ---

    private boolean deleteMail(UUID playerUUID, int index) {
        List<MailManager.MailEntry> sorted = getMail(playerUUID);
        if (index < 0 || index >= sorted.size())
            return false;
        MailManager.MailEntry target = sorted.get(index);
        List<MailManager.MailEntry> entries = mailbox.get(playerUUID);
        if (entries != null)
            entries.remove(target);
        return true;
    }

    @Test
    void deleteMailPorIndice() {
        UUID target = UUID.randomUUID();
        List<MailManager.MailEntry> entries = new CopyOnWriteArrayList<>();
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "A", "m1", 1000L, false));
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "B", "m2", 2000L, false));
        mailbox.put(target, entries);

        assertTrue(deleteMail(target, 0)); // deleta a mais nova (m2)
        assertEquals(1, mailbox.get(target).size());
        assertEquals("m1", mailbox.get(target).get(0).message);
    }

    @Test
    void deleteMailIndiceInvalido() {
        UUID target = UUID.randomUUID();
        assertFalse(deleteMail(target, 0));
        assertFalse(deleteMail(target, -1));
    }

    // --- clearRead ---

    private int clearRead(UUID playerUUID) {
        List<MailManager.MailEntry> entries = mailbox.get(playerUUID);
        if (entries == null)
            return 0;
        int before = entries.size();
        entries.removeIf(e -> e.read);
        return before - entries.size();
    }

    @Test
    void clearReadRemoveLidas() {
        UUID target = UUID.randomUUID();
        List<MailManager.MailEntry> entries = new CopyOnWriteArrayList<>();
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "A", "m1", 1000L, true));
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "B", "m2", 2000L, false));
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "C", "m3", 3000L, true));
        mailbox.put(target, entries);

        assertEquals(2, clearRead(target));
        assertEquals(1, mailbox.get(target).size());
        assertEquals("m2", mailbox.get(target).get(0).message);
    }

    @Test
    void clearReadSemLidas() {
        UUID target = UUID.randomUUID();
        List<MailManager.MailEntry> entries = new CopyOnWriteArrayList<>();
        entries.add(new MailManager.MailEntry(UUID.randomUUID(), "A", "m1", 1000L, false));
        mailbox.put(target, entries);

        assertEquals(0, clearRead(target));
    }

    @Test
    void clearReadCaixaInexistente() {
        assertEquals(0, clearRead(UUID.randomUUID()));
    }
}
