package br.com.gorvax.core.migration;

import br.com.gorvax.core.migration.migrations.V0_to_V1;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Motor de migração automática de configurações do GorvaxCore.
 * Detecta a versão do config.yml e aplica migrações sequenciais até a versão
 * mais recente.
 * Cria backup automático antes de qualquer alteração.
 */
public class ConfigMigrator {

    /**
     * Versão mais recente do config — deve ser incrementada a cada nova migração
     */
    private static final int LATEST_VERSION = 1;

    private static final String VERSION_KEY = "config_version";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final List<MigrationStep> steps = new ArrayList<>();

    public ConfigMigrator(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        registerSteps();
    }

    /**
     * Registra todos os steps de migração em ordem sequencial.
     * Para adicionar uma nova migração, criar a classe e adicioná-la aqui.
     */
    private void registerSteps() {
        steps.add(new V0_to_V1());
        // Futuras migrações:
        // steps.add(new V1_to_V2());
        // steps.add(new V2_to_V3());
    }

    /**
     * Executa a migração do config.yml se necessário.
     * Chamado no onEnable() antes de inicializar os managers.
     *
     * @return true se alguma migração foi aplicada, false se já estava atualizado
     */
    public boolean migrate() {
        return migrateFile("config.yml", plugin.getConfig());
    }

    /**
     * Força re-migração (para uso via comando /gorvax migrateconfig).
     * Reaplica todas as migrações do zero.
     *
     * @return número de steps aplicados
     */
    public int forceMigrate() {
        plugin.reloadConfig();
        var config = plugin.getConfig();

        // Resetar versão para forçar re-aplicação
        config.set(VERSION_KEY, 0);

        int currentVersion = 0;
        int applied = 0;

        for (MigrationStep step : steps) {
            if (step.fromVersion() >= currentVersion) {
                try {
                    logger.info("[ConfigMigrator] Re-aplicando migração: " + step.description());
                    step.apply(config);
                    currentVersion = step.toVersion();
                    applied++;
                } catch (Exception e) {
                    logger.severe("[ConfigMigrator] Erro ao re-aplicar migração " + step.description() + ": "
                            + e.getMessage());
                    break;
                }
            }
        }

        if (applied > 0) {
            config.set(VERSION_KEY, currentVersion);
            plugin.saveConfig();
            logger.info("[ConfigMigrator] Re-migração concluída. Versão atual: " + currentVersion);
        }

        return applied;
    }

    /**
     * Migra um arquivo de configuração específico.
     */
    private boolean migrateFile(String fileName, org.bukkit.configuration.file.FileConfiguration config) {
        int currentVersion = config.getInt(VERSION_KEY, 0);

        if (currentVersion >= LATEST_VERSION) {
            logger.info("[ConfigMigrator] " + fileName + " já está atualizado (v" + currentVersion + ").");
            return false;
        }

        logger.info("[ConfigMigrator] " + fileName + " está na v" + currentVersion + ", versão mais recente é v"
                + LATEST_VERSION + ".");

        // Criar backup antes de migrar
        if (!createBackup(fileName)) {
            logger.warning(
                    "[ConfigMigrator] Falha ao criar backup de " + fileName + ". Migração cancelada por segurança.");
            return false;
        }

        int applied = 0;

        for (MigrationStep step : steps) {
            if (step.fromVersion() >= currentVersion && step.toVersion() <= LATEST_VERSION) {
                try {
                    logger.info("[ConfigMigrator] Aplicando migração: " + step.description());
                    step.apply(config);
                    currentVersion = step.toVersion();
                    applied++;
                    logger.info("[ConfigMigrator] Migração " + step.description() + " aplicada com sucesso.");
                } catch (Exception e) {
                    logger.severe(
                            "[ConfigMigrator] Erro ao aplicar migração " + step.description() + ": " + e.getMessage());
                    logger.severe(
                            "[ConfigMigrator] Migração interrompida. Restaure o backup manualmente se necessário.");
                    // Salvar o que conseguiu até aqui
                    config.set(VERSION_KEY, currentVersion);
                    plugin.saveConfig();
                    return applied > 0;
                }
            }
        }

        if (applied > 0) {
            config.set(VERSION_KEY, currentVersion);
            plugin.saveConfig();
            logger.info("[ConfigMigrator] Migração concluída! " + applied + " step(s) aplicado(s). Versão atual: v"
                    + currentVersion);
        }

        return applied > 0;
    }

    /**
     * Cria um backup do arquivo de configuração antes da migração.
     *
     * @param fileName nome do arquivo (ex: "config.yml")
     * @return true se o backup foi criado com sucesso
     */
    private boolean createBackup(String fileName) {
        File original = new File(plugin.getDataFolder(), fileName);
        if (!original.exists()) {
            return true; // Nada para fazer backup
        }

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String backupName = fileName.replace(".yml", ".yml.backup-" + date);
        File backup = new File(plugin.getDataFolder(), backupName);

        // Se já existe backup do mesmo dia, adicionar sufixo numérico
        int suffix = 1;
        while (backup.exists()) {
            backupName = fileName.replace(".yml", ".yml.backup-" + date + "-" + suffix);
            backup = new File(plugin.getDataFolder(), backupName);
            suffix++;
        }

        try {
            Files.copy(original.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("[ConfigMigrator] Backup criado: " + backupName);
            return true;
        } catch (IOException e) {
            logger.severe("[ConfigMigrator] Erro ao criar backup: " + e.getMessage());
            return false;
        }
    }

    /**
     * Migra o messages.yml separadamente (adiciona novas chaves sem sobrescrever
     * existentes).
     * Pode ser chamado opcionalmente após o migrate() principal.
     */
    public void migrateMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            return; // Será gerado pelo Bukkit na primeira execução
        }

        YamlConfiguration messages = YamlConfiguration.loadConfiguration(messagesFile);
        int currentVersion = messages.getInt(VERSION_KEY, 0);

        if (currentVersion >= LATEST_VERSION) {
            return; // Já atualizado
        }

        // Criar backup do messages.yml
        createBackup("messages.yml");

        // Adicionar chaves de migração de config (novas mensagens de B33)
        if (!messages.contains("admin.migrateconfig_success")) {
            messages.set("admin.migrateconfig_success",
                    "§a[Gorvax] ✓ Migração de configurações aplicada! {0} step(s) executado(s).");
        }
        if (!messages.contains("admin.migrateconfig_uptodate")) {
            messages.set("admin.migrateconfig_uptodate",
                    "§e[Gorvax] Configurações já estão atualizadas (v{0}).");
        }
        if (!messages.contains("admin.migrateconfig_usage")) {
            messages.set("admin.migrateconfig_usage",
                    "§e[Gorvax] Uso: §f/gorvax migrateconfig §7— Força re-migração de configurações.");
        }

        messages.set(VERSION_KEY, LATEST_VERSION);

        try {
            messages.save(messagesFile);
            logger.info("[ConfigMigrator] messages.yml atualizado para v" + LATEST_VERSION);
        } catch (IOException e) {
            logger.severe("[ConfigMigrator] Erro ao salvar messages.yml: " + e.getMessage());
        }
    }

    /**
     * @return a versão atual do config
     */
    public int getCurrentVersion() {
        return plugin.getConfig().getInt(VERSION_KEY, 0);
    }

    /**
     * @return a versão mais recente disponível
     */
    public int getLatestVersion() {
        return LATEST_VERSION;
    }
}
