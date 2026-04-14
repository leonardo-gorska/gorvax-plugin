package br.com.gorvax.core.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.QuestGUI;
import br.com.gorvax.core.managers.QuestManager;
import br.com.gorvax.core.managers.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * B16 — Comando /quests para abrir GUI, ver info e recarregar.
 */
public class QuestCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;
    private final QuestGUI questGUI;

    public QuestCommand(GorvaxCore plugin, QuestGUI questGUI) {
        this.plugin = plugin;
        this.questGUI = questGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {

        var msg = plugin.getMessageManager();

        if (!(sender instanceof Player player)) {
            msg.send(sender, "general.player_only");
            return true;
        }

        if (args.length == 0) {
            // Abrir GUI
            questGUI.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "info" -> showInfo(player);
            case "reload" -> {
                if (!player.hasPermission("gorvax.admin")) {
                    msg.send(player, "general.no_permission");
                    return true;
                }
                plugin.getQuestManager().reload();
                msg.send(player, "quests.reload_success");
            }
            default -> questGUI.open(player);
        }

        return true;
    }

    /**
     * Mostra o progresso das quests no chat.
     */
    private void showInfo(Player player) {
        var msg = plugin.getMessageManager();
        QuestManager qm = plugin.getQuestManager();
        UUID uuid = player.getUniqueId();
        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);

        // Resetar se necessário
        qm.checkAndResetIfNeeded(pd);

        msg.send(player, "quests.info_header");

        // Diárias
        msg.send(player, "quests.info_daily_title");
        for (String questId : qm.getActiveDailyIds()) {
            QuestManager.QuestDefinition def = qm.getQuest(questId);
            if (def == null) continue;

            int progress = qm.getProgress(uuid, questId);
            boolean claimed = qm.isClaimed(uuid, questId);
            boolean completed = progress >= def.amount();

            String status;
            if (claimed) {
                status = "§a✔ Resgatada";
            } else if (completed) {
                status = "§e★ Completa! Use /quests para resgatar";
            } else {
                status = "§f" + progress + "/" + def.amount();
            }

            msg.send(player, "quests.info_entry", def.name(), status);
        }

        // Semanal
        String weeklyId = qm.getActiveWeeklyId();
        if (weeklyId != null) {
            QuestManager.QuestDefinition def = qm.getQuest(weeklyId);
            if (def != null) {
                msg.send(player, "quests.info_weekly_title");
                int progress = qm.getProgress(uuid, weeklyId);
                boolean claimed = qm.isClaimed(uuid, weeklyId);
                boolean completed = progress >= def.amount();

                String status;
                if (claimed) {
                    status = "§a✔ Resgatada";
                } else if (completed) {
                    status = "§e★ Completa! Use /quests para resgatar";
                } else {
                    status = "§f" + progress + "/" + def.amount();
                }

                msg.send(player, "quests.info_entry", def.name(), status);
            }
        }

        msg.send(player, "quests.info_footer");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
            @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("info");
            if (sender.hasPermission("gorvax.admin")) {
                completions.add("reload");
            }
            // Filtrar por prefixo
            String prefix = args[0].toLowerCase();
            completions.removeIf(s -> !s.startsWith(prefix));
        }
        return completions;
    }
}
