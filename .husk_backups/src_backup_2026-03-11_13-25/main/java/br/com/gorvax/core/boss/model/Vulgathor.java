package br.com.gorvax.core.boss.model;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vulgathor, o Arauto das Cinzas — Boss Tier 2 (mais forte do T2).
 * Entidade: Blaze. Tema: Fogo e Cinzas.
 * Evento Global: "Onda de Calor" (Céu avermelhado, partículas de fumaça +
 * chamas).
 * Lore: Antigo Arauto do Rei Gorvax, forjado nas chamas do trono eterno para
 * servir como vanguarda da aliança.
 */
public class Vulgathor extends WorldBoss {

    private long lastSpecialAttack = 0;
    private int tickCounter = 0;

    private final List<Entity> summons = new ArrayList<>();
    private final java.util.Map<UUID, Long> playerMusicTracker = new ConcurrentHashMap<>();

    private boolean summoned75 = false;
    private boolean summoned50 = false;
    private boolean summoned25 = false;

    private static final Random RANDOM = new Random();

    public Vulgathor() {
        super(
                "vulgathor",
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getString("bosses.vulgathor.nome", "§c§lVulgathor, o Arauto das Cinzas")
                        .replace("&", "§"),
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getDouble("bosses.vulgathor.hp", 900.0));
    }

    private FileConfiguration getConfig() {
        return GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings();
    }

    // ================= SPAWN =================

    @Override
    public void spawn(Location loc) {
        this.entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.BLAZE);
        entity.setRemoveWhenFarAway(false);
        if (entity instanceof Mob mob)
            mob.setPersistent(true);

        double damage = getConfig().getDouble("bosses.vulgathor.normal_damage", 14.0);

        applySafeMaxHealth(entity, this.maxHealth);
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        }

        // Escala visual (80% maior — blaze gigante)
        double scale = getConfig().getDouble("bosses.vulgathor.visual_effects.scale", 1.8);
        if (entity.getAttribute(Attribute.GENERIC_SCALE) != null) {
            entity.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
        }

        double speed = getConfig().getDouble("bosses.vulgathor.movement_speed", 0.32);
        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        }

        // HP já setado por applySafeMaxHealth
        entity.customName(LegacyComponentSerializer.legacySection().deserialize(this.name));
        entity.setCustomNameVisible(true);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));

        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                "§c§lVULGATHOR §8» §fDas cinzas do trono eterno, eu retorno. Que as chamas consumam os indignos!"));

        loc.getWorld().spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 80, 3, 2, 3, 0.1);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 30, 2, 1, 2, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 3.0f, 0.3f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);

        // Garante exibição imediata da BossBar
        updateBossBar();
    }

    // ================= UPDATE =================

    @Override
    public void update() {
        if (entity == null || entity.isDead() || !entity.isValid())
            return;

        tickCounter++;

        if (tickCounter % 4 == 0)
            spawnAuraParticles();

        double hp = entity.getHealth() / maxHealth;

        // Fases são gerenciadas pelo WorldBoss.updateBossBar()

        int count75 = getConfig().getInt("bosses.vulgathor.skills.minions.count_75", 2);
        int count50 = getConfig().getInt("bosses.vulgathor.skills.minions.count_50", 4);
        int count25 = getConfig().getInt("bosses.vulgathor.skills.minions.count_25", 6);

        if (hp <= 0.75 && !summoned75) {
            spawnMinions(count75);
            summoned75 = true;
        }
        if (hp <= 0.50 && !summoned50) {
            spawnMinions(count50);
            summoned50 = true;
        }
        if (hp <= 0.25 && !summoned25) {
            spawnMinions(count25);
            summoned25 = true;
        }

        // Dano de fogo passivo a cada 2 segundos
        if (tickCounter % 40 == 0) {
            handleFireAura();
        }

        handleLocalMusic();
        handleAITargeting();
        handleAntiKite();

        long now = System.currentTimeMillis();
        long cooldown = getConfig().getLong("bosses.vulgathor.skills.cooldown_base", 7000L);
        if (phase >= 3)
            cooldown = getConfig().getLong("bosses.vulgathor.skills.cooldown_rage", 4500L);

        if (now - lastSpecialAttack > cooldown) {
            playSpecialAttack();
            lastSpecialAttack = now;
        }
    }

    /**
     * Aura de fogo passiva — incendeia jogadores próximos.
     */
    private void handleFireAura() {
        double radius = getConfig().getDouble("bosses.vulgathor.skills.fire_aura.raio", 6.0);
        double damage = getConfig().getDouble("bosses.vulgathor.skills.fire_aura.dano", 3.0);
        int fireTicks = getConfig().getInt("bosses.vulgathor.skills.fire_aura.fire_ticks", 60);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage);
                p.setFireTicks(fireTicks);
                p.spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
            }
        });
    }

    private void handleLocalMusic() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(entity.getWorld()))
                continue;
            double distSq = p.getLocation().distanceSquared(entity.getLocation());
            UUID uuid = p.getUniqueId();
            if (distSq <= 3600) {
                if (!playerMusicTracker.containsKey(uuid) || (now - playerMusicTracker.get(uuid) > 190000)) {
                    p.playSound(entity.getLocation(), Sound.MUSIC_DISC_BLOCKS, 0.5f, 0.8f);
                    playerMusicTracker.put(uuid, now);
                }
            } else {
                if (playerMusicTracker.containsKey(uuid)) {
                    p.stopSound(Sound.MUSIC_DISC_BLOCKS);
                    playerMusicTracker.remove(uuid);
                }
            }
        }
    }

    // ================= FASES =================

    @Override
    public void onPhaseChange(int phase) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_DEATH, 3f, 0.3f);
        switch (phase) {
            case 2 -> {
                say("§c§lVULGATHOR §8» §7As cinzas se agitam... a chama do Arauto cresce!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
            }
            case 3 -> {
                say("§c§lVULGATHOR §8» §cO fogo consome tudo! A vontade do Rei Gorvax se manifesta!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            }
            case 4 -> {
                say("§c§lVULGATHOR §8» §4§lFÚRIA ÍGNEA! O ARAUTO ARDE COM A FÚRIA DO TRONO!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));

                // Explosão de fogo na transição
                entity.getWorld().strikeLightningEffect(entity.getLocation());
                entity.getWorld().createExplosion(entity.getLocation(), 0, false, false);
                entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), 100, 3, 3, 3, 0.2);
                entity.getWorld().spawnParticle(Particle.LAVA, entity.getLocation(), 30, 2, 2, 2, 0.1);
            }
        }
    }

    // ================= ATAQUES =================

    @Override
    public void playSpecialAttack() {
        double r = Math.random();
        if (r < 0.20)
            executeIncendiaryRain();
        else if (r < 0.40)
            executeAshExplosion();
        else if (r < 0.60)
            executeIgneousFury();
        else if (r < 0.80)
            executeMagmaPillar();
        else
            executeInferno();
    }

    /**
     * Chuva Incandescente — Fireballs em área massiva.
     */
    private void executeIncendiaryRain() {
        say("§c§lVULGATHOR §8» §eCHUVA DE FOGO!");

        double radius = getConfig().getDouble("bosses.vulgathor.skills.incendiary_rain.raio", 14.0);
        int fireballCount = getConfig().getInt("bosses.vulgathor.skills.incendiary_rain.count", 8);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3.0f, 0.3f);

        Location center = entity.getLocation();

        for (int i = 0; i < fireballCount; i++) {
            Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
                if (entity == null || entity.isDead())
                    return;

                double x = center.getX() + (RANDOM.nextDouble() - 0.5) * radius * 2;
                double z = center.getZ() + (RANDOM.nextDouble() - 0.5) * radius * 2;
                Location target = new Location(center.getWorld(), x, center.getY() + 15, z);

                SmallFireball fb = center.getWorld().spawn(target, SmallFireball.class);
                fb.setDirection(new Vector(0, -1, 0));
                fb.setShooter(entity);
                fb.setIsIncendiary(false);
                fb.setYield(0);

                center.getWorld().spawnParticle(Particle.FLAME,
                        new Location(center.getWorld(), x, center.getY() + 12, z), 5, 0.5, 0.5, 0.5, 0.05);
            }, i * 4L);
        }

        // Dano residual em área
        Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
            if (entity == null || entity.isDead())
                return;
            entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
                if (e instanceof Player p) {
                    double damage = getConfig().getDouble("bosses.vulgathor.skills.incendiary_rain.dano", 5.0);
                    p.setFireTicks(100);
                    p.damage(damage, entity);
                }
            });
        }, fireballCount * 4L + 10L);
    }

    /**
     * Explosão de Cinzas — Onda de choque com FLAME + SMOKE, dano + Blindness.
     */
    private void executeAshExplosion() {
        say("§c§lVULGATHOR §8» §8CINZAS DO TRONO!");

        double radius = getConfig().getDouble("bosses.vulgathor.skills.ash_explosion.raio", 10.0);
        double damage = getConfig().getDouble("bosses.vulgathor.skills.ash_explosion.dano", 8.0);
        int blindnessDuration = getConfig().getInt("bosses.vulgathor.skills.ash_explosion.blindness_duration", 60);
        double knockback = getConfig().getDouble("bosses.vulgathor.skills.ash_explosion.knockback", 1.8);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.5f);

        // Onda visual expandindo
        Location center = entity.getLocation();
        for (double r = 1; r <= radius; r += 0.5) {
            double finalR = r;
            Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
                for (double angle = 0; angle < 360; angle += 15) {
                    double radians = Math.toRadians(angle);
                    double x = center.getX() + finalR * Math.cos(radians);
                    double z = center.getZ() + finalR * Math.sin(radians);
                    entity.getWorld().spawnParticle(Particle.FLAME,
                            new Location(center.getWorld(), x, center.getY() + 0.5, z), 2, 0, 0, 0, 0.02);
                    entity.getWorld().spawnParticle(Particle.SMOKE,
                            new Location(center.getWorld(), x, center.getY() + 0.5, z), 3, 0, 0, 0, 0.01);
                }
            }, (long) (r * 2));
        }

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDuration, 0));
                p.setFireTicks(80);
                Vector kb = p.getLocation().toVector().subtract(center.toVector()).normalize().multiply(knockback);
                kb.setY(0.5);
                p.setVelocity(kb);
            }
        });

        notifyAlly("AshExplosion");
    }

    /**
     * Fúria Ígnea — Buff de Strength II + Fire Resistance no boss.
     */
    private void executeIgneousFury() {
        say("§c§lVULGATHOR §8» §4A CHAMA DO TRONO ME FORTALECE!");

        int duration = getConfig().getInt("bosses.vulgathor.skills.igneous_fury.duracao_ticks", 160);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 1));

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 3.0f, 0.3f);
        entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1.5, 0), 50, 1, 1.5, 1, 0.08);
        entity.getWorld().spawnParticle(Particle.LAVA, entity.getLocation(), 20, 1, 1, 1, 0.05);
    }

    /**
     * Pilar de Magma — Cria partículas de magma vertical, dano + fire + slowness.
     */
    private void executeMagmaPillar() {
        say("§c§lVULGATHOR §8» §ePILAR DE MAGMA!");

        // Mira no jogador que mais causou dano
        Player target = null;
        double bestDmg = 0;
        for (Entity e : entity.getNearbyEntities(25, 25, 25)) {
            if (e instanceof Player p && p.getGameMode() == GameMode.SURVIVAL) {
                double dmg = damageDealt.getOrDefault(p.getUniqueId(), 0.0);
                if (dmg > bestDmg) {
                    bestDmg = dmg;
                    target = p;
                }
            }
        }
        if (target == null)
            return;

        Location pillarLoc = target.getLocation().clone();
        double pillarDamage = getConfig().getDouble("bosses.vulgathor.skills.magma_pillar.dano", 7.0);
        double pillarRadius = getConfig().getDouble("bosses.vulgathor.skills.magma_pillar.raio", 4.0);

        entity.getWorld().playSound(pillarLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);

        // Pilar de partículas (visual ascendente)
        for (int y = 0; y < 10; y++) {
            int finalY = y;
            Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
                entity.getWorld().spawnParticle(Particle.FLAME,
                        pillarLoc.clone().add(0, finalY, 0), 15, 0.8, 0.2, 0.8, 0.05);
                entity.getWorld().spawnParticle(Particle.LAVA,
                        pillarLoc.clone().add(0, finalY, 0), 5, 0.5, 0.2, 0.5, 0.02);
            }, y * 2L);
        }

        // Dano no impacto
        Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
            pillarLoc.getWorld().getNearbyEntities(pillarLoc, pillarRadius, pillarRadius, pillarRadius).forEach(e -> {
                if (e instanceof Player p) {
                    p.damage(pillarDamage, entity);
                    p.setFireTicks(100);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                }
            });
        }, 10L);
    }

    /**
     * Inferno — Explosão massiva de fogo. Ataque mais forte.
     */
    private void executeInferno() {
        say("§c§lVULGATHOR §8» §4§lINFERNO! QUE O FOGO CONSUMA TUDO!");

        double radius = getConfig().getDouble("bosses.vulgathor.skills.inferno.raio", 16.0);
        double damage = getConfig().getDouble("bosses.vulgathor.skills.inferno.dano", 10.0);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.5f, 0.3f);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);

        // Explosão visual massiva
        entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), 200, 5, 3, 5, 0.2);
        entity.getWorld().spawnParticle(Particle.LAVA, entity.getLocation(), 50, 4, 2, 4, 0.1);
        entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation(), 100, 5, 3, 5, 0.1);

        // Múltiplas ondas de dano
        for (int i = 0; i < 3; i++) {
            int wave = i;
            Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
                if (entity == null || entity.isDead())
                    return;

                entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
                    if (e instanceof Player p) {
                        p.damage(damage / 3, entity);
                        p.setFireTicks(120);
                        p.spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.05);
                    }
                });
                entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), 50, 4, 2, 4, 0.1);
            }, (wave + 1) * 15L);
        }
    }

    // ================= MINIONS =================

    private void spawnMinions(int amount) {
        summons.removeIf(e -> !e.isValid() || e.isDead());
        say("§c§lVULGATHOR §8» §eFlamas eternas, sirvam o Arauto!");

        for (int i = 0; i < amount; i++) {
            Location l = entity.getLocation().add((Math.random() - 0.5) * 10, 2, (Math.random() - 0.5) * 10);
            Blaze blaze = (Blaze) l.getWorld().spawnEntity(l, EntityType.BLAZE);
            blaze.customName(LegacyComponentSerializer.legacySection().deserialize("§c§lFlama do Arauto"));
            blaze.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            blaze.setMetadata("vulgathor_minion",
                    new org.bukkit.metadata.FixedMetadataValue(GorvaxCore.getInstance(), true));
            summons.add(blaze);

            l.getWorld().spawnParticle(Particle.FLAME, l, 15, 0.3, 0.5, 0.3, 0.05);
        }
    }

    // ================= AURA =================

    private void spawnAuraParticles() {
        if (entity == null || entity.isDead())
            return;

        Location loc = entity.getLocation().add(0, 1.5, 0);
        int flameCount = getConfig().getInt("bosses.vulgathor.visual_effects.aura.flame", 12);
        int smokeCount = getConfig().getInt("bosses.vulgathor.visual_effects.aura.smoke", 6);
        int lavaCount = getConfig().getInt("bosses.vulgathor.visual_effects.aura.lava", 3);

        for (Entity e : entity.getNearbyEntities(40, 40, 40)) {
            if (e instanceof Player p) {
                p.spawnParticle(Particle.FLAME, loc, flameCount, 0.8, 1.0, 0.8, 0.04);
                p.spawnParticle(Particle.SMOKE, loc, smokeCount, 0.5, 0.8, 0.5, 0.02);
                p.spawnParticle(Particle.LAVA, loc, lavaCount, 0.3, 0.5, 0.3, 0.01);
            }
        }
    }

    // ================= CLEANUP =================

    @Override
    public void cleanup() {
        summons.forEach(e -> {
            if (e != null && e.isValid())
                e.remove();
        });
        summons.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.stopSound(Sound.MUSIC_DISC_BLOCKS);
        }
        playerMusicTracker.clear();
    }
}
