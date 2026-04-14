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
 * Skulkor, o General Reerguido — Boss Tier 2 (3º mais forte).
 * Entidade: Skeleton. Tema: Necromancia e Exército.
 * Evento Global: "Névoa da Morte" (Partículas de fumaça cinza e sons de ossos).
 * Lore: Antigo general do exército que protegia a fronteira entre o Reino de
 * Gorvax e o domínio de Indrax.
 */
public class Skulkor extends WorldBoss {

    private long lastSpecialAttack = 0;
    private int tickCounter = 0;

    private final List<Entity> summons = new ArrayList<>();
    private final java.util.Map<UUID, Long> playerMusicTracker = new ConcurrentHashMap<>();

    private boolean summoned75 = false;
    private boolean summoned50 = false;
    private boolean summoned25 = false;

    private static final Random RANDOM = new Random();

    public Skulkor() {
        super(
                "skulkor",
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getString("bosses.skulkor.nome", "§7§lSkulkor, o General Reerguido")
                        .replace("&", "§"),
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getDouble("bosses.skulkor.hp", 650.0));
    }

    private FileConfiguration getConfig() {
        return GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings();
    }

    // ================= SPAWN =================

    @Override
    public void spawn(Location loc) {
        this.entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
        entity.setRemoveWhenFarAway(false);
        if (entity instanceof Mob mob)
            mob.setPersistent(true);

        double damage = getConfig().getDouble("bosses.skulkor.normal_damage", 11.0);

        applySafeMaxHealth(entity, this.maxHealth);
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        }

        // Escala visual (80% maior — general esqueleto gigante)
        double scale = getConfig().getDouble("bosses.skulkor.visual_effects.scale", 1.8);
        if (entity.getAttribute(Attribute.GENERIC_SCALE) != null) {
            entity.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
        }

        double speed = getConfig().getDouble("bosses.skulkor.movement_speed", 0.30);
        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        }

        // HP já setado por applySafeMaxHealth
        entity.customName(LegacyComponentSerializer.legacySection().deserialize(this.name));
        entity.setCustomNameVisible(true);

        setupEquipment();

        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));

        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                "§7§lSKULKOR §8» §fA morte não é o fim... é apenas a primeira marcha. Exército, levantem-se!"));

        loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0, 1, 0), 60, 3, 2, 3, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_SKELETON_HORSE_DEATH, 3.0f, 0.3f);

        // Garante exibição imediata da BossBar
        updateBossBar();
    }

    private void setupEquipment() {
        if (entity.getEquipment() == null)
            return;

        ItemStack ironHelm = new ItemStack(Material.IRON_HELMET);
        ItemMeta hm = ironHelm.getItemMeta();
        if (hm != null) {
            hm.displayName(LegacyComponentSerializer.legacySection().deserialize("§7§lElmo do General"));
            Enchantment prot = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("protection"));
            if (prot != null)
                hm.addEnchant(prot, 4, true);
            ironHelm.setItemMeta(hm);
        }
        entity.getEquipment().setHelmet(ironHelm);
        entity.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        entity.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        entity.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));

        // Espada de ferro (general é corpo a corpo)
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta sm = sword.getItemMeta();
        if (sm != null) {
            sm.displayName(LegacyComponentSerializer.legacySection().deserialize("§7§lLâmina do General Caído"));
            Enchantment sharp = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
            if (sharp != null)
                sm.addEnchant(sharp, 5, true);
            sword.setItemMeta(sm);
        }
        entity.getEquipment().setItemInMainHand(sword);
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

        // Skulkor spawna mais minions que outros bosses (foco em exército)
        int count75 = getConfig().getInt("bosses.skulkor.skills.minions.count_75", 4);
        int count50 = getConfig().getInt("bosses.skulkor.skills.minions.count_50", 6);
        int count25 = getConfig().getInt("bosses.skulkor.skills.minions.count_25", 8);

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
        long cooldown = getConfig().getLong("bosses.skulkor.skills.cooldown_base", 7500L);
        if (phase >= 3)
            cooldown = getConfig().getLong("bosses.skulkor.skills.cooldown_rage", 5000L);

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
                    p.playSound(entity, Sound.MUSIC_DISC_11, 3.75f, 1.0f);
                    playerMusicTracker.put(uuid, now);
                }
            } else {
                if (playerMusicTracker.containsKey(uuid)) {
                    p.stopSound(Sound.MUSIC_DISC_11);
                    playerMusicTracker.remove(uuid);
                }
            }
        }
    }

    // ================= FASES =================

    @Override
    public void onPhaseChange(int phase) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, 3f, 0.3f);
        switch (phase) {
            case 2 -> {
                say("§7§lSKULKOR §8» §7A marcha dos mortos apenas começou...");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));
            }
            case 3 -> {
                say("§7§lSKULKOR §8» §cO exército do além não conhece misericórdia!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));

                // Fortalecer minions ativos
                strengthenMinions();
            }
            case 4 -> {
                say("§7§lSKULKOR §8» §4§lLEVANTEM-SE! TODOS! A ÚLTIMA MARCHA COMEÇOU!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                entity.getWorld().strikeLightningEffect(entity.getLocation());
                entity.getWorld().strikeLightningEffect(entity.getLocation().add(3, 0, 0));
                entity.getWorld().strikeLightningEffect(entity.getLocation().add(-3, 0, 0));

                strengthenMinions();
            }
        }
    }

    private void strengthenMinions() {
        summons.removeIf(e -> !e.isValid() || e.isDead());
        for (Entity minion : summons) {
            if (minion instanceof LivingEntity le) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                le.getWorld().spawnParticle(Particle.SMOKE, le.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
            }
        }
    }

    // ================= ATAQUES =================

    @Override
    public void playSpecialAttack() {
        double r = Math.random();
        if (r < 0.20)
            executeCallOfDead();
        else if (r < 0.40)
            executeArrowRain();
        else if (r < 0.60)
            executeBoneShield();
        else if (r < 0.80)
            executeWarCry();
        else
            executeDeathMarch();
    }

    /**
     * Chamado dos Mortos — Spawna esqueletos extras fora da mecânica de threshold.
     */
    private void executeCallOfDead() {
        say("§7§lSKULKOR §8» §7LEVANTEM-SE, SOLDADOS!");

        int count = getConfig().getInt("bosses.skulkor.skills.call_of_dead.count", 3);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 3.0f, 0.3f);
        entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation(), 40, 2, 1, 2, 0.05);

        for (int i = 0; i < count; i++) {
            Location l = entity.getLocation().add((Math.random() - 0.5) * 8, 0, (Math.random() - 0.5) * 8);
            Skeleton skel = (Skeleton) l.getWorld().spawnEntity(l, EntityType.SKELETON);
            skel.customName(LegacyComponentSerializer.legacySection().deserialize("§7§lSoldado Reerguido"));
            skel.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            skel.setMetadata("skulkor_minion",
                    new org.bukkit.metadata.FixedMetadataValue(GorvaxCore.getInstance(), true));
            if (skel.getEquipment() != null) {
                skel.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                skel.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            }
            summons.add(skel);

            l.getWorld().spawnParticle(Particle.SMOKE, l, 15, 0.3, 0.5, 0.3, 0.02);
            l.getWorld().playSound(l, Sound.ENTITY_SKELETON_AMBIENT, 1.0f, 0.5f);
        }
    }

    /**
     * Chuva de Flechas — Flechas envenenadas caem do céu sobre os jogadores.
     */
    private void executeArrowRain() {
        say("§7§lSKULKOR §8» §eChuva de ossos! Cubram-se!");

        double radius = getConfig().getDouble("bosses.skulkor.skills.arrow_rain.raio", 14.0);
        int arrowsPerPlayer = getConfig().getInt("bosses.skulkor.skills.arrow_rain.arrows_per_player", 4);
        double arrowDamage = getConfig().getDouble("bosses.skulkor.skills.arrow_rain.dano", 3.0);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 2.5f, 0.5f);

        for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p) {
                for (int i = 0; i < arrowsPerPlayer; i++) {
                    Location arrowLoc = p.getLocation().add(
                            (RANDOM.nextDouble() - 0.5) * 5, 15, (RANDOM.nextDouble() - 0.5) * 5);
                    Arrow arrow = p.getWorld().spawnArrow(arrowLoc, new Vector(0, -1, 0), 1.2f, 3f);
                    arrow.setShooter(entity);
                    arrow.setDamage(arrowDamage);
                }
                p.spawnParticle(Particle.SMOKE, p.getLocation().add(0, 8, 0), 20, 3, 1, 3, 0.05);
            }
        }
    }

    /**
     * Escudo Ósseo — Boss ganha Resistance III temporário.
     */
    private void executeBoneShield() {
        say("§7§lSKULKOR §8» §fOssos do além, protejam vosso general!");

        int duration = getConfig().getInt("bosses.skulkor.skills.bone_shield.duracao_ticks", 100);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 2));
        entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation().add(0, 1.5, 0), 40, 1, 1.5, 1, 0.03);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_STEP, 3.0f, 0.3f);

        notifyAlly("BoneShield");
    }

    /**
     * Grito de Guerra — Dano AoE + Weakness em jogadores.
     */
    private void executeWarCry() {
        say("§7§lSKULKOR §8» §c§lPELA ALIANÇA ENTRE O TRONO E O ABISMO!");

        double radius = getConfig().getDouble("bosses.skulkor.skills.war_cry.raio", 12.0);
        double damage = getConfig().getDouble("bosses.skulkor.skills.war_cry.dano", 7.0);
        int weaknessDuration = getConfig().getInt("bosses.skulkor.skills.war_cry.weakness_duration", 80);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2.5f, 0.8f);
        entity.getWorld().spawnParticle(Particle.SONIC_BOOM, entity.getLocation().add(0, 1, 0), 2);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, 1));
                p.spawnParticle(Particle.SMOKE, p.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
            }
        });
    }

    /**
     * Marcha Fúnebre — Teleporta atrás de um jogador e aplica Wither + dano.
     */
    private void executeDeathMarch() {
        say("§7§lSKULKOR §8» §5A morte marcha em silêncio...");

        int range = getConfig().getInt("bosses.skulkor.skills.death_march.range", 20);
        double damage = getConfig().getDouble("bosses.skulkor.skills.death_march.dano", 9.0);
        long focusDuration = getConfig().getLong("bosses.skulkor.skills.death_march.focus_duration_ms", 4000L);

        for (Entity e : entity.getNearbyEntities(range, range, range)) {
            if (e instanceof Player p) {
                Vector dir = p.getLocation().getDirection().normalize().multiply(-2.0);
                Location behind = p.getLocation().add(dir);
                behind.setY(p.getLocation().getY());

                entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation(), 30, 0.5, 1, 0.5, 0.05);
                entity.teleport(behind);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
                entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation(), 30, 0.5, 1, 0.5, 0.05);

                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1));
                p.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_dialogue.skulkor_stalk"));

                setTeleportLock(p.getUniqueId(), focusDuration);
                break;
            }
        }
    }

    // ================= MINIONS =================

    private void spawnMinions(int amount) {
        summons.removeIf(e -> !e.isValid() || e.isDead());
        say("§7§lSKULKOR §8» §7Exército, às armas!");

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 3.0f, 0.3f);

        for (int i = 0; i < amount; i++) {
            Location l = entity.getLocation().add((Math.random() - 0.5) * 10, 0, (Math.random() - 0.5) * 10);

            // Varia entre esqueletos e esqueletos wither para diversidade
            EntityType type = RANDOM.nextBoolean() ? EntityType.SKELETON : EntityType.WITHER_SKELETON;
            AbstractSkeleton skel = (AbstractSkeleton) l.getWorld().spawnEntity(l, type);

            skel.customName(LegacyComponentSerializer.legacySection().deserialize(
                    type == EntityType.WITHER_SKELETON ? "§8§lCapitão Morto-Vivo" : "§7§lSoldado do General"));
            skel.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            skel.setMetadata("skulkor_minion",
                    new org.bukkit.metadata.FixedMetadataValue(GorvaxCore.getInstance(), true));

            if (skel.getEquipment() != null) {
                if (type == EntityType.WITHER_SKELETON) {
                    skel.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
                    skel.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                } else {
                    skel.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
                }
            }
            summons.add(skel);

            l.getWorld().spawnParticle(Particle.SMOKE, l, 10, 0.3, 0.5, 0.3, 0.02);
        }
    }

    // ================= AURA =================

    private void spawnAuraParticles() {
        if (entity == null || entity.isDead())
            return;

        Location loc = entity.getLocation().add(0, 1.5, 0);
        int smokeCount = getConfig().getInt("bosses.skulkor.visual_effects.aura.smoke", 10);
        int soulCount = getConfig().getInt("bosses.skulkor.visual_effects.aura.soul", 5);

        for (Entity e : entity.getNearbyEntities(40, 40, 40)) {
            if (e instanceof Player p) {
                p.spawnParticle(Particle.SMOKE, loc, smokeCount, 0.5, 0.8, 0.5, 0.02);
                p.spawnParticle(Particle.SOUL, loc, soulCount, 0.3, 0.5, 0.3, 0.03);
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
            p.stopSound(Sound.MUSIC_DISC_11);
        }
        playerMusicTracker.clear();
    }
}
