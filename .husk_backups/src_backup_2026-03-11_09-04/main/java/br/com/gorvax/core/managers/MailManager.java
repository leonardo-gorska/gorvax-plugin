package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * B17.1 — Sistema de Correio (Mailbox).
 * Permite enviar cartas para jogadores online ou offline.
 * Persistência em mail.yml com save assíncrono.
 */
public class MailManager {

    private final GorvaxCore plugin;
    private final File mailFile;
    private final Map<UUID, List<MailEntry>> mailbox = new ConcurrentHashMap<>();
    private final int maxUnread;
    private final int maxMessageLength;

    public MailManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.mailFile = new File(plugin.getDataFolder(), "mail.yml");
        this.maxUnread = plugin.getConfig().getInt("mail.max_unread", 20);
        this.maxMessageLength = plugin.getConfig().getInt("mail.max_message_length", 200);
        loadMail();

        // Save assíncrono a cada 5 minutos
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveMail, 6000L, 6000L);
    }

    // --- Modelo de Dados ---

    public static class MailEntry {
        public final UUID senderUUID;
        public final String senderName;
        public final String message;
        public final long timestamp;
        public boolean read;

        public MailEntry(UUID senderUUID, String senderName, String message, long timestamp, boolean read) {
            this.senderUUID = senderUUID;
            this.senderName = senderName;
            this.message = message;
            this.timestamp = timestamp;
            this.read = read;
        }

        public String getFormattedDate() {
            return new SimpleDateFormat("dd/MM HH:mm").format(new Date(timestamp));
        }
    }

    // --- API Pública ---

    /**
     * Envia uma carta para um jogador (online ou offline).
     * 
     * @return true se enviado com sucesso, false se caixa cheia ou mensagem inválida
     */
    public boolean sendMail(UUID senderUUID, String senderName, UUID targetUUID, String message) {
        if (message == null || message.trim().isEmpty()) return false;
        if (message.length() > maxMessageLength) return false;

        List<MailEntry> entries = mailbox.computeIfAbsent(targetUUID, k -> new CopyOnWriteArrayList<>());

        // Contar não lidas
        long unreadCount = entries.stream().filter(e -> !e.read).count();
        if (unreadCount >= maxUnread) return false;

        // Sanitizar % para evitar exploits com formatting
        String sanitized = message.replace("%", "%%");

        entries.add(new MailEntry(senderUUID, senderName, sanitized, System.currentTimeMillis(), false));
        return true;
    }

    /**
     * Retorna todas as cartas de um jogador (mais recentes primeiro).
     */
    public List<MailEntry> getMail(UUID playerUUID) {
        List<MailEntry> entries = mailbox.get(playerUUID);
        if (entries == null || entries.isEmpty()) return Collections.emptyList();
        List<MailEntry> sorted = new ArrayList<>(entries);
        sorted.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return sorted;
    }

    /**
     * Retorna o número de cartas não lidas.
     */
    public int getUnreadCount(UUID playerUUID) {
        List<MailEntry> entries = mailbox.get(playerUUID);
        if (entries == null) return 0;
        return (int) entries.stream().filter(e -> !e.read).count();
    }

    /**
     * Marca todas as cartas como lidas.
     */
    public void markAllRead(UUID playerUUID) {
        List<MailEntry> entries = mailbox.get(playerUUID);
        if (entries == null) return;
        entries.forEach(e -> e.read = true);
    }

    /**
     * Deleta uma carta pelo índice (baseado na lista ordenada por data desc).
     * 
     * @return true se deletado com sucesso
     */
    public boolean deleteMail(UUID playerUUID, int index) {
        List<MailEntry> sorted = getMail(playerUUID);
        if (index < 0 || index >= sorted.size()) return false;

        MailEntry target = sorted.get(index);
        List<MailEntry> entries = mailbox.get(playerUUID);
        if (entries != null) {
            entries.remove(target);
        }
        return true;
    }

    /**
     * Limpa todas as cartas lidas de um jogador.
     * 
     * @return número de cartas removidas
     */
    public int clearRead(UUID playerUUID) {
        List<MailEntry> entries = mailbox.get(playerUUID);
        if (entries == null) return 0;
        int before = entries.size();
        entries.removeIf(e -> e.read);
        return before - entries.size();
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    // --- Persistência ---

    private void loadMail() {
        if (!mailFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(mailFile);
        ConfigurationSection playersSection = yaml.getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID playerUUID = UUID.fromString(uuidStr);
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidStr);
                if (playerSection == null) continue;

                List<MailEntry> entries = new CopyOnWriteArrayList<>();
                ConfigurationSection mailsSection = playerSection.getConfigurationSection("mails");
                if (mailsSection != null) {
                    for (String key : mailsSection.getKeys(false)) {
                        ConfigurationSection mailSection = mailsSection.getConfigurationSection(key);
                        if (mailSection == null) continue;

                        UUID senderUUID = UUID.fromString(mailSection.getString("sender_uuid", ""));
                        String senderName = mailSection.getString("sender_name", "Desconhecido");
                        String message = mailSection.getString("message", "");
                        long timestamp = mailSection.getLong("timestamp", 0);
                        boolean read = mailSection.getBoolean("read", false);

                        entries.add(new MailEntry(senderUUID, senderName, message, timestamp, read));
                    }
                }

                if (!entries.isEmpty()) {
                    mailbox.put(playerUUID, entries);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[MailManager] UUID inválido no mail.yml: " + uuidStr);
            }
        }

        plugin.getLogger().info("[MailManager] Carregadas " + mailbox.size() + " caixas de correio.");
    }

    private synchronized void saveMail() {
        // Snapshot na Main Thread → I/O em Async (já rodamos em async via timer)
        Map<UUID, List<MailEntry>> snapshot = new HashMap<>();
        mailbox.forEach((uuid, entries) -> snapshot.put(uuid, new ArrayList<>(entries)));

        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<UUID, List<MailEntry>> entry : snapshot.entrySet()) {
            String basePath = "players." + entry.getKey().toString() + ".mails";
            int i = 0;
            for (MailEntry mail : entry.getValue()) {
                String path = basePath + "." + i;
                yaml.set(path + ".sender_uuid", mail.senderUUID.toString());
                yaml.set(path + ".sender_name", mail.senderName);
                yaml.set(path + ".message", mail.message);
                yaml.set(path + ".timestamp", mail.timestamp);
                yaml.set(path + ".read", mail.read);
                i++;
            }
        }

        try {
            yaml.save(mailFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[MailManager] Erro ao salvar mail.yml: " + e.getMessage());
        }
    }

    /**
     * Save síncrono (chamado no onDisable).
     */
    public void saveSync() {
        saveMail();
    }

    /**
     * Recarrega os dados do disco.
     */
    public void reload() {
        mailbox.clear();
        loadMail();
    }
}
