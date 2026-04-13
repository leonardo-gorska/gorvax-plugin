package br.com.gorvax.core.boss.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final GorvaxCore plugin;

    // Arquivo de Recompensas (Loot)
    private File rewardsFile;
    private FileConfiguration rewardsConfig;

    // Arquivo de Status/Configurações (HP, Nome, etc)
    private File settingsFile;
    private FileConfiguration settingsConfig;

    public ConfigManager(GorvaxCore plugin) {
        this.plugin = plugin;
        setup();
    }

    /**
     * Inicializa os arquivos na pasta do plugin.
     * Se os arquivos não existirem, eles serão copiados do jar (resources).
     */
    public void setup() {
        // Cria a pasta do plugin caso não exista
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // 1. Gerenciamento do boss_rewards.yml
        rewardsFile = new File(plugin.getDataFolder(), "boss_rewards.yml");
        if (!rewardsFile.exists()) {
            plugin.saveResource("boss_rewards.yml", false);
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);

        // 2. Gerenciamento do boss_settings.yml (O NOVO ARQUIVO)
        settingsFile = new File(plugin.getDataFolder(), "boss_settings.yml");
        if (!settingsFile.exists()) {
            // Este método extrai o arquivo padrão de dentro da pasta 'resources' do seu .jar
            plugin.saveResource("boss_settings.yml", false);
        }
        settingsConfig = YamlConfiguration.loadConfiguration(settingsFile);
    }

    /**
     * @return O arquivo de configuração de recompensas (loot).
     */
    public FileConfiguration getRewards() {
        return rewardsConfig;
    }

    /**
     * @return O arquivo de configuração de status (HP, Nomes, etc).
     */
    public FileConfiguration getSettings() {
        return settingsConfig;
    }

    /**
     * Recarrega ambos os arquivos da pasta do servidor para a memória.
     * Útil para aplicar alterações feitas manualmente sem reiniciar o servidor.
     */
    public void reload() {
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
        settingsConfig = YamlConfiguration.loadConfiguration(settingsFile);
    }
}