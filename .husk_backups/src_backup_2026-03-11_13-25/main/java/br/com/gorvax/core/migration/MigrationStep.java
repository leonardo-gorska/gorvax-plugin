package br.com.gorvax.core.migration;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Interface funcional para cada patch de migração de configuração.
 * Cada step é responsável por migrar um config de uma versão para a próxima.
 */
@FunctionalInterface
public interface MigrationStep {

    /**
     * Aplica a migração no config fornecido.
     *
     * @param config o FileConfiguration a ser migrado
     */
    void apply(FileConfiguration config);

    /**
     * @return versão de origem desta migração (ex: 0 para V0→V1)
     */
    default int fromVersion() {
        return 0;
    }

    /**
     * @return versão de destino desta migração (ex: 1 para V0→V1)
     */
    default int toVersion() {
        return 1;
    }

    /**
     * @return descrição legível da migração
     */
    default String description() {
        return "V" + fromVersion() + " → V" + toVersion();
    }
}
