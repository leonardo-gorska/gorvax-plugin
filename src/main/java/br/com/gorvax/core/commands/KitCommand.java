package br.com.gorvax.core.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.RankManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Comando /kit — Abre GUI de seleção de kits por rank.
 */
public class KitCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public KitCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var msg = plugin.getMessageManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.get("kit.player_only"));
            return true;
        }

        RankManager rm = plugin.getRankManager();
        if (rm == null || !rm.isEnabled()) {
            player.sendMessage(msg.get("kit.disabled"));
            return true;
        }

        rm.openKitGUI(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
