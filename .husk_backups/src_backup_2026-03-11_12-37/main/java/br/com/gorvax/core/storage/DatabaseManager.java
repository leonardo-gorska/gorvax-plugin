package br.com.gorvax.core.storage;

import br.com.gorvax.core.GorvaxCore;

import java.util.logging.Level;

/**
 * B18 — Gerenciador central de storage.
 * Lê config.yml para determinar qual backend usar (yaml, sqlite, mysql)
 * e expõe o DataStore ativo para os managers do plugin.
 *
 * Uso: GorvaxCore inicializa o DatabaseManager no onEnable(),
 * e cada manager chama plugin.getDatabaseManager().getDataStore() para persistência.
 */
public class DatabaseManager {

    private final GorvaxCore plugin;
    private DataStore dataStore;

    public DatabaseManager(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicializa o backend de armazenamento baseado na configuração.
     * Deve ser chamado no onEnable() ANTES dos managers.
     */
    public void init() {
        String type = plugin.getConfig().getString("storage.type", "yaml").toLowerCase();

        switch (type) {
            case "sqlite" -> {
                plugin.getLogger().info("[Storage] Iniciando backend SQLite...");
                dataStore = new SQLiteDataStore(plugin);
            }
            case "mysql" -> {
                plugin.getLogger().info("[Storage] Iniciando backend MySQL...");
                dataStore = new MySQLDataStore(plugin);
            }
            default -> {
                plugin.getLogger().info("[Storage] Usando backend YAML (padrão).");
                dataStore = new YamlDataStore(plugin);
            }
        }

        try {
            dataStore.init();
            plugin.getLogger().info("[Storage] Backend " + dataStore.getType().name()
                    + " inicializado com sucesso.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "[Storage] Falha ao inicializar backend " + type + ". Revertendo para YAML.", e);
            // Fallback seguro para YAML se o backend escolhido falhar
            dataStore = new YamlDataStore(plugin);
            try {
                dataStore.init();
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "[Storage] Falha crítica no fallback YAML.", ex);
            }
        }
    }

    /**
     * Encerra o backend de armazenamento.
     * Deve ser chamado no onDisable() DEPOIS dos managers salvarem.
     */
    public void shutdown() {
        if (dataStore != null) {
            dataStore.shutdown();
            plugin.getLogger().info("[Storage] Backend " + dataStore.getType().name() + " encerrado.");
        }
    }

    /**
     * Retorna o DataStore ativo.
     */
    public DataStore getDataStore() {
        return dataStore;
    }

    /**
     * Retorna o tipo de armazenamento ativo.
     */
    public DataStore.StorageType getStorageType() {
        return dataStore != null ? dataStore.getType() : DataStore.StorageType.YAML;
    }

    /**
     * Migra dados de um backend para outro.
     * Carrega do source, salva no target.
     *
     * @param sourceType tipo de origem
     * @param targetType tipo de destino
     * @throws Exception se a migração falhar
     */
    public void migrateData(DataStore.StorageType sourceType, DataStore.StorageType targetType) throws Exception {
        plugin.getLogger().info("[Storage] Iniciando migração: " + sourceType + " -> " + targetType);

        DataStore source = createDataStore(sourceType);
        DataStore target = createDataStore(targetType);

        try {
            source.init();
            target.init();

            // Migrar cada domínio de dados
            plugin.getLogger().info("[Storage] Migrando claims...");
            target.saveClaims(source.loadClaims());

            plugin.getLogger().info("[Storage] Migrando player data...");
            target.saveAllPlayerData(source.loadAllPlayerData());

            plugin.getLogger().info("[Storage] Migrando audit log...");
            target.saveAuditEntries(source.loadAuditEntries());

            plugin.getLogger().info("[Storage] Migrando mail...");
            target.saveAllMail(source.loadAllMail());

            plugin.getLogger().info("[Storage] Migrando bounties...");
            target.saveBounties(source.loadBounties());

            plugin.getLogger().info("[Storage] Migrando votes...");
            target.saveVotes(source.loadVotes());

            plugin.getLogger().info("[Storage] Migrando auctions...");
            target.saveAuctions(source.loadAuctions());

            plugin.getLogger().info("[Storage] Migrando pending collections...");
            target.savePendingCollections(source.loadPendingCollections());

            plugin.getLogger().info("[Storage] Migrando price history...");
            target.savePriceHistory(source.loadPriceHistory());

            plugin.getLogger().info("[Storage] Migração concluída com sucesso: " + sourceType + " -> " + targetType);
        } finally {
            // Fechar DataStores temporários (não o ativo!)
            if (source != dataStore) source.shutdown();
            if (target != dataStore) target.shutdown();
        }
    }

    /**
     * Cria uma instância de DataStore pelo tipo.
     */
    private DataStore createDataStore(DataStore.StorageType type) {
        return switch (type) {
            case SQLITE -> new SQLiteDataStore(plugin);
            case MYSQL -> new MySQLDataStore(plugin);
            default -> new YamlDataStore(plugin);
        };
    }
}
