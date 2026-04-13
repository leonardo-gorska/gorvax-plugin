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
 * Kaldur, o Coração de Gelo — Boss Tier 2 (4º mais forte).
 * Entidade: Stray. Tema: Gelo e Lentidão.
 * Evento Global: "Nevasca Eterna" (Tempestade + neve + céu branco).
 * Lore: Antigo guardião das terras geladas que selou o caminho norte entre o
 * Reino e o Abismo.
 */
public class Kaldur extends WorldBoss {

    private long lastSpecialAttack = 0;
    private int tickCounter = 0;

    private final List<Entity> summons = new ArrayList<>();
    private final java.util.Map<UUID, Long> playerMusicTracker = new ConcurrentHashMap<>();

    private boolean summoned75 = false;
    private boolean summoned50 = false;
    private boolean summoned25 = false;

    private static final Random RANDOM = new Random();

    public Kaldur() {
        super(
                "kaldur",
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getString("bosses.kaldur.nome", "§b§lKaldur, o Coração de Gelo")
                        .replace("&", "§"),
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getDouble("bosses.kaldur.hp", 550.0));
    }

    private FileConfiguration getConfig() {
        return GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings();
    }

    // ================= SPAWN =================

    @Override
    public void spawn(Location loc) {
        this.entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.STRAY);
        entity.setRemoveWhenFarAway(false);
        if (entity instanceof Mob mob)
            mob.setPersistent(true);

        double damage = getConfig().getDouble("bosses.kaldur.normal_damage", 10.0);

        applySafeMaxHealth(entity, this.maxHealth);
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        }

        // Escala visual (70% maior — esqueleto de gelo gigante)
        double scale = getConfig().getDouble("bosses.kaldur.visual_effects.scale", 1.7);
        if (entity.getAttribute(Attribute.GENERIC_SCALE) != null) {
            entity.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
        }

        double speed = getConfig().getDouble("bosses.kaldur.movement_speed", 0.28);
        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        }

        // HP já setado por applySafeMaxHealth
        entity.customName(LegacyComponentSerializer.legacySection().deserialize(this.name));
        entity.setCustomNameVisible(true);

        setupEquipment();

        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));

        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                "§b§lKALDUR §8» §fO frio eterno chegou... e com ele, a morte silenciosa."));

        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.add(0, 1, 0), 80, 3, 2, 3, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_POLAR_BEAR_WARNING, 3.0f, 0.5f);

        // Garante exibição imediata da BossBar
        updateBossBar();
    }

    private void setupEquipment() {
        if (entity.getEquipment() == null)
            return;

        // Capacete de diamante (gelo)
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        ItemMeta hm = helmet.getItemMeta();
        if (hm != null) {
            hm.displayName(LegacyComponentSerializer.legacySection().deserialize("§b§lCoroa de Permafrost"));
            Enchantment prot = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("protection"));
            if (prot != null)
                hm.addEnchant(prot, 4, true);
            helmet.setItemMeta(hm);
        }
        entity.getEquipment().setHelmet(helmet);
        entity.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        entity.getEquipment().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        entity.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));

        // Arco de gelo
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bm = bow.getItemMeta();
        if (bm != null) {
            bm.displayName(LegacyComponentSerializer.legacySection().deserialize("§b§lArco do Inverno Eterno"));
            Enchantment power = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("power"));
            if (power != null)
                bm.addEnchant(power, 5, true);
            bow.setItemMeta(bm);
        }
        entity.getEquipment().setItemInMainHand(bow);
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

        int count75 = getConfig().getInt("bosses.kaldur.skills.minions.count_75", 2);
        int count50 = getConfig().getInt("bosses.kaldur.skills.minions.count_50", 3);
        int count25 = getConfig().getInt("bosses.kaldur.skills.minions.count_25", 5);

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

        // Aura congelante passiva (a cada 3 segundos)
        if (tickCounter % 60 == 0) {
            handleFrostAura();
        }

        handleLocalMusic();
        handleAITargeting();
        handleAntiKite();

        long now = System.currentTimeMillis();
        long cooldown = getConfig().getLong("bosses.kaldur.skills.cooldown_base", 8000L);
        if (phase >= 3)
            cooldown = getConfig().getLong("bosses.kaldur.skills.cooldown_rage", 5500L);

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
     * Aura congelante passiva — dano + Mining Fatigue em área.
     */
    private void handleFrostAura() {
        double radius = getConfig().getDouble("bosses.kaldur.skills.frost_aura.raio", 8.0);
        double damage = getConfig().getDouble("bosses.kaldur.skills.frost_aura.dano", 2.0);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage);
                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 1));
                p.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.02);
            }
        });
    }

    // ================= FASES =================

    @Override
    public void onPhaseChange(int phase) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, 3f, 0.3f);
        switch (phase) {
            case 2 -> {
                say("§b§lKALDUR §8» §7O frio se aprofunda...");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
            }
            case 3 -> {
                say("§b§lKALDUR §8» §cA nevasca se intensifica! Seus ossos congelarão!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
            }
            case 4 -> {
                say("§b§lKALDUR §8» §4§lO INVERNO ABSOLUTO CHEGOU!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
                entity.getWorld().strikeLightningEffect(entity.getLocation());
            }
        }
    }

    // ================= ATAQUES =================

    @Override
    public void playSpecialAttack() {
        double r = Math.random();
        if (r < 0.20)
            executeBlizzard();
        else if (r < 0.40)
            executeIceLance();
        else if (r < 0.60)
            executeGlacialPrison();
        else if (r < 0.80)
            executeFrostShield();
        else
            executeIceStorm();
    }

    /**
     * Nevasca — Partículas de neve + Slowness III em área.
     */
    private void executeBlizzard() {
        say("§b§lKALDUR §8» §fA nevasca consome tudo!");

        double radius = getConfig().getDouble("bosses.kaldur.skills.blizzard.raio", 14.0);
        double damage = getConfig().getDouble("bosses.kaldur.skills.blizzard.dano", 5.0);
        int slowDuration = getConfig().getInt("bosses.kaldur.skills.blizzard.slow_duration", 100);

        entity.getWorld().playSound(entity.getLocation(), Sound.WEATHER_RAIN, 3.0f, 0.3f);
        entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 2, 0), 80, 5, 2, 5, 0.1);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, 2));
                p.spawnParticle(Particle.SNOWFLAKE, p.getLocation(), 20, 0.5, 1, 0.5, 0.05);
            }
        });
    }

    /**
     * Lança de Gelo — Projétil de flecha com Slow + dano extra.
     */
    private void executeIceLance() {
        say("§b§lKALDUR §8» §3Lança de gelo perfurante!");

        for (Entity e : entity.getNearbyEntities(30, 30, 30)) {
            if (e instanceof Player p) {
                Vector dir = p.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
                Arrow arrow = entity.getWorld().spawnArrow(entity.getLocation().add(0, 1.5, 0),
                        dir, 2.5f, 0f);
                arrow.setShooter(entity);
                arrow.setDamage(getConfig().getDouble("bosses.kaldur.skills.ice_lance.dano", 8.0));

                entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 1.5, 0), 15,
                        0.3, 0.3, 0.3, 0.05);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ARROW_SHOOT, 2.0f, 0.5f);
                break; // Uma lança por vez
            }
        }
    }

    /**
     * Prisão Glacial — Imobiliza jogador com Slowness 255 por 2 segundos.
     */
    private void executeGlacialPrison() {
        say("§b§lKALDUR §8» §b§lCONGELE!");

        int duration = getConfig().getInt("bosses.kaldur.skills.glacial_prison.duracao_ticks", 40);

        entity.getNearbyEntities(12, 12, 12).forEach(e -> {
            if (e instanceof Player p) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 10));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0));
                p.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.02);
                p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 1.5f);
            }
        });
    }

    /**
     * Escudo de Permafrost — O boss ganha Resistance III temporário.
     */
    private void executeFrostShield() {
        say("§b§lKALDUR §8» §fO gelo é minha armadura eterna!");

        int duration = getConfig().getInt("bosses.kaldur.skills.frost_shield.duracao_ticks", 120);
        int level = getConfig().getInt("bosses.kaldur.skills.frost_shield.resistance_level", 2);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, level));
        entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 1.5, 0), 50, 1, 1.5, 1, 0.05);
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GLASS_PLACE, 2.0f, 0.3f);

        notifyAlly("FrostShield");
    }

    /**
     * Tempestade de Gelo — AoE massivo com dano + Slowness + queda de flechas.
     */
    private void executeIceStorm() {
        say("§b§lKALDUR §8» §4§lTEMPESTADE DE GELO!");

        double radius = getConfig().getDouble("bosses.kaldur.skills.ice_storm.raio", 16.0);
        double damage = getConfig().getDouble("bosses.kaldur.skills.ice_storm.dano", 7.0);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.5f);

        // Flechas caindo do céu
        for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p) {
                for (int i = 0; i < 3; i++) {
                    Location arrowLoc = p.getLocation().add(
                            (RANDOM.nextDouble() - 0.5) * 4, 12, (RANDOM.nextDouble() - 0.5) * 4);
                    Arrow arrow = p.getWorld().spawnArrow(arrowLoc, new Vector(0, -1, 0), 1.5f, 2f);
                    arrow.setShooter(entity);
                    arrow.setDamage(damage / 3);
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                p.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 5, 0), 40, 3, 2, 3, 0.1);
            }
        }
    }

    // ================= MINIONS =================

    private void spawnMinions(int amount) {
        summons.removeIf(e -> !e.isValid() || e.isDead());
        say("§b§lKALDUR §8» §fSentinelas de gelo, defendam a nevasca!");

        for (int i = 0; i < amount; i++) {
            Location l = entity.getLocation().add((Math.random() - 0.5) * 10, 0, (Math.random() - 0.5) * 10);
            Stray stray = (Stray) l.getWorld().spawnEntity(l, EntityType.STRAY);
            stray.customName(LegacyComponentSerializer.legacySection().deserialize("§b§lSentinela de Gelo"));
            stray.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
            stray.setMetadata("kaldur_minion",
                    new org.bukkit.metadata.FixedMetadataValue(GorvaxCore.getInstance(), true));
            if (stray.getEquipment() != null) {
                stray.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
                stray.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            }
            summons.add(stray);
        }
    }

    // ================= AURA =================

    private void spawnAuraParticles() {
        if (entity == null || entity.isDead())
            return;

        Location loc = entity.getLocation().add(0, 1.5, 0);
        int snowCount = getConfig().getInt("bosses.kaldur.visual_effects.aura.snowflake", 10);
        int endRodCount = getConfig().getInt("bosses.kaldur.visual_effects.aura.end_rod", 4);

        for (Entity e : entity.getNearbyEntities(40, 40, 40)) {
            if (e instanceof Player p) {
                p.spawnParticle(Particle.SNOWFLAKE, loc, snowCount, 0.6, 0.8, 0.6, 0.03);
                p.spawnParticle(Particle.END_ROD, loc, endRodCount, 0.4, 0.6, 0.4, 0.02);
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
