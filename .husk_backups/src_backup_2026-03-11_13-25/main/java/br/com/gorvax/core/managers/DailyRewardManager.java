package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

/**
 * B5 — Gerencia o sistema de Daily Rewards & Login Streak.
 * Recompensas diárias com ciclo de 7 dias, GUI interativa e adaptação Bedrock.
 */
public class DailyRewardManager implements Listener, CommandExecutor {

    private final GorvaxCore plugin;
    private final MessageManager msg;

    // Config
    private boolean enabled;
    private int minHours;
    private int maxHours;

    public DailyRewardManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
        loadConfig();
    }

    private void loadConfig() {
        var config = plugin.getConfig();
        this.enabled = config.getBoolean("daily_rewards.enabled", true);
        this.minHours = config.getInt("daily_rewards.min_hours", 20);
        this.maxHours = config.getInt("daily_rewards.max_hours", 48);
    }

    /**
     * Recarrega configuração do daily rewards.
     */
    public void reload() {
        loadConfig();
    }

    // --- Listener de Join ---

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();

        // Atraso de 3s para o jogador carregar totalmente
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline())
                return;
            checkAndNotify(player);
        }, 60L); // 3 segundos
    }

    /**
     * Verifica se o jogador pode resgatar recompensa diária e notifica.
     */
    public void checkAndNotify(Player player) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        long now = System.currentTimeMillis();
        long lastReward = pd.getLastDailyReward();
        long hoursSinceReward = (lastReward > 0) ? (now - lastReward) / (1000 * 60 * 60) : Long.MAX_VALUE;

        // Se passou mais de maxHours, resetar streak
        if (lastReward > 0 && hoursSinceReward > maxHours) {
            pd.setLoginStreak(0);
        }

        // Se passou mais de minHours (ou nunca resgatou), marcar como pendente
        if (hoursSinceReward >= minHours) {
            pd.setDailyRewardPending(true);
            int nextDay = Math.min(pd.getLoginStreak() + 1, 7);
            String message = msg.get("daily.available", String.valueOf(nextDay));
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        }

        plugin.getPlayerDataManager().saveData(player.getUniqueId());
    }

    /**
     * Resgata a recompensa diária do jogador.
     */
    public boolean claimReward(Player player) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        long now = System.currentTimeMillis();
        long lastReward = pd.getLastDailyReward();
        long hoursSinceReward = (lastReward > 0) ? (now - lastReward) / (1000 * 60 * 60) : Long.MAX_VALUE;

        // Verificar se pode resgatar
        if (hoursSinceReward < minHours) {
            long hoursLeft = minHours - hoursSinceReward;
            String message = msg.get("daily.already_claimed", String.valueOf(hoursLeft));
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
            return false;
        }

        // Se passou mais de maxHours, resetar streak
        if (lastReward > 0 && hoursSinceReward > maxHours) {
            pd.setLoginStreak(0);
            String resetMsg = msg.get("daily.streak_reset");
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(resetMsg));
        }

        // Incrementar streak (máximo 7, depois volta para 1)
        int newStreak = pd.getLoginStreak() + 1;
        if (newStreak > 7)
            newStreak = 1;
        pd.setLoginStreak(newStreak);
        pd.setLastDailyReward(now);
        pd.setDailyRewardPending(false);

        // Aplicar recompensas do dia
        StringBuilder rewardDesc = applyDayReward(player, pd, newStreak);

        // Enviar mensagem de sucesso
        String claimedMsg = msg.get("daily.claimed", String.valueOf(newStreak), rewardDesc.toString());
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(claimedMsg));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);

        plugin.getPlayerDataManager().saveData(player.getUniqueId());
        return true;
    }

    /**
     * Aplica as recompensas configuradas para um determinado dia.
     */
    private StringBuilder applyDayReward(Player player, PlayerData pd, int day) {
        ConfigurationSection dayConfig = plugin.getConfig().getConfigurationSection("daily_rewards.day_" + day);
        StringBuilder desc = new StringBuilder();

        if (dayConfig == null) {
            // Fallback: $100 por dia
            if (GorvaxCore.getEconomy() != null) {
                GorvaxCore.getEconomy().depositPlayer(player, 100.0);
            }
            desc.append(msg.get("daily.reward_money", "100"));
            return desc;
        }

        // Dinheiro
        double money = dayConfig.getDouble("money", 0);
        if (money > 0 && GorvaxCore.getEconomy() != null) {
            GorvaxCore.getEconomy().depositPlayer(player, money);
            if (desc.length() > 0)
                desc.append(" ");
            desc.append(msg.get("daily.reward_money", String.format("%.0f", money)));
        }

        // Blocos de claim
        int blocks = dayConfig.getInt("claim_blocks", 0);
        if (blocks > 0) {
            pd.addClaimBlocks(blocks);
            if (desc.length() > 0)
                desc.append(" ");
            desc.append(msg.get("daily.reward_blocks", String.valueOf(blocks)));
        }

        // Título
        String title = dayConfig.getString("title", null);
        if (title != null && !title.isEmpty()) {
            pd.getUnlockedTitles().add(title);
            if (desc.length() > 0)
                desc.append(" ");
            desc.append(msg.get("daily.reward_title", title));
        }

        // B12 — Crate Key
        String crateKeyType = dayConfig.getString("crate_key", null);
        if (crateKeyType != null && !crateKeyType.isEmpty() && plugin.getCrateManager() != null) {
            int keyAmount = dayConfig.getInt("crate_key_amount", 1);
            plugin.getCrateManager().giveKey(player, crateKeyType, keyAmount);
            if (desc.length() > 0)
                desc.append(" ");
            desc.append(msg.get("daily.reward_crate_key", String.valueOf(keyAmount), crateKeyType));
        }

        // Fallback se nenhuma recompensa foi configurada
        if (desc.length() == 0) {
            if (GorvaxCore.getEconomy() != null) {
                GorvaxCore.getEconomy().depositPlayer(player, 100.0);
            }
            desc.append(msg.get("daily.reward_money", "100"));
        }

        return desc;
    }

    /**
     * Retorna a descrição da recompensa de um dia (para exibição na GUI).
     */
    public String getRewardDescription(int day) {
        ConfigurationSection dayConfig = plugin.getConfig().getConfigurationSection("daily_rewards.day_" + day);
        if (dayConfig == null) {
            return msg.get("daily.reward_money", "100");
        }

        StringBuilder desc = new StringBuilder();
        double money = dayConfig.getDouble("money", 0);
        if (money > 0) {
            desc.append(msg.get("daily.reward_money", String.format("%.0f", money)));
        }
        int blocks = dayConfig.getInt("claim_blocks", 0);
        if (blocks > 0) {
            if (desc.length() > 0)
                desc.append(" ");
            desc.append(msg.get("daily.reward_blocks", String.valueOf(blocks)));
        }
        String title = dayConfig.getString("title", null);
        if (title != null && !title.isEmpty()) {
            if (desc.length() > 0)
                desc.append(" ");
            desc.append(msg.get("daily.reward_title", title));
        }
        // B12 — Crate Key
        String crateKeyType = dayConfig.getString("crate_key", null);
        if (crateKeyType != null && !crateKeyType.isEmpty()) {
            int keyAmount = dayConfig.getInt("crate_key_amount", 1);
            if (desc.length() > 0)
                desc.append(" ");
            desc.append(msg.get("daily.reward_crate_key", String.valueOf(keyAmount), crateKeyType));
        }
        if (desc.length() == 0) {
            desc.append(msg.get("daily.reward_money", "100"));
        }
        return desc.toString();
    }

    /**
     * Abre a GUI de recompensas diárias para o jogador.
     * Se Bedrock, usa SimpleForm.
     */
    public void openGUI(Player player) {
        if (!enabled) {
            String message = msg.get("daily.not_available");
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
            return;
        }

        // Verificar se é Bedrock — usar SimpleForm
        BedrockFormManager bedrock = plugin.getBedrockFormManager();
        if (bedrock.isAvailable() && InputManager.isBedrockPlayer(player)) {
            openBedrockForm(player);
            return;
        }

        // Java: abrir GUI inventário
        DailyRewardGUI gui = new DailyRewardGUI(plugin, this);
        gui.open(player);
    }

    /**
     * Abre form nativo para jogadores Bedrock.
     */
    private void openBedrockForm(Player player) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        long now = System.currentTimeMillis();
        long lastReward = pd.getLastDailyReward();
        long hoursSinceReward = (lastReward > 0) ? (now - lastReward) / (1000 * 60 * 60) : Long.MAX_VALUE;

        StringBuilder content = new StringBuilder();
        int currentStreak = pd.getLoginStreak();

        for (int day = 1; day <= 7; day++) {
            String reward = getRewardDescription(day);
            if (day <= currentStreak) {
                content.append("✅ Dia ").append(day).append(" — Resgatado\n");
            } else if (day == currentStreak + 1 && hoursSinceReward >= minHours) {
                content.append("🎁 Dia ").append(day).append(" — DISPONÍVEL! (").append(reward).append(")\n");
            } else {
                content.append("⏳ Dia ").append(day).append(" — ").append(reward).append("\n");
            }
        }

        List<String> buttons = new java.util.ArrayList<>();
        boolean canClaim = hoursSinceReward >= minHours;
        if (canClaim) {
            buttons.add("🎁 Resgatar Dia " + Math.min(currentStreak + 1, 7));
        }
        buttons.add("Fechar");

        plugin.getBedrockFormManager().sendSimpleForm(player,
                "🎁 Recompensas Diárias",
                content.toString(),
                buttons,
                index -> {
                    if (canClaim && index == 0) {
                        claimReward(player);
                    }
                });
    }

    // --- CommandExecutor para /daily ---

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    msg.get("general.player_only")));
            return true;
        }

        openGUI(player);
        return true;
    }
}
