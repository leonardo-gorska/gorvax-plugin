package br.com.gorvax.core.towns.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.AuditManager;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.towns.managers.KingdomManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Subcomando de membros do reino: convidar, aceitar, recusar, membros,
 * transferir.
 * Extraído do KingdomCommand no Batch B20.
 */
public class KingdomMemberSubcommand {

    private final GorvaxCore plugin;

    public KingdomMemberSubcommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Despacha o subcomando de membros.
     */
    public boolean handle(Player p, String sub, String[] args) {
        var msg = plugin.getMessageManager();
        switch (sub) {
            case "convidar":
                if (args.length < 2) {
                    msg.send(p, "kingdom.invite_usage");
                    return true;
                }
                handleKingdomInvite(p, args[1]);
                return true;
            case "aceitar":
                handleKingdomAccept(p);
                return true;
            case "recusar":
                handleKingdomDeny(p);
                return true;
            case "membros":
            case "suditos":
                handleKingdomMembers(p);
                return true;
            case "transferir":
            case "sucessor":
                if (args.length < 2) {
                    msg.send(p, "kingdom.transfer_usage");
                    return true;
                }
                handleKingdomTransfer(p, args[1]);
                return true;
            default:
                return false;
        }
    }

    private void handleKingdomInvite(Player p, String targetName) {
        var msg = plugin.getMessageManager();
        Claim kingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        if (kingdom == null || !kingdom.getOwner().equals(p.getUniqueId())) {
            msg.send(p, "kingdom.invite_not_king");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            msg.send(p, "kingdom.invite_target_offline");
            return;
        }

        if (target.getUniqueId().equals(p.getUniqueId())) {
            msg.send(p, "kingdom.invite_self");
            return;
        }

        if (plugin.getKingdomManager().isSudito(kingdom.getId(), target.getUniqueId())) {
            msg.send(p, "kingdom.invite_already_member");
            return;
        }

        long expireSeconds = plugin.getConfig().getLong("kingdoms.invite_expire", 60);
        plugin.getKingdomManager().invitePlayer(kingdom.getId(), p.getUniqueId(), target.getUniqueId());

        String kingdomName = kingdom.getKingdomName() != null ? kingdom.getKingdomName() : "Sem nome";
        msg.send(p, "kingdom.invite_sent", target.getName(), expireSeconds);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

        msg.send(target, "kingdom.invite_received", kingdomName);
        msg.send(target, "kingdom.invite_received_hint", expireSeconds);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    private void handleKingdomAccept(Player p) {
        var msg = plugin.getMessageManager();
        KingdomManager.Invite invite = plugin.getKingdomManager().getPendingInvite(p.getUniqueId());
        if (invite == null) {
            msg.send(p, "kingdom.invite_none");
            return;
        }

        if (plugin.getKingdomManager().acceptInvite(p.getUniqueId())) {
            String kingdomName = plugin.getKingdomManager().getNome(invite.kingdomId());
            msg.send(p, "kingdom.invite_accepted", kingdomName);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
            msg.sendTitle(p, "kingdom.invite_accepted_title", "kingdom.invite_accepted_subtitle", 10, 60, 20,
                    kingdomName);

            // Notificar o rei
            Player inviter = Bukkit.getPlayer(invite.inviter());
            if (inviter != null) {
                msg.send(inviter, "kingdom.invite_accepted_notify", p.getName());
                inviter.playSound(inviter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        } else {
            msg.send(p, "kingdom.invite_error");
        }
    }

    private void handleKingdomDeny(Player p) {
        var msg = plugin.getMessageManager();
        KingdomManager.Invite invite = plugin.getKingdomManager().getPendingInvite(p.getUniqueId());
        if (invite == null) {
            msg.send(p, "kingdom.invite_none_deny");
            return;
        }

        String kingdomName = plugin.getKingdomManager().getNome(invite.kingdomId());
        plugin.getKingdomManager().denyInvite(p.getUniqueId());
        msg.send(p, "kingdom.invite_declined", kingdomName);

        // Notificar o rei
        Player inviter = Bukkit.getPlayer(invite.inviter());
        if (inviter != null) {
            msg.send(inviter, "kingdom.invite_declined_notify", p.getName());
        }
    }

    private void handleKingdomMembers(Player p) {
        var msg = plugin.getMessageManager();
        Claim kingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        if (kingdom == null) {
            msg.send(p, "kingdom.error_no_kingdom");
            return;
        }
        plugin.getKingdomInventory().openMembersMenu(p, kingdom.getId());
    }

    private void handleKingdomTransfer(Player p, String targetName) {
        var msg = plugin.getMessageManager();
        Claim kingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        if (kingdom == null || !kingdom.getOwner().equals(p.getUniqueId())) {
            msg.send(p, "kingdom.transfer_not_king");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            msg.send(p, "general.player_not_found");
            return;
        }

        if (!plugin.getKingdomManager().isSudito(kingdom.getId(), target.getUniqueId())) {
            msg.send(p, "kingdom.transfer_not_member");
            return;
        }

        // Transfer Logic
        plugin.getKingdomManager().setRei(kingdom.getId(), target.getUniqueId());
        plugin.getKingdomManager().addSudito(kingdom.getId(), p.getUniqueId()); // Antigo rei vira súdito
        plugin.getKingdomManager().removeSudito(kingdom.getId(), target.getUniqueId()); // Novo rei sai da lista

        // B10 — Log de auditoria
        if (plugin.getAuditManager() != null) {
            plugin.getAuditManager().log(
                    AuditManager.AuditAction.KINGDOM_TRANSFER,
                    p.getUniqueId(), p.getName(),
                    "Coroa de '" + kingdom.getKingdomName() + "' transferida para " + target.getName());
        }

        msg.send(p, "kingdom.transfer_success", target.getName());
        msg.send(target, "kingdom.transfer_received");
        plugin.refreshPlayerName(p);
        plugin.refreshPlayerName(target);
    }

    /**
     * Tab completion para subcomandos de membros.
     */
    public List<String> tabComplete(String sub, String[] args) {
        if (args.length == 2) {
            switch (sub) {
                case "transferir":
                case "sucessor":
                case "convidar":
                    return filterOnlinePlayers(args[1]);
            }
        }
        return new ArrayList<>();
    }

    private List<String> filterOnlinePlayers(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
