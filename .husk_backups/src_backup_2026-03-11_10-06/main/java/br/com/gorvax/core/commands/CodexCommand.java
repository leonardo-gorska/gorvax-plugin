package br.com.gorvax.core.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.CodexManager;
import br.com.gorvax.core.managers.CodexManager.CodexCategory;
import br.com.gorvax.core.managers.PlayerData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * B28 — Comando /codex.
 * Subcomandos: (vazio)=GUI, progresso, <categoria>, give, reset.
 */
public class CodexCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public CodexCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // Sem argumentos → abre GUI
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("general.player_only")));
                return true;
            }
            plugin.getCodexGUI().openMain(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // /codex progresso
        if (sub.equals("progresso")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("general.player_only")));
                return true;
            }
            showProgress(player);
            return true;
        }

        // /codex give <player> <cat.entry>
        if (sub.equals("give")) {
            if (!sender.hasPermission("gorvax.admin")) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("general.no_permission")));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("codex.give_usage")));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("general.player_not_found")));
                return true;
            }
            String fullKey = args[2];
            if (plugin.getCodexManager().getEntry(fullKey) == null) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("codex.not_found").replace("{key}", fullKey)));
                return true;
            }
            boolean unlocked = plugin.getCodexManager().tryUnlock(target, fullKey);
            if (unlocked) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("codex.give_success")
                                .replace("{entry}", fullKey)
                                .replace("{player}", target.getName())));
            } else {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("codex.already_unlocked")));
            }
            return true;
        }

        // /codex reset <player>
        if (sub.equals("reset")) {
            if (!sender.hasPermission("gorvax.admin")) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("general.no_permission")));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("codex.reset_usage")));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("general.player_not_found")));
                return true;
            }
            PlayerData pd = plugin.getPlayerDataManager().getData(target.getUniqueId());
            if (pd != null) {
                pd.getUnlockedCodex().clear();
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("codex.reset_success")
                                .replace("{player}", target.getName())));
            }
            return true;
        }

        // /codex <categoria> — abre categoria específica
        CodexManager mgr = plugin.getCodexManager();
        if (mgr.getCategories().containsKey(sub)) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getMessageManager().get("general.player_only")));
                return true;
            }
            plugin.getCodexGUI().openCategory(player, sub);
            return true;
        }

        // Subcomando desconhecido → mostra ajuda
        showHelp(sender);
        return true;
    }

    private void showProgress(Player player) {
        CodexManager mgr = plugin.getCodexManager();
        int[] total = mgr.getProgress(player.getUniqueId());
        int percent = total[1] > 0 ? (total[0] * 100 / total[1]) : 0;

        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                plugin.getMessageManager().get("codex.progress_header")
                        .replace("{unlocked}", String.valueOf(total[0]))
                        .replace("{total}", String.valueOf(total[1]))
                        .replace("{percent}", String.valueOf(percent))));

        for (var entry : mgr.getCategories().entrySet()) {
            int[] catProg = mgr.getCategoryProgress(player.getUniqueId(), entry.getKey());
            int catPct = catProg[1] > 0 ? (catProg[0] * 100 / catProg[1]) : 0;
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    plugin.getMessageManager().get("codex.progress_category")
                            .replace("{category}", entry.getValue().nome())
                            .replace("{unlocked}", String.valueOf(catProg[0]))
                            .replace("{total}", String.valueOf(catProg[1]))
                            .replace("{percent}", String.valueOf(catPct))));
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("§6§l══ Códex de Gorvax ══"));
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("§e/codex §7— Abre a enciclopédia"));
        sender.sendMessage(
                LegacyComponentSerializer.legacySection().deserialize("§e/codex progresso §7— Mostra o percentual"));
        sender.sendMessage(
                LegacyComponentSerializer.legacySection().deserialize("§e/codex <categoria> §7— Abre uma categoria"));
        if (sender.hasPermission("gorvax.admin")) {
            sender.sendMessage(LegacyComponentSerializer.legacySection()
                    .deserialize("§c/codex give <player> <cat.entry> §7— Desbloqueia"));
            sender.sendMessage(LegacyComponentSerializer.legacySection()
                    .deserialize("§c/codex reset <player> §7— Reseta progresso"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("progresso");
            completions.addAll(plugin.getCodexManager().getCategories().keySet());
            if (sender.hasPermission("gorvax.admin")) {
                completions.add("give");
                completions.add("reset");
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("reset"))) {
            return null; // Sugestão de players padrão
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Sugerir chaves de entries
            CodexManager mgr = plugin.getCodexManager();
            for (var catEntry : mgr.getCategories().entrySet()) {
                for (String entryId : catEntry.getValue().entries().keySet()) {
                    completions.add(catEntry.getKey() + "." + entryId);
                }
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return completions;
    }
}
