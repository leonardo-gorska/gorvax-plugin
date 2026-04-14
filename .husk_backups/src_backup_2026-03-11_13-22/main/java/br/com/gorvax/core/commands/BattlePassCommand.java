package br.com.gorvax.core.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.BattlePassManager;
import br.com.gorvax.core.managers.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * B15 — Comando /pass para o sistema de Battle Pass.
 * Subcomandos: (nenhum)=GUI, info, nivel, resgatar, premium, reset, reload
 */
public class BattlePassCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public BattlePassCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String[] args) {
        var msg = plugin.getMessageManager();
        BattlePassManager bpm = plugin.getBattlePassManager();

        if (bpm == null || !bpm.isEnabled()) {
            msg.send(sender, "battlepass.disabled");
            return true;
        }

        // /pass — abre GUI
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                msg.send(sender, "general.player_only");
                return true;
            }
            plugin.getBattlePassGUI().open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "info" -> {
                // Info da temporada no chat
                sender.sendMessage(msg.get("battlepass.info_header"));
                sender.sendMessage(msg.get("battlepass.info_season", bpm.getSeasonName()));
                sender.sendMessage(msg.get("battlepass.info_season_number", bpm.getSeasonNumber()));
                sender.sendMessage(msg.get("battlepass.info_days", bpm.getDaysRemaining()));
                sender.sendMessage(msg.get("battlepass.info_active", bpm.isSeasonActive() ? "§aSim" : "§cNão"));
                sender.sendMessage(msg.get("battlepass.info_levels", bpm.getMaxLevel()));

                if (sender instanceof Player player) {
                    PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
                    sender.sendMessage(msg.get("battlepass.info_your_level", pd.getBattlePassLevel()));
                    sender.sendMessage(msg.get("battlepass.info_your_xp", pd.getBattlePassXp()));
                    sender.sendMessage(msg.get("battlepass.info_your_premium",
                            pd.isBattlePassPremium() ? "§d✦ Ativo" : "§cInativo"));
                }
                sender.sendMessage(msg.get("battlepass.info_header"));
            }

            case "nivel", "level" -> {
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "general.player_only");
                    return true;
                }
                PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
                int level = pd.getBattlePassLevel();
                int xp = pd.getBattlePassXp();
                int xpNeeded = level < bpm.getMaxLevel() ? bpm.getXpForLevel(level + 1) : 0;
                sender.sendMessage(msg.get("battlepass.level_info", level, bpm.getMaxLevel(), xp, xpNeeded));
            }

            case "resgatar", "claim" -> {
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "general.player_only");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(msg.get("battlepass.claim_usage"));
                    return true;
                }
                try {
                    int level = Integer.parseInt(args[1]);
                    // Tentar free
                    boolean claimedFree = bpm.claimReward(player, level, false);
                    // Tentar premium se tem
                    PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
                    if (pd.isBattlePassPremium()) {
                        bpm.claimReward(player, level, true);
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(msg.get("battlepass.invalid_level"));
                }
            }

            case "premium" -> {
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(msg.get("battlepass.premium_usage"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    msg.send(sender, "general.player_not_found");
                    return true;
                }
                bpm.activatePremium(target);
                sender.sendMessage(msg.get("battlepass.premium_activated", target.getName()));
                target.sendMessage(msg.get("battlepass.premium_received"));
            }

            case "reset" -> {
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(msg.get("battlepass.reset_usage"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    msg.send(sender, "general.player_not_found");
                    return true;
                }
                bpm.resetPlayer(target);
                sender.sendMessage(msg.get("battlepass.reset_done", target.getName()));
            }

            case "reload" -> {
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                bpm.reload();
                sender.sendMessage(msg.get("battlepass.reloaded"));
            }

            default -> {
                sender.sendMessage(msg.get("battlepass.help_header"));
                sender.sendMessage("§e/pass §7— Abre o menu do Battle Pass");
                sender.sendMessage("§e/pass info §7— Informações da temporada");
                sender.sendMessage("§e/pass nivel §7— Mostra seu nível e XP");
                sender.sendMessage("§e/pass resgatar <nível> §7— Resgata recompensas");
                if (sender.hasPermission("gorvax.admin")) {
                    sender.sendMessage("§e/pass premium <jogador> §7— Ativa premium");
                    sender.sendMessage("§e/pass reset <jogador> §7— Reseta progresso");
                    sender.sendMessage("§e/pass reload §7— Recarrega config");
                }
                sender.sendMessage(msg.get("battlepass.help_header"));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("info");
            completions.add("nivel");
            completions.add("resgatar");
            if (sender.hasPermission("gorvax.admin")) {
                completions.add("premium");
                completions.add("reset");
                completions.add("reload");
            }
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("resgatar") || sub.equals("claim")) {
                // Sugestões de nível (1-30)
                for (int i = 1; i <= 30; i++) {
                    completions.add(String.valueOf(i));
                }
                return filterCompletions(completions, args[1]);
            }
            if ((sub.equals("premium") || sub.equals("reset")) && sender.hasPermission("gorvax.admin")) {
                // Sugestões de jogadores online
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                return filterCompletions(completions, args[1]);
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
