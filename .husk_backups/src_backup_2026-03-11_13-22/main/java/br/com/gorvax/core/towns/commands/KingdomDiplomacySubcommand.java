package br.com.gorvax.core.towns.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.towns.Relation;
import br.com.gorvax.core.towns.managers.KingdomManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Subcomando de diplomacia do reino: aliança, inimigo, neutro.
 * Extraído do KingdomCommand no Batch B20.
 */
public class KingdomDiplomacySubcommand {

    private final GorvaxCore plugin;

    public KingdomDiplomacySubcommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Despacha o subcomando de diplomacia.
     */
    public boolean handle(Player p, String sub, String[] args) {
        var msg = plugin.getMessageManager();
        switch (sub) {
            case "alianca":
            case "aliança":
            case "alliance":
                if (args.length < 2) {
                    msg.send(p, "kingdom.visit_usage");
                    return true;
                }
                handleAlliancePropose(p, args[1]);
                return true;
            case "aceitaralianca":
            case "acceptalliance":
                handleAllianceAccept(p);
                return true;
            case "recusaralianca":
            case "denyalliance":
                handleAllianceDeny(p);
                return true;
            case "inimigo":
            case "enemy":
                if (args.length < 2) {
                    msg.send(p, "kingdom.visit_usage");
                    return true;
                }
                handleEnemyDeclare(p, args[1]);
                return true;
            case "neutro":
            case "neutral":
                if (args.length < 2) {
                    msg.send(p, "kingdom.visit_usage");
                    return true;
                }
                handleNeutralSet(p, args[1]);
                return true;
            default:
                return false;
        }
    }

    private void handleAlliancePropose(Player p, String targetKingdomName) {
        var msg = plugin.getMessageManager();
        KingdomManager km = plugin.getKingdomManager();

        Claim myKingdom = km.getKingdom(p.getUniqueId());
        if (myKingdom == null) {
            msg.send(p, "kingdom.error_no_kingdom");
            return;
        }
        if (!km.isRei(myKingdom.getId(), p.getUniqueId())) {
            msg.send(p, "diplomacy.error_not_king_diplomacy");
            return;
        }

        String targetId = km.tryFindKingdomIdByName(targetKingdomName);
        if (targetId == null) {
            msg.send(p, "diplomacy.error_kingdom_not_found", targetKingdomName);
            return;
        }
        if (targetId.equals(myKingdom.getId())) {
            msg.send(p, "diplomacy.error_same_kingdom");
            return;
        }
        if (km.areAllied(myKingdom.getId(), targetId)) {
            msg.send(p, "diplomacy.alliance_already");
            return;
        }

        int maxAllies = plugin.getConfig().getInt("diplomacy.max_allies", 3);
        if (km.getAllianceCount(myKingdom.getId()) >= maxAllies) {
            msg.send(p, "diplomacy.alliance_max", km.getAllianceCount(myKingdom.getId()), maxAllies);
            return;
        }

        km.proposeAlliance(myKingdom.getId(), targetId);
        String targetName = km.getNome(targetId);
        msg.send(p, "diplomacy.alliance_proposed", targetName);

        // Notificar Rei do reino alvo (se online)
        UUID targetKing = km.getRei(targetId);
        if (targetKing != null) {
            Player kingPlayer = Bukkit.getPlayer(targetKing);
            if (kingPlayer != null && kingPlayer.isOnline()) {
                msg.send(kingPlayer, "diplomacy.alliance_received", km.getNome(myKingdom.getId()));
                msg.send(kingPlayer, "diplomacy.alliance_received_hint");
            }
        }
    }

    private void handleAllianceAccept(Player p) {
        var msg = plugin.getMessageManager();
        KingdomManager km = plugin.getKingdomManager();

        Claim myKingdom = km.getKingdom(p.getUniqueId());
        if (myKingdom == null) {
            msg.send(p, "kingdom.error_no_kingdom");
            return;
        }
        if (!km.isRei(myKingdom.getId(), p.getUniqueId())) {
            msg.send(p, "diplomacy.error_not_king_diplomacy");
            return;
        }

        KingdomManager.AllianceProposal proposal = km.getPendingAlliance(myKingdom.getId());
        if (proposal == null) {
            msg.send(p, "diplomacy.alliance_no_pending");
            return;
        }

        int maxAllies = plugin.getConfig().getInt("diplomacy.max_allies", 3);
        if (km.getAllianceCount(myKingdom.getId()) >= maxAllies) {
            msg.send(p, "diplomacy.alliance_max", km.getAllianceCount(myKingdom.getId()), maxAllies);
            return;
        }

        km.acceptAlliance(myKingdom.getId());
        String fromName = km.getNome(proposal.fromKingdomId());
        String myName = km.getNome(myKingdom.getId());
        msg.send(p, "diplomacy.alliance_accepted", fromName);
        msg.broadcast("diplomacy.alliance_accepted_broadcast", fromName, myName);

        // Notificar rei do reino propositor
        UUID proposerKing = km.getRei(proposal.fromKingdomId());
        if (proposerKing != null) {
            Player proposer = Bukkit.getPlayer(proposerKing);
            if (proposer != null && proposer.isOnline()) {
                msg.send(proposer, "diplomacy.alliance_accepted", myName);
            }
        }
    }

    private void handleAllianceDeny(Player p) {
        var msg = plugin.getMessageManager();
        KingdomManager km = plugin.getKingdomManager();

        Claim myKingdom = km.getKingdom(p.getUniqueId());
        if (myKingdom == null) {
            msg.send(p, "kingdom.error_no_kingdom");
            return;
        }
        if (!km.isRei(myKingdom.getId(), p.getUniqueId())) {
            msg.send(p, "diplomacy.error_not_king_diplomacy");
            return;
        }

        KingdomManager.AllianceProposal proposal = km.getPendingAlliance(myKingdom.getId());
        if (proposal == null) {
            msg.send(p, "diplomacy.alliance_no_pending");
            return;
        }

        km.denyAlliance(myKingdom.getId());
        String fromName = km.getNome(proposal.fromKingdomId());
        msg.send(p, "diplomacy.alliance_denied", fromName);

        // Notificar rei do propositor
        UUID proposerKing = km.getRei(proposal.fromKingdomId());
        if (proposerKing != null) {
            Player proposer = Bukkit.getPlayer(proposerKing);
            if (proposer != null && proposer.isOnline()) {
                msg.send(proposer, "diplomacy.alliance_denied_notify", km.getNome(myKingdom.getId()));
            }
        }
    }

    private void handleEnemyDeclare(Player p, String targetKingdomName) {
        var msg = plugin.getMessageManager();
        KingdomManager km = plugin.getKingdomManager();

        Claim myKingdom = km.getKingdom(p.getUniqueId());
        if (myKingdom == null) {
            msg.send(p, "kingdom.error_no_kingdom");
            return;
        }
        if (!km.isRei(myKingdom.getId(), p.getUniqueId())) {
            msg.send(p, "diplomacy.error_not_king_diplomacy");
            return;
        }

        String targetId = km.tryFindKingdomIdByName(targetKingdomName);
        if (targetId == null) {
            msg.send(p, "diplomacy.error_kingdom_not_found", targetKingdomName);
            return;
        }
        if (targetId.equals(myKingdom.getId())) {
            msg.send(p, "diplomacy.error_same_kingdom");
            return;
        }
        if (km.areEnemies(myKingdom.getId(), targetId)) {
            msg.send(p, "diplomacy.enemy_already");
            return;
        }

        int maxEnemies = plugin.getConfig().getInt("diplomacy.max_enemies", 5);
        if (km.getEnemyCount(myKingdom.getId()) >= maxEnemies) {
            msg.send(p, "diplomacy.enemy_max", km.getEnemyCount(myKingdom.getId()), maxEnemies);
            return;
        }

        km.setRelation(myKingdom.getId(), targetId, Relation.ENEMY);
        String targetName = km.getNome(targetId);
        msg.send(p, "diplomacy.enemy_declared", targetName);

        // Notificar rei do reino alvo
        UUID targetKing = km.getRei(targetId);
        if (targetKing != null) {
            Player kingPlayer = Bukkit.getPlayer(targetKing);
            if (kingPlayer != null && kingPlayer.isOnline()) {
                msg.send(kingPlayer, "diplomacy.enemy_declared_target", km.getNome(myKingdom.getId()));
            }
        }
    }

    private void handleNeutralSet(Player p, String targetKingdomName) {
        var msg = plugin.getMessageManager();
        KingdomManager km = plugin.getKingdomManager();

        Claim myKingdom = km.getKingdom(p.getUniqueId());
        if (myKingdom == null) {
            msg.send(p, "kingdom.error_no_kingdom");
            return;
        }
        if (!km.isRei(myKingdom.getId(), p.getUniqueId())) {
            msg.send(p, "diplomacy.error_not_king_diplomacy");
            return;
        }

        String targetId = km.tryFindKingdomIdByName(targetKingdomName);
        if (targetId == null) {
            msg.send(p, "diplomacy.error_kingdom_not_found", targetKingdomName);
            return;
        }
        if (targetId.equals(myKingdom.getId())) {
            msg.send(p, "diplomacy.error_same_kingdom");
            return;
        }

        km.setRelation(myKingdom.getId(), targetId, Relation.NEUTRAL);
        String targetName = km.getNome(targetId);
        msg.send(p, "diplomacy.neutral_set", targetName);

        // Notificar rei do outro reino
        UUID targetKing = km.getRei(targetId);
        if (targetKing != null) {
            Player kingPlayer = Bukkit.getPlayer(targetKing);
            if (kingPlayer != null && kingPlayer.isOnline()) {
                msg.send(kingPlayer, "diplomacy.neutral_notify", km.getNome(myKingdom.getId()));
            }
        }
    }

    /**
     * Tab completion para subcomandos de diplomacia.
     */
    public List<String> tabComplete(String sub, String[] args) {
        if (args.length == 2) {
            switch (sub) {
                case "alianca":
                case "aliança":
                case "alliance":
                case "inimigo":
                case "enemy":
                case "neutro":
                case "neutral":
                    return filterCompletions(plugin.getKingdomManager().getAllKingdomNames(), args[1]);
            }
        }
        return new ArrayList<>();
    }

    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
