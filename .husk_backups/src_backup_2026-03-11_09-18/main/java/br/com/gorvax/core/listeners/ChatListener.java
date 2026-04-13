package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.ChatManager;
import br.com.gorvax.core.managers.ChatManager.ChatChannel;
import br.com.gorvax.core.managers.ChatManager.FilterResult;
import br.com.gorvax.core.managers.ChatManager.FilterResultData;
import br.com.gorvax.core.managers.ChatManager.SpamCheckResult;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.RankManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * B8 — Listener de chat refatorado para suportar múltiplos canais.
 * B9 — Anti-spam (rate limit), filtro de palavras e /ignore.
 * Roteia mensagens pelo canal ativo do jogador via ChatManager.
 */
public class ChatListener implements Listener {

    private final GorvaxCore plugin;

    public ChatListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player player = e.getPlayer();
        ChatManager chatManager = plugin.getChatManager();

        // B9 — Bypass para admins
        if (player.hasPermission("gorvax.chat.bypass")) {
            routeMessage(e, player, chatManager, null);
            return;
        }

        // B9 — Extrair texto bruto para verificações
        String rawText = LegacyComponentSerializer.legacySection().serialize(e.message());

        // B9.1 — Anti-spam (rate limit + duplicata + mute)
        SpamCheckResult spamResult = chatManager.checkSpam(player.getUniqueId(), rawText);
        if (spamResult != SpamCheckResult.OK) {
            e.setCancelled(true);
            var msg = plugin.getMessageManager();
            switch (spamResult) {
                case RATE_LIMITED -> msg.send(player, "chat.spam_rate_limited");
                case DUPLICATE_MESSAGE -> msg.send(player, "chat.spam_duplicate");
                case MUTED -> msg.send(player, "chat.spam_muted",
                        chatManager.getMuteRemaining(player.getUniqueId()));
                default -> {} // OK already handled above
            }
            return;
        }

        // B9.2 — Filtro de palavras
        FilterResultData filterData = chatManager.applyFilter(player.getUniqueId(), rawText);
        if (filterData.result() != FilterResult.CLEAN) {
            var msg = plugin.getMessageManager();
            switch (filterData.result()) {
                case BLOCKED -> {
                    e.setCancelled(true);
                    msg.send(player, "chat.filter_blocked");
                    return;
                }
                case MUTED -> {
                    e.setCancelled(true);
                    msg.send(player, "chat.filter_muted",
                            chatManager.getMuteRemaining(player.getUniqueId()));
                    return;
                }
                case CENSORED -> {
                    // Substituir a mensagem pelo texto censurado
                    Component censored = LegacyComponentSerializer.legacySection()
                            .deserialize(filterData.filteredMessage());
                    e.message(censored);
                }
                default -> {} // CLEAN
            }
        }

        // Rotear para o canal correto
        routeMessage(e, player, chatManager, filterData);
    }

    /**
     * Roteia a mensagem para o canal correto do jogador.
     */
    private void routeMessage(AsyncChatEvent e, Player player, ChatManager chatManager,
                              FilterResultData filterData) {
        ChatChannel channel = chatManager.getChannel(player.getUniqueId());

        // Se o canal é GLOBAL, usa o renderizador padrão com tag do reino
        if (channel == ChatChannel.GLOBAL) {
            handleGlobalChat(e, player);
            return;
        }

        // Para todos os outros canais, interceptamos e cancelamos o evento padrão
        e.setCancelled(true);

        // Extrair o texto da mensagem (pode já estar censurado)
        String plainText = LegacyComponentSerializer.legacySection()
                .serialize(e.message());

        // Enviar pelo canal usando o ChatCommand helper
        String prefix = chatManager.getChannelPrefix(channel);
        String formatted = chatManager.formatMessage(player, plainText, channel);
        String fullMessage = prefix + formatted;

        UUID senderUuid = player.getUniqueId();

        // Enviar conforme o canal (schedule na main thread para segurança)
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (channel) {
                case KINGDOM -> sendKingdomChat(player, senderUuid, fullMessage);
                case ALLIANCE -> sendAllianceChat(player, senderUuid, fullMessage);
                case LOCAL -> sendLocalChat(player, senderUuid, fullMessage, chatManager.getLocalRadius());
                case TRADE -> sendTradeChat(senderUuid, fullMessage);
                default -> sendGlobalBroadcast(senderUuid, fullMessage);
            }
            // Log no console
            Bukkit.getConsoleSender().sendMessage("§7[" + channel.getLabel() + "] " + fullMessage);
        });
    }

    /**
     * Canal GLOBAL — comportamento original com tag do reino e formatação por hierarquia.
     * B9 — Filtra jogadores ignorados por viewer no renderer.
     */
    private void handleGlobalChat(AsyncChatEvent e, Player player) {
        ChatManager chatManager = plugin.getChatManager();
        UUID senderUuid = player.getUniqueId();

        // Busca o reino do jogador
        Claim city = plugin.getKingdomManager().getKingdom(player.getUniqueId());

        // Monta a tag + rank
        String legacyPrefix = "";

        // B12 — Título ativo do jogador
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        String title = pd.getActiveTitle();
        if (title != null && !title.isEmpty()) {
            legacyPrefix += title + " ";
        }

        if (city != null) {
            String tag = city.getTag();
            if (tag != null && !tag.isEmpty()) {
                String color = city.getTagColor();
                legacyPrefix += "§f[" + color + tag + "§f] ";
            }
        }

        // Game Rank (Aventureiro, Explorador, Guerreiro, Lendário)
        RankManager rm = plugin.getRankManager();
        if (rm != null && rm.isEnabled()) {
            RankManager.GameRank gameRank = rm.getPlayerRank(player);
            legacyPrefix += gameRank.getDisplayName() + " ";
        }

        // Kingdom role icon (👑, ⚔️, 🛡️, 🏠)
        String rankIcon = chatManager.getRankIcon(player);
        if (!rankIcon.isEmpty()) {
            legacyPrefix += rankIcon + " ";
        }

        // B9 — Filtrar viewers que ignoram o remetente
        e.viewers().removeIf(viewer -> {
            if (viewer instanceof Player viewerPlayer && !viewerPlayer.getUniqueId().equals(senderUuid)) {
                return chatManager.isIgnoring(viewerPlayer.getUniqueId(), senderUuid);
            }
            return false;
        });

        // Se há algo para prefixar, usa o renderer customizado
        if (!legacyPrefix.isEmpty()) {
            String finalPrefix = legacyPrefix;
            Component tagComponent = LegacyComponentSerializer.legacySection().deserialize(finalPrefix);

            e.renderer((source, sourceDisplayName, message, viewer) ->
                    tagComponent
                            .append(sourceDisplayName)
                            .append(Component.text(": "))
                            .append(message)
            );
        }

        // B8 — Discord Chat Sync (MC → Discord)
        String plainText = LegacyComponentSerializer.legacySection().serialize(e.message());
        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
        String kingdomTag = (kingdom != null && kingdom.getTag() != null) ? kingdom.getTag() : "";
        plugin.getDiscordManager().sendChatMessage(player.getName(), kingdomTag, plainText);
    }

    /**
     * Limpa o canal ativo ao sair do servidor.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getChatManager().removePlayer(e.getPlayer().getUniqueId());
    }

    // ========================================================================
    // Métodos de envio por canal — B9: filtra jogadores ignorados
    // ========================================================================

    private void sendKingdomChat(Player sender, UUID senderUuid, String formattedMsg) {
        Claim kingdom = plugin.getKingdomManager().getKingdom(sender.getUniqueId());
        if (kingdom == null) {
            plugin.getMessageManager().send(sender, "chat.channel_no_kingdom");
            return;
        }

        Set<UUID> recipients = new HashSet<>();
        recipients.add(kingdom.getOwner());
        for (String uuidStr : plugin.getKingdomManager().getSuditosList(kingdom.getId())) {
            try {
                recipients.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {
            }
        }

        ChatManager chatManager = plugin.getChatManager();
        for (UUID uuid : recipients) {
            // B9 — Skip se o receptor ignora o remetente
            if (!uuid.equals(senderUuid) && chatManager.isIgnoring(uuid, senderUuid)) continue;
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(formattedMsg);
            }
        }
    }

    private void sendAllianceChat(Player sender, UUID senderUuid, String formattedMsg) {
        Claim kingdom = plugin.getKingdomManager().getKingdom(sender.getUniqueId());
        if (kingdom == null) {
            plugin.getMessageManager().send(sender, "chat.channel_no_kingdom");
            return;
        }

        List<String> allies = plugin.getKingdomManager().getAlliances(kingdom.getId());
        allies.add(kingdom.getId());

        ChatManager chatManager = plugin.getChatManager();
        for (Player online : Bukkit.getOnlinePlayers()) {
            Claim pk = plugin.getKingdomManager().getKingdom(online.getUniqueId());
            if (pk != null && allies.contains(pk.getId())) {
                // B9 — Skip se o receptor ignora o remetente
                if (!online.getUniqueId().equals(senderUuid)
                        && chatManager.isIgnoring(online.getUniqueId(), senderUuid)) continue;
                online.sendMessage(formattedMsg);
            }
        }
    }

    private void sendLocalChat(Player sender, UUID senderUuid, String formattedMsg, int radius) {
        double radiusSq = (double) radius * radius;
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

    private void sendTradeChat(UUID senderUuid, String formattedMsg) {
        ChatManager chatManager = plugin.getChatManager();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(senderUuid)
                    && chatManager.isIgnoring(online.getUniqueId(), senderUuid)) continue;
            online.sendMessage(formattedMsg);
        }
    }

    private void sendGlobalBroadcast(UUID senderUuid, String formattedMsg) {
        ChatManager chatManager = plugin.getChatManager();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(senderUuid)
                    && chatManager.isIgnoring(online.getUniqueId(), senderUuid)) continue;
            online.sendMessage(formattedMsg);
        }
    }
}
