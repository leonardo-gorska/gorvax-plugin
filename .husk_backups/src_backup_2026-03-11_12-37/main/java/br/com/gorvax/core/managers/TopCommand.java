package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
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
 * B7 — Comando /top (aliases: /ranking, /rankings)
 * Exibe os rankings do servidor por categoria.
 */
public class TopCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;
    private final MessageManager msg;

    public TopCommand(GorvaxCore plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String[] args) {
        LeaderboardManager lm = plugin.getLeaderboardManager();

        // /top (sem argumentos) — mostrar lista de categorias
        if (args.length == 0) {
            showCategories(sender);
            return true;
        }

        String category = args[0].toLowerCase();

        if (!lm.isValidCategory(category)) {
            msg.send(sender, "top.invalid_category", args[0]);
            return true;
        }

        List<LeaderboardManager.LeaderboardEntry> entries = lm.getTop(category);

        if (entries.isEmpty()) {
            msg.send(sender, "top.no_data");
            return true;
        }

        // Nome traduzido da categoria
        String catName = msg.get("top.category." + category);

        // Cabeçalho
        sender.sendMessage(msg.get("top.header"));
        sender.sendMessage(msg.get("top.title", catName));
        sender.sendMessage("");

        // Lista
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardManager.LeaderboardEntry entry = entries.get(i);
            String formattedValue = formatValue(category, entry.value());
            sender.sendMessage(msg.get("top.entry", (i + 1), entry.name(), formattedValue));
        }

        // Rodapé
        sender.sendMessage("");
        sender.sendMessage(msg.get("top.footer"));

        // Posição do jogador (se for player e não está no top)
        if (sender instanceof Player player) {
            int pos = lm.getPosition(category, player.getUniqueId());
            if (pos > 0) {
                sender.sendMessage(msg.get("top.your_position", pos));
            }
        }

        return true;
    }

    /**
     * Mostra as categorias disponíveis ao jogador.
     */
    private void showCategories(CommandSender sender) {
        sender.sendMessage(msg.get("top.header"));
        sender.sendMessage(msg.get("top.categories_title"));
        sender.sendMessage("");
        for (String cat : LeaderboardManager.CATEGORIES) {
            String catName = msg.get("top.category." + cat);
            sender.sendMessage(msg.get("top.category_entry", cat, catName));
        }
        sender.sendMessage("");
        sender.sendMessage(msg.get("top.categories_hint"));
        sender.sendMessage(msg.get("top.footer"));
    }

    /**
     * Formata o valor de acordo com a categoria.
     */
    private String formatValue(String category, double value) {
        return switch (category) {
            case "kdr" -> String.format("%.2f", value);
            case "riqueza" -> String.format("$%,.0f", value);
            case "playtime" -> {
                long totalSeconds = (long) value / 1000;
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                yield hours + "h " + minutes + "m";
            }
            case "reinos" -> String.format("%.0f pts", value);
            default -> String.format("%.0f", value);
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return Arrays.stream(LeaderboardManager.CATEGORIES)
                    .filter(cat -> cat.startsWith(input))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
