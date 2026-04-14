package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B4 — Gerenciador do Tutorial Interativo + Welcome Kit.
 * Controla a sequência de 6 passos do tutorial para novos jogadores,
 * exibindo BossBar dinâmico e mensagens progressivas no chat.
 */
public class TutorialManager {

    private final GorvaxCore plugin;
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();

    // Número total de passos do tutorial
    private static final int TOTAL_STEPS = 6;

    // Cache de itens do welcome kit (parseado uma vez do config)
    private List<ItemStack> welcomeKitItems;

    public TutorialManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadWelcomeKit();
    }

    /**
     * Carrega a lista de itens do welcome kit a partir do config.yml.
     * Formato esperado: "MATERIAL:quantidade" ou "MATERIAL" (quantidade 1).
     */
    private void loadWelcomeKit() {
        this.welcomeKitItems = new ArrayList<>();
        List<String> kitEntries = plugin.getConfig().getStringList("tutorial.welcome_kit");
        for (String entry : kitEntries) {
            try {
                String[] parts = entry.split(":");
                Material mat = Material.matchMaterial(parts[0].trim());
                int amount = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 1;
                if (mat != null) {
                    welcomeKitItems.add(new ItemStack(mat, amount));
                } else {
                    plugin.getLogger().warning("[Tutorial] Material desconhecido no welcome kit: " + parts[0]);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Tutorial] Entrada inválida no welcome kit: " + entry);
            }
        }
        plugin.getLogger().info("[GorvaxCore] Welcome Kit carregado com " + welcomeKitItems.size() + " itens.");
    }

    /**
     * Verifica se o sistema de tutorial está habilitado no config.yml.
     */
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("tutorial.enabled", true);
    }

    /**
     * Inicia o tutorial para um jogador novo (primeiro login).
     * Dá o welcome kit e começa o passo 1.
     */
    public void startTutorial(Player player) {
        if (!isEnabled()) return;

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

        // Dar welcome kit se ainda não recebeu
        if (!pd.hasReceivedKit()) {
            giveWelcomeKit(player);
            pd.setHasReceivedKit(true);
        }

        // Iniciar tutorial no passo 1
        pd.setTutorialStep(1);
        showStep(player, 1);
    }

    /**
     * Dá o welcome kit ao jogador.
     * Itens que não cabem no inventário vão para o chão.
     */
    private void giveWelcomeKit(Player player) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(
                welcomeKitItems.toArray(new ItemStack[0])
        );
        // Dropar no chão o que não coube
        for (ItemStack item : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        plugin.getMessageManager().send(player, "tutorial.kit_received");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    /**
     * Exibe o passo atual do tutorial com BossBar e mensagem no chat.
     */
    public void showStep(Player player, int step) {
        if (step < 1 || step > TOTAL_STEPS) return;

        String msgKey = "tutorial.step_" + step;
        String bossBarKey = "tutorial.bossbar_" + step;

        // Enviar mensagem no chat
        plugin.getMessageManager().send(player, msgKey);

        // Criar/atualizar BossBar
        float progress = (float) step / TOTAL_STEPS;
        String barText = plugin.getMessageManager().get(bossBarKey);
        if (barText.startsWith("§c[Mensagem ausente")) {
            barText = "§bTutorial — Passo " + step + "/" + TOTAL_STEPS;
        }

        BossBar bar = activeBossBars.get(player.getUniqueId());
        Component barComponent = LegacyComponentSerializer.legacySection().deserialize(barText);

        if (bar == null) {
            bar = BossBar.bossBar(barComponent, progress, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
            activeBossBars.put(player.getUniqueId(), bar);
            player.showBossBar(bar);
        } else {
            bar.name(barComponent);
            bar.progress(progress);
        }

        // Som sutil ao avançar
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);

        // Auto-avanço para passos que não requerem ação específica
        if (step == 1 || step == 2) {
            // Passos 1 e 2 são informativos — avança após 8 segundos
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && isInTutorial(player) && getCurrentStep(player) == step) {
                    advanceStep(player);
                }
            }, 160L); // 8 segundos
        } else if (step == 6) {
            // Passo 6 é o final — completa após 10 segundos
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && isInTutorial(player) && getCurrentStep(player) == 6) {
                    completeTutorial(player);
                }
            }, 200L); // 10 segundos
        }
    }

    /**
     * Avança o tutorial para o próximo passo.
     * Se já completou todos os passos, finaliza o tutorial.
     */
    public void advanceStep(Player player) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        int current = pd.getTutorialStep();

        if (current <= 0 || pd.isTutorialCompleted()) return;

        int next = current + 1;
        if (next > TOTAL_STEPS) {
            completeTutorial(player);
        } else {
            pd.setTutorialStep(next);
            showStep(player, next);
        }
    }

    /**
     * Completa o tutorial com sucesso. Dá recompensas e remove a BossBar.
     */
    public void completeTutorial(Player player) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        pd.setTutorialStep(TOTAL_STEPS + 1);
        pd.setTutorialCompleted(true);

        // Recompensas
        double rewardMoney = plugin.getConfig().getDouble("tutorial.completion_reward_money", 500.0);
        int rewardBlocks = plugin.getConfig().getInt("tutorial.completion_reward_blocks", 200);

        if (rewardMoney > 0 && GorvaxCore.getEconomy() != null) {
            GorvaxCore.getEconomy().depositPlayer(player, rewardMoney);
        }
        if (rewardBlocks > 0) {
            pd.addClaimBlocks(rewardBlocks);
        }

        // Mensagem de conclusão
        plugin.getMessageManager().send(player, "tutorial.completed",
                String.format("%.0f", rewardMoney), String.valueOf(rewardBlocks));

        // Sons e efeitos
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Remover BossBar
        removeBossBar(player);
    }

    /**
     * Pula o tutorial sem dar as recompensas de conclusão.
     * O welcome kit já foi dado separadamente.
     */
    public void skipTutorial(Player player) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        pd.setTutorialCompleted(true);
        pd.setTutorialStep(TOTAL_STEPS + 1);

        plugin.getMessageManager().send(player, "tutorial.skipped");
        removeBossBar(player);
    }

    /**
     * Verifica se o jogador está em um tutorial ativo (em progresso).
     */
    public boolean isInTutorial(Player player) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        int step = pd.getTutorialStep();
        return step >= 1 && step <= TOTAL_STEPS && !pd.isTutorialCompleted();
    }

    /**
     * Retorna o passo atual do tutorial do jogador.
     */
    public int getCurrentStep(Player player) {
        return plugin.getPlayerDataManager().getData(player.getUniqueId()).getTutorialStep();
    }

    /**
     * Remove a BossBar de um jogador (ao sair, completar ou pular).
     */
    public void removeBossBar(Player player) {
        BossBar bar = activeBossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    /**
     * Restaura a BossBar ao reconectar se o tutorial está em progresso.
     */
    public void restoreBossBar(Player player) {
        if (!isEnabled()) return;
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        int step = pd.getTutorialStep();
        if (step >= 1 && step <= TOTAL_STEPS && !pd.isTutorialCompleted()) {
            showStep(player, step);
        }
    }

    /**
     * Cleanup: remove todas as BossBars ativas (chamado no onDisable).
     */
    public void cleanup() {
        for (Map.Entry<UUID, BossBar> entry : activeBossBars.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isOnline()) {
                p.hideBossBar(entry.getValue());
            }
        }
        activeBossBars.clear();
    }
}
