package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * B10 — Comando administrativo para custom items.
 * /customitem give <jogador> <item_id> [quantidade]
 * /customitem giveset <jogador> <set_name>
 * /customitem list
 * /customitem sets
 */
public class CustomItemCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public CustomItemCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (!sender.hasPermission("gorvax.admin")) {
            msg.send(sender, "general.no_permission");
            return true;
        }

        if (args.length == 0) {
            msg.send(sender, "custom_items.usage");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "give", "dar" -> handleGive(sender, args, msg);
            case "giveset", "darset" -> handleGiveSet(sender, args, msg);
            case "list", "lista" -> handleList(sender, msg);
            case "sets" -> handleSets(sender);
            default -> msg.send(sender, "custom_items.usage");
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args, MessageManager msg) {
        // /customitem give <jogador> <item_id> [quantidade]
        if (args.length < 3) {
            msg.send(sender, "custom_items.usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            msg.send(sender, "custom_items.player_not_found");
            return;
        }

        String itemId = args[2].toLowerCase();
        CustomItemManager cim = plugin.getCustomItemManager();
        ItemStack item = cim.getItem(itemId);

        if (item == null) {
            msg.send(sender, "custom_items.item_not_found");
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1 || amount > 64)
                    amount = 1;
            } catch (NumberFormatException e) {
                amount = 1;
            }
        }

        item.setAmount(amount);

        // Dar ao jogador (se inventário cheio, dropa no chão)
        var leftover = target.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(remain -> target.getWorld().dropItemNaturally(target.getLocation(), remain));
        }

        String itemName = cim.getItemName(itemId);
        // Notificar admin
        sender.sendMessage(
                "§b§lGORVAX §8» §aItem " + itemName + " §a(x" + amount + ") dado a §e" + target.getName() + "§a!");

        // Notificar jogador
        target.sendMessage("§b§lGORVAX §8» §aVocê recebeu " + itemName + " §a(x" + amount + ")!");
    }

    private void handleGiveSet(CommandSender sender, String[] args, MessageManager msg) {
        // /customitem giveset <jogador> <set_name>
        if (args.length < 3) {
            sender.sendMessage("§b§lGORVAX §8» §cUso: /customitem giveset <jogador> <set_name>");
            sender.sendMessage("§7  Use §e/customitem sets §7para ver os sets disponíveis.");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            msg.send(sender, "custom_items.player_not_found");
            return;
        }

        String setName = args[2].toLowerCase();
        CustomItemManager cim = plugin.getCustomItemManager();
        List<String> setItems = cim.getItemsBySet(setName);

        if (setItems.isEmpty()) {
            sender.sendMessage("§b§lGORVAX §8» §cSet '" + setName
                    + "' não encontrado! Use §e/customitem sets §cpara ver os disponíveis.");
            return;
        }

        int given = 0;
        for (String itemId : setItems) {
            ItemStack item = cim.getItem(itemId);
            if (item == null)
                continue;

            var leftover = target.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(remain -> target.getWorld().dropItemNaturally(target.getLocation(), remain));
            }
            given++;
        }

        sender.sendMessage("§b§lGORVAX §8» §aSet §e" + setName.toUpperCase() + " §a(" + given + " itens) dado a §e"
                + target.getName() + "§a!");
        target.sendMessage("§b§lGORVAX §8» §aVocê recebeu o set completo §e" + setName.toUpperCase() + " §a(" + given
                + " itens)!");
    }

    private void handleList(CommandSender sender, MessageManager msg) {
        CustomItemManager cim = plugin.getCustomItemManager();
        List<String> ids = cim.getAllItemIds();

        if (ids.isEmpty()) {
            msg.send(sender, "custom_items.list_empty");
            return;
        }

        sender.sendMessage("§b§lGORVAX §8» §6§lItens Lendários Disponíveis §7(" + ids.size() + ")§6§l:");
        for (String id : ids) {
            String name = cim.getItemName(id);
            sender.sendMessage("  §7• §e" + id + " §7→ " + name);
        }
    }

    private void handleSets(CommandSender sender) {
        CustomItemManager cim = plugin.getCustomItemManager();
        List<String> sets = cim.getAllSetNames();

        if (sets.isEmpty()) {
            sender.sendMessage("§b§lGORVAX §8» §cNenhum set encontrado.");
            return;
        }

        sender.sendMessage("§b§lGORVAX §8» §6§lSets Disponíveis §7(" + sets.size() + ")§6§l:");
        for (String set : sets) {
            List<String> items = cim.getItemsBySet(set);
            sender.sendMessage("  §7• §eset_" + set + " §7→ §f" + items.size() + " itens");
        }
        sender.sendMessage("§7  Uso: §e/customitem giveset <jogador> <set_name>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("gorvax.admin"))
            return List.of();

        if (args.length == 1) {
            return filterStartsWith(List.of("give", "giveset", "list", "sets"), args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2
                && (sub.equals("give") || sub.equals("dar") || sub.equals("giveset") || sub.equals("darset"))) {
            // Nomes de jogadores online
            return filterStartsWith(
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()),
                    args[1]);
        }

        if (args.length == 3 && (sub.equals("give") || sub.equals("dar"))) {
            // IDs de itens
            return filterStartsWith(plugin.getCustomItemManager().getAllItemIds(), args[2]);
        }

        if (args.length == 3 && (sub.equals("giveset") || sub.equals("darset"))) {
            // Nomes de sets
            return filterStartsWith(plugin.getCustomItemManager().getAllSetNames(), args[2]);
        }

        return List.of();
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
