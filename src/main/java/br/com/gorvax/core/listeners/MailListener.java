package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * B17.1 — Notifica jogadores sobre cartas não lidas ao entrar no servidor.
 */
public class MailListener implements Listener {

    private final GorvaxCore plugin;

    public MailListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay de 3 segundos para não sobrecarregar o jogador ao logar
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (plugin.getMailManager() == null) return;

            int unread = plugin.getMailManager().getUnreadCount(player.getUniqueId());
            if (unread > 0) {
                plugin.getMessageManager().send(player, "mail.login_notification", unread);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
            }
        }, 60L); // 3 segundos (60 ticks)
    }
}
