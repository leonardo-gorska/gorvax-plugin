package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.towns.managers.NationManager;
import br.com.gorvax.core.towns.managers.NationManager.Nation;
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
 * B19 — Comando /nacao (aliases: /nation, /nação).
 * Subcomandos: criar, dissolver, convidar, aceitar, recusar, sair, expulsar,
 *              depositar, sacar, banco, info, lista, ajuda
 */
public class NationCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public NationCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "general.player_only");
            return true;
        }

        var msg = plugin.getMessageManager();
        var nm = plugin.getNationManager();

        if (nm == null) {
            msg.send(player, "nation.error_disabled");
            return true;
        }

        if (args.length == 0) {
            handleInfo(player, nm, msg);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "criar", "create" -> handleCreate(player, args, nm, msg);
            case "dissolver", "disband" -> handleDisband(player, nm, msg);
            case "convidar", "invite" -> handleInvite(player, args, nm, msg);
            case "aceitar", "accept" -> handleAccept(player, nm);
            case "recusar", "deny" -> handleDeny(player, nm);
            case "sair", "leave" -> handleLeave(player, nm);
            case "expulsar", "kick" -> handleKick(player, args, nm, msg);
            case "depositar", "deposit" -> handleDeposit(player, args, nm, msg);
            case "sacar", "withdraw" -> handleWithdraw(player, args, nm, msg);
            case "banco", "bank" -> handleBank(player, nm, msg);
            case "info" -> handleInfo(player, nm, msg);
            case "lista", "list" -> handleList(player, nm, msg);
            case "ajuda", "help" -> handleHelp(player, msg);
            default -> handleHelp(player, msg);
        }

        return true;
    }

    // ========================================================================
    // Subcomandos
    // ========================================================================

    private void handleCreate(Player player, String[] args, NationManager nm, MessageManager msg) {
        if (args.length < 2) {
            msg.send(player, "nation.create_usage");
            return;
        }
        nm.createNation(player, args[1]);
    }

    private void handleDisband(Player player, NationManager nm, MessageManager msg) {
        nm.disbandNation(player);
    }

    private void handleInvite(Player player, String[] args, NationManager nm, MessageManager msg) {
        if (args.length < 2) {
            msg.send(player, "nation.invite_usage");
            return;
        }
        nm.inviteKingdom(player, args[1]);
    }

    private void handleAccept(Player player, NationManager nm) {
        nm.acceptInvite(player);
    }

    private void handleDeny(Player player, NationManager nm) {
        nm.denyInvite(player);
    }

    private void handleLeave(Player player, NationManager nm) {
        nm.leaveNation(player);
    }

    private void handleKick(Player player, String[] args, NationManager nm, MessageManager msg) {
        if (args.length < 2) {
            msg.send(player, "nation.kick_usage");
            return;
        }
        nm.kickKingdom(player, args[1]);
    }

    private void handleDeposit(Player player, String[] args, NationManager nm, MessageManager msg) {
        if (args.length < 2) {
            msg.send(player, "nation.deposit_usage");
            return;
        }
        try {
            double amount = Double.parseDouble(args[1]);
            nm.depositBank(player, amount);
        } catch (NumberFormatException e) {
            msg.send(player, "general.invalid_value");
        }
    }

    private void handleWithdraw(Player player, String[] args, NationManager nm, MessageManager msg) {
        if (args.length < 2) {
            msg.send(player, "nation.withdraw_usage");
            return;
        }
        try {
            double amount = Double.parseDouble(args[1]);
            nm.withdrawBank(player, amount);
        } catch (NumberFormatException e) {
            msg.send(player, "general.invalid_value");
        }
    }

    private void handleBank(Player player, NationManager nm, MessageManager msg) {
        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
        if (kingdom == null) {
            msg.send(player, "nation.error_no_kingdom");
            return;
        }

        Nation nation = nm.getNationByKingdom(kingdom.getId());
        if (nation == null) {
            msg.send(player, "nation.error_not_in_nation");
            return;
        }

        msg.send(player, "nation.bank_header");
        msg.send(player, "nation.bank_balance", String.format("%.2f", nation.getBankBalance()));
        msg.send(player, "nation.bank_footer");
    }

    private void handleInfo(Player player, NationManager nm, MessageManager msg) {
        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
        if (kingdom == null) {
            msg.send(player, "nation.error_no_kingdom");
            return;
        }

        Nation nation = nm.getNationByKingdom(kingdom.getId());
        if (nation == null) {
            msg.send(player, "nation.error_not_in_nation");
            return;
        }

        int level = nm.getNationLevel(nation);
        String founderName = plugin.getKingdomManager().getNome(nation.getFounderKingdomId());
        if (founderName == null) founderName = "Desconhecido";

        // Listar nomes dos reinos membros
        StringBuilder members = new StringBuilder();
        for (String kid : nation.getMemberKingdomIds()) {
            String kName = plugin.getKingdomManager().getNome(kid);
            if (kName == null) kName = "???";
            if (members.length() > 0) members.append("§7, §f");
            members.append(kName);
        }

        msg.send(player, "nation.info_header");
        msg.send(player, "nation.info_name", nation.getName());
        msg.send(player, "nation.info_emperor", founderName);
        msg.send(player, "nation.info_level", String.valueOf(level));
        msg.send(player, "nation.info_kingdoms", String.valueOf(nation.getKingdomCount()));
        msg.send(player, "nation.info_members_list", members.toString());
        msg.send(player, "nation.info_bank", String.format("%.2f", nation.getBankBalance()));
        msg.send(player, "nation.info_footer");
    }

    private void handleList(Player player, NationManager nm, MessageManager msg) {
        var nations = nm.getAllNations();
        if (nations.isEmpty()) {
            msg.send(player, "nation.list_empty");
            return;
        }

        msg.send(player, "nation.list_header");
        int idx = 1;
        for (Nation n : nations) {
            String founderName = plugin.getKingdomManager().getNome(n.getFounderKingdomId());
            if (founderName == null) founderName = "???";
            msg.send(player, "nation.list_entry",
                    String.valueOf(idx),
                    n.getName(),
                    founderName,
                    String.valueOf(n.getKingdomCount()));
            idx++;
        }
        msg.send(player, "nation.list_footer");
    }

    private void handleHelp(Player player, MessageManager msg) {
        msg.send(player, "nation.help_header");
        msg.send(player, "nation.help_create");
        msg.send(player, "nation.help_disband");
        msg.send(player, "nation.help_invite");
        msg.send(player, "nation.help_accept");
        msg.send(player, "nation.help_deny");
        msg.send(player, "nation.help_leave");
        msg.send(player, "nation.help_kick");
        msg.send(player, "nation.help_deposit");
        msg.send(player, "nation.help_withdraw");
        msg.send(player, "nation.help_bank");
        msg.send(player, "nation.help_info");
        msg.send(player, "nation.help_list");
        msg.send(player, "nation.help_footer");
    }

    // ========================================================================
    // Tab Complete
    // ========================================================================

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = List.of(
                    "criar", "dissolver", "convidar", "aceitar", "recusar",
                    "sair", "expulsar", "depositar", "sacar", "banco",
                    "info", "lista", "ajuda"
            );
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            // Para convidar/expulsar: sugerir nomes de reinos
            if (sub.equals("convidar") || sub.equals("invite")
                    || sub.equals("expulsar") || sub.equals("kick")) {
                List<String> kingdoms = plugin.getKingdomManager().getAllKingdomNames();
                if (kingdoms != null) {
                    return kingdoms.stream()
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        return new ArrayList<>();
    }
}
