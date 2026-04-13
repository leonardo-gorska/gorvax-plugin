package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * B3 — Comando /duel com subcomandos.
 * Subcomandos: <nick> [valor], aceitar, recusar, ajuda
 */
public class DuelCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;
    private final MessageManager msg;

    public DuelCommand(GorvaxCore plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "general.player_only");
            return true;
        }

        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager == null || !duelManager.isEnabled()) {
            msg.send(player, "duel.disabled");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "aceitar", "accept" -> duelManager.accept(player);
            case "recusar", "refuse", "negar", "deny" -> duelManager.refuse(player);
            case "ajuda", "help" -> showHelp(player);
            default -> handleChallenge(player, args, duelManager);
        }

        return true;
    }

    /**
     * Trata desafio: /duel <nick> [valor]
     */
    private void handleChallenge(Player player, String[] args, DuelManager duelManager) {
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            msg.send(player, "duel.player_not_found", targetName);
            return;
        }

        double bet = 0;
        if (args.length >= 2) {
            try {
                bet = Double.parseDouble(args[1]);
                if (bet < 0) bet = 0;
            } catch (NumberFormatException e) {
                msg.send(player, "duel.invalid_bet");
                return;
            }
        }

        duelManager.challenge(player, target, bet);
    }

    /**
     * Exibe ajuda do duelo.
     */
    private void showHelp(Player player) {
        msg.send(player, "duel.help_header");
        msg.send(player, "duel.help_challenge");
        msg.send(player, "duel.help_accept");
        msg.send(player, "duel.help_refuse");
        msg.send(player, "duel.help_footer");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) return completions;

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            // Subcomandos
            List<String> subs = Arrays.asList("aceitar", "recusar", "ajuda");
            for (String s : subs) {
                if (s.startsWith(input)) completions.add(s);
            }

            // Jogadores online
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList()));
        } else if (args.length == 2) {
            // Sugestões de valor
            String input = args[1].toLowerCase();
            List<String> values = Arrays.asList("100", "500", "1000", "5000", "10000");
            for (String v : values) {
                if (v.startsWith(input)) completions.add(v);
            }
        }

        return completions;
    }
}
