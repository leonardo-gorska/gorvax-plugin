package br.com.gorvax.core.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.RankManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comando /rank — Abre GUI de progresso de ranks.
 * Subcomandos:
 * /rank → Abre GUI de progresso
 * /rank info → Mostra rank atual no chat
 * /rank check → Verifica se pode subir de rank (admin debug)
 */
public class RankCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public RankCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var msg = plugin.getMessageManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.get("rank.player_only"));
            return true;
        }

        RankManager rm = plugin.getRankManager();
        if (rm == null || !rm.isEnabled()) {
            player.sendMessage(msg.get("rank.disabled"));
            return true;
        }

        if (args.length == 0) {
            rm.openProgressGUI(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> {
                RankManager.GameRank rank = rm.getPlayerRank(player);
                player.sendMessage(msg.get("rank.info_header"));
                player.sendMessage(msg.get("rank.info_title"));
                player.sendMessage(msg.get("rank.info_current", rank.getDisplayName()));
                RankManager.GameRank next = rank.next();
                if (next != null) {
                    player.sendMessage(msg.get("rank.info_next", next.getDisplayName()));
                    player.sendMessage(msg.get("rank.info_use_rank"));
                } else {
                    player.sendMessage(msg.get("rank.info_max"));
                }
                player.sendMessage(msg.get("rank.info_header"));
            }
            case "check" -> {
                if (!player.hasPermission("gorvax.admin")) {
                    player.sendMessage(msg.get("rank.no_permission"));
                    return true;
                }
                boolean promoted = rm.checkAndPromote(player);
                if (!promoted) {
                    player.sendMessage(msg.get("rank.check_not_ready"));
                }
            }
            default -> {
                player.sendMessage(msg.get("rank.unknown_command"));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("info", "check").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
