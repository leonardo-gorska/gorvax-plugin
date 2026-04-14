package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.AuditManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

public class MarketCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public MarketCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
        if (!(sender instanceof Player p)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                plugin.getMarketManager().reload();
                sender.sendMessage(plugin.getMessageManager().get("market_cmd.reload_console"));
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!p.hasPermission("gorvax.admin")) {
                plugin.getMessageManager().send(p, "general.no_permission");
                return true;
            }
            plugin.getMarketManager().reload();
            plugin.getMessageManager().send(p, "market.reload_success");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("local")) {
            // FIXED: Procura pelo Reino onde o jogador está, mesmo se estiver em um Feudo
            // ou Subplot
            Claim targetKingdom = null;

            // 1. Tenta pegar o claim exato onde o player está
            Claim current = plugin.getClaimManager().getClaimAt(p.getLocation());
            if (current != null && current.isKingdom()) {
                targetKingdom = current;
            } else {
                // 2. Fallback: Procura por claims nos arredores (garante detecção se ele
                // estiver na borda)
                java.util.Set<Claim> nearbyClaims = plugin.getClaimManager().getClaimsNearby(p.getLocation(), 0);
                for (Claim c : nearbyClaims) {
                    if (c.contains(p.getLocation()) && c.isKingdom()) {
                        targetKingdom = c;
                        break;
                    }
                }
            }

            if (targetKingdom == null) {
                plugin.getMessageManager().send(p, "market.error_not_in_kingdom");
                return true;
            }

            plugin.getMarketManager().openLocalMarket(p, targetKingdom.getId(), 1);
            return true;
        }

        // B10 — Histórico de transações
        if (args.length > 0 && args[0].equalsIgnoreCase("historico")) {
            showMarketHistory(p);
            return true;
        }

        // B14 — Histórico de preços
        if (args.length > 0 && args[0].equalsIgnoreCase("preco")) {
            showPriceHistory(p, args);
            return true;
        }

        plugin.getMarketManager().openGlobalMarket(p);
        return true;
    }

    // B10 — Histórico de transações do mercado do jogador
    private void showMarketHistory(Player p) {
        var msg = plugin.getMessageManager();
        AuditManager audit = plugin.getAuditManager();

        if (audit == null) {
            msg.send(p, "audit.not_available");
            return;
        }

        List<AuditManager.AuditEntry> history = audit.getMarketHistory(
                p.getUniqueId(), audit.getMarketHistoryPerPlayer());

        if (history.isEmpty()) {
            msg.send(p, "market_history.empty");
            return;
        }

        p.sendMessage(msg.get("market_history.header"));
        p.sendMessage("");

        int shown = Math.min(history.size(), 10);
        for (int i = 0; i < shown; i++) {
            AuditManager.AuditEntry entry = history.get(i);
            String actionLabel = switch (entry.action) {
                case MARKET_BUY -> "§a▲ Compra";
                case MARKET_SELL -> "§e▼ Venda";
                case MARKET_CANCEL -> "§7✖ Cancelado";
                default -> "§7?";
            };
            String valueStr = entry.value != 0.0 ? String.format(" §6$%.2f", entry.value) : "";
            p.sendMessage(String.format(
                    "  %s §7%s §8» §f%s%s",
                    actionLabel,
                    entry.getFormattedDate(),
                    entry.details,
                    valueStr
            ));
        }

        p.sendMessage("");
        if (history.size() > shown) {
            p.sendMessage(msg.get("market_history.more", history.size() - shown));
        }
        p.sendMessage(msg.get("market_history.footer"));
    }

    // B14 — Histórico de preços do mercado global
    private void showPriceHistory(Player p, String[] args) {
        var msg = plugin.getMessageManager();
        PriceHistoryManager phm = plugin.getPriceHistoryManager();

        if (phm == null) {
            msg.send(p, "price_history.not_available");
            return;
        }

        if (args.length < 2) {
            msg.send(p, "price_history.usage");
            return;
        }

        String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        MarketManager.MarketItem item = phm.findItem(query);

        if (item == null) {
            msg.send(p, "price_history.item_not_found", query);
            return;
        }

        p.sendMessage(msg.get("price_history.header", item.name));
        p.sendMessage("");
        for (String line : phm.getPriceHistory(item.id)) {
            p.sendMessage(line);
        }
        p.sendMessage("");
        p.sendMessage(msg.get("price_history.footer"));
    }

    // B6.2 — Tab Completion para /market
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("local");
            completions.add("historico");
            completions.add("preco");
            if (sender.hasPermission("gorvax.admin")) {
                completions.add("reload");
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("preco")) {
            // Sugerir nomes dos itens do mercado
            MarketManager mm = plugin.getMarketManager();
            if (mm != null) {
                for (MarketManager.MarketCategory cat : mm.getCategories().values()) {
                    for (MarketManager.MarketItem item : cat.items) {
                        completions.add(item.id);
                    }
                }
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
