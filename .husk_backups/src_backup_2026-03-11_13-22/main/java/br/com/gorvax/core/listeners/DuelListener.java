package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.DuelManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;

/**
 * B3 — Listener para eventos relacionados ao sistema de duelos.
 * Gerencia PvP forçado, proteção contra interferência, morte, desconexão e bloqueio de comandos.
 */
public class DuelListener implements Listener {

    private final GorvaxCore plugin;

    public DuelListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Gerencia dano entre jogadores durante duelo.
     * - Se ambos são duelistas do mesmo duelo: permitir PvP forçado
     * - Se apenas um é duelista e o outro é terceiro: cancelar dano
     * - Se duelista em countdown (not started): cancelar
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager == null) return;

        // Resolver atacante (pode ser projétil)
        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        if (!(event.getEntity() instanceof Player victim)) return;

        UUID attackerUUID = attacker.getUniqueId();
        UUID victimUUID = victim.getUniqueId();

        DuelManager.ActiveDuel attackerDuel = duelManager.getActiveDuel(attackerUUID);
        DuelManager.ActiveDuel victimDuel = duelManager.getActiveDuel(victimUUID);

        // Caso 1: Atacante está em duelo
        if (attackerDuel != null) {
            // Duelo não iniciou (countdown) — cancelar qualquer dano
            if (!attackerDuel.started) {
                event.setCancelled(true);
                return;
            }

            // Atacante e vítima estão no mesmo duelo — PvP forçado
            if (attackerDuel == victimDuel) {
                // Forçar PvP (não cancelar mesmo que PvP esteja off no claim)
                event.setCancelled(false);
                return;
            }

            // Atacante está em duelo mas atacando terceiro — bloquear
            event.setCancelled(true);
            return;
        }

        // Caso 2: Vítima está em duelo mas atacante não — proteger duelista
        if (victimDuel != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Quando duelista morre — determina vencedor.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager == null) return;

        Player dead = event.getEntity();
        UUID deadUUID = dead.getUniqueId();

        DuelManager.ActiveDuel duel = duelManager.getActiveDuel(deadUUID);
        if (duel == null || !duel.started) return;

        UUID winnerUUID = duel.getOpponent(deadUUID);

        // Keep inventory para o perdedor
        if (duelManager.isKeepInventory()) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        // Finalizar duelo
        duelManager.endDuel(duel, winnerUUID);
    }

    /**
     * Quando duelista desconecta — o oponente vence.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager == null) return;

        duelManager.handleDisconnect(event.getPlayer().getUniqueId());
    }

    /**
     * Bloqueia comandos durante o duelo (mesma lista de combat.blocked_commands).
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager == null) return;

        Player player = event.getPlayer();
        if (!duelManager.isInDuel(player.getUniqueId())) return;

        // Permitir /duel aceitar e /duel recusar sempre
        String message = event.getMessage().toLowerCase();
        if (message.startsWith("/duel ") || message.equals("/duel")) return;

        // Verificar lista de comandos bloqueados
        List<String> blocked = plugin.getConfig().getStringList("combat.blocked_commands");
        String command = message.split(" ")[0].substring(1); // Remove '/'

        for (String blockedCmd : blocked) {
            if (command.equalsIgnoreCase(blockedCmd)) {
                event.setCancelled(true);
                plugin.getMessageManager().send(player, "duel.blocked_command", command);
                return;
            }
        }
    }

    /**
     * Bloqueia abertura de GUIs durante o duelo.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager == null) return;

        if (!(event.getPlayer() instanceof Player player)) return;

        if (duelManager.isInDuel(player.getUniqueId())) {
            boolean blockGui = plugin.getConfig().getBoolean("combat.block_gui_in_combat", true);
            if (blockGui) {
                event.setCancelled(true);
                plugin.getMessageManager().send(player, "duel.gui_blocked");
            }
        }
    }

    /**
     * Resolve o atacante real (lida com projéteis).
     */
    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) {
            return p;
        }
        if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }
}
