package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gerencia os comandos /permitir e /remover (trust e untrust).
 * Extraído do antigo ClaimCommand no Batch B9.
 */
public class TrustCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public TrustCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;

        if (label.equalsIgnoreCase("permitir") || label.equalsIgnoreCase("trust")) {
            if (args.length < 1) {
                var msg = plugin.getMessageManager();
                msg.send(p, "trust.usage_permit");
                return true;
            }
            Claim.TrustType type = Claim.TrustType.GERAL;
            if (args.length >= 2) {
                try {
                    String typeStr = args[1].toUpperCase();
                    if (typeStr.equals("CONSTRUIR") || typeStr.equals("BUILD"))
                        typeStr = "CONSTRUCAO";
                    if (typeStr.equals("ALL") || typeStr.equals("TUDO"))
                        typeStr = "GERAL";

                    type = Claim.TrustType.valueOf(typeStr);
                } catch (Exception e) {
                    p.sendMessage(
                            plugin.getMessageManager().get("trust.error_invalid_category"));
                    return true;
                }
            }
            return handleTrust(p, args[0], type, true);
        }

        if (label.equalsIgnoreCase("remover") || label.equalsIgnoreCase("untrust")) {
            if (args.length < 1) {
                plugin.getMessageManager().send(p, "trust.usage_remove");
                return true;
            }
            return handleTrust(p, args[0], null, false);
        }

        return true;
    }

    /**
     * Adiciona ou remove trust de um jogador no claim/subplot atual.
     * Público para que PlotCommand possa delegar o /lote amigo.
     */
    public boolean handleTrust(Player p, String targetName, Claim.TrustType type, boolean add) {
        Claim claim = plugin.getClaimManager().getClaimAt(p.getLocation());
        if (claim == null) {
            p.sendMessage(plugin.getMessageManager().get("trust.error_not_in_territory"));
            return true;
        }

        SubPlot plot = claim.getSubPlotAt(p.getLocation());
        boolean isStaff = p.isOp() || p.hasPermission("gorvax.admin");
        boolean isOwner;

        if (plot != null) {
            isOwner = plot.getOwner() != null && plot.getOwner().equals(p.getUniqueId());
            if (!isOwner && plot.getRenter() != null && plot.getRenter().equals(p.getUniqueId()))
                isOwner = true;
        } else {
            isOwner = claim.getOwner().equals(p.getUniqueId());
        }

        if (!isStaff && !isOwner && !claim.hasPermission(p.getUniqueId(), Claim.TrustType.VICE)) {
            p.sendMessage(plugin.getMessageManager().get("general.no_permission"));
            return true;
        }

        if (isStaff && !isOwner && !claim.hasPermission(p.getUniqueId(), Claim.TrustType.VICE)) {
            p.sendMessage(plugin.getMessageManager().get("trust.staff_warning"));
        }

        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID;
        if (target != null)
            targetUUID = target.getUniqueId();
        else {
            OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
            if (!op.hasPlayedBefore()) {
                p.sendMessage(plugin.getMessageManager().get("general.player_never_played"));
                return true;
            }
            targetUUID = op.getUniqueId();
        }

        var mm = plugin.getMessageManager();
        if (add) {
            if (plot != null) {
                plot.addTrust(targetUUID, type);
                mm.send(p, "trust.added_plot", type, targetName);
            } else {
                claim.addTrust(targetUUID, type);
                mm.send(p, "trust.added_kingdom", type, targetName);
                if (type == Claim.TrustType.VICE) {
                    // Add Duque msg
                }
            }
            // B10 — Log de auditoria
            if (plugin.getAuditManager() != null) {
                plugin.getAuditManager().log(
                        AuditManager.AuditAction.TRUST_ADD,
                        p.getUniqueId(), p.getName(),
                        "Adicionou " + type + " para " + targetName
                                + (plot != null ? " no lote" : " no claim"));
            }
        } else {
            if (plot != null) {
                plot.removeTrust(targetUUID);
                mm.send(p, "trust.removed_plot", targetName);
            } else {
                claim.removeTrust(targetUUID);
                mm.send(p, "trust.removed_kingdom", targetName);
            }
            // B10 — Log de auditoria
            if (plugin.getAuditManager() != null) {
                plugin.getAuditManager().log(
                        AuditManager.AuditAction.TRUST_REMOVE,
                        p.getUniqueId(), p.getName(),
                        "Removeu permissões de " + targetName
                                + (plot != null ? " no lote" : " no claim"));
            }
        }
        plugin.getClaimManager().saveClaims();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player)) return completions;

        if (label.equalsIgnoreCase("permitir") || label.equalsIgnoreCase("trust")) {
            if (args.length == 1) {
                return filterOnlinePlayers(args[0]);
            }
            if (args.length == 2) {
                completions.addAll(List.of("GERAL", "CONSTRUCAO", "CONTEINER", "ACESSO", "VICE"));
                return filterCompletions(completions, args[1]);
            }
            return completions;
        }

        if (label.equalsIgnoreCase("remover") || label.equalsIgnoreCase("untrust")) {
            if (args.length == 1) {
                return filterOnlinePlayers(args[0]);
            }
            return completions;
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
