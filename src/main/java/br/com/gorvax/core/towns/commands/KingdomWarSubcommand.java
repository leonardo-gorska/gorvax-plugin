package br.com.gorvax.core.towns.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.AuditManager;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.towns.managers.KingdomManager;
import br.com.gorvax.core.towns.managers.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Subcomando de guerra do reino: declarar, renderse, status.
 * Extraído do KingdomCommand no Batch B20.
 */
public class KingdomWarSubcommand {

    private final GorvaxCore plugin;

    public KingdomWarSubcommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Despacha o subcomando de guerra.
     */
    public boolean handle(Player p, String[] args) {
        var msg = plugin.getMessageManager();
        WarManager wm = plugin.getWarManager();
        KingdomManager km = plugin.getKingdomManager();

        if (wm == null) {
            msg.send(p, "war.error_disabled");
            return true;
        }

        // Verificar se é Rei
        Claim kingdom = km.getKingdom(p.getUniqueId());
        if (kingdom == null) {
            msg.send(p, "war.error_not_king");
            return true;
        }

        UUID rei = km.getRei(kingdom.getId());
        boolean isKing = rei != null && rei.equals(p.getUniqueId());

        if (args.length < 2) {
            msg.send(p, "war.usage");
            return true;
        }

        String warSub = args[1].toLowerCase();

        switch (warSub) {
            case "declarar":
            case "declare": {
                if (!isKing) {
                    msg.send(p, "war.error_not_king");
                    return true;
                }
                if (args.length < 3) {
                    msg.send(p, "war.usage_declare");
                    return true;
                }

                // Concatenar nome do reino alvo (pode ter espaços)
                StringBuilder targetNameBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    if (i > 2)
                        targetNameBuilder.append(" ");
                    targetNameBuilder.append(args[i]);
                }
                String targetName = targetNameBuilder.toString().trim();

                // Encontrar o reino alvo pelo nome
                String targetId = km.tryFindKingdomIdByName(targetName);
                if (targetId == null) {
                    msg.send(p, "kingdom.not_found", targetName);
                    return true;
                }

                // Verificar auto-guerra
                if (kingdom.getId().equals(targetId)) {
                    msg.send(p, "war.error_self");
                    return true;
                }

                // Declarar guerra
                String error = wm.declareWar(kingdom.getId(), targetId);
                if (error != null) {
                    // Enviar mensagem de erro com parâmetros relevantes
                    switch (error) {
                        case "war.error_already_at_war":
                            msg.send(p, error, km.getNome(targetId));
                            break;
                        case "war.error_insufficient_funds":
                            msg.send(p, error, String.format("%.2f", wm.getDeclarationCost()));
                            break;
                        case "war.error_cooldown":
                            msg.send(p, error, String.valueOf(wm.getCooldownDaysRemaining(kingdom.getId(), targetId)));
                            break;
                        case "war.error_min_level":
                            msg.send(p, error, String.valueOf(plugin.getConfig().getInt("war.min_kingdom_level", 3)));
                            break;
                        case "war.error_min_members":
                            msg.send(p, error, String.valueOf(plugin.getConfig().getInt("war.min_members", 3)));
                            break;
                        default:
                            msg.send(p, error);
                            break;
                    }
                    return true;
                }

                // Sucesso
                String defenderName = km.getNome(targetId);
                String attackerName = km.getNome(kingdom.getId());
                long prepHours = plugin.getConfig().getLong("war.preparation_hours", 24);

                // Broadcast global
                msg.broadcast("war.declared_broadcast", attackerName, defenderName, String.valueOf(prepHours));

                // B8 — Discord: alerta de guerra declarada
                plugin.getDiscordManager().sendWarDeclaredAlert(attackerName, defenderName);

                // Notificar atacante
                msg.send(p, "war.declared_attacker", defenderName, String.valueOf(prepHours));

                // Notificar membros do defensor
                UUID defKing = km.getRei(targetId);
                if (defKing != null) {
                    Player defPlayer = Bukkit.getPlayer(defKing);
                    if (defPlayer != null && defPlayer.isOnline()) {
                        msg.send(defPlayer, "war.declared_defender", attackerName, String.valueOf(prepHours));
                    }
                }
                List<String> defMembers = km.getSuditosList(targetId);
                if (defMembers != null) {
                    for (String memberStr : defMembers) {
                        try {
                            UUID memberId = UUID.fromString(memberStr);
                            Player member = Bukkit.getPlayer(memberId);
                            if (member != null && member.isOnline()) {
                                msg.send(member, "war.declared_defender", attackerName, String.valueOf(prepHours));
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }

                // Audit log
                if (plugin.getAuditManager() != null) {
                    plugin.getAuditManager().log(
                            AuditManager.AuditAction.KINGDOM_CREATE,
                            p.getUniqueId(), p.getName(),
                            "Declarou guerra: " + attackerName + " vs " + defenderName);
                }
                break;
            }

            case "renderse":
            case "surrender": {
                if (!isKing) {
                    msg.send(p, "war.error_not_king");
                    return true;
                }

                WarManager.War war = wm.getWarForKingdom(kingdom.getId());
                if (war == null || war.getState() == WarManager.WarState.ENDED) {
                    msg.send(p, "war.error_not_at_war");
                    return true;
                }

                // Confirmação
                if (args.length < 3 || !args[2].equalsIgnoreCase("confirmar")) {
                    double spoilPercent = plugin.getConfig().getDouble("war.spoils_bank_percent", 25.0)
                            + plugin.getConfig().getDouble("war.surrender_penalty_percent", 10.0);
                    int debuffDays = plugin.getConfig().getInt("war.loser_debuff_days", 3);
                    msg.send(p, "war.surrender_confirm", String.format("%.0f", spoilPercent),
                            String.valueOf(debuffDays));
                    msg.send(p, "war.surrender_confirm_hint");
                    return true;
                }

                // Processar rendição
                String surrenderError = wm.surrender(kingdom.getId());
                if (surrenderError != null) {
                    msg.send(p, surrenderError);
                }
                // O WarManager já faz broadcast e notificações
                break;
            }

            case "status": {
                List<WarManager.War> wars = wm.getActiveWars();
                msg.send(p, "war.status_header");

                boolean hasWars = false;
                for (WarManager.War war : wars) {
                    // Mostrar guerras do reino do jogador, ou todas se for admin
                    if (war.involves(kingdom.getId()) || p.hasPermission("gorvax.admin")) {
                        String attackerName = km.getNome(war.getAttackerId());
                        String defenderName = km.getNome(war.getDefenderId());
                        String phase = war.getState() == WarManager.WarState.PREPARATION ? "Preparação" : "Ativa";

                        long remaining;
                        if (war.getState() == WarManager.WarState.PREPARATION) {
                            remaining = war.getStartsAt() - System.currentTimeMillis();
                        } else {
                            remaining = war.getEndsAt() - System.currentTimeMillis();
                        }
                        String timeRemaining = WarManager.formatDuration(remaining);

                        msg.send(p, "war.status_entry",
                                attackerName, defenderName,
                                String.valueOf(war.getAttackerPoints()),
                                String.valueOf(war.getDefenderPoints()),
                                phase, timeRemaining);
                        hasWars = true;
                    }
                }

                if (!hasWars) {
                    msg.send(p, "war.status_no_wars");
                }

                msg.send(p, "war.status_footer");
                break;
            }

            default:
                msg.send(p, "war.usage");
                break;
        }
        return true;
    }

    /**
     * Tab completion para subcomandos de guerra.
     */
    public List<String> tabComplete(String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.addAll(List.of("declarar", "renderse", "status"));
            return filterCompletions(completions, args[1]);
        }
        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("declarar")) {
                return filterCompletions(plugin.getKingdomManager().getAllKingdomNames(), args[2]);
            }
            if (args[1].equalsIgnoreCase("renderse")) {
                completions.add("confirmar");
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
}
