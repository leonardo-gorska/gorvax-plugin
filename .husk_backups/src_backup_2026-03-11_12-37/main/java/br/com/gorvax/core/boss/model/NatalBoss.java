package br.com.gorvax.core.boss.model;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss Sazonal de Natal — Rei do Gelo Eterno.
 * Entidade: Iron Golem. Tema: Gelo, neve e cristais.
 * Evento: "Festival de Inverno" (Dezembro 15-31).
 * Lore: Um antigo golem de gelo que desperta apenas no solstício de inverno,
 * trazendo uma era glacial temporária ao mundo. Diferente do Kaldur
 * (esqueleto),
 * este é uma fortaleza de gelo viva — lento, massivo e devastador.
 */
public class NatalBoss extends WorldBoss {

    private long lastSpecialAttack = 0;
    private int tickCounter = 0;

    private final List<Entity> summons = new ArrayList<>();
    private final java.util.Map<UUID, Long> playerMusicTracker = new ConcurrentHashMap<>();

    private boolean summoned75 = false;
    private boolean summoned50 = false;
    private boolean summoned25 = false;

    private static final Random RANDOM = new Random();

    public NatalBoss() {
        super(
                "natal_boss",
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getString("bosses.natal_boss.nome", "§b§lRei do Gelo Eterno")
                        .replace("&", "§"),
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getDouble("bosses.natal_boss.hp", 7000.0));
    }

    private FileConfiguration getConfig() {
        return GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings();
    }

    // ================= SPAWN =================

    @Override
    public void spawn(Location loc) {
        this.entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.IRON_GOLEM);
        entity.setRemoveWhenFarAway(false);
        if (entity instanceof Mob mob)
            mob.setPersistent(true);

        double damage = getConfig().getDouble("bosses.natal_boss.normal_damage", 20.0);

        applySafeMaxHealth(entity, this.maxHealth);
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        }

        // Escala: golem gigantesco de gelo
        double scale = getConfig().getDouble("bosses.natal_boss.visual_effects.scale", 2.3);
        if (entity.getAttribute(Attribute.GENERIC_SCALE) != null) {
            entity.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
        }

        // Lento, mas poderoso
        double speed = getConfig().getDouble("bosses.natal_boss.movement_speed", 0.22);
        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        }

        // Knockback Resistance (golem pesado)
        if (entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE) != null) {
            entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(0.8);
        }

        entity.customName(LegacyComponentSerializer.legacySection().deserialize(this.name));
        entity.setCustomNameVisible(true);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));

        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                "§b§lREI DO GELO §8» §fO inverno absoluto desperta... §bprostem-se perante o frio eterno!"));

        // Explosão de flocos de neve + cristais de gelo
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.add(0, 2, 0), 150, 5, 3, 5, 0.1);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 50, 3, 2, 3, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 3.0f, 0.3f);
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 3.0f, 0.5f);

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

        int count75 = getConfig().getInt("bosses.natal_boss.skills.minions.count_75", 2);
        int count50 = getConfig().getInt("bosses.natal_boss.skills.minions.count_50", 4);
        int count25 = getConfig().getInt("bosses.natal_boss.skills.minions.count_25", 6);

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

        // Aura de frio passiva (a cada 3 segundos)
        if (tickCounter % 60 == 0) {
            handleFrostAura();
        }

        handleLocalMusic();
        handleAITargeting();
        handleAntiKite();

        long now = System.currentTimeMillis();
        long cooldown = getConfig().getLong("bosses.natal_boss.skills.cooldown_base", 8000L);
        if (phase >= 3)
            cooldown = getConfig().getLong("bosses.natal_boss.skills.cooldown_rage", 5000L);

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
                    p.playSound(entity, Sound.MUSIC_DISC_STRAD, 3.75f, 1.0f);
                    playerMusicTracker.put(uuid, now);
                }
            } else {
                if (playerMusicTracker.containsKey(uuid)) {
                    p.stopSound(Sound.MUSIC_DISC_STRAD);
                    playerMusicTracker.remove(uuid);
                }
            }
        }
    }

    /**
     * Aura gelada passiva — Slowness + Frost Damage.
     */
    private void handleFrostAura() {
        double radius = getConfig().getDouble("bosses.natal_boss.skills.frost_aura.raio", 10.0);
        double damage = getConfig().getDouble("bosses.natal_boss.skills.frost_aura.dano", 3.0);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                p.setFreezeTicks(p.getFreezeTicks() + 40);
                p.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.02);
            }
        });
    }

    // ================= FASES =================

    @Override
    public void onPhaseChange(int phase) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 3f, 0.3f);
        switch (phase) {
            case 2 -> {
                say("§b§lREI DO GELO §8» §7A temperatura cai... o frio se aproxima!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
            }
            case 3 -> {
                say("§b§lREI DO GELO §8» §cA Era Glacial se intensifica! Seus movimentos se tornam lentos!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2));
            }
            case 4 -> {
                say("§b§lREI DO GELO §8» §4§lO INVERNO ABSOLUTO CHEGOU! O MUNDO CONGELARÁ!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2));
                entity.getWorld().strikeLightningEffect(entity.getLocation());
                // Nevasca intensa no spawn
                entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 5, 0), 200, 10, 5,
                        10, 0.1);
            }
        }
    }

    // ================= ATAQUES =================

    @Override
    public void playSpecialAttack() {
        double r = Math.random();
        if (r < 0.20)
            executeAbsoluteZero();
        else if (r < 0.40)
            executeIceCrystalBarrage();
        else if (r < 0.60)
            executeGlacialStomp();
        else if (r < 0.80)
            executeBlizzardVortex();
        else
            executePermafrostPrison();
    }

    /**
     * Zero Absoluto — Congela todos os jogadores próximos (Freeze + grande dano).
     */
    private void executeAbsoluteZero() {
        say("§b§lREI DO GELO §8» §b§lZERO ABSOLUTO!");

        double radius = getConfig().getDouble("bosses.natal_boss.skills.absolute_zero.raio", 14.0);
        double damage = getConfig().getDouble("bosses.natal_boss.skills.absolute_zero.dano", 10.0);
        int freezeTicks = getConfig().getInt("bosses.natal_boss.skills.absolute_zero.freeze_ticks", 140);

        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GLASS_BREAK, 3.0f, 0.3f);
        entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 2, 0), 100, 5, 2, 5, 0.15);
        entity.getWorld().spawnParticle(Particle.END_ROD, entity.getLocation().add(0, 2, 0), 30, 3, 2, 3, 0.05);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 2));
                p.setFreezeTicks(p.getMaxFreezeTicks() + freezeTicks);
                p.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.05);
                p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 2.0f, 1.5f);
            }
        });
    }

    /**
     * Estilhaços de Gelo — Bolas de neve gigantes com dano de impacto.
     */
    private void executeIceCrystalBarrage() {
        say("§b§lREI DO GELO §8» §3Recebam meus cristais!");

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ARROW_SHOOT, 2.0f, 0.5f);

        for (Entity e : entity.getNearbyEntities(30, 30, 30)) {
            if (e instanceof Player p) {
                double damage = getConfig().getDouble("bosses.natal_boss.skills.ice_crystal_barrage.dano", 6.0);
                int count = getConfig().getInt("bosses.natal_boss.skills.ice_crystal_barrage.count", 5);

                for (int i = 0; i < count; i++) {
                    // Snowball com delay escalonado (cada 4 ticks)
                    final int delay = i * 4;
                    Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
                        if (entity == null || entity.isDead())
                            return;
                        Vector dir = p.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
                        dir.add(new Vector((RANDOM.nextDouble() - 0.5) * 0.3, 0.1, (RANDOM.nextDouble() - 0.5) * 0.3));

                        Snowball snowball = entity.getWorld().spawn(
                                entity.getLocation().add(0, 2, 0), Snowball.class);
                        snowball.setShooter(entity);
                        snowball.setVelocity(dir.multiply(2.0));
                        snowball.customName(LegacyComponentSerializer.legacySection().deserialize("§b§lCristal de Gelo"));

                        entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 2, 0), 5,
                                0.3, 0.3, 0.3, 0.05);
                    }, delay);
                }

                // Dano adicional por impacto
                Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
                    if (p.isOnline() && !p.isDead()) {
                        p.damage(damage, entity);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                    }
                }, count * 4 + 10);
                break;
            }
        }
    }

    /**
     * Pisada Glacial — Knockback massivo + dano em área (como o Iron Golem faz).
     */
    private void executeGlacialStomp() {
        say("§b§lREI DO GELO §8» §fO CHÃO TREME SOB MEUS PÉS!");

        double radius = getConfig().getDouble("bosses.natal_boss.skills.glacial_stomp.raio", 12.0);
        double damage = getConfig().getDouble("bosses.natal_boss.skills.glacial_stomp.dano", 12.0);
        double knockback = getConfig().getDouble("bosses.natal_boss.skills.glacial_stomp.knockback", 2.5);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 3.0f, 0.3f);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 5);
        entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation(), 60, 4, 0.5, 4, 0.1);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                Vector kb = p.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize()
                        .multiply(knockback);
                kb.setY(0.6);
                p.setVelocity(kb);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                p.setFreezeTicks(p.getFreezeTicks() + 60);
            }
        });
    }

    /**
     * Vórtice de Nevasca — Puxa jogadores para o centro e aplica AoE.
     */
    private void executeBlizzardVortex() {
        say("§b§lREI DO GELO §8» §b§lA NEVASCA DEVORA TUDO!");

        double radius = getConfig().getDouble("bosses.natal_boss.skills.blizzard_vortex.raio", 18.0);
        double damage = getConfig().getDouble("bosses.natal_boss.skills.blizzard_vortex.dano", 8.0);
        double pullForce = getConfig().getDouble("bosses.natal_boss.skills.blizzard_vortex.pull_force", 1.5);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.5f);
        entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 3, 0), 120, 6, 3, 6, 0.15);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                // Puxa em direção ao boss
                Vector pull = entity.getLocation().toVector().subtract(p.getLocation().toVector()).normalize()
                        .multiply(pullForce);
                pull.setY(0.3);
                p.setVelocity(pull);

                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                p.setFreezeTicks(p.getFreezeTicks() + 80);
                p.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 2, 0), 30, 2, 2, 2, 0.1);
            }
        });
    }

    /**
     * Prisão de Permafrost — Imobiliza completamente por 3 segundos + Blindness.
     */
    private void executePermafrostPrison() {
        say("§b§lREI DO GELO §8» §b§lCONGELEM POR TODA A ETERNIDADE!");

        int duration = getConfig().getInt("bosses.natal_boss.skills.permafrost_prison.duracao_ticks", 60);

        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GLASS_PLACE, 3.0f, 0.3f);

        entity.getNearbyEntities(14, 14, 14).forEach(e -> {
            if (e instanceof Player p) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 10));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 10));
                p.setFreezeTicks(p.getMaxFreezeTicks() + 100);
                p.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.02);
                p.spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1.5, 0), 10, 0.3, 0.5, 0.3, 0.01);
                p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 1.5f);
            }
        });
    }

    // ================= MINIONS =================

    private void spawnMinions(int amount) {
        summons.removeIf(e -> !e.isValid() || e.isDead());
        say("§b§lREI DO GELO §8» §fSentinelas de cristal, protejam seu Rei!");

        for (int i = 0; i < amount; i++) {
            Location l = entity.getLocation().add((Math.random() - 0.5) * 10, 0, (Math.random() - 0.5) * 10);
            Stray stray = (Stray) l.getWorld().spawnEntity(l, EntityType.STRAY);
            stray.customName(LegacyComponentSerializer.legacySection().deserialize("§b§lSentinela de Cristal"));
            stray.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
            stray.setMetadata("natal_minion",
                    new org.bukkit.metadata.FixedMetadataValue(GorvaxCore.getInstance(), true));
            if (stray.getEquipment() != null) {
                stray.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
                stray.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                stray.getEquipment().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
            }
            summons.add(stray);
        }
    }

    // ================= AURA =================

    private void spawnAuraParticles() {
        if (entity == null || entity.isDead())
            return;

        Location loc = entity.getLocation().add(0, 2.0, 0);
        int snowCount = getConfig().getInt("bosses.natal_boss.visual_effects.aura.snowflake", 12);
        int endRodCount = getConfig().getInt("bosses.natal_boss.visual_effects.aura.end_rod", 6);

        for (Entity e : entity.getNearbyEntities(40, 40, 40)) {
            if (e instanceof Player p) {
                p.spawnParticle(Particle.SNOWFLAKE, loc, snowCount, 0.8, 1.0, 0.8, 0.03);
                p.spawnParticle(Particle.END_ROD, loc, endRodCount, 0.5, 0.8, 0.5, 0.02);
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
            p.stopSound(Sound.MUSIC_DISC_STRAD);
        }
        playerMusicTracker.clear();
    }
}
