package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * B10 — Sistema de Log de Auditoria.
 * Registra ações importantes (claims, reinos, mercado, permissões)
 * com persistência em audit_log.yml e rotação automática.
 */
public class AuditManager {

    // Tipos de ação rastreadas pelo sistema de auditoria
    public enum AuditAction {
        CLAIM_CREATE,
        CLAIM_DELETE,
        KINGDOM_CREATE,
        KINGDOM_DELETE,
        KINGDOM_TRANSFER,
        KINGDOM_KICK,
        MARKET_BUY,
        MARKET_SELL,
        MARKET_CANCEL,
        TRUST_ADD,
        TRUST_REMOVE,
        PERMISSION_CHANGE
    }

    // Entrada individual no log de auditoria
    public static class AuditEntry {
        public final long timestamp;
        public final AuditAction action;
        public final UUID playerUUID;
        public final String playerName;
        public final String details;
        public final double value;

        public AuditEntry(long timestamp, AuditAction action, UUID playerUUID,
                          String playerName, String details, double value) {
            this.timestamp = timestamp;
            this.action = action;
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.details = details;
            this.value = value;
        }

        /**
         * Formata a data para exibição.
         */
        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
            return sdf.format(new Date(timestamp));
        }

        /**
         * Retorna ícone colorido por tipo de ação.
         */
        public String getActionIcon() {
            return switch (action) {
                case CLAIM_CREATE -> "§a§l+";
                case CLAIM_DELETE -> "§c§l-";
                case KINGDOM_CREATE -> "§6§l★";
                case KINGDOM_DELETE -> "§4§l✖";
                case KINGDOM_TRANSFER -> "§e§l⇄";
                case KINGDOM_KICK -> "§c§l⊘";
                case MARKET_BUY -> "§a§l$";
                case MARKET_SELL -> "§e§l$";
                case MARKET_CANCEL -> "§7§l✖";
                case TRUST_ADD -> "§a§l✔";
                case TRUST_REMOVE -> "§c§l✔";
                case PERMISSION_CHANGE -> "§b§l⚙";
            };
        }

        /**
         * Retorna o nome legível da ação em PT-BR.
         */
        public String getActionName() {
            return switch (action) {
                case CLAIM_CREATE -> "Claim Criado";
                case CLAIM_DELETE -> "Claim Deletado";
                case KINGDOM_CREATE -> "Reino Criado";
                case KINGDOM_DELETE -> "Reino Deletado";
                case KINGDOM_TRANSFER -> "Coroa Transferida";
                case KINGDOM_KICK -> "Membro Expulso";
                case MARKET_BUY -> "Compra";
                case MARKET_SELL -> "Venda";
                case MARKET_CANCEL -> "Listagem Cancelada";
                case TRUST_ADD -> "Permissão Adicionada";
                case TRUST_REMOVE -> "Permissão Removida";
                case PERMISSION_CHANGE -> "Permissão Alterada";
            };
        }

        /**
         * Verifica se é uma ação de mercado.
         */
        public boolean isMarketAction() {
            return action == AuditAction.MARKET_BUY
                    || action == AuditAction.MARKET_SELL
                    || action == AuditAction.MARKET_CANCEL;
        }
    }

    private final GorvaxCore plugin;
    private final File auditFile;
    private final CopyOnWriteArrayList<AuditEntry> entries = new CopyOnWriteArrayList<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    // Configurações (carregadas do config.yml)
    private int maxEntries;
    private int marketHistoryPerPlayer;

    public AuditManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.auditFile = new File(plugin.getDataFolder(), "audit_log.yml");

        // Carregar configurações
        this.maxEntries = plugin.getConfig().getInt("audit.max_entries", 1000);
        this.marketHistoryPerPlayer = plugin.getConfig().getInt("audit.market_history_per_player", 50);

        // Carregar dados existentes
        loadEntries();

        // Task de save periódico async
        int saveInterval = plugin.getConfig().getInt("audit.save_interval", 6000);
        if (plugin.getConfig().getBoolean("audit.enabled", true)) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (dirty.compareAndSet(true, false)) {
                    saveEntriesAsync();
                }
            }, saveInterval, saveInterval);
        }

        plugin.getLogger().info("[Auditoria] Sistema de log inicializado (" + entries.size() + " entradas carregadas).");
    }

    // ==========================================
    // API PÚBLICA — Registrar ações
    // ==========================================

    /**
     * Registra uma ação no log de auditoria.
     */
    public void log(AuditAction action, UUID playerUUID, String playerName, String details) {
        log(action, playerUUID, playerName, details, 0.0);
    }

    /**
     * Registra uma ação no log de auditoria com valor monetário.
     */
    public void log(AuditAction action, UUID playerUUID, String playerName, String details, double value) {
        if (!plugin.getConfig().getBoolean("audit.enabled", true)) return;

        AuditEntry entry = new AuditEntry(
                System.currentTimeMillis(), action, playerUUID, playerName, details, value
        );
        entries.add(entry);
        dirty.set(true);

        // Rotação: se exceder o limite, remover as mais antigas
        if (entries.size() > maxEntries) {
            int removeCount = entries.size() - maxEntries + 200; // Remove 200 extras para evitar rotação frequente
            if (removeCount > 0) {
                for (int i = 0; i < removeCount && !entries.isEmpty(); i++) {
                    entries.remove(0);
                }
            }
        }
    }

    // ==========================================
    // API PÚBLICA — Consultar entradas
    // ==========================================

    /**
     * Retorna as últimas N entradas do log.
     */
    public List<AuditEntry> getRecentEntries(int limit) {
        int size = entries.size();
        int from = Math.max(0, size - limit);
        return new ArrayList<>(entries.subList(from, size));
    }

    /**
     * Retorna entradas filtradas por jogador.
     */
    public List<AuditEntry> getPlayerHistory(UUID playerUUID, int limit) {
        return entries.stream()
                .filter(e -> e.playerUUID.equals(playerUUID))
                .sorted(Comparator.comparingLong((AuditEntry e) -> e.timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Retorna histórico de transações do mercado de um jogador.
     */
    public List<AuditEntry> getMarketHistory(UUID playerUUID, int limit) {
        return entries.stream()
                .filter(e -> e.isMarketAction() && e.playerUUID.equals(playerUUID))
                .sorted(Comparator.comparingLong((AuditEntry e) -> e.timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Retorna entradas filtradas por tipo de ação.
     */
    public List<AuditEntry> getByAction(AuditAction action, int limit) {
        return entries.stream()
                .filter(e -> e.action == action)
                .sorted(Comparator.comparingLong((AuditEntry e) -> e.timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Busca entradas que contenham o texto no campo details (case-insensitive).
     */
    public List<AuditEntry> search(String query, int limit) {
        String lowerQuery = query.toLowerCase();
        return entries.stream()
                .filter(e -> e.details.toLowerCase().contains(lowerQuery)
                        || e.playerName.toLowerCase().contains(lowerQuery))
                .sorted(Comparator.comparingLong((AuditEntry e) -> e.timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Retorna todas as transações de mercado recentes (para admins).
     */
    public List<AuditEntry> getAllMarketHistory(int limit) {
        return entries.stream()
                .filter(AuditEntry::isMarketAction)
                .sorted(Comparator.comparingLong((AuditEntry e) -> e.timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ==========================================
    // PERSISTÊNCIA
    // ==========================================

    /**
     * Carrega entradas do audit_log.yml.
     */
    private void loadEntries() {
        if (!auditFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(auditFile);
        List<Map<?, ?>> list = yaml.getMapList("entries");

        for (Map<?, ?> map : list) {
            try {
                long timestamp = ((Number) map.get("timestamp")).longValue();
                AuditAction action = AuditAction.valueOf((String) map.get("action"));
                UUID uuid = UUID.fromString((String) map.get("uuid"));
                String name = (String) map.get("name");
                String details = (String) map.get("details");
                double value = map.containsKey("value") ? ((Number) map.get("value")).doubleValue() : 0.0;

                entries.add(new AuditEntry(timestamp, action, uuid, name, details, value));
            } catch (Exception e) {
                plugin.getLogger().warning("[Auditoria] Entrada corrupta ignorada: " + e.getMessage());
            }
        }
    }

    /**
     * Salva entradas no audit_log.yml de forma assíncrona.
     */
    private void saveEntriesAsync() {
        // Snapshot dos dados na thread atual
        List<AuditEntry> snapshot = new ArrayList<>(entries);
        saveSnapshot(snapshot);
    }

    /**
     * Salva um snapshot de entradas no YAML.
     */
    private void saveSnapshot(List<AuditEntry> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();

        for (AuditEntry entry : snapshot) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("timestamp", entry.timestamp);
            map.put("action", entry.action.name());
            map.put("uuid", entry.playerUUID.toString());
            map.put("name", entry.playerName);
            map.put("details", entry.details);
            if (entry.value != 0.0) {
                map.put("value", entry.value);
            }
            list.add(map);
        }

        yaml.set("entries", list);

        try {
            yaml.save(auditFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Auditoria] Erro ao salvar audit_log.yml: " + e.getMessage());
        }
    }

    /**
     * Salva de forma síncrona (chamado no onDisable).
     */
    public void saveSync() {
        if (dirty.compareAndSet(true, false)) {
            saveSnapshot(new ArrayList<>(entries));
        }
    }

    /**
     * Recarrega as entradas do disco.
     */
    public void reload() {
        entries.clear();
        this.maxEntries = plugin.getConfig().getInt("audit.max_entries", 1000);
        this.marketHistoryPerPlayer = plugin.getConfig().getInt("audit.market_history_per_player", 50);
        loadEntries();
        plugin.getLogger().info("[Auditoria] Log recarregado (" + entries.size() + " entradas).");
    }

    /**
     * Retorna configuração de histórico por jogador.
     */
    public int getMarketHistoryPerPlayer() {
        return marketHistoryPerPlayer;
    }
}
