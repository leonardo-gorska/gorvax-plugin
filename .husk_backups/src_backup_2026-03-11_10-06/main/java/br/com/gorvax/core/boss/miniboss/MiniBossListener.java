package br.com.gorvax.core.boss.miniboss;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.UUID;

/**
 * Listener de eventos para o sistema de Mini-Bosses (B11).
 * Trata dano, morte e remoção de entidades.
 */
public class MiniBossListener implements Listener {

    private final GorvaxCore plugin;

    public MiniBossListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Registra dano causado por jogadores em mini-bosses
     * e aplica efeitos on-hit nos jogadores.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity victim)) return;

        MiniBossManager manager = plugin.getMiniBossManager();
        if (manager == null) return;

        MiniBoss miniBoss = manager.getByEntity(victim);
        if (miniBoss == null) return;

        // Identificar o atacante (pode ser projétil)
        Player attacker = resolveAttacker(e.getDamager());
        if (attacker == null) return;

        double damage = e.getFinalDamage();
        miniBoss.addDamage(attacker.getUniqueId(), damage);

        // Aplicar efeitos on-hit no jogador
        for (MiniBoss.EffectOnHit effect : miniBoss.getEffectsOnHit()) {
            attacker.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier()));
        }
    }

    /**
     * Trata a morte de um mini-boss: distribui recompensas e loot.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();

        MiniBossManager manager = plugin.getMiniBossManager();
        if (manager == null) return;

        // Verificar se é minion (sem loot especial)
        if (manager.isMinion(entity)) return;

        MiniBoss miniBoss = manager.getByEntity(entity);
        if (miniBoss == null) return;

        // Cancelar drops vanilla
        e.getDrops().clear();
        e.setDroppedExp(0);

        // Encontrar o top damager
        UUID topDamager = miniBoss.getTopDamager();
        Player killer = topDamager != null ? Bukkit.getPlayer(topDamager) : null;

        // Se ninguém deu dano significativo, usar o que o evento indica
        if (killer == null) {
            killer = entity.getKiller();
        }

        String killerName = killer != null ? killer.getName() : "Desconhecido";

        // Broadcast de morte (100 blocos)
        String deathMsg = miniBoss.getDeathMessage().replace("{0}", killerName);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(entity.getWorld())
                    && p.getLocation().distanceSquared(entity.getLocation()) <= 10000) {
                p.sendMessage(deathMsg);
            }
        }

        // Distribuir recompensas para todos que causaram dano
        distributeRewards(miniBoss);

        // Dropar loot no chão
        dropLoot(miniBoss);

        // Dar XP ao killer
        if (killer != null) {
            killer.giveExp(miniBoss.getXpReward());
        }

        // Remover do manager
        manager.removeMiniBoss(miniBoss.getId());
    }

    /**
     * Remove mini-bosses que saem do chunk carregado.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent e) {
        MiniBossManager manager = plugin.getMiniBossManager();
        if (manager == null) return;

        for (Entity entity : e.getEntities()) {
            if (entity instanceof LivingEntity living) {
                MiniBoss miniBoss = manager.getByEntity(living);
                if (miniBoss != null) {
                    manager.removeMiniBoss(miniBoss.getId());
                }
            }
        }
    }

    // ================= UTILIDADES =================

    /**
     * Resolve o atacante real (trata projéteis).
     */
    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    /**
     * Distribui recompensas em dinheiro para todos os participantes.
     */
    private void distributeRewards(MiniBoss miniBoss) {
        double totalDamage = miniBoss.getDamageDealt().values().stream().mapToDouble(d -> d).sum();
        if (totalDamage <= 0) return;

        var msg = plugin.getMessageManager();

        for (var entry : miniBoss.getDamageDealt().entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;

            // Recompensa proporcional ao dano causado
            double proportion = entry.getValue() / totalDamage;
            double reward = miniBoss.getMoneyReward() * proportion;

            if (reward < 1.0) reward = 1.0; // Mínimo de $1

            GorvaxCore.getEconomy().depositPlayer(p, reward);
            msg.send(p, "miniboss.reward_money", String.format("%.2f", reward));
        }
    }

    /**
     * Dropa itens de loot na posição do mini-boss morto.
     */
    private void dropLoot(MiniBoss miniBoss) {
        if (!miniBoss.isAlive() && miniBoss.getEntity() != null) {
            // A entidade já morreu, usar última localização
            org.bukkit.Location loc = miniBoss.getEntity().getLocation();
            dropLootAt(miniBoss, loc);
        }
    }

    private void dropLootAt(MiniBoss miniBoss, org.bukkit.Location loc) {
        for (MiniBoss.LootEntry entry : miniBoss.getLoot()) {
            if (Math.random() <= entry.chance()) {
                ItemStack item = new ItemStack(entry.material(), entry.amount());
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }
    }
}
