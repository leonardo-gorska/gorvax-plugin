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
 * Zar'ith, a Presa da Selva — Boss Tier 2 (mais fraco).
 * Entidade: Spider. Tema: Veneno e Agilidade.
 * Evento Global: "Crepúsculo Tóxico" (Céu esverdeado, partículas venenosas).
 * Lore: Antiga guardiã das selvas que serviam de fronteira entre o Reino de
 * Gorvax e o Abismo de Indrax.
 */
public class Zarith extends WorldBoss {

    private long lastSpecialAttack = 0;
    private int tickCounter = 0;

    private final List<Entity> summons = new ArrayList<>();
    private final java.util.Map<UUID, Long> playerMusicTracker = new ConcurrentHashMap<>();

    private boolean summoned75 = false;
    private boolean summoned50 = false;
    private boolean summoned25 = false;

    private static final Random RANDOM = new Random();

    public Zarith() {
        super(
                "zarith",
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getString("bosses.zarith.nome", "§a§lZar'ith, a Presa da Selva")
                        .replace("&", "§"),
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getDouble("bosses.zarith.hp", 450.0));
    }

    private FileConfiguration getConfig() {
        return GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings();
    }

    // ================= SPAWN =================

    @Override
    public void spawn(Location loc) {
        this.entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.SPIDER);
        entity.setRemoveWhenFarAway(false);
        if (entity instanceof Mob mob)
            mob.setAware(true);

        double damage = getConfig().getDouble("bosses.zarith.normal_damage", 9.0);

        applySafeMaxHealth(entity, this.maxHealth);
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        }

        // Escala visual (80% maior — aranha gigante)
        double scale = getConfig().getDouble("bosses.zarith.visual_effects.scale", 1.8);
        if (entity.getAttribute(Attribute.GENERIC_SCALE) != null) {
            entity.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
        }

        // Velocidade aumentada (aranha ágil)
        double speed = getConfig().getDouble("bosses.zarith.movement_speed", 0.38);
        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        }

        // HP já setado por applySafeMaxHealth
        entity.customName(LegacyComponentSerializer.legacySection().deserialize(this.name));
        entity.setCustomNameVisible(true);

        // Efeitos de spawn
        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));

        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                "§a§lZAR'ITH §8» §fA selva sussurra meu nome... e a presa da caçadora será vossa perdição!"));

        // Partículas de veneno no spawn
        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.add(0, 1, 0), 50, 2.0, 1.0, 2.0, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_SPIDER_AMBIENT, 3.0f, 0.5f);

        // Garante exibição imediata da BossBar
        updateBossBar();
    }

    // ================= UPDATE =================

    @Override
    public void update() {
        if (entity == null || entity.isDead() || !entity.isValid())
            return;

        tickCounter++;

        // Auras constantes (throttled a cada 4 ticks)
        if (tickCounter % 4 == 0) {
            spawnAuraParticles();
        }

        double hp = entity.getHealth() / maxHealth;

        // Fases são gerenciadas pelo WorldBoss.updateBossBar()

        // Invocação de minions por threshold
        int count75 = getConfig().getInt("bosses.zarith.skills.minions.count_75", 3);
        int count50 = getConfig().getInt("bosses.zarith.skills.minions.count_50", 4);
        int count25 = getConfig().getInt("bosses.zarith.skills.minions.count_25", 6);

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

        handleLocalMusic();
        handleAITargeting();
        handleAntiKite();

        long now = System.currentTimeMillis();

        long cooldown = getConfig().getLong("bosses.zarith.skills.cooldown_base", 7000L);
        if (phase >= 3)
            cooldown = getConfig().getLong("bosses.zarith.skills.cooldown_rage", 4500L);

        if (now - lastSpecialAttack > cooldown) {
            playSpecialAttack();
            lastSpecialAttack = now;
        }
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
                    p.playSound(entity, Sound.MUSIC_DISC_WARD, 3.75f, 1.0f);
                    playerMusicTracker.put(uuid, now);
                }
            } else {
                if (playerMusicTracker.containsKey(uuid)) {
                    p.stopSound(Sound.MUSIC_DISC_WARD);
                    playerMusicTracker.remove(uuid);
                }
            }
        }
    }

    // ================= FASES =================

    @Override
    public void onPhaseChange(int phase) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 3f, 0.3f);
        switch (phase) {
            case 2 -> {
                say("§a§lZAR'ITH §8» §7Vocês despertaram a fúria da selva...");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            }
            case 3 -> {
                say("§a§lZAR'ITH §8» §cA presa da caçadora se aprofunda! Não há escapatória!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
            }
            case 4 -> {
                say("§a§lZAR'ITH §8» §4§lFRENESI! A SELVA DEVORA TUDO!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
                entity.getWorld().strikeLightningEffect(entity.getLocation());
            }
        }
    }

    // ================= ATAQUES =================

    @Override
    public void playSpecialAttack() {
        double r = Math.random();
        if (r < 0.20)
            executePoisonWeb();
        else if (r < 0.40)
            executePredatorLeap();
        else if (r < 0.60)
            executeAmbush();
        else if (r < 0.80)
            executeToxicBurst();
        else
            executeFrenzy();
    }

    /**
     * Teia Venenosa — Aplica Poison II + Slowness em área.
     */
    private void executePoisonWeb() {
        say("§a§lZAR'ITH §8» §2Sintam o veneno da selva!");
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 2.5f, 0.6f);

        double radius = getConfig().getDouble("bosses.zarith.skills.poison_web.raio", 10.0);
        int poisonDuration = getConfig().getInt("bosses.zarith.skills.poison_web.poison_duration", 100);
        int slowDuration = getConfig().getInt("bosses.zarith.skills.poison_web.slow_duration", 80);
        double damage = getConfig().getDouble("bosses.zarith.skills.poison_web.dano", 4.0);

        // Partículas de teia/veneno
        entity.getWorld().spawnParticle(Particle.ITEM_SLIME, entity.getLocation().add(0, 1, 0), 30, 2.0, 1.0, 2.0, 0.1);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, 1));
                p.spawnParticle(Particle.ITEM_SLIME,
                        p.getLocation().add((RANDOM.nextDouble() - 0.5) * 15, 5, (RANDOM.nextDouble() - 0.5) * 15),
                        6, 1.5, 0.5, 1.5, 0.03);
            }
        });
    }

    /**
     * Salto Predador — Pula no alvo com dano + knockback.
     */
    private void executePredatorLeap() {
        say("§a§lZAR'ITH §8» §ePRESA LOCALIZADA!");

        // Encontrar alvo
        Player target = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Entity e : entity.getNearbyEntities(25, 25, 25)) {
            if (e instanceof Player p && p.getGameMode() == GameMode.SURVIVAL && !p.isDead()) {
                double dmg = damageDealt.getOrDefault(p.getUniqueId(), 0.0);
                if (dmg > bestScore) {
                    bestScore = dmg;
                    target = p;
                }
            }
        }
        if (target == null)
            return;

        Player finalTarget = target;

        // Salto na direção do alvo
        Vector dir = target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
        dir.multiply(1.8);
        dir.setY(0.8);
        entity.setVelocity(dir);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SPIDER_STEP, 2.0f, 0.3f);
        entity.getWorld().spawnParticle(Particle.ITEM_SLIME, entity.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);

        Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
            if (entity == null || entity.isDead())
                return;

            double damage = getConfig().getDouble("bosses.zarith.skills.predator_leap.dano", 8.0);
            double radius = getConfig().getDouble("bosses.zarith.skills.predator_leap.raio", 5.0);
            double knockback = getConfig().getDouble("bosses.zarith.skills.predator_leap.knockback", 1.5);

            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
            entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 5);

            entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
                if (e instanceof Player p) {
                    p.damage(damage, entity);
                    Vector kb = p.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize()
                            .multiply(knockback);
                    kb.setY(0.4);
                    p.setVelocity(kb);
                }
            });

            setTeleportLock(finalTarget.getUniqueId(), 3000L);
        }, 12L);
    }

    /**
     * Emboscada — Teleporte atrás do jogador (similar ao Gorvax).
     */
    private void executeAmbush() {
        say("§a§lZAR'ITH §8» §cNa selva, a presa nunca vê o predador...");

        int range = getConfig().getInt("bosses.zarith.skills.ambush.range", 20);
        long focusDuration = getConfig().getLong("bosses.zarith.skills.ambush.focus_duration_ms", 4000L);

        for (Entity e : entity.getNearbyEntities(range, range, range)) {
            if (e instanceof Player p) {
                Vector dir = p.getLocation().getDirection().normalize().multiply(-1.5);
                Location behind = p.getLocation().add(dir);
                behind.setY(p.getLocation().getY());

                entity.getWorld().spawnParticle(Particle.ITEM_SLIME, entity.getLocation().add(0, 1, 0), 20, 0.5, 0.5,
                        0.5, 0.1);
                entity.teleport(behind);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 1.2f);
                entity.getWorld().spawnParticle(Particle.ITEM_SLIME, entity.getLocation(), 30, 0.5, 1.0, 0.5, 0.1);

                // Aplica veneno no alvo surpreso
                double damage = getConfig().getDouble("bosses.zarith.skills.ambush.dano", 6.0);
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1));
                p.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_dialogue.zarith_prey"));

                setTeleportLock(p.getUniqueId(), focusDuration);
                break;
            }
        }
    }

    /**
     * Explosão Tóxica — Explosão de veneno em área com dano e Wither.
     */
    private void executeToxicBurst() {
        say("§a§lZAR'ITH §8» §2§lTOXICIDADE MÁXIMA!");

        double radius = getConfig().getDouble("bosses.zarith.skills.toxic_burst.raio", 12.0);
        double damage = getConfig().getDouble("bosses.zarith.skills.toxic_burst.dano", 6.0);
        int witherDuration = getConfig().getInt("bosses.zarith.skills.toxic_burst.wither_duration", 80);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SLIME_SQUISH, 2.5f, 0.5f);

        // Onda de veneno expandindo
        Location center = entity.getLocation();
        for (double angle = 0; angle < 360; angle += 15) {
            double radians = Math.toRadians(angle);
            double x = center.getX() + radius * 0.5 * Math.cos(radians);
            double z = center.getZ() + radius * 0.5 * Math.sin(radians);
            entity.getWorld().spawnParticle(Particle.ITEM_SLIME,
                    new Location(center.getWorld(), x, center.getY() + 0.5, z), 5, 0.2, 0.3, 0.2, 0.02);
        }

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherDuration, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0));
                p.spawnParticle(Particle.ITEM_SLIME, p.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
            }
        });

        notifyAlly("ToxicBurst");
    }

    /**
     * Frenesi — Buff temporário massivo (Speed III + Strength I).
     */
    private void executeFrenzy() {
        say("§a§lZAR'ITH §8» §c§lA CAÇADORA ENLOUQUECEU!");

        int duration = getConfig().getInt("bosses.zarith.skills.frenzy.duracao_ticks", 120);
        int speedLevel = getConfig().getInt("bosses.zarith.skills.frenzy.speed_level", 2);
        int strengthLevel = getConfig().getInt("bosses.zarith.skills.frenzy.strength_level", 0);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, speedLevel));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, strengthLevel));

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 3.0f, 0.3f);
        entity.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, entity.getLocation().add(0, 1.5, 0), 10, 0.5, 0.5,
                0.5, 0.1);
    }

    // ================= MINIONS =================

    private void spawnMinions(int amount) {
        summons.removeIf(e -> !e.isValid() || e.isDead());
        say("§a§lZAR'ITH §8» §2Filhotes, ataquem!");

        for (int i = 0; i < amount; i++) {
            Location l = entity.getLocation().add((Math.random() - 0.5) * 10, 0, (Math.random() - 0.5) * 10);
            CaveSpider spider = (CaveSpider) l.getWorld().spawnEntity(l, EntityType.CAVE_SPIDER);
            spider.customName(LegacyComponentSerializer.legacySection().deserialize("§2§lCria Tóxica"));
            spider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
            spider.setMetadata("zarith_minion",
                    new org.bukkit.metadata.FixedMetadataValue(GorvaxCore.getInstance(), true));
            summons.add(spider);
        }
    }

    // ================= AURA =================

    private void spawnAuraParticles() {
        if (entity == null || entity.isDead())
            return;

        Location loc = entity.getLocation().add(0, 0.8, 0);
        int slimeCount = getConfig().getInt("bosses.zarith.visual_effects.aura.slime", 8);
        int smokeCount = getConfig().getInt("bosses.zarith.visual_effects.aura.smoke", 4);

        for (Entity e : entity.getNearbyEntities(40, 40, 40)) {
            if (e instanceof Player p) {
                p.spawnParticle(Particle.ITEM_SLIME, loc, slimeCount, 0.6, 0.4, 0.6, 0.03);
                p.spawnParticle(Particle.SMOKE, loc, smokeCount, 0.4, 0.3, 0.4, 0.01);
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
            p.stopSound(Sound.MUSIC_DISC_WARD);
        }
        playerMusicTracker.clear();
    }
}
