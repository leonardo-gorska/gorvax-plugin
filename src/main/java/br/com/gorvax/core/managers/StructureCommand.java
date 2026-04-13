package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * B22 — Comando /estrutura para gerenciar estruturas (reinos pré-construídos).
 * Subcomandos: criar, deletar, lista, tp, info, reload
 */
public class StructureCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;
    private final MessageManager msg;

    public StructureCommand(GorvaxCore plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        return switch (sub) {
            case "criar", "create" -> handleCreate(sender, args);
            case "deletar", "delete", "remover" -> handleDelete(sender, args);
            case "lista", "list", "listar" -> handleList(sender, args);
            case "tp", "teleport", "ir" -> handleTp(sender, args);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    // ========== Subcomandos ==========

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(msg.get("structure.player_only"));
            return true;
        }
        if (!p.hasPermission("gorvax.admin")) {
            msg.send(p, "general.no_permission");
            return true;
        }
        // /estrutura criar <id> <"nome"> <tema> <raio>
        if (args.length < 5) {
            p.sendMessage(msg.get("structure.usage_create"));
            p.sendMessage(msg.get("structure.usage_create_example"));
            p.sendMessage(msg.get("structure.usage_create_hint"));
            return true;
        }

        String id = args[1].toLowerCase();
        String nome = args[2].replace('_', ' ');
        String tema = args[3].toLowerCase();
        int raio;
        try {
            raio = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            p.sendMessage(msg.get("structure.invalid_radius"));
            return true;
        }

        if (raio < 10 || raio > 1000) {
            p.sendMessage(msg.get("structure.radius_out_of_range"));
            return true;
        }

        StructureManager sm = plugin.getStructureManager();
        if (sm.get(id) != null) {
            p.sendMessage(msg.get("structure.already_exists", id));
            return true;
        }

        boolean ok = sm.create(id, nome, tema, raio, p);
        if (ok) {
            Location loc = p.getLocation();
            p.sendMessage(msg.get("structure.created_success"));
            p.sendMessage(msg.get("structure.created_details", id, nome, tema));
            p.sendMessage(msg.get("structure.created_location",
                    String.format("%.0f", loc.getX()),
                    String.format("%.0f", loc.getY()),
                    String.format("%.0f", loc.getZ()),
                    raio));
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            plugin.getLogger().info("[Estruturas] '" + id + "' criada por " + p.getName()
                    + " em " + loc.getWorld().getName() + " (" + (int) loc.getX() + ", " + (int) loc.getZ() + ")");
        } else {
            p.sendMessage(msg.get("structure.create_error"));
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gorvax.admin")) {
            msg.send(sender, "general.no_permission");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(msg.get("structure.usage_delete"));
            return true;
        }

        String id = args[1].toLowerCase();
        StructureManager sm = plugin.getStructureManager();
        StructureManager.StructureData data = sm.get(id);
        if (data == null) {
            sender.sendMessage(msg.get("structure.not_found", id));
            return true;
        }

        sm.delete(id);
        sender.sendMessage(msg.get("structure.deleted", id));
        plugin.getLogger().info("[Estruturas] '" + id + "' deletada por " + sender.getName());
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        StructureManager sm = plugin.getStructureManager();
        var all = sm.getAll();

        if (all.isEmpty()) {
            sender.sendMessage(msg.get("structure.list_empty"));
            sender.sendMessage(msg.get("structure.list_empty_hint"));
            return true;
        }

        sender.sendMessage(msg.get("structure.list_header"));

        Location playerLoc = (sender instanceof Player p) ? p.getLocation() : null;

        for (StructureManager.StructureData s : all) {
            String dist = "";
            if (playerLoc != null) {
                double d = s.distanceTo(playerLoc);
                if (d < Double.MAX_VALUE) {
                    dist = msg.get("structure.list_dist", (int) d);
                } else {
                    dist = msg.get("structure.list_other_world");
                }
            }
            String temaIcon = getTemaIcon(s.tema());
            sender.sendMessage(msg.get("structure.list_entry", temaIcon, s.id(), s.nome(), dist));
        }

        sender.sendMessage(msg.get("structure.list_footer"));
        sender.sendMessage(msg.get("structure.list_total", all.size()));
        sender.sendMessage(msg.get("structure.list_hint"));
        return true;
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(msg.get("structure.player_only_tp"));
            return true;
        }
        if (args.length < 2) {
            p.sendMessage(msg.get("structure.usage_tp"));
            return true;
        }

        String id = args[1].toLowerCase();
        StructureManager sm = plugin.getStructureManager();
        StructureManager.StructureData data = sm.get(id);
        if (data == null) {
            p.sendMessage(msg.get("structure.not_found", id));
            return true;
        }

        Location loc = data.toLocation();
        if (loc == null) {
            p.sendMessage(msg.get("structure.world_not_loaded", data.mundo()));
            return true;
        }

        p.teleport(loc);
        p.sendMessage(msg.get("structure.tp_success", data.nome()));
        p.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        StructureManager sm = plugin.getStructureManager();
        StructureManager.StructureData data;

        if (args.length >= 2) {
            // /estrutura info <id>
            data = sm.get(args[1].toLowerCase());
        } else if (sender instanceof Player p) {
            // /estrutura info (sem args = mostra a que o jogador está dentro)
            data = sm.getStructureAt(p.getLocation());
        } else {
            sender.sendMessage(msg.get("structure.usage_info"));
            return true;
        }

        if (data == null) {
            sender.sendMessage(msg.get("structure.info_not_found"));
            sender.sendMessage(msg.get("structure.info_not_found_hint"));
            return true;
        }

        String temaIcon = getTemaIcon(data.tema());
        sender.sendMessage(msg.get("structure.info_header"));
        sender.sendMessage(msg.get("structure.info_id", data.id()));
        sender.sendMessage(msg.get("structure.info_name", data.nome()));
        sender.sendMessage(msg.get("structure.info_theme", temaIcon, data.tema()));
        sender.sendMessage(msg.get("structure.info_world", data.mundo()));
        sender.sendMessage(msg.get("structure.info_center",
                String.format("%.0f", data.x()),
                String.format("%.0f", data.y()),
                String.format("%.0f", data.z())));
        sender.sendMessage(msg.get("structure.info_radius", data.raio()));
        sender.sendMessage(msg.get("structure.info_creator", data.criadoPor()));
        sender.sendMessage(msg.get("structure.info_date", data.criadoEm()));
        sender.sendMessage(msg.get("structure.info_footer"));
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gorvax.admin")) {
            msg.send(sender, "general.no_permission");
            return true;
        }
        plugin.getStructureManager().reload();
        sender.sendMessage(msg.get("structure.reload_success"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg.get("structure.help_header"));
        sender.sendMessage(msg.get("structure.help_list"));
        sender.sendMessage(msg.get("structure.help_tp"));
        sender.sendMessage(msg.get("structure.help_info"));
        if (sender.hasPermission("gorvax.admin")) {
            sender.sendMessage(msg.get("structure.help_create"));
            sender.sendMessage(msg.get("structure.help_delete"));
            sender.sendMessage(msg.get("structure.help_reload"));
        }
        sender.sendMessage(msg.get("structure.help_footer"));
    }

    private String getTemaIcon(String tema) {
        return switch (tema) {
            case "deserto" -> "§6🏜️";
            case "gelo", "glacial", "neve" -> "§b❄️";
            case "nether", "fogo" -> "§c🔥";
            case "floresta", "selva" -> "§a🌿";
            case "medieval", "castelo" -> "§7🏰";
            case "porto", "oceano", "mar" -> "§9🌊";
            case "montanha" -> "§f⛰️";
            case "pantano", "swamp" -> "§2🍄";
            default -> "§e⚔️";
        };
    }

    // ========== Tab Complete ==========

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("lista", "tp", "info"));
            if (sender.hasPermission("gorvax.admin")) {
                subs.addAll(List.of("criar", "deletar", "reload"));
            }
            return filterPartial(subs, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("tp") || sub.equals("info") || sub.equals("deletar") || sub.equals("delete")) {
                return filterPartial(plugin.getStructureManager().getAllIds(), args[1]);
            }
            if (sub.equals("criar") || sub.equals("create")) {
                return List.of("<id>");
            }
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("criar") || args[0].equalsIgnoreCase("create"))) {
            return List.of("<nome_com_underscores>");
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("criar") || args[0].equalsIgnoreCase("create"))) {
            return filterPartial(
                    List.of("deserto", "gelo", "nether", "floresta", "medieval", "porto", "montanha", "pantano"),
                    args[3]);
        }
        if (args.length == 5 && (args[0].equalsIgnoreCase("criar") || args[0].equalsIgnoreCase("create"))) {
            return List.of("50", "100", "150", "200");
        }
        return Collections.emptyList();
    }

    private List<String> filterPartial(List<String> options, String partial) {
        if (partial == null || partial.isEmpty())
            return options;
        String lower = partial.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }
}
