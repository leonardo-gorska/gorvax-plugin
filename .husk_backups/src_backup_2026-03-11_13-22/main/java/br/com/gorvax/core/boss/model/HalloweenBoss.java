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
 * Boss Sazonal de Halloween — Ceifador das Sombras.
 * Entidade: Wither Skeleton. Tema: Morte, escuridão e almas.
 * Evento: "Noite Sombria" (Outubro 20-31).
 * Lore: Uma entidade ancestral que desperta apenas quando o véu entre
 * os mundos dos vivos e dos mortos se torna mais fino.
 */
public class HalloweenBoss extends WorldBoss {

    private long lastSpecialAttack = 0;
    private int tickCounter = 0;

    private final List<Entity> summons = new ArrayList<>();
    private final java.util.Map<UUID, Long> playerMusicTracker = new ConcurrentHashMap<>();

    private boolean summoned75 = false;
    private boolean summoned50 = false;
    private boolean summoned25 = false;

    private static final Random RANDOM = new Random();

    public HalloweenBoss() {
        super(
                "halloween_boss",
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getString("bosses.halloween_boss.nome", "§5§lCeifador das Sombras")
                        .replace("&", "§"),
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getDouble("bosses.halloween_boss.hp", 6000.0));
    }

    private FileConfiguration getConfig() {
        return GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings();
    }

    // ================= SPAWN =================

    @Override
    public void spawn(Location loc) {
        this.entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
        entity.setRemoveWhenFarAway(false);
        if (entity instanceof Mob mob)
            mob.setPersistent(true);

        double damage = getConfig().getDouble("bosses.halloween_boss.normal_damage", 18.0);

        applySafeMaxHealth(entity, this.maxHealth);
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        }

        // Escala: enorme esqueleto sombrio
        double scale = getConfig().getDouble("bosses.halloween_boss.visual_effects.scale", 2.0);
        if (entity.getAttribute(Attribute.GENERIC_SCALE) != null) {
            entity.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
        }

        double speed = getConfig().getDouble("bosses.halloween_boss.movement_speed", 0.32);
        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        }

        entity.customName(LegacyComponentSerializer.legacySection().deserialize(this.name));
        entity.setCustomNameVisible(true);

        setupEquipment();

        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));

        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                "§5§lCEIFADOR §8» §7O véu se rompeu... §5eu vim colher vossas almas."));

        // Partículas de almas e fumaça no spawn
        loc.getWorld().spawnParticle(Particle.SOUL, loc.add(0, 1, 0), 80, 3, 2, 3, 0.05);
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 40, 2, 1, 2, 0.03);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 3.0f, 0.5f);

        updateBossBar();
    }

    private void setupEquipment() {
        if (entity.getEquipment() == null)
            return;

        // Capacete de abóbora esculpida (Halloween)
        entity.getEquipment().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));

        // Peitoral de netherite (sombrio)
        ItemStack chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta cm = chest.getItemMeta();
        if (cm != null) {
            cm.displayName(LegacyComponentSerializer.legacySection().deserialize("§5§lManto das Sombras"));
            Enchantment prot = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("protection"));
            if (prot != null)
                cm.addEnchant(prot, 4, true);
            chest.setItemMeta(cm);
        }
        entity.getEquipment().setChestplate(chest);
        entity.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        entity.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));

        // Foice (machado de netherite como arma)
        ItemStack scythe = new ItemStack(Material.NETHERITE_HOE);
        ItemMeta sm = scythe.getItemMeta();
        if (sm != null) {
            sm.displayName(LegacyComponentSerializer.legacySection().deserialize("§5§lFoice das Almas"));
            Enchantment sharp = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
            if (sharp != null)
                sm.addEnchant(sharp, 5, true);
            scythe.setItemMeta(sm);
        }
        entity.getEquipment().setItemInMainHand(scythe);
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

        int count75 = getConfig().getInt("bosses.halloween_boss.skills.minions.count_75", 3);
        int count50 = getConfig().getInt("bosses.halloween_boss.skills.minions.count_50", 5);
        int count25 = getConfig().getInt("bosses.halloween_boss.skills.minions.count_25", 8);

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

        // Aura de escuridão passiva (a cada 3 segundos)
        if (tickCounter % 60 == 0) {
            handleDarknessAura();
        }

        handleLocalMusic();
        handleAITargeting();
        handleAntiKite();

        long now = System.currentTimeMillis();
        long cooldown = getConfig().getLong("bosses.halloween_boss.skills.cooldown_base", 7000L);
        if (phase >= 3)
            cooldown = getConfig().getLong("bosses.halloween_boss.skills.cooldown_rage", 4500L);

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

    /**
     * Aura de escuridão passiva — Darkness + Wither leve em área.
     */
    private void handleDarknessAura() {
        double radius = getConfig().getDouble("bosses.halloween_boss.skills.darkness_aura.raio", 10.0);
        double damage = getConfig().getDouble("bosses.halloween_boss.skills.darkness_aura.dano", 3.0);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage);
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                p.spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.02);
            }
        });
    }

    // ================= FASES =================

    @Override
    public void onPhaseChange(int phase) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 3f, 0.3f);
        switch (phase) {
            case 2 -> {
                say("§5§lCEIFADOR §8» §7As sombras se adensam ao meu redor...");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            }
            case 3 -> {
                say("§5§lCEIFADOR §8» §cO véu se rompe! Sintam o toque da morte!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
            }
            case 4 -> {
                say("§5§lCEIFADOR §8» §4§lA NOITE ETERNA DESCEU! NINGUÉM ESCAPA DA CEIFA!");
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
            executeSoulHarvest();
        else if (r < 0.40)
            executeShadowTeleport();
        else if (r < 0.60)
            executeGhostWail();
        else if (r < 0.80)
            executeCursedFlames();
        else
            executeDeathMark();
    }

    /**
     * Colheita de Almas — Drena vida dos jogadores e cura o boss.
     */
    private void executeSoulHarvest() {
        say("§5§lCEIFADOR §8» §dVossas almas me pertencem!");

        double radius = getConfig().getDouble("bosses.halloween_boss.skills.soul_harvest.raio", 14.0);
        double damage = getConfig().getDouble("bosses.halloween_boss.skills.soul_harvest.dano", 8.0);
        double healPerPlayer = getConfig().getDouble("bosses.halloween_boss.skills.soul_harvest.cura_por_jogador",
                50.0);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.5f, 0.5f);
        entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0, 2, 0), 50, 3, 2, 3, 0.1);

        int playersHit = 0;
        for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1));

                // Partículas de alma saindo do jogador em direção ao boss
                p.spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
                playersHit++;
            }
        }

        // Cura o boss por jogador atingido
        if (playersHit > 0 && entity.getHealth() < maxHealth) {
            double heal = Math.min(healPerPlayer * playersHit, maxHealth - entity.getHealth());
            entity.setHealth(entity.getHealth() + heal);
        }
    }

    /**
     * Teleporte Sombrio — Aparece atrás de um jogador com ataque surpresa.
     */
    private void executeShadowTeleport() {
        say("§5§lCEIFADOR §8» §8Onde há sombra... eu estou.");

        int range = getConfig().getInt("bosses.halloween_boss.skills.shadow_teleport.range", 25);
        double damage = getConfig().getDouble("bosses.halloween_boss.skills.shadow_teleport.dano", 10.0);

        for (Entity e : entity.getNearbyEntities(range, range, range)) {
            if (e instanceof Player p) {
                // Partículas de desaparecimento
                entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation().add(0, 1, 0), 30,
                        0.5, 0.5, 0.5, 0.1);

                // Teleporta atrás do jogador
                Vector dir = p.getLocation().getDirection().normalize().multiply(-2.0);
                Location behind = p.getLocation().add(dir);
                behind.setY(p.getLocation().getY());
                entity.teleport(behind);

                // Partículas de aparecimento
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
                entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 30, 0.5, 1, 0.5, 0.1);

                // Dano + Darkness
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));

                setTeleportLock(p.getUniqueId(), 4000L);
                break;
            }
        }
    }

    /**
     * Lamento Fantasmagórico — AoE de som que dá Darkness + Nausea.
     */
    private void executeGhostWail() {
        say("§5§lCEIFADOR §8» §5§lOUÇAM O LAMENTO DOS MORTOS!");

        double radius = getConfig().getDouble("bosses.halloween_boss.skills.ghost_wail.raio", 16.0);
        double damage = getConfig().getDouble("bosses.halloween_boss.skills.ghost_wail.dano", 6.0);
        int darknessDuration = getConfig().getInt("bosses.halloween_boss.skills.ghost_wail.darkness_duration", 100);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GHAST_SCREAM, 3.0f, 0.3f);
        entity.getWorld().spawnParticle(Particle.SCULK_SOUL, entity.getLocation().add(0, 2, 0), 40, 4, 2, 4, 0.05);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, darknessDuration, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0));
                p.spawnParticle(Particle.SCULK_SOUL, p.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.03);
            }
        });
    }

    /**
     * Chamas Amaldiçoadas — Soul Fire em área que causa Wither.
     */
    private void executeCursedFlames() {
        say("§5§lCEIFADOR §8» §dAs chamas do além consomem os vivos!");

        double radius = getConfig().getDouble("bosses.halloween_boss.skills.cursed_flames.raio", 12.0);
        double damage = getConfig().getDouble("bosses.halloween_boss.skills.cursed_flames.dano", 7.0);
        int witherDuration = getConfig().getInt("bosses.halloween_boss.skills.cursed_flames.wither_duration", 100);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.5f);

        // Anel de chamas de alma
        Location center = entity.getLocation();
        for (double angle = 0; angle < 360; angle += 10) {
            double radians = Math.toRadians(angle);
            double x = center.getX() + radius * 0.6 * Math.cos(radians);
            double z = center.getZ() + radius * 0.6 * Math.sin(radians);
            entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                    new Location(center.getWorld(), x, center.getY() + 0.5, z), 5, 0.2, 0.3, 0.2, 0.02);
        }

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherDuration, 1));
                p.setFireTicks(80); // 4 segundos de fogo
                p.spawnParticle(Particle.SOUL_FIRE_FLAME, p.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
            }
        });
    }

    /**
     * Marca da Morte — Marca um jogador com Glowing + reflexão de dano.
     */
    private void executeDeathMark() {
        say("§5§lCEIFADOR §8» §4A morte te escolheu. Não há fuga.");

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

        int glowDuration = getConfig().getInt("bosses.halloween_boss.skills.death_mark.glow_duration", 200);

        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowDuration, 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));

        target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 2, 0), 30, 0.5, 1, 0.5, 0.05);
        target.playSound(target.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.3f);

        setTeleportLock(target.getUniqueId(), 5000L);
    }

    // ================= MINIONS =================

    private void spawnMinions(int amount) {
        summons.removeIf(e -> !e.isValid() || e.isDead());
        say("§5§lCEIFADOR §8» §7Ergam-se, servos das sombras!");

        for (int i = 0; i < amount; i++) {
            Location l = entity.getLocation().add((Math.random() - 0.5) * 10, 0, (Math.random() - 0.5) * 10);
            // Mix de fantasmas (Vex) e zumbis
            if (RANDOM.nextBoolean()) {
                Vex vex = (Vex) l.getWorld().spawnEntity(l, EntityType.VEX);
                vex.customName(LegacyComponentSerializer.legacySection().deserialize("§5§lEspírito Atormentado"));
                vex.setMetadata("halloween_minion",
                        new org.bukkit.metadata.FixedMetadataValue(GorvaxCore.getInstance(), true));
                summons.add(vex);
            } else {
                Zombie zombie = (Zombie) l.getWorld().spawnEntity(l, EntityType.ZOMBIE);
                zombie.customName(LegacyComponentSerializer.legacySection().deserialize("§8§lMorto-Vivo"));
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                zombie.setMetadata("halloween_minion",
                        new org.bukkit.metadata.FixedMetadataValue(GorvaxCore.getInstance(), true));
                if (zombie.getEquipment() != null) {
                    zombie.getEquipment().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
                }
                summons.add(zombie);
            }
        }
    }

    // ================= AURA =================

    private void spawnAuraParticles() {
        if (entity == null || entity.isDead())
            return;

        Location loc = entity.getLocation().add(0, 1.5, 0);
        int soulCount = getConfig().getInt("bosses.halloween_boss.visual_effects.aura.soul", 8);
        int flameCount = getConfig().getInt("bosses.halloween_boss.visual_effects.aura.soul_fire", 5);
        int smokeCount = getConfig().getInt("bosses.halloween_boss.visual_effects.aura.smoke", 4);

        for (Entity e : entity.getNearbyEntities(40, 40, 40)) {
            if (e instanceof Player p) {
                p.spawnParticle(Particle.SOUL, loc, soulCount, 0.6, 0.8, 0.6, 0.03);
                p.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, flameCount, 0.4, 0.6, 0.4, 0.02);
                p.spawnParticle(Particle.SMOKE, loc, smokeCount, 0.5, 0.5, 0.5, 0.01);
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
