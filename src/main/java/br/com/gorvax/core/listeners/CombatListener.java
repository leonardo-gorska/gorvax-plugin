package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.CombatManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * B2 — Listener de eventos do Sistema de Combate.
 * Integra Combat Tag, PvP Logger, Kill Streaks e Spawn Protection.
 */
public class CombatListener implements Listener {

    private final GorvaxCore plugin;

    public CombatListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // B2.1 — Combat Tag + B2.4 — Spawn Protection: Dano PvP
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Verificar se é dano PvP (jogador → jogador, incluindo projéteis)
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) return;
        if (attacker.equals(victim)) return; // Auto-dano

        CombatManager combat = plugin.getCombatManager();

        // Verificar spawn protection da vítima — cancelar dano
        if (combat.isSpawnProtected(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Se o atacante tem spawn protection, remover (atacou alguém)
        if (combat.isSpawnProtected(attacker.getUniqueId())) {
            combat.removeSpawnProtection(attacker.getUniqueId());
        }

        // Tagar ambos os jogadores em combate
        combat.tagPlayer(attacker.getUniqueId());
        combat.tagPlayer(victim.getUniqueId());
    }

    // =========================================================================
    // B2.1 — Bloquear comandos durante combat tag
    // =========================================================================

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        CombatManager combat = plugin.getCombatManager();

        if (!combat.isInCombat(player.getUniqueId())) return;

        // Admin bypass
        if (player.hasPermission("gorvax.combat.bypass")) return;

        String message = event.getMessage();
        if (combat.isCommandBlocked(message)) {
            event.setCancelled(true);
            // Extrair o nome do comando para a mensagem
            String cmd = message.startsWith("/") ? message.substring(1) : message;
            cmd = cmd.split("\\s+")[0];
            plugin.getMessageManager().send(player, "combat.command_blocked", cmd);
        }
    }

    // =========================================================================
    // B2.1 — Bloquear abertura de GUIs durante combat tag
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        CombatManager combat = plugin.getCombatManager();

        if (!combat.isInCombat(player.getUniqueId())) return;
        if (!combat.isGuiBlockedInCombat()) return;
        if (player.hasPermission("gorvax.combat.bypass")) return;

        // Permitir inventário próprio (crafting, inventário do jogador)
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Player) return;

        // Bloquear GUIs externas (menus, baús de NPCs, etc.)
        event.setCancelled(true);
        plugin.getMessageManager().send(player, "combat.gui_blocked");
    }

    // =========================================================================
    // B2.2 — PvP Logger: Deslogar em combate
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        CombatManager combat = plugin.getCombatManager();

        if (combat.isInCombat(uuid)) {
            // Spawnar NPC logger
            combat.spawnLoggerNPC(player);
            // Remover combat tag (já que saiu)
            combat.removeCombatTag(uuid);
        } else {
            // Limpar dados do jogador que saiu normalmente
            combat.cleanupPlayer(uuid);
        }
    }

    // =========================================================================
    // B2.2 — PvP Logger: Jogador logar e verificar se foi morto via logger
    // =========================================================================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        CombatManager combat = plugin.getCombatManager();

        if (combat.wasKilledByLogger(uuid)) {
            // O jogador foi morto via NPC logger
            plugin.getMessageManager().send(player, "combat.logger_death");

            // Definir vida para 0 (morrer ao logar) — usar scheduler para evitar NPE no join
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setHealth(0);
                }
            }, 5L);
        }
    }

    // =========================================================================
    // B2.2 — PvP Logger: Morte do NPC
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        UUID entityUUID = entity.getUniqueId();
        CombatManager combat = plugin.getCombatManager();

        if (!combat.isLoggerNPC(entityUUID)) return;

        // Limpar drops padrão do Villager (nós dropamos o inventário do jogador)
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Processar morte do NPC — dropar inventário do jogador
        UUID ownerUUID = combat.getLoggerOwner(entityUUID);
        combat.removeLoggerNPC(entityUUID, true);

        // Broadcast — getKiller() requer LivingEntity (EntityDeathEvent.getEntity() retorna LivingEntity)
        Player killer = event.getEntity().getKiller();
        if (killer != null && ownerUUID != null) {
            String ownerName = plugin.getPlayerName(ownerUUID);
            plugin.getMessageManager().broadcast("combat.logger_killed_broadcast",
                    killer.getName(), ownerName);
        }
    }

    // =========================================================================
    // B2.3 — Kill Streaks + B2.4 — Spawn Protection: Morte de jogador
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        CombatManager combat = plugin.getCombatManager();

        // Resetar kill streak da vítima
        combat.resetKillStreak(victim.getUniqueId());

        // Remover combat tag da vítima
        combat.removeCombatTag(victim.getUniqueId());

        // Se foi morto por jogador, incrementar kill streak do matador
        Player killer = victim.getKiller();
        if (killer != null) {
            combat.incrementKillStreak(killer);
        }
    }

    // =========================================================================
    // B2.4 — Spawn Protection: Aplicar ao respawnar
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        CombatManager combat = plugin.getCombatManager();

        // Aplicar spawn protection com delay para garantir que o jogador já respawnou
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                combat.applySpawnProtection(player.getUniqueId());
            }
        }, 2L);
    }

    // =========================================================================
    // UTIL — Resolver atacante (incluindo projéteis)
    // =========================================================================

    /**
     * Resolve o atacante real a partir de uma entidade (trata projéteis).
     */
    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                return shooter;
            }
        }
        return null;
    }
}
