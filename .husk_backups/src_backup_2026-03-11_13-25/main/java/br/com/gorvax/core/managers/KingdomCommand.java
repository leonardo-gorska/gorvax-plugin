package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.towns.commands.KingdomBankSubcommand;
import br.com.gorvax.core.towns.commands.KingdomDiplomacySubcommand;
import br.com.gorvax.core.towns.commands.KingdomMemberSubcommand;
import br.com.gorvax.core.towns.commands.KingdomOutpostSubcommand;
import br.com.gorvax.core.towns.commands.KingdomVoteSubcommand;
import br.com.gorvax.core.towns.commands.KingdomWarSubcommand;
import br.com.gorvax.core.towns.managers.KingdomManager;
import br.com.gorvax.core.towns.tasks.KingdomMaintenanceTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gerencia o comando /reino (/kingdom, /k, /cidade, /city, /town) e
 * subcomandos.
 * B20 — Refatorado como router fino: delega para subcomandos especializados.
 */
public class KingdomCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;
    private final RenameCommand renameCommand;

    // Subcomandos extraídos (B20)
    private final KingdomBankSubcommand bankSub;
    private final KingdomDiplomacySubcommand diplomacySub;
    private final KingdomMemberSubcommand memberSub;
    private final KingdomOutpostSubcommand outpostSub;
    private final KingdomWarSubcommand warSub;
    private final KingdomVoteSubcommand voteSub;

    // B1.4 — Cooldown de visita
    private final Map<UUID, Long> visitCooldowns = new ConcurrentHashMap<>();
    // B1.4 — Tarefa de warmup ativa (para cancelar ao mover)
    private final Map<UUID, BukkitTask> visitWarmups = new ConcurrentHashMap<>();

    public KingdomCommand(GorvaxCore plugin, RenameCommand renameCommand) {
        this.plugin = plugin;
        this.renameCommand = renameCommand;

        // Inicializar subcomandos
        this.bankSub = new KingdomBankSubcommand(plugin);
        this.diplomacySub = new KingdomDiplomacySubcommand(plugin);
        this.memberSub = new KingdomMemberSubcommand(plugin);
        this.outpostSub = new KingdomOutpostSubcommand(plugin);
        this.warSub = new KingdomWarSubcommand(plugin);
        this.voteSub = new KingdomVoteSubcommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // B35 — Subcomandos admin que funcionam pelo console
        if (args.length >= 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("darblocos") || sub.equals("giveblocks")) {
                handleGiveBlocksConsole(sender, args);
                return true;
            }
        }

        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;

        // Sem argumentos: abre menu principal do reino
        if (args.length == 0) {
            plugin.getKingdomInventory().openMainMenu(p);
            return true;
        }

        String sub = args[0].toLowerCase();
        var msg = plugin.getMessageManager();

        switch (sub) {
            // --- Administração ---
            case "reload":
                if (!p.hasPermission("gorvax.admin")) {
                    msg.send(p, "kingdom.error_no_permission");
                    return true;
                }
                plugin.getClaimManager().reload();
                plugin.getKingdomManager().reload();
                msg.send(p, "kingdom.reload_success");
                break;

            // --- Reino Básico ---
            case "lista":
            case "list":
            case "claims":
            case "terrenos":
                handleKingdomList(p);
                break;

            case "nome":
                if (args.length < 2) {
                    msg.send(p, "kingdom.name_usage");
                    return true;
                }
                StringBuilder nomeComp = new StringBuilder();
                for (int i = 1; i < args.length; i++)
                    nomeComp.append(args[i]).append(" ");
                renameCommand.handleKingdomRename(p, nomeComp.toString().trim());
                break;

            case "criar":
                msg.send(p, "kingdom.criar_hint");
                break;

            case "spawn":
                handleKingdomSpawn(p);
                break;

            case "setspawn":
                handleKingdomSetSpawn(p);
                break;

            case "deletar":
            case "excluir":
                if (args.length >= 2 && args[1].equalsIgnoreCase("confirmar")) {
                    handleKingdomDelete(p, true);
                } else {
                    handleKingdomDelete(p, false);
                }
                break;

            case "debugxp":
                handleDebugXp(p);
                break;

            case "adm-manutencao":
                if (p.hasPermission("gorvax.admin")) {
                    new KingdomMaintenanceTask(plugin).run();
                    msg.send(p, "kingdom.admin_maintenance");
                }
                break;

            case "pvp":
                handleKingdomPvp(p, args);
                break;

            case "darblocos":
            case "giveblocks":
                handleGiveBlocks(p, args);
                break;

            // B1.4 — Visitar reino
            case "visitar":
                if (args.length < 2) {
                    msg.send(p, "kingdom.visit_usage");
                    return true;
                }
                StringBuilder nomeReino = new StringBuilder();
                for (int i = 1; i < args.length; i++)
                    nomeReino.append(args[i]).append(" ");
                handleKingdomVisit(p, nomeReino.toString().trim());
                break;

            // --- Membros/Convites (delegado) ---
            case "convidar":
            case "aceitar":
            case "recusar":
            case "membros":
            case "suditos":
            case "transferir":
            case "sucessor":
                memberSub.handle(p, sub, args);
                break;

            // --- Banco (delegado) ---
            case "depositar":
            case "deposit":
            case "sacar":
            case "withdraw":
            case "banco":
            case "bank":
                bankSub.handle(p, sub, args);
                break;

            // --- Diplomacia (delegado) ---
            case "alianca":
            case "aliança":
            case "alliance":
            case "aceitaralianca":
            case "acceptalliance":
            case "recusaralianca":
            case "denyalliance":
            case "inimigo":
            case "enemy":
            case "neutro":
            case "neutral":
                diplomacySub.handle(p, sub, args);
                break;

            // --- Outposts (delegado) ---
            case "outpost":
            case "posto":
                outpostSub.handle(p, args);
                break;

            // --- Guerra (delegado) ---
            case "guerra":
            case "war":
                warSub.handle(p, args);
                break;

            // --- Votação (delegado) ---
            case "votar":
            case "vote":
                voteSub.handle(p, args);
                break;

            default:
                msg.send(p, "kingdom.unknown_command");
                break;
        }

        return true;
    }

    // --- Métodos de Reino Básico (mantidos no router) ---

    private void handleKingdomSpawn(Player p) {
        var msg = plugin.getMessageManager();
        Claim kingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        if (kingdom == null) {
            msg.send(p, "kingdom.spawn_no_kingdom");
            return;
        }

        Location spawn = plugin.getKingdomManager().getSpawn(kingdom.getId());
        if (spawn == null) {
            msg.send(p, "kingdom.spawn_no_spawn");
            return;
        }

        p.teleport(spawn);
        msg.send(p, "kingdom.spawn_teleport");
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    private void handleKingdomSetSpawn(Player p) {
        var msg = plugin.getMessageManager();
        Claim claim = plugin.getClaimManager().getClaimAt(p.getLocation());
        if (claim == null || !claim.getOwner().equals(p.getUniqueId())) {
            msg.send(p, "kingdom.setspawn_not_in_claim");
            return;
        }
        plugin.getKingdomManager().setSpawn(claim.getId(), p.getLocation());
        msg.send(p, "kingdom.setspawn_success");
    }

    private void handleKingdomDelete(Player p, boolean confirm) {
        var msg = plugin.getMessageManager();
        Claim kingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        if (kingdom == null || !kingdom.getOwner().equals(p.getUniqueId())) {
            msg.send(p, "kingdom.delete_not_king");
            return;
        }

        if (!confirm) {
            msg.send(p, "kingdom.delete_confirm");
            msg.send(p, "kingdom.delete_confirm_hint");
            return;
        }

        // B10 — Log de auditoria
        if (plugin.getAuditManager() != null) {
            plugin.getAuditManager().log(
                    AuditManager.AuditAction.KINGDOM_DELETE,
                    p.getUniqueId(), p.getName(),
                    "Reino '" + kingdom.getKingdomName() + "' deletado");
        }

        plugin.getKingdomManager().deleteKingdom(kingdom.getId());
        msg.send(p, "kingdom.delete_refund");
        msg.send(p, "kingdom.delete_success");
        msg.sendTitle(p, "kingdom.delete_title", "kingdom.delete_subtitle", 10, 70, 20);
        msg.broadcast("kingdom.delete_broadcast", kingdom.getKingdomName());
    }

    private void handleKingdomList(Player p) {
        var msg = plugin.getMessageManager();
        Claim kingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        if (kingdom == null) {
            msg.send(p, "kingdom.error_no_kingdom");
            return;
        }
        msg.send(p, "kingdom.list_header");
        msg.send(p, "kingdom.list_title");
        msg.send(p, "kingdom.list_name", kingdom.getKingdomName() != null ? kingdom.getKingdomName() : "Sem nome");
        msg.send(p, "kingdom.list_king", plugin.getPlayerName(kingdom.getOwner()));
        msg.send(p, "kingdom.list_members", plugin.getKingdomManager().getSuditosCount(kingdom.getId()));
        msg.send(p, "kingdom.list_area", kingdom.getArea());
        msg.send(p, "kingdom.list_header");
    }

    private void handleDebugXp(Player p) {
        Claim c = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        if (c == null)
            return;
        long time = plugin.getKingdomManager().getData()
                .getLong("reino." + c.getId() + ".suditos_atividade." + p.getUniqueId(), 0);
        p.sendMessage("Debug Time: " + time);
    }

    private void handleKingdomPvp(Player p, String[] args) {
        var msg = plugin.getMessageManager();
        if (args.length < 2) {
            msg.send(p, "kingdom.pvp_usage");
            return;
        }
        Claim k = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        if (k == null) {
            // Fallback: Check if standing in a kingdom as OP
            if (p.isOp()) {
                k = plugin.getClaimManager().getClaimAt(p.getLocation());
            }
        }

        boolean isStaff = p.isOp() || p.hasPermission("gorvax.admin");
        boolean isOwner = k != null && k.getOwner().equals(p.getUniqueId());

        if (k == null || (!isOwner && !isStaff)) {
            msg.send(p, "kingdom.pvp_no_permission");
            return;
        }

        if (isStaff && !isOwner) {
            msg.send(p, "kingdom.pvp_staff_warning");
        }
        boolean state = args[1].equalsIgnoreCase("on");
        k.setPvp(state);
        plugin.getClaimManager().saveClaims();
        msg.send(p, "kingdom.pvp_state", state ? "ATIVADO" : "DESATIVADO");
    }

    private void handleGiveBlocks(Player p, String[] args) {
        handleGiveBlocksConsole(p, args);
    }

    /**
     * B35 — Versão console-compatível de darblocos.
     * Aceita CommandSender para funcionar tanto do console quanto in-game.
     */
    private void handleGiveBlocksConsole(CommandSender sender, String[] args) {
        var msg = plugin.getMessageManager();
        if (sender instanceof Player p && !p.hasPermission("gorvax.admin")) {
            msg.send(p, "general.no_permission");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUso: /reino darblocos <jogador> <quantidade>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJogador '" + args[1] + "' não está online.");
            return;
        }
        try {
            int amount = Integer.parseInt(args[2]);
            PlayerData pd = plugin.getPlayerDataManager().getData(target.getUniqueId());
            pd.addClaimBlocks(amount);
            plugin.getPlayerDataManager().saveData(target.getUniqueId());

            sender.sendMessage("§a[GorvaxCore] Adicionados " + amount + " blocos para " + target.getName() + ".");
            msg.send(target, "kingdom.givblocks_received", amount);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cQuantidade inválida: " + args[2]);
        }
    }

    // --- B1.4: Visitar Reino (mantido no router — usa estado local de
    // cooldown/warmup) ---

    private void handleKingdomVisit(Player p, String kingdomName) {
        var msg = plugin.getMessageManager();
        String kingdomId = plugin.getKingdomManager().tryFindKingdomIdByName(kingdomName);
        if (kingdomId == null) {
            msg.send(p, "kingdom.visit_not_found", kingdomName);
            return;
        }

        Claim kingdom = plugin.getClaimManager().getClaimById(kingdomId);
        if (kingdom == null) {
            msg.send(p, "kingdom.visit_no_claim");
            return;
        }

        // Verificar se o reino é público
        if (!kingdom.isPublic()) {
            msg.send(p, "kingdom.visit_private");
            return;
        }

        Location spawn = plugin.getKingdomManager().getSpawn(kingdomId);
        if (spawn == null) {
            msg.send(p, "kingdom.visit_no_spawn");
            return;
        }

        // Verificar cooldown
        int cooldownSeconds = plugin.getConfig().getInt("kingdoms.visit_cooldown", 60);
        Long lastVisit = visitCooldowns.get(p.getUniqueId());
        if (lastVisit != null) {
            long remaining = cooldownSeconds - (System.currentTimeMillis() - lastVisit) / 1000;
            if (remaining > 0) {
                msg.send(p, "kingdom.visit_cooldown", remaining);
                return;
            }
        }

        // Cobrar custo
        double cost = plugin.getConfig().getDouble("kingdoms.visit_cost", 500.0);
        Economy econ = GorvaxCore.getEconomy();
        if (cost > 0 && econ != null) {
            if (!econ.has(p, cost)) {
                msg.send(p, "kingdom.visit_no_money", String.format("%.0f", cost));
                return;
            }
            econ.withdrawPlayer(p, cost);
            msg.send(p, "kingdom.visit_cost", String.format("%.0f", cost));
        }

        // Warmup
        int warmupSeconds = plugin.getConfig().getInt("kingdoms.visit_warmup", 5);
        if (warmupSeconds > 0) {
            Location startLoc = p.getLocation().clone();
            msg.send(p, "kingdom.visit_warmup", warmupSeconds);

            BukkitTask task = new BukkitRunnable() {
                int ticks = warmupSeconds * 20;

                @Override
                public void run() {
                    if (!p.isOnline()) {
                        visitWarmups.remove(p.getUniqueId());
                        cancel();
                        return;
                    }
                    // Cancelar se moveu mais de 1 bloco
                    if (p.getLocation().distanceSquared(startLoc) > 1.0) {
                        msg.send(p, "kingdom.visit_cancelled");
                        // Reembolso
                        if (cost > 0 && econ != null) {
                            econ.depositPlayer(p, cost);
                            msg.send(p, "kingdom.visit_refund", String.format("%.0f", cost));
                        }
                        visitWarmups.remove(p.getUniqueId());
                        cancel();
                        return;
                    }
                    ticks--;
                    if (ticks <= 0) {
                        p.teleport(spawn);
                        msg.send(p, "kingdom.visit_success", kingdomName);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        msg.sendTitle(p, "kingdom.visit_title", "kingdom.visit_subtitle", 10, 60, 20, kingdomName);
                        visitCooldowns.put(p.getUniqueId(), System.currentTimeMillis());
                        visitWarmups.remove(p.getUniqueId());
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
            visitWarmups.put(p.getUniqueId(), task);
        } else {
            // Sem warmup
            p.teleport(spawn);
            msg.send(p, "kingdom.visit_success", kingdomName);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            msg.sendTitle(p, "kingdom.visit_title", "kingdom.visit_subtitle", 10, 60, 20, kingdomName);
            visitCooldowns.put(p.getUniqueId(), System.currentTimeMillis());
        }
    }

    // --- Tab Completion ---

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player))
            return completions;

        if (args.length == 1) {
            completions.addAll(List.of("criar", "nome", "spawn", "setspawn", "membros", "deletar", "transferir", "pvp",
                    "lista", "convidar", "aceitar", "recusar", "visitar", "depositar", "sacar", "banco", "alianca",
                    "aceitaralianca", "recusaralianca", "inimigo", "neutro", "outpost", "guerra", "votar"));
            if (sender.hasPermission("gorvax.admin")) {
                completions.addAll(List.of("reload", "darblocos"));
            }
            return filterCompletions(completions, args[0]);
        }

        if (args.length >= 2) {
            String s = args[0].toLowerCase();

            // Delegados
            switch (s) {
                case "transferir":
                case "sucessor":
                case "convidar":
                    return memberSub.tabComplete(s, args);
                case "alianca":
                case "aliança":
                case "alliance":
                case "inimigo":
                case "enemy":
                case "neutro":
                case "neutral":
                    return diplomacySub.tabComplete(s, args);
                case "outpost":
                case "posto":
                    return outpostSub.tabComplete(args);
                case "guerra":
                case "war":
                    return warSub.tabComplete(args);
                case "votar":
                case "vote":
                    return voteSub.tabComplete(args);
            }

            // Mantidos no router
            if (args.length == 2) {
                switch (s) {
                    case "visitar":
                        return filterCompletions(plugin.getKingdomManager().getAllKingdomNames(), args[1]);
                    case "deletar":
                    case "excluir":
                        completions.add("confirmar");
                        return filterCompletions(completions, args[1]);
                    case "pvp":
                        completions.addAll(List.of("global"));
                        return filterCompletions(completions, args[1]);
                    case "darblocos":
                    case "giveblocks":
                        if (sender.hasPermission("gorvax.admin"))
                            return filterOnlinePlayers(args[1]);
                        break;
                }
            }

            if (args.length == 3 && (args[0].equalsIgnoreCase("pvp")) && args[1].equalsIgnoreCase("global")) {
                completions.addAll(List.of("on", "off"));
                return filterCompletions(completions, args[2]);
            }
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
