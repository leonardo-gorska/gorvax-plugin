package br.com.gorvax.core.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.ReputationManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
 * B18 — Comando /karma para visualizar e gerenciar reputação.
 * /karma — mostra seu karma
 * /karma <nick> — mostra karma de outro jogador
 * /karma top — ranking de karma (heróis e vilões)
 * /karma set <nick> <valor> — admin: setar karma
 * /karma add <nick> <valor> — admin: adicionar/remover karma
 */
public class ReputationCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public ReputationCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
        var msg = plugin.getMessageManager();
        ReputationManager rep = plugin.getReputationManager();

        if (rep == null || !rep.isEnabled()) {
            sender.sendMessage("§b[Gorvax] §cSistema de karma desabilitado.");
            return true;
        }

        // /karma — mostra info do próprio jogador
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                msg.send(sender, "general.player_only");
                return true;
            }
            showKarmaInfo(p, p);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "top":
            case "ranking":
                showTopKarma(sender);
                return true;

            case "set":
            case "setar":
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(msg.get("karma.usage_set"));
                    return true;
                }
                handleSet(sender, args[1], args[2]);
                return true;

            case "add":
            case "adicionar":
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(msg.get("karma.usage_add"));
                    return true;
                }
                handleAdd(sender, args[1], args[2]);
                return true;

            default:
                // /karma <nick> — mostra karma de outro jogador
                OfflinePlayer target = Bukkit.getOfflinePlayer(sub);
                if (target.hasPlayedBefore() || target.isOnline()) {
                    if (sender instanceof Player p) {
                        showKarmaInfoTarget(p, target);
                    } else {
                        showKarmaInfoConsole(sender, target);
                    }
                } else {
                    sender.sendMessage(msg.get("karma.player_not_found"));
                }
                return true;
        }
    }

    private void showKarmaInfo(Player viewer, Player target) {
        var msg = plugin.getMessageManager();
        ReputationManager rep = plugin.getReputationManager();
        PlayerData pd = plugin.getPlayerDataManager().getData(target.getUniqueId());

        int karma = pd.getKarma();
        ReputationManager.KarmaRank rank = rep.getKarmaRank(karma);

        viewer.sendMessage(msg.get("karma.header"));
        viewer.sendMessage(msg.get("karma.info_value", rank.getColor() + karma));
        viewer.sendMessage(msg.get("karma.info_rank", rank.getLabel()));

        // Mostrar efeitos ativos
        double discount = rep.getMarketDiscount(karma);
        double priceMult = rep.getPriceMultiplier(karma);
        if (discount > 0) {
            viewer.sendMessage(msg.get("karma.effect_discount", String.format("%.0f", discount)));
        }
        if (priceMult > 1.0) {
            viewer.sendMessage(msg.get("karma.effect_price_increase",
                    String.format("%.0f", (priceMult - 1.0) * 100)));
        }
        viewer.sendMessage(msg.get("karma.header"));
    }

    private void showKarmaInfoTarget(Player viewer, OfflinePlayer target) {
        var msg = plugin.getMessageManager();
        ReputationManager rep = plugin.getReputationManager();
        PlayerData pd = plugin.getPlayerDataManager().getData(target.getUniqueId());

        int karma = pd.getKarma();
        ReputationManager.KarmaRank rank = rep.getKarmaRank(karma);

        String name = target.getName() != null ? target.getName() : "Desconhecido";
        viewer.sendMessage(msg.get("karma.header"));
        viewer.sendMessage(msg.get("karma.info_player", name));
        viewer.sendMessage(msg.get("karma.info_value", rank.getColor() + karma));
        viewer.sendMessage(msg.get("karma.info_rank", rank.getLabel()));
        viewer.sendMessage(msg.get("karma.header"));
    }

    private void showKarmaInfoConsole(CommandSender sender, OfflinePlayer target) {
        var msg = plugin.getMessageManager();
        ReputationManager rep = plugin.getReputationManager();
        PlayerData pd = plugin.getPlayerDataManager().getData(target.getUniqueId());

        int karma = pd.getKarma();
        ReputationManager.KarmaRank rank = rep.getKarmaRank(karma);

        String name = target.getName() != null ? target.getName() : "Desconhecido";
        sender.sendMessage(msg.get("karma.info_player", name));
        sender.sendMessage(msg.get("karma.info_value", rank.getColor() + karma));
        sender.sendMessage(msg.get("karma.info_rank", rank.getLabel()));
    }

    /**
     * Mostra top 10 heróis e top 10 vilões.
     */
    private void showTopKarma(CommandSender sender) {
        var msg = plugin.getMessageManager();
        ReputationManager rep = plugin.getReputationManager();

        // Ler todos os jogadores do playerdata
        var dataConfig = plugin.getPlayerDataManager().getDataConfig();
        if (dataConfig == null) {
            sender.sendMessage("§c[Gorvax] Dados indisponíveis.");
            return;
        }

        // Coletar todos os jogadores com karma
        List<KarmaEntry> entries = new ArrayList<>();
        for (String uuidStr : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                int karma = dataConfig.getInt(uuidStr + ".karma", 0);
                String name = plugin.getPlayerName(uuid);
                entries.add(new KarmaEntry(name, karma));
            } catch (IllegalArgumentException ignored) {
                // Ignorar chaves que não são UUIDs
            }
        }

        sender.sendMessage(msg.get("karma.top_header"));
        sender.sendMessage("");

        // Top 10 Heróis (maior karma primeiro)
        sender.sendMessage("§a§l  ✦ Top 10 Heróis");
        entries.stream()
                .sorted((a, b) -> Integer.compare(b.karma, a.karma))
                .limit(10)
                .forEach(entry -> {
                    ReputationManager.KarmaRank rank = rep.getKarmaRank(entry.karma);
                    sender.sendMessage(String.format("  %s%s §7— %s%d §8(%s§8)",
                            rank.getColor(), entry.name, rank.getColor(), entry.karma, rank.getLabel()));
                });

        sender.sendMessage("");

        // Top 10 Vilões (menor karma primeiro)
        sender.sendMessage("§c§l  ☠ Top 10 Vilões");
        entries.stream()
                .sorted((a, b) -> Integer.compare(a.karma, b.karma))
                .limit(10)
                .forEach(entry -> {
                    ReputationManager.KarmaRank rank = rep.getKarmaRank(entry.karma);
                    sender.sendMessage(String.format("  %s%s §7— %s%d §8(%s§8)",
                            rank.getColor(), entry.name, rank.getColor(), entry.karma, rank.getLabel()));
                });

        sender.sendMessage("");
        sender.sendMessage(msg.get("karma.top_header"));
    }

    private void handleSet(CommandSender sender, String targetName, String valueStr) {
        var msg = plugin.getMessageManager();
        int value;
        try {
            value = Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(msg.get("karma.invalid_number"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(msg.get("karma.player_not_found"));
            return;
        }

        PlayerData pd = plugin.getPlayerDataManager().getData(target.getUniqueId());
        pd.setKarma(value);
        plugin.getPlayerDataManager().saveData(target.getUniqueId());

        String name = target.getName() != null ? target.getName() : "Desconhecido";
        sender.sendMessage(msg.get("karma.admin_set", name, pd.getKarma()));
    }

    private void handleAdd(CommandSender sender, String targetName, String valueStr) {
        var msg = plugin.getMessageManager();
        int value;
        try {
            value = Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(msg.get("karma.invalid_number"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(msg.get("karma.player_not_found"));
            return;
        }

        PlayerData pd = plugin.getPlayerDataManager().getData(target.getUniqueId());
        pd.addKarma(value);
        plugin.getPlayerDataManager().saveData(target.getUniqueId());

        String name = target.getName() != null ? target.getName() : "Desconhecido";
        sender.sendMessage(msg.get("karma.admin_add", value, name, pd.getKarma()));
    }

    private record KarmaEntry(String name, int karma) {
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
            @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("top");
            if (sender.hasPermission("gorvax.admin")) {
                completions.add("set");
                completions.add("add");
            }
            // Nomes de jogadores online
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2 && sender.hasPermission("gorvax.admin")) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set") || sub.equals("add") || sub.equals("setar") || sub.equals("adicionar")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                return filterCompletions(completions, args[1]);
            }
        }

        if (args.length == 3 && sender.hasPermission("gorvax.admin")) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set") || sub.equals("setar")) {
                completions.add("0");
                completions.add("50");
                completions.add("100");
                completions.add("-50");
                completions.add("-100");
            } else if (sub.equals("add") || sub.equals("adicionar")) {
                completions.add("10");
                completions.add("25");
                completions.add("50");
                completions.add("-10");
                completions.add("-25");
            }
            return filterCompletions(completions, args[2]);
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
