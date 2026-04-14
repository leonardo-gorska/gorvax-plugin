package br.com.gorvax.core.migration.migrations;

import br.com.gorvax.core.migration.MigrationStep;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Migração V0 → V1: Garante que todas as chaves de configuração
 * da v1.0.0 (lançamento) estejam presentes com valores default.
 */
public class V0_to_V1 implements MigrationStep {

    @Override
    public int fromVersion() {
        return 0;
    }

    @Override
    public int toVersion() {
        return 1;
    }

    @Override
    public String description() {
        return "V0 → V1: Baseline v1.0.0 — garante chaves padrão";
    }

    @Override
    public void apply(FileConfiguration config) {
        // Garantir que chaves introduzidas em batches recentes existam com defaults.
        // Se o admin já configurou, o valor existente é preservado (não sobrescreve).

        // bStats (plugin ID registrado em bstats.org)
        if (!config.contains("bstats.plugin_id")) {
            config.set("bstats.plugin_id", 30054);
        }

        // Garantir seção de chat filter
        if (!config.contains("chat.filter.action")) {
            config.set("chat.filter.action", "censor");
        }
        if (!config.contains("chat.filter.mute_duration")) {
            config.set("chat.filter.mute_duration", 300);
        }

        // Garantir seção de ranks habilitada (sistemas mais recentes)
        if (!config.contains("ranks.enabled")) {
            config.set("ranks.enabled", true);
        }

        // Garantir seção de RTP
        if (!config.contains("rtp.cooldown_seconds")) {
            config.set("rtp.cooldown_seconds", 600);
        }
        if (!config.contains("rtp.max_radius")) {
            config.set("rtp.max_radius", 5000);
        }
        if (!config.contains("rtp.min_radius")) {
            config.set("rtp.min_radius", 500);
        }
        if (!config.contains("rtp.max_attempts")) {
            config.set("rtp.max_attempts", 10);
        }

        // Garantir seção de karma
        if (!config.contains("karma.enabled")) {
            config.set("karma.enabled", true);
        }

        // Garantir seção de battle pass
        if (!config.contains("battle_pass.enabled")) {
            config.set("battle_pass.enabled", true);
        }

        // config_version é definido pelo motor após a migração, não aqui
    }
}
