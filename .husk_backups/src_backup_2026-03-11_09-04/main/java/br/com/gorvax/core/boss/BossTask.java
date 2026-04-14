package br.com.gorvax.core.boss;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.boss.model.KingGorvax;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public class BossTask extends BukkitRunnable {

    private final GorvaxCore plugin;

    public BossTask(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // O tick do AtmosphereManager deve rodar sempre para processar fades e limpezas
        try {
            plugin.getBossManager().getAtmosphereManager().tick();
        } catch (Exception e) {
            plugin.getLogger().severe("[BossTask] Erro ao processar AtmosphereManager: " + e.getMessage());
        }

        // Se não houver bosses ativos, não há mais o que processar individualmente
        if (plugin.getBossManager().getActiveBosses().isEmpty())
            return;

        // Criamos uma cópia das chaves para evitar ConcurrentModificationException ao
        // remover o boss da lista
        new ArrayList<>(plugin.getBossManager().getActiveBosses().values()).forEach(boss -> {
            try {
                if (boss.getEntity() == null || (!boss.getEntity().isValid() && !boss.getEntity().isDead())) {
                    plugin.getBossManager().removeBoss(boss);
                    return;
                }

                long idleTime = (System.currentTimeMillis() - boss.getLastDamageTimestamp()) / 1000;
                if (idleTime >= 1800) { // 1800 segundos = 30 minutos
                    Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                            "§e[!] O Boss " + boss.getName() + " §eretornou às sombras por falta de combatentes."));

                    // Usa removeBoss() para garantir limpeza completa (AtmosphereManager, chunks,
                    // etc.)
                    boss.getEntity().remove();
                    plugin.getBossManager().removeBoss(boss);
                    return; // Pula o restante do processamento para este boss
                }

                // 1. Atualiza Barra de Vida, Fases e Visibilidade (raio gerenciado em
                // WorldBoss)
                boss.updateBossBar();

                // 2. Executa o update do Boss (IA, Música e Efeitos)
                boss.update();

                // Nota: Visibilidade da BossBar agora é gerenciada SOMENTE em
                // WorldBoss.updateBossBar() para evitar flickering por conflito de raios.
            } catch (Exception ex) {
                plugin.getLogger().warning("[BossTask] Erro no update de '" + boss.getName() + "': " + ex.getMessage());
            }
        });
    }
}