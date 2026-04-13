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
 * Xylos, o Devorador de Éter — Boss Tier 2 (2º mais forte).
 * Entidade: Enderman. Tema: Distorção Espacial e Éter.
 * Evento Global: "Eclipse de Partículas" (Céu roxo escuro, partículas
 * REVERSE_PORTAL).
 * Lore: Servo do Éter que patrulhava as fendas dimensionais entre o mundo
 * mortal e o Abismo de Indrax.
 */
public class XylosDevorador extends WorldBoss {

    private long lastSpecialAttack = 0;
    private int tickCounter = 0;

    private final List<Entity> summons = new ArrayList<>();
    private final java.util.Map<UUID, Long> playerMusicTracker = new ConcurrentHashMap<>();

    private boolean summoned75 = false;
    private boolean summoned50 = false;
    private boolean summoned25 = false;

    private static final Random RANDOM = new Random();

    public XylosDevorador() {
        super(
                "xylos",
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getString("bosses.xylos.nome", "§5§lXylos, o Devorador de Éter")
                        .replace("&", "§"),
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getDouble("bosses.xylos.hp", 750.0));
    }

    private FileConfiguration getConfig() {
        return GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings();
    }

    // ================= SPAWN =================

    @Override
    public void spawn(Location loc) {
        this.entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.ENDERMAN);
        entity.setRemoveWhenFarAway(false);
        if (entity instanceof Mob mob)
            mob.setPersistent(true);

        double damage = getConfig().getDouble("bosses.xylos.normal_damage", 12.0);

        applySafeMaxHealth(entity, this.maxHealth);
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        }

        // Escala visual (100% maior — Enderman gigante)
        double scale = getConfig().getDouble("bosses.xylos.visual_effects.scale", 2.0);
        if (entity.getAttribute(Attribute.GENERIC_SCALE) != null) {
            entity.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
        }

        double speed = getConfig().getDouble("bosses.xylos.movement_speed", 0.35);
        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        }

        // HP já setado por applySafeMaxHealth
        entity.customName(LegacyComponentSerializer.legacySection().deserialize(this.name));
        entity.setCustomNameVisible(true);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));

        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                "§5§lXYLOS §8» §dO Éter sangra... e de suas feridas, eu nasço. Sintam a distorção da realidade!"));

        loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.add(0, 1, 0), 80, 2, 2, 2, 0.1);
        loc.getWorld().spawnParticle(Particle.PORTAL, loc, 60, 3, 2, 3, 1.0);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, 3.0f, 0.3f);

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

        int count75 = getConfig().getInt("bosses.xylos.skills.minions.count_75", 2);
        int count50 = getConfig().getInt("bosses.xylos.skills.minions.count_50", 3);
        int count25 = getConfig().getInt("bosses.xylos.skills.minions.count_25", 4);

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

        // Teleporte passivo aleatório (Enderman instável) a cada ~5 segundos
        if (tickCounter % 100 == 0 && phase >= 2) {
            passiveTeleport();
        }

        handleLocalMusic();
        handleAITargeting();
        handleAntiKite();

        long now = System.currentTimeMillis();
        long cooldown = getConfig().getLong("bosses.xylos.skills.cooldown_base", 7000L);
        if (phase >= 3)
            cooldown = getConfig().getLong("bosses.xylos.skills.cooldown_rage", 4500L);

        if (now - lastSpecialAttack > cooldown) {
            playSpecialAttack();
            lastSpecialAttack = now;
        }
    }

    private void passiveTeleport() {
        if (entity == null || entity.isDead())
            return;
        Location newLoc = entity.getLocation().add(
                (RANDOM.nextDouble() - 0.5) * 10, 0, (RANDOM.nextDouble() - 0.5) * 10);
        newLoc.setY(entity.getWorld().getHighestBlockYAt(newLoc));
        entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL, entity.getLocation(), 20, 0.5, 1, 0.5, 0.1);
        entity.teleport(newLoc);
        entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL, entity.getLocation(), 20, 0.5, 1, 0.5, 0.1);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.0f);
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
                    p.playSound(entity, Sound.MUSIC_DISC_5, 3.75f, 1.0f);
                    playerMusicTracker.put(uuid, now);
                }
            } else {
                if (playerMusicTracker.containsKey(uuid)) {
                    p.stopSound(Sound.MUSIC_DISC_5);
                    playerMusicTracker.remove(uuid);
                }
            }
        }
    }

    // ================= FASES =================

    @Override
    public void onPhaseChange(int phase) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 3f, 0.3f);
        switch (phase) {
            case 2 -> {
                say("§5§lXYLOS §8» §dA realidade começa a rachar...");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            }
            case 3 -> {
                say("§5§lXYLOS §8» §cO Éter consome! A distorção é imparável!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
            }
            case 4 -> {
                say("§5§lXYLOS §8» §4§lCOLAPSO DO ÉTER! A REALIDADE SE DESFAZ!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                entity.getWorld().strikeLightningEffect(entity.getLocation());

                // Teleporta todos os jogadores próximos aleatoriamente
                entity.getNearbyEntities(15, 15, 15).forEach(e -> {
                    if (e instanceof Player p) {
                        Location tpLoc = entity.getLocation().add(
                                (RANDOM.nextDouble() - 0.5) * 20, 0, (RANDOM.nextDouble() - 0.5) * 20);
                        tpLoc.setY(entity.getWorld().getHighestBlockYAt(tpLoc));
                        p.teleport(tpLoc);
                        p.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_dialogue.xylos_ripped"));
                    }
                });
            }
        }
    }

    // ================= ATAQUES =================

    @Override
    public void playSpecialAttack() {
        double r = Math.random();
        if (r < 0.20)
            executeSpatialRupture();
        else if (r < 0.40)
            executeGravitationalImplosion();
        else if (r < 0.60)
            executeEtherFragment();
        else if (r < 0.80)
            executeDistortion();
        else
            executeVoidPhase();
    }

    /**
     * Ruptura Espacial — Teleporta jogadores aleatoriamente e causa dano.
     */
    private void executeSpatialRupture() {
        say("§5§lXYLOS §8» §dESPAÇO, DISTORÇA-SE!");

        double radius = getConfig().getDouble("bosses.xylos.skills.spatial_rupture.raio", 15.0);
        double damage = getConfig().getDouble("bosses.xylos.skills.spatial_rupture.dano", 6.0);
        double tpRange = getConfig().getDouble("bosses.xylos.skills.spatial_rupture.tp_range", 10.0);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 3.0f, 0.3f);
        entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL, entity.getLocation(), 50, 3, 2, 3, 0.2);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                Location tpLoc = p.getLocation().add(
                        (RANDOM.nextDouble() - 0.5) * tpRange * 2,
                        0,
                        (RANDOM.nextDouble() - 0.5) * tpRange * 2);
                tpLoc.setY(p.getWorld().getHighestBlockYAt(tpLoc));

                p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, p.getLocation(), 20, 0.3, 1, 0.3, 0.1);
                p.teleport(tpLoc);
                p.damage(damage, entity);
                p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, p.getLocation(), 20, 0.3, 1, 0.3, 0.1);
                p.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_dialogue.xylos_bend"));
            }
        });
    }

    /**
     * Implosão Gravitacional — Puxa jogadores + levitação leve.
     */
    private void executeGravitationalImplosion() {
        say("§5§lXYLOS §8» §dSINGULARIDADE!");

        double radius = getConfig().getDouble("bosses.xylos.skills.gravitational_implosion.raio", 14.0);
        double damage = getConfig().getDouble("bosses.xylos.skills.gravitational_implosion.dano", 5.0);
        int levitationDuration = getConfig().getInt("bosses.xylos.skills.gravitational_implosion.levitation_duration",
                40);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.5f);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                // Puxa o jogador em direção ao boss
                Vector pull = entity.getLocation().toVector().subtract(p.getLocation().toVector()).normalize()
                        .multiply(1.2);
                pull.setY(0.5);
                p.setVelocity(pull);

                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, levitationDuration, 0));
                p.spawnParticle(Particle.PORTAL, p.getLocation(), 20, 0.5, 1, 0.5, 0.3);
            }
        });

        // Anel de partículas no centro
        Location center = entity.getLocation();
        for (double angle = 0; angle < 360; angle += 10) {
            double radians = Math.toRadians(angle);
            double x = center.getX() + 3 * Math.cos(radians);
            double z = center.getZ() + 3 * Math.sin(radians);
            entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                    new Location(center.getWorld(), x, center.getY() + 1, z), 3, 0, 0, 0, 0);
        }
    }

    /**
     * Fragmento do Éter — Projéteis de Ender Pearl com dano AoE.
     */
    private void executeEtherFragment() {
        say("§5§lXYLOS §8» §eFRAGMENTOS DO ÉTER!");

        int count = getConfig().getInt("bosses.xylos.skills.ether_fragment.count", 3);
        double damage = getConfig().getDouble("bosses.xylos.skills.ether_fragment.dano", 5.0);
        double radius = getConfig().getDouble("bosses.xylos.skills.ether_fragment.raio_impacto", 4.0);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 2.5f, 0.5f);

        for (Entity e : entity.getNearbyEntities(25, 25, 25)) {
            if (e instanceof Player p && count > 0) {
                Vector dir = p.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize()
                        .multiply(1.5);
                dir.setY(0.3);

                // Lançar ender pearl como projétil visual
                EnderPearl pearl = entity.getWorld().spawn(entity.getLocation().add(0, 2, 0), EnderPearl.class);
                pearl.setVelocity(dir);
                pearl.setShooter(entity);

                // Dano AoE ao impactar (simulado com delay)
                Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
                    Location impactLoc = pearl.isValid() ? pearl.getLocation() : p.getLocation();
                    impactLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, impactLoc, 30, 1, 1, 1, 0.1);
                    impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.5f);

                    impactLoc.getWorld().getNearbyEntities(impactLoc, radius, radius, radius).forEach(nearby -> {
                        if (nearby instanceof Player np) {
                            np.damage(damage, entity);
                        }
                    });
                    if (pearl.isValid())
                        pearl.remove();
                }, 25L);
                count--;
            }
        }
    }

    /**
     * Distorção — Aplica Nausea + Darkness em área.
     */
    private void executeDistortion() {
        say("§5§lXYLOS §8» §dA realidade se distorce...");

        double radius = getConfig().getDouble("bosses.xylos.skills.distortion.raio", 12.0);
        int nauseaDuration = getConfig().getInt("bosses.xylos.skills.distortion.nausea_duration", 100);
        int darknessDuration = getConfig().getInt("bosses.xylos.skills.distortion.darkness_duration", 80);

        entity.getWorld().playSound(entity.getLocation(), Sound.AMBIENT_CAVE, 3.0f, 0.3f);
        entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL, entity.getLocation(), 40, 4, 2, 4, 0.1);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, nauseaDuration, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, darknessDuration, 0));
                p.spawnParticle(Particle.REVERSE_PORTAL, p.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.05);
            }
        });

        notifyAlly("Distortion");
    }

    /**
     * Fase do Vazio — Ganha Speed II + invisibilidade breve + teleporte agressivo.
     */
    private void executeVoidPhase() {
        say("§5§lXYLOS §8» §4§lFASE DO VAZIO!");

        int duration = getConfig().getInt("bosses.xylos.skills.void_phase.duracao_ticks", 80);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0));

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 3.0f, 0.5f);
        entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL, entity.getLocation(), 50, 1, 2, 1, 0.2);

        // Teleporte agressivo — aparece atrás de um jogador
        Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
            if (entity == null || entity.isDead())
                return;

            for (Entity e : entity.getNearbyEntities(30, 30, 30)) {
                if (e instanceof Player p) {
                    Vector dir = p.getLocation().getDirection().normalize().multiply(-2.0);
                    Location behind = p.getLocation().add(dir);
                    behind.setY(p.getLocation().getY());

                    entity.teleport(behind);
                    entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL, entity.getLocation(), 30, 0.5, 1, 0.5,
                            0.1);
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 2.0f, 1.0f);

                    double damage = getConfig().getDouble("bosses.xylos.skills.void_phase.dano", 8.0);
                    p.damage(damage, entity);
                    p.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_dialogue.xylos_void"));
                    setTeleportLock(p.getUniqueId(), 4000L);
                    break;
                }
            }
        }, 20L);
    }

    // ================= MINIONS =================

    private void spawnMinions(int amount) {
        summons.removeIf(e -> !e.isValid() || e.isDead());
        say("§5§lXYLOS §8» §dServos do Éter, materializem-se!");

        for (int i = 0; i < amount; i++) {
            Location l = entity.getLocation().add((Math.random() - 0.5) * 10, 0, (Math.random() - 0.5) * 10);
            Endermite mite = (Endermite) l.getWorld().spawnEntity(l, EntityType.ENDERMITE);
            mite.customName(LegacyComponentSerializer.legacySection().deserialize("§5§lFragmento do Éter"));
            mite.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
            mite.setMetadata("xylos_minion",
                    new org.bukkit.metadata.FixedMetadataValue(GorvaxCore.getInstance(), true));
            summons.add(mite);

            l.getWorld().spawnParticle(Particle.REVERSE_PORTAL, l, 10, 0.3, 0.5, 0.3, 0.05);
        }
    }

    // ================= AURA =================

    private void spawnAuraParticles() {
        if (entity == null || entity.isDead())
            return;

        Location loc = entity.getLocation().add(0, 2, 0);
        int portalCount = getConfig().getInt("bosses.xylos.visual_effects.aura.reverse_portal", 12);
        int portalNormalCount = getConfig().getInt("bosses.xylos.visual_effects.aura.portal", 6);

        for (Entity e : entity.getNearbyEntities(40, 40, 40)) {
            if (e instanceof Player p) {
                p.spawnParticle(Particle.REVERSE_PORTAL, loc, portalCount, 0.8, 1.0, 0.8, 0.05);
                p.spawnParticle(Particle.PORTAL, loc, portalNormalCount, 0.5, 0.8, 0.5, 0.5);
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
            p.stopSound(Sound.MUSIC_DISC_5);
        }
        playerMusicTracker.clear();
    }
}
