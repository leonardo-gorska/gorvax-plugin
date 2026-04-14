package br.com.gorvax.core.boss.model;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class IndraxAbissal extends WorldBoss {

    private long lastSpecialAttack = 0;
    private int tickCounter = 0;

    private final List<Entity> summons = new ArrayList<>();
    private final Map<Location, Material> originalBlocks = new HashMap<>();

    private boolean summoned75 = false;
    private boolean summoned50 = false;
    private boolean summoned25 = false;

    private static final Random RANDOM = new Random();
    private static final Material[] CORRUPTION_BLOCKS = {
            Material.SCULK,
            Material.SCULK_CATALYST,
            Material.BLACK_CONCRETE,
            Material.COAL_BLOCK,
            Material.OBSIDIAN,
            Material.CRYING_OBSIDIAN,
            Material.SOUL_SOIL,
            Material.SOUL_SOIL,
            Material.NETHER_BRICKS,
            Material.MUD_BRICKS,
            Material.MUD_BRICKS,
            Material.DEEPSLATE,
            Material.DEEPSLATE_TILES
    };

    public IndraxAbissal() {
        super(
                "indrax",
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getString("bosses.indrax.nome", "§5§lIndrax Abissal")
                        .replace("&", "§"),
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings().getDouble("bosses.indrax.hp",
                        1100.0));
    }

    private FileConfiguration getConfig() {
        return GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings();
    }

    // ================= SPAWN =================

    @Override
    public void spawn(Location loc) {
        this.entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.WARDEN);
        Warden warden = (Warden) entity;

        double damage = getConfig().getDouble("bosses.indrax.normal_damage", 16.0);

        applySafeMaxHealth(warden, maxHealth);
        if (warden.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null)
            warden.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        if (warden.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null)
            warden.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.30);

        // HP já setado por applySafeMaxHealth
        warden.customName(LegacyComponentSerializer.legacySection().deserialize(name));
        warden.setCustomNameVisible(true);
        warden.setRemoveWhenFarAway(false);

        // Escala visual (30% maior - gigante das sombras)
        double scale = getConfig().getDouble("bosses.indrax.visual_effects.scale", 1.3);
        if (warden.getAttribute(Attribute.GENERIC_SCALE) != null) {
            warden.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
        }

        warden.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0)); // Resistência I
                                                                                                     // (nerfado de II)
        warden.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));

        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                "§5§lINDRAX §8» §fTantos vieram com chamas de esperança... todos serviram apenas para tornar minha sombra mais densa."));

        int radius = getConfig().getInt("bosses.indrax.skills.contamination.radius_spawn", 12);
        applyContamination(loc, radius);

        // Clima e tempo são gerenciados pelo AtmosphereManager

        // Garante exibição imediata da BossBar
        updateBossBar();
    }

    // ================= UPDATE =================

    @Override
    public void update() {
        if (entity == null || entity.isDead())
            return;

        tickCounter++;

        // Auras constantes (throttled a cada 4 ticks = 5x/s)
        if (tickCounter % 4 == 0) {
            spawnAuraParticles();
        }

        long currentTime = System.currentTimeMillis();
        double hp = entity.getHealth() / maxHealth;

        // Fases são gerenciadas pelo WorldBoss.updateBossBar()

        // ===== INVOCAÇÕES =====
        int count75 = getConfig().getInt("bosses.indrax.skills.minions.count_75", 4);
        int count50 = getConfig().getInt("bosses.indrax.skills.minions.count_50", 6);
        int count25 = getConfig().getInt("bosses.indrax.skills.minions.count_25", 8);

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
            executeAbyssCollapse();
            summoned25 = true;
        }

        // ===== AURA DO ABISMO =====
        handleLocalMusic();

        int pulseInterval = getConfig().getInt("bosses.indrax.skills.pulso.interval_ticks", 60);
        if (tickCounter % pulseInterval == 0) {
            int contaminationRadius = getConfig().getInt("bosses.indrax.skills.contamination.radius_periodic", 8);
            applyContamination(entity.getLocation(), contaminationRadius);

            double pulseRadius = getConfig().getDouble("bosses.indrax.skills.pulso.raio", 12.0);
            entity.getNearbyEntities(pulseRadius, pulseRadius, pulseRadius).forEach(e -> {
                if (e instanceof Player p) {
                    double damage = getConfig().getDouble("bosses.indrax.skills.pulso.dano", 3.0);
                    p.damage(damage);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
                    p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.5f);
                    p.spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.05);
                }
            });
        }

        // ===== ATAQUES =====
        long now = System.currentTimeMillis();

        handleAITargeting();
        handleAntiKite();

        long cooldown = getConfig().getLong("bosses.indrax.skills.cooldown_base", 7000L);
        if (phase >= 3)
            cooldown = getConfig().getLong("bosses.indrax.skills.cooldown_rage", 5000L);

        if (now - lastSpecialAttack > cooldown) {
            playSpecialAttack();
            lastSpecialAttack = now;
        }

        if (tickCounter % 150 == 0) {
            executeVoidPulse();
        }
    }

    private final Map<UUID, Long> playerMusicTracker = new java.util.concurrent.ConcurrentHashMap<>();

    private void handleLocalMusic() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(entity.getWorld()))
                continue;

            double distSq = p.getLocation().distanceSquared(entity.getLocation());
            UUID uuid = p.getUniqueId();

            // Alcance 60 blocos (60^2 = 3600)
            if (distSq <= 3600) {
                if (!playerMusicTracker.containsKey(uuid) || (now - playerMusicTracker.get(uuid) > 190000)) {
                    p.playSound(entity, Sound.MUSIC_DISC_OTHERSIDE, 3.75f, 1.0f);
                    playerMusicTracker.put(uuid, now);
                }
            } else {
                if (playerMusicTracker.containsKey(uuid)) {
                    p.stopSound(Sound.MUSIC_DISC_OTHERSIDE);
                    playerMusicTracker.remove(uuid);
                }
            }
        }
    }

    // ================= FASE =================

    @Override
    public void onPhaseChange(int phase) {
        switch (phase) {
            case 2 -> {
                say("§5§lINDRAX §8» §7Vou apagar as luzes agora. Tente não entrar em pânico enquanto morre.");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1)); // Resistência
                                                                                                             // II
                                                                                                             // (nerfado
                                                                                                             // de III)
            }
            case 3 -> {
                say("§5§lINDRAX §8» §cVocê é só um borrão na minha história. Em dez minutos, nem vou lembrar que você existiu...");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1)); // Força II
                                                                                                           // (nerfado
                                                                                                           // de III)
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0)); // Speed I
                                                                                                        // (nerfado de
                                                                                                        // II)
            }
            case 4 -> {
                say("§5§lINDRAX §8» §4Vou simplificar o seu destino: você entra no meu vazio e o mundo esquece que você sequer nasceu!");
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1)); // Resistência
                                                                                                             // II
                                                                                                             // (nerfado
                                                                                                             // de IV)
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2)); // Força III
                                                                                                           // (nerfado
                                                                                                           // de IV)
            }
        }

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WARDEN_ROAR, 3f, 0.3f);
    }

    // ================= ATAQUES =================

    @Override
    public void playSpecialAttack() {
        double r = Math.random();

        if (r < 0.15)
            executeSingularity();
        else if (r < 0.30)
            executeDarkParalysis();
        else if (r < 0.45)
            executeVoidErase();
        else if (r < 0.60)
            executeAbyssalRage();
        else if (r < 0.75)
            executeSoulDrain();
        else
            executeAbyssCollapse();
    }

    private void executeVoidPulse() {
        say("§5§lINDRAX §8» §0Onde a luz toca, ela cega. Na minha escuridão, finalmente você verá a verdade!");

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 2f, 0.5f);

        double radius = getConfig().getDouble("bosses.indrax.skills.pulso.raio", 12.0);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                double damage = getConfig().getDouble("bosses.indrax.skills.pulso.dano", 3.0);
                p.damage(damage);
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1));
                p.spawnParticle(Particle.SONIC_BOOM, p.getLocation(), 1);
            }
        });
    }

    private void executeSingularity() {
        say("§5§lINDRAX §8» §5Tudo será arrastado para o fim.");

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 0.4f);
        double radius = getConfig().getDouble("bosses.indrax.skills.singularidade.raio", 16.0);
        double force = getConfig().getDouble("bosses.indrax.skills.singularidade.pull_force", 1.5);
        double pullY = getConfig().getDouble("bosses.indrax.skills.singularidade.pull_y", 0.4);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                Vector pull = entity.getLocation().toVector()
                        .subtract(p.getLocation().toVector()).normalize().multiply(force);
                pull.setY(pullY); // Nerfado de 0.8 para 0.4
                p.setVelocity(pull);

                Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
                    double damage = getConfig().getDouble("bosses.indrax.skills.singularidade.dano", 10.0);
                    p.damage(damage);
                    p.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, p.getLocation(), 1);
                }, 20L);
            }
        });
    }

    private void executeDarkParalysis() {
        say("§5§lINDRAX §8» §7Ajoelhe-se. O solo que você pisa já pertence ao meu vazio, você apenas ainda não percebeu.");

        double radius = getConfig().getDouble("bosses.indrax.skills.dark_paralysis.raio", 14.0);
        int slow = getConfig().getInt("bosses.indrax.skills.dark_paralysis.slow_duration", 100);
        int slowLevel = getConfig().getInt("bosses.indrax.skills.dark_paralysis.slow_level", 5);
        int blind = getConfig().getInt("bosses.indrax.skills.dark_paralysis.blind_duration", 120);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slow, slowLevel)); // Nerfado de 255 para
                                                                                                 // 5
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blind, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, blind, 0));
            }
        });
    }

    private void executeVoidErase() {
        say("§5§lINDRAX §8» §5Sua existência termina agora.");

        double radius = getConfig().getDouble("bosses.indrax.skills.void_erase.raio", 12.0);
        int duration = getConfig().getInt("bosses.indrax.skills.void_erase.wither_duration", 120);
        int level = getConfig().getInt("bosses.indrax.skills.void_erase.wither_level", 2);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, level)); // Nerfado de nível 4
                                                                                               // para 2
                p.spawnParticle(Particle.REVERSE_PORTAL, p.getLocation(), 50);
            }
        });
    }

    private void executeAbyssalRage() {
        say("§5§lINDRAX §8» §cMEU ÓDIO É INFINITO.");

        int duration = getConfig().getInt("bosses.indrax.skills.rage.duracao_ticks", 140);
        int level = getConfig().getInt("bosses.indrax.skills.rage.nivel_forca", 2);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, level)); // Nerfado de 4 para 2
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0)); // Nerfado de 2 para 1
    }

    private void executeSoulDrain() {
        say("§5§lINDRAX §8» §dVocê chama isso de determinação? Para mim, é apenas o espasmo de um cadáver que ainda não aceitou que o coração parou de bater.");

        double radius = getConfig().getDouble("bosses.indrax.skills.dreno.raio", 10.0);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                double damage = getConfig().getDouble("bosses.indrax.skills.dreno.dano", 10.0);
                double heal = getConfig().getDouble("bosses.indrax.skills.dreno.cura_boss", 50.0); // Nerfado de 120
                                                                                                   // para 50
                p.damage(damage);
                entity.setHealth(Math.min(entity.getHealth() + heal, maxHealth));
                p.spawnParticle(Particle.SOUL_FIRE_FLAME, p.getLocation(), 30);
            }
        });
    }

    private void executeAbyssCollapse() {
        say("§5§lINDRAX §8» §4§lCOLAPSO DO ABISMO!");
        double radius = getConfig().getDouble("bosses.indrax.skills.abyss_collapse.raio", 18.0);
        int duration = getConfig().getInt("bosses.indrax.skills.abyss_collapse.levitation_duration", 30);
        int level = getConfig().getInt("bosses.indrax.skills.abyss_collapse.levitation_level", 1);

        // Efeito de vácuo visual (SONIC_BOOM no centro + SMOKE aspirado)
        Location center = entity.getLocation().add(0, 2, 0);
        int sonicCount = getConfig().getInt("bosses.indrax.visual_effects.skill_particles.abyss_collapse.sonic_boom",
                10);
        int smokeCount = getConfig().getInt("bosses.indrax.visual_effects.skill_particles.abyss_collapse.smoke_large",
                20);
        int portalCount = getConfig()
                .getInt("bosses.indrax.visual_effects.skill_particles.abyss_collapse.reverse_portal", 15);

        entity.getWorld().spawnParticle(Particle.SONIC_BOOM, center, sonicCount, 0.1, 0.1, 0.1, 0);
        entity.getWorld().spawnParticle(Particle.SMOKE, center, smokeCount, 2, 0.5, 2, 0.2); // Aspirado para cima
        entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, portalCount, 1.5, 1.5, 1.5, 0.1);

        // Som em camadas: carga do Warden + âncora de respawn
        entity.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2.0f, 0.7f);
        entity.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f, 0.5f);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration, level)); // Nerfado: duração
                                                                                                   // 30t, nível 1
                // Velocity Y=2 REMOVIDO — reduz dano de queda drasticamente
                // Agora só levitação leve, causando ~8-12 HP de dano de queda ao invés de ~40+
            }
        });

        // Notificar aliado (sinergia cross-boss)
        notifyAlly("AbyssCollapse");
    }

    // ================= MINIONS =================

    private void spawnMinions(int amount) {
        summons.removeIf(e -> !e.isValid() || e.isDead());
        say("§5§lINDRAX §8» §dBasta de brincadeiras. Cães de guarda, estraçalhem o que restou desses vermes!");

        for (int i = 0; i < amount; i++) {
            Location l = entity.getLocation().add((Math.random() - 0.5) * 12, 0, (Math.random() - 0.5) * 12);
            Enderman m = (Enderman) l.getWorld().spawnEntity(l, EntityType.ENDERMAN);
            m.customName(LegacyComponentSerializer.legacySection().deserialize("§8§lSombra do Vazio"));
            m.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
            m.setMetadata("indrax_minion", new org.bukkit.metadata.FixedMetadataValue(GorvaxCore.getInstance(), true));
            summons.add(m);
        }
    }

    @Override
    protected void handleAITargeting() {
        if (!(entity instanceof Warden warden) || warden.isDead())
            return;

        // Clear anger against minions
        for (Entity minion : summons) {
            if (minion instanceof LivingEntity livingMinion) {
                warden.clearAnger(livingMinion);
            }
        }

        // Sincronização de teleporte
        long now = System.currentTimeMillis();
        if (teleportTargetUUID != null && now < teleportLockUntil) {
            Player tpTarget = Bukkit.getPlayer(teleportTargetUUID);
            if (tpTarget != null && tpTarget.isOnline() && !tpTarget.isDead()
                    && tpTarget.getWorld().equals(entity.getWorld())) {
                warden.setTarget(tpTarget);
                warden.setAnger(tpTarget, 150);
                return;
            }
        } else if (teleportTargetUUID != null && now >= teleportLockUntil) {
            teleportTargetUUID = null;
        }

        LivingEntity currentTarget = warden.getTarget();

        // Sistema de threat score integrado para Warden
        if (currentTarget == null || currentTarget.getLocation().distanceSquared(warden.getLocation()) > 400
                || Math.random() < 0.12) {

            Player best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            double maxDps = 0;
            List<Player> nearbyPlayers = new ArrayList<>();
            for (Entity e : warden.getNearbyEntities(40, 40, 40)) {
                if (e instanceof Player p && p.getGameMode() == GameMode.SURVIVAL && !p.isDead()) {
                    nearbyPlayers.add(p);
                    double dmg = damageDealt.getOrDefault(p.getUniqueId(), 0.0);
                    if (dmg > maxDps)
                        maxDps = dmg;
                }
            }

            for (Player p : nearbyPlayers) {
                double score = 0;
                double distSq = p.getLocation().distanceSquared(warden.getLocation());

                // DPS score
                double playerDmg = damageDealt.getOrDefault(p.getUniqueId(), 0.0);
                if (maxDps > 0)
                    score += (playerDmg / maxDps) * 40;

                // Fugitivo
                if (distSq > 625)
                    score += 30; // > 25 blocos
                else if (distSq > 225)
                    score += 15; // > 15 blocos

                // Proximidade
                if (distSq <= 100)
                    score += 10; // <= 10 blocos

                // HP baixo
                if (p.getHealth() < 6.0)
                    score -= 10;

                if (score > bestScore) {
                    bestScore = score;
                    best = p;
                }
            }

            if (best != null) {
                warden.setTarget(best);
                warden.setAnger(best, 150);
            }
        } else if (currentTarget instanceof Player p) {
            warden.setAnger(p, 150);
        }
    }

    // ================= CONTAMINAÇÃO =================

    private void applyContamination(Location center, int radius) {
        double chance = getConfig().getDouble("bosses.indrax.skills.contamination.chance", 0.35);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block b = center.clone().add(x, -1, z).getBlock();

                if (b.getType() != Material.AIR && Math.random() < chance) {
                    originalBlocks.putIfAbsent(b.getLocation(), b.getType());
                    Material fake = CORRUPTION_BLOCKS[RANDOM.nextInt(CORRUPTION_BLOCKS.length)]; // Usa RANDOM estático

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getWorld().equals(center.getWorld())
                                && p.getLocation().distanceSquared(center) < 2304) { // 48^2
                            p.sendBlockChange(b.getLocation(), fake.createBlockData());
                            if (RANDOM.nextDouble() < 0.1) {
                                p.spawnParticle(Particle.SCULK_CHARGE_POP, b.getLocation().add(0.5, 1, 0.5), 1);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Spawna partículas de aura constante (SCULK_SOUL, VIBRATION, END_ROD dourado
     * de Gorvax, SMOKE_LARGE)
     */
    private void spawnAuraParticles() {
        if (entity == null || entity.isDead())
            return;

        Location loc = entity.getLocation().add(0, 1.5, 0);
        int sculkCount = getConfig().getInt("bosses.indrax.visual_effects.aura.sculk_soul", 12);
        int vibrationCount = getConfig().getInt("bosses.indrax.visual_effects.aura.vibration", 6);
        int endRodCount = getConfig().getInt("bosses.indrax.visual_effects.aura.end_rod", 3);
        int smokeLargeCount = getConfig().getInt("bosses.indrax.visual_effects.aura.smoke_large", 4);

        // Apenas spawn para jogadores próximos (< 40 blocos) - otimização
        for (Entity e : entity.getNearbyEntities(40, 40, 40)) {
            if (e instanceof Player p) {
                p.spawnParticle(Particle.SCULK_SOUL, loc, sculkCount, 0.5, 0.8, 0.5, 0.05);
                p.spawnParticle(Particle.SCULK_CHARGE_POP, loc, vibrationCount, 0.4, 0.6, 0.4, 0.02);
                p.spawnParticle(Particle.END_ROD, loc, endRodCount, 0.3, 0.5, 0.3, 0.03); // Influência de Gorvax
                p.spawnParticle(Particle.SMOKE, loc.clone().add(0, -1, 0), smokeLargeCount, 0.4, 0.1, 0.4, 0.01); // Névoa
                                                                                                                  // aos
                                                                                                                  // pés
            }
        }
    }

    @Override
    public void cleanup() {
        originalBlocks.forEach((l, m) -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(l.getWorld()) && p.getLocation().distanceSquared(l) < 2304) { // 48^2
                    p.sendBlockChange(l, m.createBlockData());
                }
            }
        });
        originalBlocks.clear();
        summons.forEach(e -> {
            if (e.isValid())
                e.remove();
        });
        summons.clear();

        // Parar música (clima e tempo são gerenciados pelo AtmosphereManager)
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.stopSound(Sound.MUSIC_DISC_OTHERSIDE);
        }
        playerMusicTracker.clear();
    }
}
