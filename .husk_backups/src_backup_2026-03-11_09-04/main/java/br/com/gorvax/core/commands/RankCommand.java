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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        RankManager rm = plugin.getRankManager();
        if (rm == null || !rm.isEnabled()) {
            player.sendMessage("§c§lRANK §8» §7Sistema de ranks está desativado.");
            return true;
        }

        if (args.length == 0) {
            rm.openProgressGUI(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> {
                RankManager.GameRank rank = rm.getPlayerRank(player);
                player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━");
                player.sendMessage("§6§l  ⬆ SEU RANK");
                player.sendMessage("§7  Rank atual: " + rank.getDisplayName());
                RankManager.GameRank next = rank.next();
                if (next != null) {
                    player.sendMessage("§7  Próximo: " + next.getDisplayName());
                    player.sendMessage("§7  Use §f/rank §7para ver os requisitos.");
                } else {
                    player.sendMessage("§a  ★ Você está no rank máximo!");
                }
                player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━");
            }
            case "check" -> {
                if (!player.hasPermission("gorvax.admin")) {
                    player.sendMessage("§cSem permissão.");
                    return true;
                }
                boolean promoted = rm.checkAndPromote(player);
                if (!promoted) {
                    player.sendMessage("§6§lRANK §8» §7Você ainda não atinge os requisitos para o próximo rank.");
                }
            }
            default -> {
                player.sendMessage(
                        "§6§lRANK §8» §7Use §f/rank §7para abrir o menu ou §f/rank info §7para ver seu rank.");
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
