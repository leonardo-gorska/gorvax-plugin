package br.com.gorvax.core.boss.miniboss;

import br.com.gorvax.core.GorvaxCore;
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
 * Comando /miniboss para administração de mini-bosses (B11).
 * Subcomandos: spawn, list, kill, reload
 */
public class MiniBossCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public MiniBossCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String[] args) {
        var msg = plugin.getMessageManager();
        MiniBossManager manager = plugin.getMiniBossManager();

        if (manager == null) {
            sender.sendMessage("§c[MiniBoss] Sistema de mini-bosses não inicializado.");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "spawn" -> {
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                if (!(sender instanceof Player p)) {
                    msg.send(sender, "general.player_only");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§c§lUso: §f/miniboss spawn <id>");
                    sender.sendMessage("§7IDs disponíveis: §f" +
                            String.join(", ", manager.getConfiguredBosses().keySet()));
                    return true;
                }
                String bossId = args[1].toLowerCase();
                MiniBoss spawned = manager.spawnMiniBoss(bossId, p.getLocation());
                if (spawned != null) {
                    msg.send(sender, "miniboss.spawn_success", spawned.getName());
                } else {
                    sender.sendMessage("§c§lERRO §8» §7Mini-boss não encontrado ou limite atingido.");
                    sender.sendMessage("§7IDs disponíveis: §f" +
                            String.join(", ", manager.getConfiguredBosses().keySet()));
                }
            }

            case "list" -> {
                var active = manager.getActiveMiniBosses();
                if (active.isEmpty()) {
                    msg.send(sender, "miniboss.list_empty");
                } else {
                    msg.send(sender, "miniboss.list_header");
                    for (MiniBoss boss : active.values()) {
                        if (boss.isAlive()) {
                            var loc = boss.getEntity().getLocation();
                            sender.sendMessage(String.format(
                                    "§8- %s §f(HP: §c%.0f§f/§c%.0f§f) §7em %s §e%d, %d, %d",
                                    boss.getName(),
                                    boss.getEntity().getHealth(),
                                    boss.getMaxHealth(),
                                    loc.getWorld().getName(),
                                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()
                            ));
                        }
                    }
                }
            }

            case "kill" -> {
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                int count = manager.removeAll();
                msg.send(sender, "miniboss.kill_success", count);
            }

            case "reload" -> {
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                manager.reload();
                msg.send(sender, "miniboss.reload_success");
            }

            default -> showHelp(sender);
        }

        return true;
    }

    /**
     * Mostra a ajuda do comando.
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6§l⚔ MINI-BOSSES §7- Comandos:");
        sender.sendMessage("§e/miniboss list §7- Lista mini-bosses ativos.");
        if (sender.hasPermission("gorvax.admin")) {
            sender.sendMessage("§e/miniboss spawn <id> §7- Spawna um mini-boss.");
            sender.sendMessage("§e/miniboss kill §7- Remove todos os mini-bosses.");
            sender.sendMessage("§e/miniboss reload §7- Recarrega configurações.");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("list");
            if (sender.hasPermission("gorvax.admin")) {
                completions.add("spawn");
                completions.add("kill");
                completions.add("reload");
            }
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")
                && sender.hasPermission("gorvax.admin")) {
            MiniBossManager manager = plugin.getMiniBossManager();
            if (manager != null) {
                completions.addAll(manager.getConfiguredBosses().keySet());
            }
            return filterCompletions(completions, args[1]);
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
