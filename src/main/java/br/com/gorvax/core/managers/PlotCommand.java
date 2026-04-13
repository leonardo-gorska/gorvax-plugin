package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gerencia o comando /lote (/plot, /subplot, /terreno, /feudo) e subcomandos.
 * Extraído do antigo ClaimCommand no Batch B9.
 */
public class PlotCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;
    private final TrustCommand trustCommand;

    public PlotCommand(GorvaxCore plugin, TrustCommand trustCommand) {
        this.plugin = plugin;
        this.trustCommand = trustCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;

        if (args.length == 0) {
            handlePlotInfo(p);
            return true;
        }

        String sub = args[0].toLowerCase();
        var msg = plugin.getMessageManager();
        switch (sub) {
            case "criar":
                p.sendMessage(
                        msg.get("plot.criar_hint"));
                break;
            case "comprar":
                handlePlotBuy(p);
                break;
            case "abandonar":
            case "sair":
                handlePlotLeave(p);
                break;
            case "preco":
            case "valor":
                if (args.length < 2) {
                    msg.send(p, "plot.price_usage");
                    return true;
                }
                handlePlotSetPrice(p, args[1]);
                break;
            case "info":
                handlePlotInfo(p);
                break;
            case "retomar":
                handlePlotRevoke(p);
                break;
            case "amigo":
            case "trust":
                if (args.length < 2) {
                    msg.send(p, "plot.friend_usage");
                    return true;
                }
                // Delega ao TrustCommand
                trustCommand.handleTrust(p, args[1], Claim.TrustType.GERAL, true);
                break;
            case "expandir":
            case "ajustar":
                msg.send(p, "plot.expand_hint");
                break;
            case "alugar":
            case "arrendar":
                handlePlotRent(p);
                break;
            case "aluguel":
            case "setrent":
                if (args.length < 2) {
                    msg.send(p, "plot.rent_usage");
                    return true;
                }
                handlePlotSetRent(p, args[1]);
                break;
            default:
                msg.send(p, "plot.unknown_command");
                break;
        }
        return true;
    }

    // --- Métodos de Feudo ---

    private void handlePlotInfo(Player p) {
        Claim claim = plugin.getClaimManager().getClaimAt(p.getLocation());
        if (claim == null) {
            plugin.getMessageManager().send(p, "plot.error_not_in_territory");
            return;
        }

        SubPlot plot = claim.getSubPlotAt(p.getLocation());

        p.sendMessage(" ");
        p.sendMessage(plugin.getMessageManager().get("plot.info_header"));
        if (plot != null) {
            // Informações do SubPlot
            p.sendMessage(plugin.getMessageManager().get("plot.info_type_lot"));
            if (plot.getRenter() != null) {
                p.sendMessage(plugin.getMessageManager().get("plot.info_rented"));
                p.sendMessage(
                        plugin.getMessageManager().get("plot.info_renter", plugin.getPlayerName(plot.getRenter())));
                long diff = plot.getRentExpire() - System.currentTimeMillis();
                if (diff > 0) {
                    long horas = diff / (1000 * 60 * 60);
                    p.sendMessage(plugin.getMessageManager().get("plot.info_expires", horas));
                } else {
                    p.sendMessage(plugin.getMessageManager().get("plot.info_processing"));
                }
            } else if (plot.getOwner() != null) {
                p.sendMessage(plugin.getMessageManager().get("plot.info_owner", plugin.getPlayerName(plot.getOwner())));
            } else {
                p.sendMessage(plugin.getMessageManager().get("plot.info_available"));
            }

            if (plot.isForSale()) {
                p.sendMessage(plugin.getMessageManager().get("plot.info_sale_price", plot.getPrice()));
                p.sendMessage(plugin.getMessageManager().get("plot.info_sale_hint"));
            }
            if (plot.isForRent()) {
                p.sendMessage(plugin.getMessageManager().get("plot.info_rent_price", plot.getRentPrice()));
                if (plot.getRenter() == null)
                    p.sendMessage(plugin.getMessageManager().get("plot.info_rent_hint"));
            }
            int num = plugin.getKingdomManager().getNumeroLote(claim.getId(), plot.getId());
            p.sendMessage(plugin.getMessageManager().get("plot.info_id", num));
        } else {
            // Informações do Reino
            p.sendMessage(plugin.getMessageManager().get("plot.info_type_kingdom"));
            p.sendMessage(plugin.getMessageManager().get("plot.info_king", plugin.getPlayerName(claim.getOwner())));
            p.sendMessage(plugin.getMessageManager().get("plot.info_kingdom_name",
                    claim.getKingdomName() != null ? claim.getKingdomName() : "Sem Nome"));
        }
        p.sendMessage(" ");
    }

    private void handlePlotSetPrice(Player p, String priceStr) {
        var msg = plugin.getMessageManager();
        Claim claim = plugin.getClaimManager().getClaimAt(p.getLocation());
        if (claim == null) {
            msg.send(p, "plot.error_not_in_territory");
            return;
        }

        SubPlot plot = claim.getSubPlotAt(p.getLocation());
        if (plot == null) {
            msg.send(p, "plot.error_not_in_subplot");
            return;
        }

        if (!claim.getOwner().equals(p.getUniqueId()) && !claim.hasPermission(p.getUniqueId(), Claim.TrustType.VICE)) {
            msg.send(p, "plot.error_no_permission");
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            plot.setPrice(price);
            plot.setForSale(price > 0);
            if (price > 0)
                plot.setForRent(false);

            // Sync with KingdomManager
            plugin.getKingdomManager().setPrecoLote(claim.getId(), plot.getId(), price);

            plugin.getClaimManager().saveClaims();

            if (price > 0)
                msg.send(p, "plot.sell_set", price);
            else
                msg.send(p, "plot.sell_removed");

        } catch (NumberFormatException e) {
            msg.send(p, "general.invalid_value");
        }
    }

    private void handlePlotSetRent(Player p, String priceStr) {
        var msg = plugin.getMessageManager();
        Claim claim = plugin.getClaimManager().getClaimAt(p.getLocation());
        if (claim == null) {
            msg.send(p, "plot.error_not_in_territory");
            return;
        }

        SubPlot plot = claim.getSubPlotAt(p.getLocation());
        if (plot == null) {
            msg.send(p, "plot.error_not_in_subplot");
            return;
        }

        if (!claim.getOwner().equals(p.getUniqueId()) && !claim.hasPermission(p.getUniqueId(), Claim.TrustType.VICE)) {
            msg.send(p, "plot.error_no_permission");
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            if (price < 0)
                throw new NumberFormatException();

            plot.setRentPrice(price);
            plot.setForRent(price > 0);
            if (price > 0)
                plot.setForSale(false);

            plugin.getClaimManager().saveClaims();

            if (price > 0)
                msg.send(p, "plot.rent_set", price);
            else
                msg.send(p, "plot.sell_removed");

        } catch (NumberFormatException e) {
            msg.send(p, "general.invalid_value");
        }
    }

    private void handlePlotRent(Player renter) {
        var msg = plugin.getMessageManager();
        Claim claim = plugin.getClaimManager().getClaimAt(renter.getLocation());
        if (claim == null)
            return;

        SubPlot plot = claim.getSubPlotAt(renter.getLocation());
        if (plot == null || !plot.isForRent()) {
            msg.send(renter, "plot.error_not_for_rent");
            return;
        }

        if (plot.getRenter() != null) {
            msg.send(renter, "plot.error_already_rented", plugin.getPlayerName(plot.getRenter()));
            return;
        }

        if (plot.getOwner() != null) {
            msg.send(renter, "plot.error_already_owned");
            return;
        }

        double price = plot.getRentPrice();
        EconomyResponse r = GorvaxCore.getEconomy().withdrawPlayer(renter, price);

        if (!r.transactionSuccess()) {
            msg.send(renter, "plot.error_insufficient_funds", price);
            return;
        }

        OfflinePlayer king = Bukkit.getOfflinePlayer(claim.getOwner());
        GorvaxCore.getEconomy().depositPlayer(king, price);

        plot.setRenter(renter.getUniqueId());
        plot.setRentExpire(System.currentTimeMillis() + java.util.concurrent.TimeUnit.DAYS.toMillis(1));
        plot.setForSale(false);

        // Adiciona como súdito no reino
        plugin.getKingdomManager().addSudito(claim.getId(), renter.getUniqueId());

        plugin.getClaimManager().saveClaims();

        msg.send(renter, "plot.rent_success", price);
        renter.playSound(renter.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        renter.spawnParticle(Particle.HAPPY_VILLAGER, renter.getLocation(), 20, 0.5, 1, 0.5, 0.1);

        if (king.isOnline())
            msg.send(king.getPlayer(), "plot.rent_notify_owner", renter.getName());
    }

    private void handlePlotBuy(Player buyer) {
        var msg = plugin.getMessageManager();
        Claim claim = plugin.getClaimManager().getClaimAt(buyer.getLocation());
        if (claim == null)
            return;

        SubPlot plot = claim.getSubPlotAt(buyer.getLocation());
        if (plot == null || !plot.isForSale()) {
            msg.send(buyer, "plot.error_not_for_sale");
            return;
        }

        if (plot.getOwner() != null) {
            msg.send(buyer, "plot.error_already_owned");
            return;
        }

        double price = plot.getPrice();
        EconomyResponse r = GorvaxCore.getEconomy().withdrawPlayer(buyer, price);

        if (!r.transactionSuccess()) {
            msg.send(buyer, "plot.error_insufficient_funds", price);
            return;
        }

        OfflinePlayer king = Bukkit.getOfflinePlayer(claim.getOwner());
        GorvaxCore.getEconomy().depositPlayer(king, price);

        plot.setOwner(buyer.getUniqueId());
        plot.setForSale(false);
        plot.setPrice(0);

        plugin.getKingdomManager().setPrecoLote(claim.getId(), plot.getId(), 0);
        plugin.getKingdomManager().addSudito(claim.getId(), buyer.getUniqueId());

        plugin.getClaimManager().saveClaims();

        msg.send(buyer, "plot.buy_success", price);
        buyer.playSound(buyer.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        msg.sendTitle(buyer, "plot.buy_title", "plot.buy_subtitle", 10, 60, 20);

        if (king.isOnline())
            msg.send(king.getPlayer(), "plot.buy_notify_seller", buyer.getName(), price);
    }

    private void handlePlotLeave(Player p) {
        var msg = plugin.getMessageManager();
        Claim claim = plugin.getClaimManager().getClaimAt(p.getLocation());
        if (claim == null)
            return;
        SubPlot plot = claim.getSubPlotAt(p.getLocation());

        if (plot == null || plot.getOwner() == null || !plot.getOwner().equals(p.getUniqueId())) {
            msg.send(p, "plot.error_not_owner");
            return;
        }

        plot.setOwner(null);
        plugin.getKingdomManager().removeSudito(claim.getId(), p.getUniqueId());
        plugin.getClaimManager().saveClaims();

        msg.send(p, "plot.abandon_success");
    }

    private void handlePlotRevoke(Player p) {
        var msg = plugin.getMessageManager();
        Claim claim = plugin.getClaimManager().getClaimAt(p.getLocation());
        if (claim == null)
            return;
        SubPlot plot = claim.getSubPlotAt(p.getLocation());

        if (plot == null) {
            msg.send(p, "plot.error_not_in_subplot");
            return;
        }

        if (!claim.getOwner().equals(p.getUniqueId())) {
            msg.send(p, "plot.error_not_kingdom_owner");
            return;
        }

        if (plot.getOwner() == null && plot.getRenter() == null) {
            msg.send(p, "plot.already_crown");
            return;
        }

        java.util.UUID oldOwner = plot.getOwner();
        if (oldOwner != null)
            plugin.getKingdomManager().removeSudito(claim.getId(), oldOwner);

        java.util.UUID oldRenter = plot.getRenter();
        if (oldRenter != null)
            plugin.getKingdomManager().removeSudito(claim.getId(), oldRenter);

        plot.setOwner(null);
        plot.setRenter(null);
        plugin.getClaimManager().saveClaims();
        msg.send(p, "plot.confiscate_success");
    }

    // --- Tab Completion ---

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player))
            return completions;

        if (args.length == 1) {
            completions.addAll(
                    List.of("criar", "comprar", "abandonar", "preco", "info", "retomar", "amigo", "alugar", "aluguel"));
            return filterCompletions(completions, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("amigo") || args[0].equalsIgnoreCase("trust"))) {
            return filterOnlinePlayers(args[1]);
        }
        return completions;
    }

    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> filterOnlinePlayers(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
