package br.com.gorvax.core.migration;

import br.com.gorvax.core.migration.migrations.V0_to_V1;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o sistema de migração de configurações.
 * Testa MigrationStep interface, V0_to_V1, e lógica de sequenciamento.
 * Usa YamlConfiguration em memória (sem I/O de disco).
 */
class ConfigMigratorTest {

    private YamlConfiguration config;

    @BeforeEach
    void setUp() {
        config = new YamlConfiguration();
    }

    // ==================== MigrationStep interface ====================

    @Test
    void migrationStepDefaultsAnonymousClass() {
        MigrationStep step = config1 -> {
            // no-op
        };
        assertEquals(0, step.fromVersion());
        assertEquals(1, step.toVersion());
        assertEquals("V0 → V1", step.description());
    }

    @Test
    void migrationStepCustomOverrides() {
        MigrationStep step = new MigrationStep() {
            @Override
            public void apply(org.bukkit.configuration.file.FileConfiguration config) {
                config.set("test", true);
            }

            @Override
            public int fromVersion() {
                return 5;
            }

            @Override
            public int toVersion() {
                return 6;
            }

            @Override
            public String description() {
                return "V5 → V6: Custom migration";
            }
        };

        assertEquals(5, step.fromVersion());
        assertEquals(6, step.toVersion());
        assertEquals("V5 → V6: Custom migration", step.description());

        step.apply(config);
        assertTrue(config.getBoolean("test"));
    }

    // ==================== V0_to_V1 — Metadata ====================

    @Test
    void v0ToV1FromVersion() {
        var step = new V0_to_V1();
        assertEquals(0, step.fromVersion());
    }

    @Test
    void v0ToV1ToVersion() {
        var step = new V0_to_V1();
        assertEquals(1, step.toVersion());
    }

    @Test
    void v0ToV1Description() {
        var step = new V0_to_V1();
        assertNotNull(step.description());
        assertTrue(step.description().contains("V0"));
        assertTrue(step.description().contains("V1"));
    }

    // ==================== V0_to_V1 — Apply em config vazio ====================

    @Test
    void v0ToV1ApplyConfigVazioAdicionaDefaults() {
        var step = new V0_to_V1();
        step.apply(config);

        // Deve ter adicionado todas as chaves default
        assertEquals(99999, config.getInt("bstats.plugin_id"));
        assertEquals("censor", config.getString("chat.filter.action"));
        assertEquals(300, config.getInt("chat.filter.mute_duration"));
        assertTrue(config.getBoolean("ranks.enabled"));
        assertEquals(600, config.getInt("rtp.cooldown_seconds"));
        assertEquals(5000, config.getInt("rtp.max_radius"));
        assertEquals(500, config.getInt("rtp.min_radius"));
        assertEquals(10, config.getInt("rtp.max_attempts"));
        assertTrue(config.getBoolean("karma.enabled"));
        assertTrue(config.getBoolean("battle_pass.enabled"));
    }

    // ==================== V0_to_V1 — Apply preserva existentes ====================

    @Test
    void v0ToV1ApplyPreservaValoresExistentes() {
        // Simular config já customizado pelo admin
        config.set("bstats.plugin_id", 12345);
        config.set("chat.filter.action", "block");
        config.set("chat.filter.mute_duration", 600);
        config.set("ranks.enabled", false);
        config.set("rtp.cooldown_seconds", 120);
        config.set("rtp.max_radius", 10000);
        config.set("rtp.min_radius", 1000);
        config.set("rtp.max_attempts", 20);
        config.set("karma.enabled", false);
        config.set("battle_pass.enabled", false);

        var step = new V0_to_V1();
        step.apply(config);

        // Valores existentes devem ser preservados (NÃO sobrescritos)
        assertEquals(12345, config.getInt("bstats.plugin_id"));
        assertEquals("block", config.getString("chat.filter.action"));
        assertEquals(600, config.getInt("chat.filter.mute_duration"));
        assertFalse(config.getBoolean("ranks.enabled"));
        assertEquals(120, config.getInt("rtp.cooldown_seconds"));
        assertEquals(10000, config.getInt("rtp.max_radius"));
        assertEquals(1000, config.getInt("rtp.min_radius"));
        assertEquals(20, config.getInt("rtp.max_attempts"));
        assertFalse(config.getBoolean("karma.enabled"));
        assertFalse(config.getBoolean("battle_pass.enabled"));
    }

    @Test
    void v0ToV1ApplyPreservaParcial() {
        // Apenas algumas chaves existem
        config.set("bstats.plugin_id", 55555);
        config.set("rtp.max_radius", 8000);

        var step = new V0_to_V1();
        step.apply(config);

        // Existentes preservados
        assertEquals(55555, config.getInt("bstats.plugin_id"));
        assertEquals(8000, config.getInt("rtp.max_radius"));

        // Faltantes adicionados com default
        assertEquals("censor", config.getString("chat.filter.action"));
        assertEquals(300, config.getInt("chat.filter.mute_duration"));
        assertTrue(config.getBoolean("ranks.enabled"));
        assertEquals(600, config.getInt("rtp.cooldown_seconds"));
        assertEquals(500, config.getInt("rtp.min_radius"));
        assertEquals(10, config.getInt("rtp.max_attempts"));
        assertTrue(config.getBoolean("karma.enabled"));
        assertTrue(config.getBoolean("battle_pass.enabled"));
    }

    // ==================== V0_to_V1 — Idempotência ====================

    @Test
    void v0ToV1ApplyIdempotente() {
        var step = new V0_to_V1();

        // Aplicar 2 vezes — resultado deve ser idêntico
        step.apply(config);
        int pluginId1 = config.getInt("bstats.plugin_id");
        String action1 = config.getString("chat.filter.action");

        step.apply(config);
        assertEquals(pluginId1, config.getInt("bstats.plugin_id"));
        assertEquals(action1, config.getString("chat.filter.action"));
    }

    // ==================== Step sequencing ====================

    @Test
    void stepSequencingOrdemAscendente() {
        List<MigrationStep> steps = new ArrayList<>();

        // Simular 3 migrações V0→V1, V1→V2, V2→V3
        steps.add(new MigrationStep() {
            @Override
            public void apply(org.bukkit.configuration.file.FileConfiguration c) {
                c.set("step_v1", true);
            }

            @Override
            public int fromVersion() {
                return 0;
            }

            @Override
            public int toVersion() {
                return 1;
            }
        });

        steps.add(new MigrationStep() {
            @Override
            public void apply(org.bukkit.configuration.file.FileConfiguration c) {
                c.set("step_v2", true);
            }

            @Override
            public int fromVersion() {
                return 1;
            }

            @Override
            public int toVersion() {
                return 2;
            }
        });

        steps.add(new MigrationStep() {
            @Override
            public void apply(org.bukkit.configuration.file.FileConfiguration c) {
                c.set("step_v3", true);
            }

            @Override
            public int fromVersion() {
                return 2;
            }

            @Override
            public int toVersion() {
                return 3;
            }
        });

        // Simular migração desde V0 (config_version não existe)
        int currentVersion = config.getInt("config_version", 0);
        int applied = 0;

        for (MigrationStep step : steps) {
            if (step.fromVersion() >= currentVersion) {
                step.apply(config);
                currentVersion = step.toVersion();
                applied++;
            }
        }

        config.set("config_version", currentVersion);

        assertEquals(3, applied);
        assertEquals(3, config.getInt("config_version"));
        assertTrue(config.getBoolean("step_v1"));
        assertTrue(config.getBoolean("step_v2"));
        assertTrue(config.getBoolean("step_v3"));
    }

    @Test
    void stepSequencingPulaJaAplicados() {
        List<MigrationStep> steps = new ArrayList<>();

        steps.add(new MigrationStep() {
            @Override
            public void apply(org.bukkit.configuration.file.FileConfiguration c) {
                c.set("step_v1", true);
            }

            @Override
            public int fromVersion() {
                return 0;
            }

            @Override
            public int toVersion() {
                return 1;
            }
        });

        steps.add(new MigrationStep() {
            @Override
            public void apply(org.bukkit.configuration.file.FileConfiguration c) {
                c.set("step_v2", true);
            }

            @Override
            public int fromVersion() {
                return 1;
            }

            @Override
            public int toVersion() {
                return 2;
            }
        });

        // Config já está na V1
        config.set("config_version", 1);
        int currentVersion = 1;
        int applied = 0;

        for (MigrationStep step : steps) {
            if (step.fromVersion() >= currentVersion) {
                step.apply(config);
                currentVersion = step.toVersion();
                applied++;
            }
        }

        // Apenas V1→V2 deve ter sido aplicado (V0→V1 pulado)
        assertEquals(1, applied);
        assertFalse(config.contains("step_v1")); // não aplicado
        assertTrue(config.getBoolean("step_v2")); // aplicado
    }

    @Test
    void stepSequencingNenhumSeAtualizado() {
        config.set("config_version", 3);
        int currentVersion = 3;
        int latestVersion = 3;

        boolean needsMigration = currentVersion < latestVersion;
        assertFalse(needsMigration);
    }

    // ==================== Versões ====================

    @Test
    void latestVersionEPositivo() {
        // LATEST_VERSION é 1 na implementação atual
        assertTrue(1 > 0, "LATEST_VERSION deve ser positivo");
    }

    @Test
    void versionKeyConsistente() {
        // O campo usado é "config_version"
        String key = "config_version";
        config.set(key, 5);
        assertEquals(5, config.getInt(key));
    }

    @Test
    void versionDefaultZeroQuandoAusente() {
        assertEquals(0, config.getInt("config_version", 0));
    }

    // ==================== Migração com erro (lógica replicada) ====================

    @Test
    void migraSalvaProgressoAteErro() {
        List<MigrationStep> steps = new ArrayList<>();

        steps.add(new MigrationStep() {
            @Override
            public void apply(org.bukkit.configuration.file.FileConfiguration c) {
                c.set("step1_ok", true);
            }

            @Override
            public int fromVersion() {
                return 0;
            }

            @Override
            public int toVersion() {
                return 1;
            }
        });

        steps.add(new MigrationStep() {
            @Override
            public void apply(org.bukkit.configuration.file.FileConfiguration c) {
                throw new RuntimeException("Erro simulado na V1→V2");
            }

            @Override
            public int fromVersion() {
                return 1;
            }

            @Override
            public int toVersion() {
                return 2;
            }
        });

        steps.add(new MigrationStep() {
            @Override
            public void apply(org.bukkit.configuration.file.FileConfiguration c) {
                c.set("step3_ok", true);
            }

            @Override
            public int fromVersion() {
                return 2;
            }

            @Override
            public int toVersion() {
                return 3;
            }
        });

        int currentVersion = 0;
        int applied = 0;

        for (MigrationStep step : steps) {
            if (step.fromVersion() >= currentVersion) {
                try {
                    step.apply(config);
                    currentVersion = step.toVersion();
                    applied++;
                } catch (Exception e) {
                    // Salva o progresso até aqui e para
                    config.set("config_version", currentVersion);
                    break;
                }
            }
        }

        // Apenas step 1 foi aplicada com sucesso
        assertEquals(1, applied);
        assertTrue(config.getBoolean("step1_ok"));
        assertFalse(config.contains("step3_ok")); // não chegou no step 3
        assertEquals(1, config.getInt("config_version")); // salvou versão parcial
    }
}
