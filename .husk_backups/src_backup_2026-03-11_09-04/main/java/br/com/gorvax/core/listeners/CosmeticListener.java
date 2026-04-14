package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.CosmeticManager;
import br.com.gorvax.core.managers.CosmeticManager.CosmeticEntry;
import br.com.gorvax.core.managers.CosmeticManager.CosmeticType;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * B13 — Listener de cosméticos: kill effects/particles e arrow trails.
 */
public class CosmeticListener implements Listener {

    private final GorvaxCore plugin;

    public CosmeticListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    // ──────────────── Kill Effects & Particles ────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (victim.getKiller() == null) return;

        Player killer = victim.getKiller();
        CosmeticManager cm = plugin.getCosmeticManager();
        if (cm == null) return;

        Location deathLoc = victim.getLocation();

        // Kill Effect (relâmpago, fogos, explosão)
        CosmeticEntry killEffect = cm.getActiveCosmetic(killer, CosmeticType.KILL_EFFECT);
        if (killEffect != null && killEffect.effect() != null) {
            applyKillEffect(deathLoc, killEffect.effect());
        }

        // Kill Particle
        CosmeticEntry killParticle = cm.getActiveCosmetic(killer, CosmeticType.KILL_PARTICLE);
        if (killParticle != null && killParticle.particle() != null) {
            deathLoc.getWorld().spawnParticle(
                    killParticle.particle(),
                    deathLoc.add(0, 1.0, 0),
                    killParticle.count(),
                    0.5, 0.5, 0.5,
                    0.05
            );
        }
    }

    /**
     * Aplica o efeito visual de kill na localização.
     */
    private void applyKillEffect(Location loc, String effectType) {
        World world = loc.getWorld();
        if (world == null) return;

        switch (effectType.toUpperCase()) {
            case "LIGHTNING" -> {
                // Relâmpago cosmético (sem dano)
                world.strikeLightningEffect(loc);
            }
            case "FIREWORK" -> {
                // Fogos de artifício
                Firework fw = world.spawn(loc, Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.PURPLE, Color.AQUA, Color.YELLOW)
                        .withFade(Color.WHITE)
                        .flicker(true)
                        .trail(true)
                        .build());
                meta.setPower(0); // Explode imediatamente
                fw.setFireworkMeta(meta);
                // Detonar no próximo tick para garantir explosão imediata
                Bukkit.getScheduler().runTaskLater(plugin, fw::detonate, 1L);
            }
            case "EXPLOSION" -> {
                // Explosão visual (partículas + som, sem dano)
                world.spawnParticle(Particle.EXPLOSION, loc.add(0, 1, 0), 1);
                world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
            }
        }
    }

    // ──────────────── Arrow Trail ────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        CosmeticManager cm = plugin.getCosmeticManager();
        if (cm == null) return;

        CosmeticEntry trail = cm.getActiveCosmetic(shooter, CosmeticType.ARROW_TRAIL);
        if (trail == null || trail.particle() == null) return;

        // Task que segue a flecha e spawna partículas
        final Particle trailParticle = trail.particle();
        final int count = trail.count();
        final double speed = trail.speed();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || arrow.isOnGround() || !arrow.isValid()) {
                    cancel();
                    return;
                }

                arrow.getWorld().spawnParticle(
                        trailParticle,
                        arrow.getLocation(),
                        count,
                        0.05, 0.05, 0.05,
                        speed
                );
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
