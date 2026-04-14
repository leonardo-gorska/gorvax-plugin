package br.com.gorvax.core.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KingdomChatCommand implements CommandExecutor {

    private final GorvaxCore plugin;

    public KingdomChatCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("kingdom_chat.player_only"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().get("kingdom_chat.usage"));
            return true;
        }

        Claim claim = plugin.getClaimManager().getClaimAt(player.getLocation());
        String kingdomId = null;

        // Busca o reino do jogador (Rei ou Súdito) usando a fonte da verdade
        Claim residentKingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());

        if (residentKingdom != null) {
            kingdomId = residentKingdom.getId();
            claim = residentKingdom;
        } else {
            // Se não encontrou reino vinculado, verifica se o jogador é dono/membro de
            // algum claim de reino
            // Fallback logic could be here, but simpler to just fail if not officially
            // resident
            player.sendMessage(plugin.getMessageManager().get("kingdom_chat.not_resident"));
            return true;
        }

        if (kingdomId == null || claim == null) {
            player.sendMessage(plugin.getMessageManager().get("kingdom_chat.no_kingdom"));
            return true;
        }

        // Verificação final de segurança
        if (!plugin.getKingdomManager().isSudito(kingdomId, player.getUniqueId()) &&
                !plugin.getKingdomManager().isRei(kingdomId, player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().get("kingdom_chat.not_resident"));
            return true;
        }

        StringBuilder message = new StringBuilder();
        for (String arg : args)
            message.append(arg).append(" ");

        String formattedMsg = "§8[§bCHAT REAL§8] " + claim.getChatColor() + player.getName() + "§8: "
                + claim.getChatColor() + message.toString().trim();

        // Coleta UUIDs únicos de todos os membros do reino
        java.util.Set<java.util.UUID> recipients = new java.util.HashSet<>();
        recipients.add(claim.getOwner()); // Rei sempre recebe
        for (String uuidStr : plugin.getKingdomManager().getSuditosList(claim.getId())) {
            try {
                recipients.add(java.util.UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {
                // UUID inválido nos dados, ignora silenciosamente
            }
        }

        // Envia para todos os membros online (sem duplicatas)
        for (java.util.UUID uuid : recipients) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(formattedMsg);
            }
        }

        // Log no console
        Bukkit.getConsoleSender().sendMessage("§7[" + claim.getKingdomName() + "] " + formattedMsg);

        return true;
    }
}
