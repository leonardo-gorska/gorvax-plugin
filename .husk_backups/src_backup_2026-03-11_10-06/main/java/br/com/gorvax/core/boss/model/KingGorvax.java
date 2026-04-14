package br.com.gorvax.core.boss.model;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class KingGorvax extends WorldBoss {

    private long lastSpecialAttack = 0;
    private long lastSummonTime = 0;
    private static final Random RANDOM = new Random();
    private final Map<UUID, Long> playerMusicTracker = new java.util.concurrent.ConcurrentHashMap<>();
    private final List<Entity> activeSummons = new ArrayList<>();
    private int tickCounter = 0;

    public KingGorvax() {
        super(
                "rei_gorvax",
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getString("bosses.rei_gorvax.nome", "§6§lREI GORVAX").replace("&", "§"),
                GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings()
                        .getDouble("bosses.rei_gorvax.hp", 1200.0));
    }

    private FileConfiguration getConfig() {
        return GorvaxCore.getInstance().getBossManager().getConfigManager().getSettings();
    }

    @Override
    public void spawn(Location loc) {
        this.entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
        entity.setRemoveWhenFarAway(false);
        entity.setPersistent(true);

        double damage = getConfig().getDouble("bosses.rei_gorvax.normal_damage", 18.0);

        applySafeMaxHealth(entity, this.maxHealth);

        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        }

        // Escala visual (60% maior)
        double scale = getConfig().getDouble("bosses.rei_gorvax.visual_effects.scale", 1.6);
        if (entity.getAttribute(Attribute.GENERIC_SCALE) != null) {
            entity.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
        }

        if (this.movementSpeed > 0 && entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(this.movementSpeed);
        }

        // HP já setado por applySafeMaxHealth
        entity.customName(LegacyComponentSerializer.legacySection().deserialize(this.name));
        entity.setCustomNameVisible(true);

        setupEquipment();

        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                this.name + " §8» §fO VERDADEIRO REI RETORNOU PARA RECLAMAR SEU TRONO!"));
    }

    @Override
    public void update() {
        if (entity == null || entity.isDead() || !entity.isValid())
            return;

        tickCounter++;
        long currentTime = System.currentTimeMillis();

        // Auras constantes (throttled a cada 4 ticks = 5x/s)
        if (tickCounter % 4 == 0) {
            spawnAuraParticles();
        }

        handleLocalMusic();
        handleAITargeting();
        handleAntiKite();
        // Clima é gerenciado pelo AtmosphereManager

        entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation().add(0, 1, 0), 20, 0.8, 1.5, 0.8,
                0.05);
        entity.getWorld().spawnParticle(Particle.LARGE_SMOKE, entity.getLocation(), 10, 0.5, 1.0, 0.5, 0.02);

        if (tickCounter % 5 == 0) {
            entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 2.5, 0), 5, 0.2, 0.2, 0.2,
                    0.01);
        }

        if (currentTime - lastSummonTime > 20000) {
            spawnServos();
            lastSummonTime = currentTime;
        }

        long cooldown = getConfig().getLong("bosses.rei_gorvax.skills.cooldown_base", 8000L);
        if (phase >= 3)
            cooldown = getConfig().getLong("bosses.rei_gorvax.skills.cooldown_rage", 5000L);

        if (currentTime - lastSpecialAttack > cooldown) {
            playSpecialAttack();
            lastSpecialAttack = currentTime;
        }
    }

    private void handleLocalMusic() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(entity.getWorld()))
                continue;
            double distSq = p.getLocation().distanceSquared(entity.getLocation());
            UUID uuid = p.getUniqueId();
            // Alcance 60 blocos (60^2 = 3600)
            if (distSq <= 3600) {
                if (!playerMusicTracker.containsKey(uuid) || (now - playerMusicTracker.get(uuid) > 140000)) {
                    p.playSound(entity, Sound.MUSIC_DISC_PIGSTEP, 3.75f, 1.0f);
                    playerMusicTracker.put(uuid, now);
                }
            } else {
                if (playerMusicTracker.containsKey(uuid)) {
                    p.stopSound(Sound.MUSIC_DISC_PIGSTEP);
                    playerMusicTracker.remove(uuid);
                }
            }
        }
    }

    private void spawnServos() {
        if (this.phase < 2)
            return;
        activeSummons.removeIf(e -> !e.isValid() || e.isDead());
        if (activeSummons.size() < 5) {
            this.say(this.name + " §8» §eSERVOS, LEVANTEM-SE!");
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
            for (int i = 0; i < 2; i++) {
                if (activeSummons.size() >= 5)
                    break;
                Location sLoc = entity.getLocation().add((Math.random() - 0.5) * 5, 0, (Math.random() - 0.5) * 5);
                sLoc.getWorld().playSound(sLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.8f);
                WitherSkeleton guard = (WitherSkeleton) sLoc.getWorld().spawnEntity(sLoc, EntityType.WITHER_SKELETON);
                guard.customName(LegacyComponentSerializer.legacySection().deserialize("§7Servo do Rei"));
                if (guard.getEquipment() != null) {
                    guard.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                    guard.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                }
                activeSummons.add(guard);
            }
        }
    }

    @Override
    public void playSpecialAttack() {
        double chance = Math.random();
        if (chance < 0.11)
            executeJumpAttack();
        else if (chance < 0.22)
            executePullAttack();
        else if (chance < 0.33)
            executePrisonAttack();
        else if (chance < 0.44)
            executeRepelAttack();
        else if (chance < 0.55)
            executeTeleportAttack();
        else if (chance < 0.66)
            executeMeteorAttack();
        else if (chance < 0.77)
            executeRoyalRoar();
        else if (chance < 0.88)
            executeThroneFlame();
        else
            executeRoyalDecree();
    }

    // ================= HABILIDADES EXISTENTES =================

    private void executeTeleportAttack() {
        this.say(this.name + " §8» §c§lSURPRESA!");

        int range = getConfig().getInt("bosses.rei_gorvax.skills.teleport.range", 25);
        long focusDuration = getConfig().getLong("bosses.rei_gorvax.skills.teleport.focus_duration_ms", 5000L);

        for (Entity e : entity.getNearbyEntities(range, range, range)) {
            if (e instanceof Player p) {
                Vector dir = p.getLocation().getDirection().normalize().multiply(-1.5);
                Location behind = p.getLocation().add(dir);
                behind.setY(p.getLocation().getY());
                entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5,
                        new Particle.DustOptions(Color.MAROON, 2.0f));
                entity.teleport(behind);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.2f);
                entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation(), 100, 0.5, 1, 0.5, 0.1);
                p.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_dialogue.gorvax_pull"));

                // Sincronização de teleporte: forçar foco neste alvo por X segundos
                setTeleportLock(p.getUniqueId(), focusDuration);
                break;
            }
        }
    }

    private void executeMeteorAttack() {
        this.say(this.name + " §8» §6§lQUE O CÉU SE FECHE!");
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.5f);
        entity.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, entity.getLocation().add(0, 2, 0), 5);
        for (Entity e : entity.getNearbyEntities(30, 30, 30)) {
            if (e instanceof Player p) {
                Location meteorLoc = p.getLocation().add(0, 15, 0);
                Fireball fireball = (Fireball) p.getWorld().spawnEntity(meteorLoc, EntityType.FIREBALL);
                fireball.setDirection(new Vector(0, -1, 0));

                float yield = (float) getConfig().getDouble("bosses.rei_gorvax.skills.meteor.explosao_yield", 2.5);

                fireball.setYield(yield);
                fireball.setShooter(entity);
                p.spawnParticle(Particle.LAVA, meteorLoc, 50, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    private void executeJumpAttack() {
        this.say(this.name + " §8» §eSINTAM O PESO DA MINHA COROA!");
        entity.setVelocity(new Vector(0, 1.3, 0));

        // Som em camadas: tiro + trovão
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // Partículas ao pular
        int explosionCount = getConfig()
                .getInt("bosses.rei_gorvax.visual_effects.skill_particles.jump_attack.explosion", 30);
        entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), explosionCount, 1, 1, 1, 0.1);
        entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), 15, 0.5, 0.5, 0.5, 0.05);

        Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
            if (entity == null || entity.isDead())
                return;
            entity.setVelocity(new Vector(0, -2.5, 0));

            // Som de aterrissagem (peso do rei)
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_ANVIL_LAND, 2.0f, 0.6f);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 0.5f);

            // Partículas ao aterrissar: poeira real + faíscas douradas
            int cloudCount = getConfig().getInt("bosses.rei_gorvax.visual_effects.skill_particles.jump_attack.cloud",
                    50);
            int critCount = getConfig().getInt("bosses.rei_gorvax.visual_effects.skill_particles.jump_attack.crit", 20);

            entity.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, entity.getLocation(), 3);
            entity.getWorld().spawnParticle(Particle.CLOUD, entity.getLocation(), cloudCount, 3, 0.2, 3, 0.1); // Poeira
                                                                                                               // real
            entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 0.5, 0), critCount, 2, 1, 2,
                    0.3); // Faíscas douradas

            double damage = getConfig().getDouble("bosses.rei_gorvax.skills.salto.dano", 22.0);
            double radius = getConfig().getDouble("bosses.rei_gorvax.skills.salto.raio", 10.0);

            entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
                if (e instanceof LivingEntity le && !activeSummons.contains(le) && le != entity) {
                    le.damage(damage, entity);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
                }
            });

            // Notificar aliado (sinergia cross-boss)
            notifyAlly("JumpAttack");
        }, 15L);
    }

    private void executePullAttack() {
        this.say(this.name + " §8» §5VENHA AQUI!");
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2f, 0.5f);
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 2f, 0.5f);

        double radius = getConfig().getDouble("bosses.rei_gorvax.skills.pull.raio", 15.0);
        double force = getConfig().getDouble("bosses.rei_gorvax.skills.pull.forca", 1.8);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                Vector pull = entity.getLocation().toVector().subtract(p.getLocation().toVector()).normalize()
                        .multiply(force);
                p.setVelocity(pull);
            }
        });
    }

    private void executePrisonAttack() {
        this.say(this.name + " §8» §b§lSUA ALMA ME PERTENCE!");

        int duration = getConfig().getInt("bosses.rei_gorvax.skills.prison.duracao_ticks", 80);

        entity.getNearbyEntities(12, 12, 12).forEach(e -> {
            if (e instanceof Player p) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 10));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, duration, 1));
                p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
            }
        });
    }

    private void executeRepelAttack() {
        this.say(this.name + " §8» §cSAIAM DE PERTO!");
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 0.8f);

        double radius = getConfig().getDouble("bosses.rei_gorvax.skills.repel.raio", 8.0);
        double force = getConfig().getDouble("bosses.rei_gorvax.skills.repel.forca", 2.2);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                Vector diff = p.getLocation().toVector().subtract(entity.getLocation().toVector());
                if (diff.lengthSquared() < 0.1)
                    diff = new Vector(1, 0, 0);
                Vector dir = diff.normalize().multiply(force);
                dir.setY(0.5);
                p.setVelocity(dir);
            }
        });
    }

    // ================= NOVAS HABILIDADES =================

    /**
     * Rugido do Soberano — Onda de choque que causa dano, Weakness e Nausea.
     * Efeitos visuais: SONIC_BOOM + DUST dourado.
     * Efeitos sonoros: RAVAGER_ROAR + WITHER_SHOOT.
     */
    private void executeRoyalRoar() {
        this.say(this.name + " §8» §6§lAJOELHEM-SE PERANTE SEU REI!");

        double radius = getConfig().getDouble("bosses.rei_gorvax.skills.royal_roar.raio", 12.0);
        double damage = getConfig().getDouble("bosses.rei_gorvax.skills.royal_roar.dano", 8.0);
        int weaknessDuration = getConfig().getInt("bosses.rei_gorvax.skills.royal_roar.weakness_duration", 80);
        int nauseaDuration = getConfig().getInt("bosses.rei_gorvax.skills.royal_roar.nausea_duration", 60);

        // Efeitos visuais no boss
        entity.getWorld().spawnParticle(Particle.SONIC_BOOM, entity.getLocation().add(0, 1, 0), 3);
        entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, 1.5, 0), 40, 2, 1.5, 2,
                new Particle.DustOptions(Color.fromRGB(255, 215, 0), 2.5f)); // Dourado

        // Efeitos sonoros
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 3.0f, 0.6f);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.3f);

        // Onda de choque expandindo
        Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
            if (entity == null || entity.isDead())
                return;
            entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, 0.5, 0), 80,
                    radius * 0.5, 0.3, radius * 0.5,
                    new Particle.DustOptions(Color.fromRGB(255, 170, 0), 1.8f));
        }, 5L);

        entity.getNearbyEntities(radius, radius, radius).forEach(e -> {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, nauseaDuration, 0));
                p.spawnParticle(Particle.SONIC_BOOM, p.getLocation().add(0, 1, 0), 1);
            }
        });
    }

    /**
     * Chama do Trono — Cria anel de fogo. Jogadores dentro recebem dano menor,
     * jogadores que fogem recebem chamas + dano maior.
     * Efeitos: FLAME + SOUL_FIRE_FLAME em anel, sons de fogo.
     */
    private void executeThroneFlame() {
        this.say(this.name + " §8» §c§lEU SOU O FOGO QUE CONSOME REINOS!");

        double innerRadius = getConfig().getDouble("bosses.rei_gorvax.skills.throne_flame.raio_interno", 6.0);
        double outerRadius = getConfig().getDouble("bosses.rei_gorvax.skills.throne_flame.raio_externo", 10.0);
        double innerDamage = getConfig().getDouble("bosses.rei_gorvax.skills.throne_flame.dano_dentro", 4.0);
        double outerDamage = getConfig().getDouble("bosses.rei_gorvax.skills.throne_flame.dano_fora", 6.0);
        int fireTicks = getConfig().getInt("bosses.rei_gorvax.skills.throne_flame.fire_ticks", 200);

        // Sons de fogo
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5f, 0.5f);
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 3.0f, 0.8f);

        // Criar anel visual de fogo (partículas em círculo)
        Location center = entity.getLocation();
        for (double angle = 0; angle < 360; angle += 10) {
            double radians = Math.toRadians(angle);
            // Anel interno
            double xInner = center.getX() + innerRadius * Math.cos(radians);
            double zInner = center.getZ() + innerRadius * Math.sin(radians);
            entity.getWorld().spawnParticle(Particle.FLAME,
                    new Location(center.getWorld(), xInner, center.getY() + 0.5, zInner), 3, 0.1, 0.3, 0.1, 0.01);

            // Anel externo
            double xOuter = center.getX() + outerRadius * Math.cos(radians);
            double zOuter = center.getZ() + outerRadius * Math.sin(radians);
            entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                    new Location(center.getWorld(), xOuter, center.getY() + 0.5, zOuter), 3, 0.1, 0.3, 0.1, 0.01);
        }

        // Aplicar dano baseado na posição:
        entity.getNearbyEntities(outerRadius, outerRadius, outerRadius).forEach(e -> {
            if (e instanceof Player p) {
                double distSq = p.getLocation().distanceSquared(entity.getLocation());
                if (distSq <= innerRadius * innerRadius) {
                    // Dentro do anel: dano menor, sem chamas
                    p.damage(innerDamage, entity);
                    p.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_dialogue.gorvax_close"));
                    p.spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
                } else {
                    // Fora do anel interno mas dentro do externo: dano maior + chamas
                    p.damage(outerDamage, entity);
                    p.setFireTicks(fireTicks);
                    p.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_dialogue.gorvax_flee"));
                    p.spawnParticle(Particle.SOUL_FIRE_FLAME, p.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
                }
            }
        });

        // Segunda onda visual após 10 ticks
        Bukkit.getScheduler().runTaskLater(GorvaxCore.getInstance(), () -> {
            if (entity == null || entity.isDead())
                return;
            for (double angle = 0; angle < 360; angle += 15) {
                double radians = Math.toRadians(angle);
                for (double r = innerRadius; r <= outerRadius; r += 1.0) {
                    double x = entity.getLocation().getX() + r * Math.cos(radians);
                    double z = entity.getLocation().getZ() + r * Math.sin(radians);
                    entity.getWorld().spawnParticle(Particle.FLAME,
                            new Location(entity.getWorld(), x, entity.getLocation().getY() + 0.3, z),
                            1, 0, 0.1, 0, 0.005);
                }
            }
        }, 10L);
    }

    /**
     * Decreto Real — Marca um jogador como "Traidor" com Glowing.
     * O traidor reflete 50% do dano que causa ao boss de volta para si.
     * Aliados próximos ao traidor recebem Slowness I.
     * Efeitos: ENCHANT + som de EVOKER.
     */
    private void executeRoyalDecree() {
        int glowDuration = getConfig().getInt("bosses.rei_gorvax.skills.royal_decree.glowing_duration", 140);
        int slowDuration = getConfig().getInt("bosses.rei_gorvax.skills.royal_decree.slowness_allies_duration", 60);
        double allyRadius = getConfig().getDouble("bosses.rei_gorvax.skills.royal_decree.raio_aliados", 8.0);

        // Encontrar jogador aleatório para marcar
        List<Player> candidates = new ArrayList<>();
        for (Entity e : entity.getNearbyEntities(25, 25, 25)) {
            if (e instanceof Player p && p.getGameMode() == GameMode.SURVIVAL) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty())
            return;

        Player target = candidates.get(RANDOM.nextInt(candidates.size()));

        this.say(this.name + " §8» §4§l" + target.getName() + " §c§lFOI DECLARADO TRAIDOR DA COROA!");

        // Efeitos sonoros
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 2.5f, 0.5f);
        entity.getWorld().playSound(target.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 2.0f, 0.7f);

        // Marcar o jogador com Glowing
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowDuration, 0));
        target.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_dialogue.gorvax_traitor"));

        // Partículas de encantamento no alvo
        target.spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1.5, 0), 50, 0.5, 1.0, 0.5, 0.5);
        target.spawnParticle(Particle.DUST, target.getLocation().add(0, 2, 0), 20, 0.3, 0.3, 0.3,
                new Particle.DustOptions(Color.RED, 1.5f));

        // Setar o decreto no WorldBoss para o BossListener usar
        this.royalDecreeTarget = target.getUniqueId();
        this.royalDecreeUntil = System.currentTimeMillis() + (glowDuration * 50L); // ticks -> ms

        // Aplicar Slowness nos aliados próximos ao traidor
        for (Entity e : target.getNearbyEntities(allyRadius, allyRadius, allyRadius)) {
            if (e instanceof Player ally && ally != target) {
                ally.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, 0));
                ally.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_dialogue.gorvax_ally_warn"));
                ally.spawnParticle(Particle.ENCHANT, ally.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.2);
            }
        }
    }

    // ================= FASES =================

    @Override
    public void onPhaseChange(int newPhase) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2f, 0.5f);
        if (newPhase == 2) {
            this.say(this.name + " §8» §eNão pense que será tão fácil!");
        } else if (newPhase == 3) {
            Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                    this.name + " §8» §cMINHA FÚRIA CONSUMIRÁ ESTE MUNDO!"));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 1)); // Força II
            entity.getWorld().strikeLightningEffect(entity.getLocation());
        } else if (newPhase == 4) {
            this.say(this.name + " §8» §4ESTE É O SEU FIM! EU SOU O REI ABSOLUTO!");
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.5f);
            entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 1)); // Força II
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 0)); // Speed I
            entity.getWorld().strikeLightningEffect(entity.getLocation());
            entity.getWorld().strikeLightningEffect(entity.getLocation().add(3, 0, 0));
            entity.getWorld().strikeLightningEffect(entity.getLocation().add(-3, 0, 0));
        }
    }

    // ================= EQUIPAMENTO =================

    private void setupEquipment() {
        if (entity.getEquipment() == null)
            return;
        ItemStack goldHelm = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta hm = goldHelm.getItemMeta();
        if (hm != null) {
            Enchantment prot = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("protection"));
            if (prot == null)
                prot = Enchantment.getByName("PROTECTION_ENVIRONMENTAL");
            if (prot != null)
                hm.addEnchant(prot, 4, true);
            goldHelm.setItemMeta(hm);
        }
        entity.getEquipment().setHelmet(goldHelm);

        ItemStack cape = new ItemStack(Material.ELYTRA);
        ItemMeta cm = cape.getItemMeta();
        if (cm != null) {
            cm.displayName(LegacyComponentSerializer.legacySection().deserialize("§5Capa das Sombras Reais"));
            cm.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            cape.setItemMeta(cm);
        }
        entity.getEquipment().setChestplate(cape);

        entity.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        entity.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));

        // Espada dourada encantada (visual imperial)
        ItemStack goldenSword = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta sm = goldenSword.getItemMeta();
        if (sm != null) {
            sm.displayName(LegacyComponentSerializer.legacySection().deserialize("§6Lâmina Imperial"));
            sm.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            Enchantment sharp = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
            if (sharp != null)
                sm.addEnchant(sharp, 10, true);
            Enchantment fire = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("fire_aspect"));
            if (fire != null)
                sm.addEnchant(fire, 3, true);
            goldenSword.setItemMeta(sm);
        }
        entity.getEquipment().setItemInMainHand(goldenSword);
    }

    /**
     * Spawna partículas de aura constante (FLAME, SMOKE, SOUL azul de Indrax)
     */
    private void spawnAuraParticles() {
        if (entity == null || entity.isDead())
            return;

        Location loc = entity.getLocation().add(0, 1.5, 0);
        int flameCount = getConfig().getInt("bosses.rei_gorvax.visual_effects.aura.flame", 15);
        int smokeCount = getConfig().getInt("bosses.rei_gorvax.visual_effects.aura.smoke", 8);
        int soulCount = getConfig().getInt("bosses.rei_gorvax.visual_effects.aura.soul", 5);

        // Apenas spawn para jogadores próximos (< 40 blocos) - otimização
        for (Entity e : entity.getNearbyEntities(40, 40, 40)) {
            if (e instanceof Player p) {
                p.spawnParticle(Particle.FLAME, loc, flameCount, 0.5, 0.8, 0.5, 0.05);
                p.spawnParticle(Particle.SMOKE, loc, smokeCount, 0.4, 0.6, 0.4, 0.02);
                p.spawnParticle(Particle.SOUL, loc, soulCount, 0.3, 0.5, 0.3, 0.03); // Influência de Indrax
            }
        }
    }

    @Override
    public void cleanup() {
        activeSummons.forEach(e -> {
            if (e != null)
                e.remove();
        });
        activeSummons.clear();
        // Parar música (clima é gerenciado pelo AtmosphereManager)
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.stopSound(Sound.MUSIC_DISC_PIGSTEP);
        }
        playerMusicTracker.clear();
    }
}
