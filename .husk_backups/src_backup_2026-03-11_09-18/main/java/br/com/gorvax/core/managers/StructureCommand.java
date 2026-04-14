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
            sender.sendMessage("§c[Estruturas] Apenas jogadores podem criar estruturas.");
            return true;
        }
        if (!p.hasPermission("gorvax.admin")) {
            msg.send(p, "general.no_permission");
            return true;
        }
        // /estrutura criar <id> <"nome"> <tema> <raio>
        if (args.length < 5) {
            p.sendMessage("§c[Estruturas] Uso: /estrutura criar <id> <nome> <tema> <raio>");
            p.sendMessage("§7Exemplo: /estrutura criar reino_deserto §6Reino_do_Deserto §7deserto §7150");
            p.sendMessage("§7Use _ no lugar de espaços no nome (serão convertidos).");
            return true;
        }

        String id = args[1].toLowerCase();
        String nome = args[2].replace('_', ' ');
        String tema = args[3].toLowerCase();
        int raio;
        try {
            raio = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            p.sendMessage("§c[Estruturas] Raio inválido. Use um número inteiro (ex: 150).");
            return true;
        }

        if (raio < 10 || raio > 1000) {
            p.sendMessage("§c[Estruturas] Raio deve ser entre 10 e 1000 blocos.");
            return true;
        }

        StructureManager sm = plugin.getStructureManager();
        if (sm.get(id) != null) {
            p.sendMessage("§c[Estruturas] Já existe uma estrutura com o ID '§e" + id + "§c'.");
            return true;
        }

        boolean ok = sm.create(id, nome, tema, raio, p);
        if (ok) {
            Location loc = p.getLocation();
            p.sendMessage("§a§l[Estruturas] §aEstrutura criada com sucesso!");
            p.sendMessage(String.format("§7ID: §e%s §7| Nome: §f%s §7| Tema: §f%s", id, nome, tema));
            p.sendMessage(String.format("§7Centro: §f%.0f, %.0f, %.0f §7| Raio: §f%d blocos",
                    loc.getX(), loc.getY(), loc.getZ(), raio));
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            plugin.getLogger().info("[Estruturas] '" + id + "' criada por " + p.getName()
                    + " em " + loc.getWorld().getName() + " (" + (int) loc.getX() + ", " + (int) loc.getZ() + ")");
        } else {
            p.sendMessage("§c[Estruturas] Erro ao criar estrutura.");
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gorvax.admin")) {
            msg.send(sender, "general.no_permission");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§c[Estruturas] Uso: /estrutura deletar <id>");
            return true;
        }

        String id = args[1].toLowerCase();
        StructureManager sm = plugin.getStructureManager();
        StructureManager.StructureData data = sm.get(id);
        if (data == null) {
            sender.sendMessage("§c[Estruturas] Estrutura '§e" + id + "§c' não encontrada.");
            return true;
        }

        sm.delete(id);
        sender.sendMessage("§a[Estruturas] Estrutura '§e" + id + "§a' deletada com sucesso.");
        plugin.getLogger().info("[Estruturas] '" + id + "' deletada por " + sender.getName());
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        StructureManager sm = plugin.getStructureManager();
        var all = sm.getAll();

        if (all.isEmpty()) {
            sender.sendMessage("§e[Estruturas] Nenhuma estrutura registrada ainda.");
            sender.sendMessage("§7Use /estrutura criar para mapear uma build.");
            return true;
        }

        sender.sendMessage("§6§l══════ Estruturas do Mundo ══════");

        Location playerLoc = (sender instanceof Player p) ? p.getLocation() : null;

        for (StructureManager.StructureData s : all) {
            String dist = "";
            if (playerLoc != null) {
                double d = s.distanceTo(playerLoc);
                if (d < Double.MAX_VALUE) {
                    dist = " §7(§f" + (int) d + " blocos§7)";
                } else {
                    dist = " §7(outro mundo)";
                }
            }
            String temaIcon = getTemaIcon(s.tema());
            sender.sendMessage(String.format("  %s §e%s §7- §f%s%s", temaIcon, s.id(), s.nome(), dist));
        }

        sender.sendMessage("§6§l══════════════════════════");
        sender.sendMessage("§7Total: §f" + all.size() + " estrutura(s)");
        sender.sendMessage("§7Use §e/estrutura tp <id> §7para teleportar.");
        return true;
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§c[Estruturas] Apenas jogadores podem teleportar.");
            return true;
        }
        if (args.length < 2) {
            p.sendMessage("§c[Estruturas] Uso: /estrutura tp <id>");
            return true;
        }

        String id = args[1].toLowerCase();
        StructureManager sm = plugin.getStructureManager();
        StructureManager.StructureData data = sm.get(id);
        if (data == null) {
            p.sendMessage("§c[Estruturas] Estrutura '§e" + id + "§c' não encontrada.");
            return true;
        }

        Location loc = data.toLocation();
        if (loc == null) {
            p.sendMessage("§c[Estruturas] Mundo '§e" + data.mundo() + "§c' não está carregado.");
            return true;
        }

        p.teleport(loc);
        p.sendMessage("§a[Estruturas] Teleportado para §f" + data.nome() + "§a!");
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
            sender.sendMessage("§c[Estruturas] Uso: /estrutura info <id>");
            return true;
        }

        if (data == null) {
            sender.sendMessage("§e[Estruturas] Nenhuma estrutura encontrada nesta posição.");
            sender.sendMessage("§7Dica: use /estrutura info <id> para ver uma específica.");
            return true;
        }

        String temaIcon = getTemaIcon(data.tema());
        sender.sendMessage("§6§l══════ Informações da Estrutura ══════");
        sender.sendMessage("§7ID: §e" + data.id());
        sender.sendMessage("§7Nome: §f" + data.nome());
        sender.sendMessage("§7Tema: " + temaIcon + " §f" + data.tema());
        sender.sendMessage("§7Mundo: §f" + data.mundo());
        sender.sendMessage(String.format("§7Centro: §f%.0f, %.0f, %.0f", data.x(), data.y(), data.z()));
        sender.sendMessage("§7Raio: §f" + data.raio() + " blocos");
        sender.sendMessage("§7Criado por: §f" + data.criadoPor());
        sender.sendMessage("§7Data: §f" + data.criadoEm());
        sender.sendMessage("§6§l═══════════════════════════════");
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gorvax.admin")) {
            msg.send(sender, "general.no_permission");
            return true;
        }
        plugin.getStructureManager().reload();
        sender.sendMessage("§a[Estruturas] Configuração recarregada com sucesso.");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l══════ Estruturas ══════");
        sender.sendMessage("§e/estrutura lista §7— Lista todas as estruturas");
        sender.sendMessage("§e/estrutura tp <id> §7— Teleporta ao centro");
        sender.sendMessage("§e/estrutura info [id] §7— Informações da estrutura");
        if (sender.hasPermission("gorvax.admin")) {
            sender.sendMessage("§c/estrutura criar <id> <nome> <tema> <raio> §7— Cria");
            sender.sendMessage("§c/estrutura deletar <id> §7— Remove");
            sender.sendMessage("§c/estrutura reload §7— Recarrega YAML");
        }
        sender.sendMessage("§6§l════════════════════");
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
