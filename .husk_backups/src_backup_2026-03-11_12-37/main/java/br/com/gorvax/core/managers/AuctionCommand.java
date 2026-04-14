package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * B14.1 — Comando /leilao com subcomandos.
 * Subcomandos: iniciar, lance, listar, coletar, cancelar
 */
public class AuctionCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public AuctionCommand(GorvaxCore plugin) {
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
        AuctionManager auctionManager = plugin.getAuctionManager();

        if (auctionManager == null || !auctionManager.isEnabled()) {
            msg.send(p, "auction.disabled");
            return true;
        }

        if (args.length == 0) {
            // Sem subcomando: abrir GUI de leilões
            AuctionGUI.openAuctionList(plugin, p, 1);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "iniciar", "start", "criar" -> handleCreate(p, args);
            case "lance", "bid" -> handleBid(p, args);
            case "listar", "list" -> AuctionGUI.openAuctionList(plugin, p, 1);
            case "coletar", "collect" -> auctionManager.collectPending(p);
            case "cancelar", "cancel" -> handleCancel(p, args);
            case "help", "ajuda" -> showHelp(p);
            default -> {
                msg.send(p, "auction.unknown_command");
                showHelp(p);
            }
        }

        return true;
    }

    private void handleCreate(Player p, String[] args) {
        var msg = plugin.getMessageManager();

        if (args.length < 2) {
            msg.send(p, "auction.usage_create");
            return;
        }

        // Parse preço mínimo
        double startPrice;
        try {
            startPrice = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            msg.send(p, "auction.invalid_price");
            return;
        }

        if (startPrice <= 0) {
            msg.send(p, "auction.invalid_price");
            return;
        }

        // Parse duração (opcional, em segundos)
        int duration = 0; // 0 = usar default
        if (args.length >= 3) {
            try {
                duration = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                msg.send(p, "auction.invalid_duration");
                return;
            }
            if (duration <= 0) {
                msg.send(p, "auction.invalid_duration");
                return;
            }
        }

        // Verificar item na mão
        ItemStack held = p.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR || held.getAmount() == 0) {
            msg.send(p, "auction.hold_item");
            return;
        }

        plugin.getAuctionManager().createAuction(p, held, startPrice, duration);
    }

    private void handleBid(Player p, String[] args) {
        var msg = plugin.getMessageManager();

        if (args.length < 2) {
            msg.send(p, "auction.usage_bid");
            return;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            msg.send(p, "auction.invalid_price");
            return;
        }

        if (bidAmount <= 0) {
            msg.send(p, "auction.invalid_price");
            return;
        }

        // Se há ID especificado
        if (args.length >= 3) {
            String auctionId = args[2].toUpperCase();
            plugin.getAuctionManager().placeBid(p, auctionId, bidAmount);
        } else {
            // Sem ID — tentar dar lance no leilão mais recente/com menos tempo
            plugin.getAuctionManager().placeBidOnLatest(p, bidAmount);
        }
    }

    private void handleCancel(Player p, String[] args) {
        var msg = plugin.getMessageManager();

        if (args.length < 2) {
            msg.send(p, "auction.usage_cancel");
            return;
        }

        String auctionId = args[1].toUpperCase();
        plugin.getAuctionManager().cancelAuction(p, auctionId);
    }

    private void showHelp(Player p) {
        var msg = plugin.getMessageManager();
        p.sendMessage(msg.get("auction.help_header"));
        p.sendMessage(msg.get("auction.help_create"));
        p.sendMessage(msg.get("auction.help_bid"));
        p.sendMessage(msg.get("auction.help_list"));
        p.sendMessage(msg.get("auction.help_collect"));
        p.sendMessage(msg.get("auction.help_cancel"));
        p.sendMessage(msg.get("auction.help_footer"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                       @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("iniciar");
            completions.add("lance");
            completions.add("listar");
            completions.add("coletar");
            completions.add("cancelar");
            completions.add("ajuda");
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("lance") || sub.equals("bid")) {
                completions.add("<valor>");
            } else if (sub.equals("iniciar") || sub.equals("start") || sub.equals("criar")) {
                completions.add("<precoMinimo>");
            } else if (sub.equals("cancelar") || sub.equals("cancel")) {
                // Listar IDs dos leilões do jogador
                if (sender instanceof Player p) {
                    AuctionManager am = plugin.getAuctionManager();
                    if (am != null) {
                        am.getActiveAuctions().stream()
                                .filter(a -> a.sellerUUID.equals(p.getUniqueId()))
                                .forEach(a -> completions.add(a.id));
                    }
                }
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("iniciar") || sub.equals("start") || sub.equals("criar")) {
                completions.add("[duracao_segundos]");
            } else if (sub.equals("lance") || sub.equals("bid")) {
                // Listar IDs de leilões ativos
                AuctionManager am = plugin.getAuctionManager();
                if (am != null) {
                    am.getActiveAuctions().forEach(a -> completions.add(a.id));
                }
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
