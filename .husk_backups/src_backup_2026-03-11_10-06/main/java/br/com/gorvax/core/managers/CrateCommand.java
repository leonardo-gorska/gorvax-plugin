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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * B12 — Comando /crate com subcomandos para gerenciar e abrir crates.
 *
 * Subcomandos:
 * - /crate → abre GUI de seleção de crates
 * - /crate abrir <tipo> → abre uma crate (consome 1 key)
 * - /crate preview <tipo> → mostra rewards possíveis
 * - /crate list → lista tipos de crate e keys
 * - /crate give <nick> <tipo> <qtd> → admin: dá keys
 */
public class CrateCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public CrateCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
            @NotNull String label, String[] args) {
        var msg = plugin.getMessageManager();
        CrateManager crateManager = plugin.getCrateManager();
        CrateGUI crateGUI = plugin.getCrateGUI();

        if (args.length == 0) {
            // Sem argumento: abrir GUI de seleção
            if (!(sender instanceof Player player)) {
                msg.send(sender, "general.player_only");
                return true;
            }
            // Verificar se é Bedrock
            if (InputManager.isBedrockPlayer(player)) {
                crateGUI.openBedrockForm(player);
            } else {
                crateGUI.openCrateSelection(player);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "abrir", "open" -> {
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "general.player_only");
                    return true;
                }
                if (args.length < 2) {
                    msg.send(player, "crate.usage_open");
                    return true;
                }
                String crateId = args[1].toLowerCase();
                crateGUI.openWithAnimation(player, crateId);
            }

            case "preview", "ver" -> {
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "general.player_only");
                    return true;
                }
                if (args.length < 2) {
                    msg.send(player, "crate.usage_preview");
                    return true;
                }
                String crateId = args[1].toLowerCase();
                if (InputManager.isBedrockPlayer(player)) {
                    // Bedrock: usar form com lista de rewards
                    openBedrockPreview(player, crateId);
                } else {
                    crateGUI.openPreview(player, crateId);
                }
            }

            case "list", "lista" -> {
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "general.player_only");
                    return true;
                }
                showCrateList(player);
            }

            case "give", "dar" -> {
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                if (args.length < 4) {
                    msg.send(sender, "crate.usage_give");
                    return true;
                }
                String targetName = args[1];
                String crateId = args[2].toLowerCase();
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                    if (amount <= 0)
                        throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    msg.send(sender, "crate.invalid_amount");
                    return true;
                }

                if (!crateManager.getCrateTypeIds().contains(crateId)) {
                    msg.send(sender, "crate.invalid_type");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    msg.send(sender, "general.player_not_found");
                    return true;
                }

                crateManager.giveKey(target, crateId, amount);
                CrateManager.CrateType crate = crateManager.getCrateType(crateId);
                msg.send(sender, "crate.give_success", amount, crate.name(), target.getName());
                msg.send(target, "crate.received_keys", amount, crate.name());
                return true;
            }

            default -> {
                msg.send(sender, "crate.usage");
            }
        }

        return true;
    }

    /**
     * Mostra a lista de crates e keys do jogador no chat.
     */
    private void showCrateList(Player player) {
        var msg = plugin.getMessageManager();
        CrateManager crateManager = plugin.getCrateManager();

        player.sendMessage(msg.get("crate.list_header"));
        player.sendMessage("");

        for (Map.Entry<String, CrateManager.CrateType> entry : crateManager.getAllCrateTypes().entrySet()) {
            CrateManager.CrateType crate = entry.getValue();
            int keys = crateManager.getKeyCount(player, entry.getKey());
            player.sendMessage(String.format("  %s §7— Keys: %s",
                    crate.name(),
                    keys > 0 ? "§a" + keys : "§c0"));
        }

        player.sendMessage("");
        player.sendMessage(msg.get("crate.list_footer"));
    }

    /**
     * Preview via Bedrock Form (SimpleForm com lista de rewards).
     */
    private void openBedrockPreview(Player player, String crateId) {
        CrateManager crateManager = plugin.getCrateManager();
        var msg = plugin.getMessageManager();
        CrateManager.CrateType crate = crateManager.getCrateType(crateId);
        if (crate == null) {
            msg.send(player, "crate.invalid_type");
            return;
        }

        if (!plugin.getBedrockFormManager().isAvailable()) {
            // Fallback: mostrar no chat
            player.sendMessage(msg.get("crate.preview_header", crate.name()));
            for (CrateManager.CrateReward reward : crate.rewards()) {
                double chance = (reward.weight() * 100.0) / crate.totalWeight();
                player.sendMessage(String.format("  %s §7(%.1f%%)", reward.display(), chance));
            }
            return;
        }

        org.geysermc.floodgate.api.FloodgateApi api = org.geysermc.floodgate.api.FloodgateApi.getInstance();
        StringBuilder content = new StringBuilder();
        for (CrateManager.CrateReward reward : crate.rewards()) {
            double chance = (reward.weight() * 100.0) / crate.totalWeight();
            content.append(String.format("• %s (%.1f%%)\n", reward.display(), chance));
        }

        org.geysermc.cumulus.form.SimpleForm form = org.geysermc.cumulus.form.SimpleForm.builder()
                .title(msg.get("crate.preview_title", crate.name()))
                .content(content.toString())
                .button("Fechar")
                .build();

        api.sendForm(player.getUniqueId(), form);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
            @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("abrir");
            completions.add("preview");
            completions.add("list");
            if (sender.hasPermission("gorvax.admin")) {
                completions.add("give");
            }
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("abrir") || sub.equals("open") || sub.equals("preview") || sub.equals("ver")) {
                completions.addAll(plugin.getCrateManager().getCrateTypeIds());
                return filterCompletions(completions, args[1]);
            }
            if ((sub.equals("give") || sub.equals("dar")) && sender.hasPermission("gorvax.admin")) {
                // Tab complete de nomes online
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                return filterCompletions(completions, args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if ((sub.equals("give") || sub.equals("dar")) && sender.hasPermission("gorvax.admin")) {
                completions.addAll(plugin.getCrateManager().getCrateTypeIds());
                return filterCompletions(completions, args[2]);
            }
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if ((sub.equals("give") || sub.equals("dar")) && sender.hasPermission("gorvax.admin")) {
                completions.add("1");
                completions.add("5");
                completions.add("10");
                return filterCompletions(completions, args[3]);
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
