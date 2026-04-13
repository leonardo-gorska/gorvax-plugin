package br.com.gorvax.core.commands;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * B37 — Comando /tp-hub para abrir o Hub de Teleportes.
 * Alternativa à bússola física.
 */
public class TeleportHubCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public TeleportHubCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "general.player_only");
            return true;
        }

        if (plugin.getTeleportHubGUI() != null) {
            plugin.getTeleportHubGUI().open(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, String[] args) {
        return Collections.emptyList();
    }
}
