package br.com.gorvax.core.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.ChatManager;
import br.com.gorvax.core.managers.ChatManager.ChatChannel;
import br.com.gorvax.core.managers.ChatManager.FilterResult;
import br.com.gorvax.core.managers.ChatManager.FilterResultData;
import br.com.gorvax.core.managers.ChatManager.SpamCheckResult;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * B8 — Comandos de chat expandido.
 * B9 — Anti-spam, filtro de palavras e /ignore.
 * Gerencia: /chat, /g, /l, /tc, /ac, /rc, /nc (B19), /ignore
 */
public class ChatCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public ChatCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "general.player_only");
            return true;
        }

        var msg = plugin.getMessageManager();
        var chatManager = plugin.getChatManager();
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "chat" -> handleChatSwitch(player, args, msg, chatManager);
            case "g" -> handleDirectSend(player, args, ChatChannel.GLOBAL);
            case "l" -> handleDirectSend(player, args, ChatChannel.LOCAL);
            case "tc" -> handleDirectSend(player, args, ChatChannel.TRADE);
            case "ac" -> handleDirectSend(player, args, ChatChannel.ALLIANCE);
            case "rc" -> handleDirectSend(player, args, ChatChannel.KINGDOM);
            case "nc" -> handleDirectSend(player, args, ChatChannel.NATION);  // B19
            case "ignore" -> handleIgnore(player, args);  // B9
            default -> msg.send(player, "chat.channel_usage");
        }

        return true;
    }

    /**
     * /chat <canal> — alterna o canal padrão do jogador.
     * /chat (sem args) — mostra o canal atual.
     */
    private void handleChatSwitch(Player player, String[] args,
            br.com.gorvax.core.managers.MessageManager msg,
            ChatManager chatManager) {
        if (args.length == 0) {
            ChatChannel current = chatManager.getChannel(player.getUniqueId());
            msg.send(player, "chat.channel_current", current.getLabel());
            return;
        }

        ChatChannel target = ChatChannel.fromString(args[0]);
        if (target == null) {
            msg.send(player, "chat.channel_invalid");
            return;
        }

        // Verificações de permissão por canal
        if (!canUseChannel(player, target, msg)) {
            return;
        }

        chatManager.setChannel(player.getUniqueId(), target);
        msg.send(player, "chat.channel_switched", target.getLabel());
    }

    /**
     * /g, /l, /tc, /ac, /rc — envia uma mensagem diretamente em um canal
     * sem alterar o canal padrão do jogador.
     */
    private void handleDirectSend(Player player, String[] args, ChatChannel channel) {
        var msg = plugin.getMessageManager();

        if (args.length == 0) {
            // Sem mensagem = alterna para o canal
            if (!canUseChannel(player, channel, msg)) return;
            plugin.getChatManager().setChannel(player.getUniqueId(), channel);
            msg.send(player, "chat.channel_switched", channel.getLabel());
            return;
        }

        // Verificar permissão
        if (!canUseChannel(player, channel, msg)) return;

        // Montar mensagem
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(args[i]);
        }
        String message = sb.toString();

        // B9 — Anti-spam (rate limit + duplicata + mute) — bypass para admins
        if (!player.hasPermission("gorvax.chat.bypass")) {
            SpamCheckResult spamResult = plugin.getChatManager().checkSpam(player.getUniqueId(), message);
            if (spamResult != SpamCheckResult.OK) {
                switch (spamResult) {
                    case RATE_LIMITED -> msg.send(player, "chat.spam_rate_limited");
                    case DUPLICATE_MESSAGE -> msg.send(player, "chat.spam_duplicate");
                    case MUTED -> msg.send(player, "chat.spam_muted",
                            plugin.getChatManager().getMuteRemaining(player.getUniqueId()));
                    default -> {} // OK already handled
                }
                return;
            }

            // B9 — Filtro de palavras
            FilterResultData filterData = plugin.getChatManager().applyFilter(player.getUniqueId(), message);
            switch (filterData.result()) {
                case BLOCKED -> {
                    msg.send(player, "chat.filter_blocked");
                    return;
                }
                case MUTED -> {
                    msg.send(player, "chat.filter_muted",
                            plugin.getChatManager().getMuteRemaining(player.getUniqueId()));
                    return;
                }
                case CENSORED -> message = filterData.filteredMessage();
                default -> {} // CLEAN
            }
        }

        // Enviar pelo canal
        sendChannelMessage(player, channel, message);
    }

    /**
     * Verifica se o jogador pode usar o canal. Envia mensagem de erro se não.
     */
    private boolean canUseChannel(Player player, ChatChannel channel,
            br.com.gorvax.core.managers.MessageManager msg) {
        if (channel == ChatChannel.GLOBAL || channel == ChatChannel.LOCAL || channel == ChatChannel.TRADE) {
            return true; // Abertos a todos
        }

        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
        if (kingdom == null) {
            msg.send(player, "chat.channel_no_kingdom");
            return false;
        }

        if (channel == ChatChannel.ALLIANCE) {
            List<String> allies = plugin.getKingdomManager().getAlliances(kingdom.getId());
            if (allies.isEmpty()) {
                msg.send(player, "chat.channel_no_allies");
                return false;
            }
        }

        // B19 — NATION precisa pertencer a uma nação
        if (channel == ChatChannel.NATION) {
            var nm = plugin.getNationManager();
            if (nm == null || nm.getNationByKingdom(kingdom.getId()) == null) {
                msg.send(player, "chat.channel_no_nation");
                return false;
            }
        }

        return true;
    }

    /**
     * Envia uma mensagem no canal especificado.
     */
    private void sendChannelMessage(Player sender, ChatChannel channel, String message) {
        var chatManager = plugin.getChatManager();
        String prefix = chatManager.getChannelPrefix(channel);
        String formatted = chatManager.formatMessage(sender, message, channel);
        String fullMessage = prefix + formatted;

        UUID senderUuid = sender.getUniqueId();

        switch (channel) {
            case GLOBAL -> {
                // B9 — Todos os jogadores online (filtrando ignorados)
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.getUniqueId().equals(senderUuid)
                            && chatManager.isIgnoring(online.getUniqueId(), senderUuid)) continue;
                    online.sendMessage(fullMessage);
                }
            }
            case KINGDOM -> sendKingdomChat(sender, fullMessage);
            case ALLIANCE -> sendAllianceChat(sender, fullMessage);
            case LOCAL -> sendLocalChat(sender, fullMessage, chatManager.getLocalRadius());
            case TRADE -> {
                // B9 — Todos recebem mensagens de comércio (filtrando ignorados)
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.getUniqueId().equals(senderUuid)
                            && chatManager.isIgnoring(online.getUniqueId(), senderUuid)) continue;
                    online.sendMessage(fullMessage);
                }
            }
            case NATION -> sendNationChat(sender, fullMessage);  // B19
        }

        // Log no console para todos os canais
        Bukkit.getConsoleSender().sendMessage("§7[" + channel.getLabel() + "] " + fullMessage);
    }

    /**
     * Envia para todos os membros do reino do jogador.
     */
    private void sendKingdomChat(Player sender, String formattedMsg) {
        Claim kingdom = plugin.getKingdomManager().getKingdom(sender.getUniqueId());
        if (kingdom == null) {
            plugin.getMessageManager().send(sender, "chat.channel_no_kingdom");
            return;
        }

        // Coletar todos os UUIDs do reino (Rei + Súditos)
        Set<UUID> recipients = new java.util.HashSet<>();
        recipients.add(kingdom.getOwner());
        for (String uuidStr : plugin.getKingdomManager().getSuditosList(kingdom.getId())) {
            try {
                recipients.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {
            }
        }

        for (UUID uuid : recipients) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(formattedMsg);
            }
        }
    }

    /**
     * Envia para todos os jogadores de reinos aliados + próprio reino.
     */
    private void sendAllianceChat(Player sender, String formattedMsg) {
        Claim kingdom = plugin.getKingdomManager().getKingdom(sender.getUniqueId());
        if (kingdom == null) {
            plugin.getMessageManager().send(sender, "chat.channel_no_kingdom");
            return;
        }

        List<String> allies = plugin.getKingdomManager().getAlliances(kingdom.getId());
        allies.add(kingdom.getId()); // Inclui o próprio reino

        for (Player online : Bukkit.getOnlinePlayers()) {
            Claim pk = plugin.getKingdomManager().getKingdom(online.getUniqueId());
            if (pk != null && allies.contains(pk.getId())) {
                online.sendMessage(formattedMsg);
            }
        }
    }

    /**
     * Envia para jogadores num raio de blocos ao redor do sender.
     * B9 — Filtra jogadores ignorados.
     */
    private void sendLocalChat(Player sender, String formattedMsg, int radius) {
        double radiusSq = (double) radius * radius;
        UUID senderUuid = sender.getUniqueId();
        ChatManager chatManager = plugin.getChatManager();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getWorld().equals(sender.getWorld())) continue;
            if (online.getLocation().distanceSquared(sender.getLocation()) <= radiusSq) {
                // B9 — Skip se o receptor ignora o remetente
                if (!online.getUniqueId().equals(senderUuid)
                        && chatManager.isIgnoring(online.getUniqueId(), senderUuid)) continue;
                online.sendMessage(formattedMsg);
            }
        }
    }

    // ========================================================================
    // B9 — /ignore Command
    // ========================================================================

    /**
     * /ignore — Lista jogadores ignorados.
     * /ignore <nick> — Adiciona/remove jogador da lista de ignorados.
     * /ignore list — Lista jogadores ignorados.
     */
    private void handleIgnore(Player player, String[] args) {
        var msg = plugin.getMessageManager();
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("list"))) {
            // Listar ignorados
            var ignored = pd.getIgnoredPlayers();
            if (ignored.isEmpty()) {
                msg.send(player, "ignore.list_empty");
                return;
            }

            msg.send(player, "ignore.list_header", ignored.size());
            for (UUID uuid : ignored) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                if (name == null) name = uuid.toString().substring(0, 8) + "...";
                msg.send(player, "ignore.list_entry", name);
            }
            return;
        }

        // /ignore <nick> — toggle
        String targetName = args[0];
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            msg.send(player, "general.player_never_played");
            return;
        }

        UUID targetUuid = target.getUniqueId();

        if (targetUuid.equals(player.getUniqueId())) {
            msg.send(player, "ignore.self");
            return;
        }

        // Toggle: se já está ignorando, desfaz.
        if (pd.getIgnoredPlayers().contains(targetUuid)) {
            pd.removeIgnoredPlayer(targetUuid);
            plugin.getPlayerDataManager().saveData(player.getUniqueId());
            msg.send(player, "ignore.removed", targetName);
        } else {
            if (!pd.addIgnoredPlayer(targetUuid)) {
                msg.send(player, "ignore.limit_reached");
                return;
            }
            plugin.getPlayerDataManager().saveData(player.getUniqueId());
            msg.send(player, "ignore.added", targetName);
        }
    }

    // ========================================================================
    // Tab Complete
    // ========================================================================

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("chat") && args.length == 1) {
            List<String> channels = List.of("global", "reino", "alianca", "local", "comercio", "nacao");
            return channels.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // B9 — Tab completion para /ignore
        if (cmdName.equals("ignore") && args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("list");
            // Sugerir jogadores online
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player p && !online.getUniqueId().equals(p.getUniqueId())) {
                    suggestions.add(online.getName());
                }
            }
            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    // B19 — Envia para todos os reinos da mesma nação
    private void sendNationChat(Player sender, String formattedMsg) {
        Claim kingdom = plugin.getKingdomManager().getKingdom(sender.getUniqueId());
        if (kingdom == null) {
            plugin.getMessageManager().send(sender, "chat.channel_no_kingdom");
            return;
        }

        var nm = plugin.getNationManager();
        if (nm == null) return;

        var nation = nm.getNationByKingdom(kingdom.getId());
        if (nation == null) {
            plugin.getMessageManager().send(sender, "chat.channel_no_nation");
            return;
        }

        // Coletar UUIDs de todos os reinos da nação
        Set<UUID> recipients = new java.util.HashSet<>();
        for (String kid : nation.getMemberKingdomIds()) {
            UUID rei = plugin.getKingdomManager().getRei(kid);
            if (rei != null) recipients.add(rei);
            for (String uuidStr : plugin.getKingdomManager().getSuditosList(kid)) {
                try {
                    recipients.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        for (UUID uuid : recipients) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(formattedMsg);
            }
        }
    }
}
