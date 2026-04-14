package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * B17.3 — Comando /bounty com subcomandos.
 * Subcomandos: colocar, listar, remover
 */
public class BountyCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public BountyCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
                              String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.getMessageManager().get("general.player_only"));
            return true;
        }

        var msg = plugin.getMessageManager();
        BountyManager bountyManager = plugin.getBountyManager();

        if (bountyManager == null || !bountyManager.isEnabled()) {
            msg.send(p, "bounty.disabled");
            return true;
        }

        if (args.length == 0) {
            showHelp(p);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "colocar", "place", "add" -> handlePlace(p, args);
            case "listar", "list" -> handleList(p);
            case "remover", "remove" -> handleRemove(p, args);
            case "ajuda", "help" -> showHelp(p);
            default -> {
                msg.send(p, "bounty.unknown_command");
                showHelp(p);
            }
        }

        return true;
    }

    private void handlePlace(Player p, String[] args) {
        var msg = plugin.getMessageManager();

        if (args.length < 3) {
            msg.send(p, "bounty.usage_place");
            return;
        }

        String targetName = args[1];
        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            msg.send(p, "bounty.invalid_value");
            return;
        }

        BountyManager bm = plugin.getBountyManager();

        if (value < bm.getMinValue()) {
            msg.send(p, "bounty.value_too_low", String.format("%.0f", bm.getMinValue()));
            return;
        }
        if (value > bm.getMaxValue()) {
            msg.send(p, "bounty.value_too_high", String.format("%.0f", bm.getMaxValue()));
            return;
        }

        // Buscar jogador alvo
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            msg.send(p, "bounty.player_not_found");
            return;
        }

        UUID targetUUID = target.getUniqueId();

        // Verificar saldo
        Economy econ = GorvaxCore.getEconomy();
        if (!econ.has(p, value)) {
            msg.send(p, "bounty.insufficient_funds", String.format("%.2f", value));
            return;
        }

        // Tentar colocar bounty
        int result = bm.placeBounty(p.getUniqueId(), targetUUID, targetName, value);

        switch (result) {
            case 0 -> {
                // Sucesso — debitar dinheiro
                econ.withdrawPlayer(p, value);
                double total = bm.getBountyValue(targetUUID);
                msg.send(p, "bounty.placed", targetName, String.format("%.2f", value),
                        String.format("%.2f", total));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);

                // Broadcast global
                plugin.getMessageManager().broadcast("bounty.placed_broadcast",
                        p.getName(), targetName, String.format("%.2f", total));

                // Notificar alvo se online
                Player onlineTarget = Bukkit.getPlayer(targetUUID);
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    msg.send(onlineTarget, "bounty.target_notification",
                            String.format("%.2f", total));
                    onlineTarget.playSound(onlineTarget.getLocation(),
                            Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.5f);
                }
            }
            case 2 -> msg.send(p, "bounty.invalid_value");
            case 3 -> msg.send(p, "bounty.same_kingdom");
            case 4 -> msg.send(p, "bounty.cannot_self");
            default -> msg.send(p, "bounty.error");
        }
    }

    private void handleList(Player p) {
        var msg = plugin.getMessageManager();
        BountyManager bm = plugin.getBountyManager();

        List<BountyManager.Bounty> bounties = bm.getAllBounties();

        if (bounties.isEmpty()) {
            msg.send(p, "bounty.none_active");
            return;
        }

        p.sendMessage(msg.get("bounty.list_header"));
        p.sendMessage(msg.get("bounty.separator"));

        int count = Math.min(bounties.size(), 15); // Limitar a 15
        for (int i = 0; i < count; i++) {
            BountyManager.Bounty bounty = bounties.get(i);
            p.sendMessage(msg.get("bounty.list_entry",
                    i + 1, bounty.targetName, String.format("%.2f", bounty.totalValue),
                    bounty.contributors.size(), bounty.getFormattedDate()));
        }

        p.sendMessage(msg.get("bounty.separator"));

        if (bounties.size() > 15) {
            p.sendMessage(msg.get("bounty.list_more", bounties.size() - 15));
        }
    }

    private void handleRemove(Player p, String[] args) {
        var msg = plugin.getMessageManager();

        if (args.length < 2) {
            msg.send(p, "bounty.usage_remove");
            return;
        }

        String targetName = args[1];

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null) {
            msg.send(p, "bounty.player_not_found");
            return;
        }

        boolean removed = plugin.getBountyManager().removeContribution(p.getUniqueId(), target.getUniqueId());

        if (removed) {
            msg.send(p, "bounty.removed", targetName);
        } else {
            msg.send(p, "bounty.not_contributor", targetName);
        }
    }

    private void showHelp(Player p) {
        var msg = plugin.getMessageManager();
        p.sendMessage(msg.get("bounty.help_header"));
        p.sendMessage(msg.get("bounty.help_place"));
        p.sendMessage(msg.get("bounty.help_list"));
        p.sendMessage(msg.get("bounty.help_remove"));
        p.sendMessage(msg.get("bounty.help_footer"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                       @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("colocar");
            completions.add("listar");
            completions.add("remover");
            completions.add("ajuda");
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("colocar") || sub.equals("place") || sub.equals("add")
                    || sub.equals("remover") || sub.equals("remove")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("colocar") || sub.equals("place") || sub.equals("add")) {
                completions.add("<valor>");
            }
        }

        return completions;
    }
}
