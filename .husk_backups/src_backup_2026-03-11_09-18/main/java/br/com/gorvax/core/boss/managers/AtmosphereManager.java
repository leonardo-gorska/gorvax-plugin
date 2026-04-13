package br.com.gorvax.core.boss.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.boss.model.*;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

/**
 * Gerenciador de Eventos Atmosféricos dos Bosses.
 * 
 * Controla:
 * - Fúria da Terra (Gorvax): Noite + Tempestade + Trovões
 * - Eclipse Estático (Indrax): Noite Profunda + Céu Limpo
 * - Ruptura Temporal (Ambos próximos): Alternância suave entre estados
 */
public class AtmosphereManager {

    public enum AtmosphereState {
        NORMAL,
        GORVAX_FURY,
        INDRAX_ECLIPSE,
        TEMPORAL_RUPTURE,
        ZARITH_JUNGLE,
        KALDUR_FROST,
        SKULKOR_DEATH,
        XYLOS_DISTORTION,
        VULGATHOR_HEAT
    }

    private final GorvaxCore plugin;
    private AtmosphereState currentState = AtmosphereState.NORMAL;

    // Ruptura Temporal
    private long lastRuptureSwitch = 0;
    private long nextSwitchInterval = 0;
    private boolean ruptureIsGorvaxPhase = true;

    // Fade de transição
    private boolean isFading = false;
    private int fadeTicks = 0;
    private int fadeMaxTicks = 4; // 4 segundos (tick() roda a cada 1s)
    private AtmosphereState fadeTarget = AtmosphereState.NORMAL;

    // Trovões (Gorvax)
    private int thunderCooldown = 0;

    // Eclipse pulso (Indrax)
    private int eclipsePulseCooldown = 0;

    // Rumble periódico (Ruptura)
    private int rumbleCooldown = 0;

    // Sinergia persistente (beam + aura)
    private int beamCooldown = 0;
    private int auraCooldown = 0;

    // Tick counter (Agora conta segundos)
    private int tickCounter = 0;

    private static final Random RANDOM = new Random();

    // Guardar estado original do mundo
    private long originalTime = -1;
    private boolean originalStorm = false;
    private boolean originalThundering = false;
    private boolean originalDaylightCycle = true;

    // Otimização PERF-01: Cache de bosses ativos
    private WorldBoss cachedGorvax, cachedIndrax, cachedZarith, cachedKaldur, cachedSkulkor, cachedXylos,
            cachedVulgathor;

    public AtmosphereManager(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration getConfig() {
        return plugin.getBossManager().getConfigManager().getSettings();
    }

    // ================= TICK PRINCIPAL =================

    /**
     * Chamado a cada tick pelo BossTask. Orquestra todo o sistema atmosférico.
     */
    public void tick() {
        tickCounter++;

        // Otimização: Só atualiza a lista de bosses ativos a cada 5 segundos
        if (tickCounter % 5 == 0 || currentState == AtmosphereState.NORMAL) {
            updateActiveBosses();
        }

        // Determinar estado desejado (prioridade: Ruptura > Gorvax/Indrax > T2 bosses)
        AtmosphereState desiredState = determineDesiredState();

        if (desiredState != currentState && !isFading) {
            startFade(desiredState);
        }

        if (isFading) {
            processFade();
        } else {
            // Processar estado atual
            switch (currentState) {
                case GORVAX_FURY:
                    processGorvaxFury();
                    break;
                case INDRAX_ECLIPSE:
                    processIndraxEclipse();
                    break;
                case TEMPORAL_RUPTURE:
                    processTemporalRupture(cachedGorvax, cachedIndrax);
                    break;
                case ZARITH_JUNGLE:
                    processZarithJungle();
                    break;
                case KALDUR_FROST:
                    processKaldurFrost();
                    break;
                case SKULKOR_DEATH:
                    processSkulkorDeath();
                    break;
                case XYLOS_DISTORTION:
                    processXylosDistortion();
                    break;
                case VULGATHOR_HEAT:
                    processVulgathorHeat();
                    break;
                default:
                    break;
            }
        }
    }

    private void updateActiveBosses() {
        cachedGorvax = findBoss(KingGorvax.class);
        cachedIndrax = findBoss(IndraxAbissal.class);
        cachedZarith = findBoss(Zarith.class);
        cachedKaldur = findBoss(Kaldur.class);
        cachedSkulkor = findBoss(Skulkor.class);
        cachedXylos = findBoss(XylosDevorador.class);
        cachedVulgathor = findBoss(Vulgathor.class);
    }

    private AtmosphereState determineDesiredState() {
        boolean gorvaxAlive = cachedGorvax != null && cachedGorvax.isAlive();
        boolean indraxAlive = cachedIndrax != null && cachedIndrax.isAlive();
        boolean zarithAlive = cachedZarith != null && cachedZarith.isAlive();
        boolean kaldurAlive = cachedKaldur != null && cachedKaldur.isAlive();
        boolean skulkorAlive = cachedSkulkor != null && cachedSkulkor.isAlive();
        boolean xylosAlive = cachedXylos != null && cachedXylos.isAlive();
        boolean vulgathorAlive = cachedVulgathor != null && cachedVulgathor.isAlive();

        if (gorvaxAlive && indraxAlive) {
            if (cachedGorvax.getEntity() != null && cachedIndrax.getEntity() != null) {
                // Se ambos estão no mesmo mundo, Ruptura Temporal é global
                if (cachedGorvax.getEntity().getWorld().equals(cachedIndrax.getEntity().getWorld())) {
                    if (getConfig().getBoolean("atmospheric_events.temporal_rupture.enabled", true)) {
                        return AtmosphereState.TEMPORAL_RUPTURE;
                    }
                }
            }
            return AtmosphereState.GORVAX_FURY;
        } else if (gorvaxAlive) {
            return getConfig().getBoolean("atmospheric_events.gorvax_fury.enabled", true)
                    ? AtmosphereState.GORVAX_FURY
                    : AtmosphereState.NORMAL;
        } else if (indraxAlive) {
            return getConfig().getBoolean("atmospheric_events.indrax_eclipse.enabled", true)
                    ? AtmosphereState.INDRAX_ECLIPSE
                    : AtmosphereState.NORMAL;
        } else if (vulgathorAlive) {
            return AtmosphereState.VULGATHOR_HEAT;
        } else if (xylosAlive) {
            return AtmosphereState.XYLOS_DISTORTION;
        } else if (skulkorAlive) {
            return AtmosphereState.SKULKOR_DEATH;
        } else if (kaldurAlive) {
            return AtmosphereState.KALDUR_FROST;
        } else if (zarithAlive) {
            return AtmosphereState.ZARITH_JUNGLE;
        } else {
            return AtmosphereState.NORMAL;
        }
    }

    // ================= SPAWN / DEATH HOOKS =================

    public void onBossSpawn(WorldBoss boss) {
        // Guardar estado original do mundo na primeira vez
        if (originalTime == -1 && boss.getEntity() != null) {
            World world = boss.getEntity().getWorld();
            originalTime = world.getTime();
            originalStorm = world.hasStorm();
            originalThundering = world.isThundering();
            originalDaylightCycle = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
        }
    }

    public void onBossDeath(WorldBoss boss) {
        // tick() vai detectar automaticamente a mudança e transicionar
        // Se nenhum boss sobrou, restaurar
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean anyAlive = false;
            for (WorldBoss b : plugin.getBossManager().getActiveBosses().values()) {
                if (b.getEntity() != null && !b.getEntity().isDead()) {
                    anyAlive = true;
                    break;
                }
            }
            if (!anyAlive) {
                restoreNormal();
            }
        }, 5L);
    }

    // ================= FÚRIA DA TERRA (GORVAX) =================

    private void processGorvaxFury() {
        for (World world : getActiveWorlds()) {
            // Transição de tempo para noite (suave)
            long currentTime = world.getTime();
            long targetTime = getConfig().getLong("atmospheric_events.gorvax_fury.target_time", 18000);
            if (currentTime != targetTime) {
                long diff = targetTime - currentTime;
                if (diff > 0) {
                    long step = Math.min(2000, diff); // Passo maior pois roda a cada 1s
                    world.setTime(currentTime + step);
                } else {
                    world.setTime(targetTime);
                }
            }

            // Congelar ciclo e forçar clima
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setStorm(true);
            world.setThundering(true);
            world.setWeatherDuration(Integer.MAX_VALUE);
        }

        // Trovões a cada ~10 segundos (mais épico com som)
        if (tickCounter % 10 == 0) {
            strikeDecorativeLightning();
        }

        // Rugidos distantes a cada 30 segundos
        if (tickCounter % 30 == 0) {
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.2f);
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.4f);
                }
            }
        }

        // Partículas intensificadas
        if (tickCounter % 5 == 0) {
            int cloudCount = 15;
            int soulCount = 6;
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    p.setPlayerWeather(WeatherType.DOWNFALL);
                    // Nuvens escuras
                    p.spawnParticle(Particle.CLOUD,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 20, 10, (RANDOM.nextDouble() - 0.5) * 20),
                            cloudCount, 3.0, 1.5, 3.0, 0.01);
                    // Cinzas de almas e chamas infernais
                    p.spawnParticle(Particle.SOUL_FIRE_FLAME,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 15, 8, (RANDOM.nextDouble() - 0.5) * 15),
                            soulCount, 4.0, 2.5, 4.0, 0.02);
                    p.spawnParticle(Particle.FLAME,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 18, 12, (RANDOM.nextDouble() - 0.5) * 18),
                            4, 5.0, 1.5, 5.0, 0.01);
                }
            }
        }
    }

    /**
     * Raio decorativo: cai longe dos jogadores, apenas visual.
     */
    private void strikeDecorativeLightning() {
        WorldBoss gorvax = findBoss(KingGorvax.class);
        if (gorvax == null || gorvax.getEntity() == null)
            return;

        Location bossLoc = gorvax.getEntity().getLocation();
        int minDist = getConfig().getInt("atmospheric_events.gorvax_fury.thunder_distance_min", 20);
        int maxDist = getConfig().getInt("atmospheric_events.gorvax_fury.thunder_distance_max", 60);

        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        double dist = minDist + RANDOM.nextDouble() * (maxDist - minDist);
        double x = bossLoc.getX() + Math.cos(angle) * dist;
        double z = bossLoc.getZ() + Math.sin(angle) * dist;

        Location strikeLoc = new Location(bossLoc.getWorld(), x,
                bossLoc.getWorld().getHighestBlockYAt((int) x, (int) z), z);

        // strikeLightningEffect = apenas visual, sem dano nem fogo
        bossLoc.getWorld().strikeLightningEffect(strikeLoc);
        // Som de trovão global/direcionado para garantir audibilidade
        bossLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 10.0f, 1.0f);
    }

    private void processIndraxEclipse() {
        for (World world : getActiveWorlds()) {
            // Noite profunda
            long targetTime = getConfig().getLong("atmospheric_events.indrax_eclipse.target_time", 18000);
            world.setTime(targetTime);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);

            // Céu limpo
            world.setStorm(false);
            world.setThundering(false);
        }

        // Pulso de Night Vision (simula escuridão engolindo o sol)
        int nvIntervalSec = getConfig().getInt("atmospheric_events.indrax_eclipse.night_vision_interval", 200) / 20;
        int nvDuration = getConfig().getInt("atmospheric_events.indrax_eclipse.night_vision_duration", 60);
        eclipsePulseCooldown--;
        if (eclipsePulseCooldown <= 0) {
            eclipsePulseCooldown = Math.max(1, nvIntervalSec);
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.NIGHT_VISION, nvDuration, 0, true, false, false));
                }
            }
        }

        // Fumaça no horizonte
        int smokeCount = getConfig().getInt("atmospheric_events.indrax_eclipse.smoke_particles", 8);
        for (World world : getActiveWorlds()) {
            for (Player p : world.getPlayers()) {
                p.resetPlayerWeather(); // Céu limpo para este jogador
                p.spawnParticle(Particle.SMOKE,
                        p.getLocation().add((RANDOM.nextDouble() - 0.5) * 30, 10 + RANDOM.nextDouble() * 5,
                                (RANDOM.nextDouble() - 0.5) * 30),
                        smokeCount, 3, 1, 3, 0.01);
            }
        }
    }

    // ================= RUPTURA TEMPORAL =================

    private void processTemporalRupture(WorldBoss gorvax, WorldBoss indrax) {
        long now = System.currentTimeMillis();

        // Calcular próximo intervalo de alternância
        if (nextSwitchInterval == 0) {
            int minSec = getConfig().getInt("atmospheric_events.temporal_rupture.switch_min_seconds", 30);
            int maxSec = getConfig().getInt("atmospheric_events.temporal_rupture.switch_max_seconds", 45);
            nextSwitchInterval = (minSec + RANDOM.nextInt(maxSec - minSec + 1)) * 1000L;
            lastRuptureSwitch = now;
        }

        // Verificar se é hora de alternar
        if (now - lastRuptureSwitch >= nextSwitchInterval && !isFading) {
            ruptureIsGorvaxPhase = !ruptureIsGorvaxPhase;
            lastRuptureSwitch = now;

            // Recalcular próximo intervalo
            int minSec = getConfig().getInt("atmospheric_events.temporal_rupture.switch_min_seconds", 30);
            int maxSec = getConfig().getInt("atmospheric_events.temporal_rupture.switch_max_seconds", 45);
            nextSwitchInterval = (minSec + RANDOM.nextInt(maxSec - minSec + 1)) * 1000L;

            // Efeito de Ruptura no momento da troca
            triggerRuptureEffect(gorvax, indrax);

            // Iniciar fade interno (sem mudar de estado principal)
            fadeMaxTicks = getConfig().getInt("atmospheric_events.temporal_rupture.fade_duration_ticks", 80) / 20;
            fadeTicks = 0;
        }

        // ========== ALTERNÂNCIA DIA/NOITE ÉPICA ==========
        for (World world : getActiveWorlds()) {
            // Ciclo: 0→dia, 1→crepúsculo, 2→noite, 3→amanhecer (cada execução = 1s)
            int phase = tickCounter % 4;
            long[] times = { 6000, 13000, 18000, 23000 };
            world.setTime(times[phase]);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }

        // Aplicar partículas/efeitos baseado na fase atual
        for (World world : getActiveWorlds()) {
            for (Player p : world.getPlayers()) {
                if (ruptureIsGorvaxPhase) {
                    p.spawnParticle(Particle.CLOUD,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 20, 10,
                                    (RANDOM.nextDouble() - 0.5) * 20),
                            5, 2, 1, 2, 0.01);
                    p.spawnParticle(Particle.SOUL_FIRE_FLAME,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 10, 5,
                                    (RANDOM.nextDouble() - 0.5) * 10),
                            3, 1, 0.5, 1, 0.02);
                } else {
                    p.spawnParticle(Particle.SMOKE,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 30, 10 + RANDOM.nextDouble() * 5,
                                    (RANDOM.nextDouble() - 0.5) * 30),
                            8, 3, 1, 3, 0.01);
                    // SCULK_CHARGE_POP não usa float data, apenas count
                    p.spawnParticle(Particle.SCULK_CHARGE_POP,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 15, 3,
                                    (RANDOM.nextDouble() - 0.5) * 15),
                            4); // Apenas count, sem float data
                    if (RANDOM.nextDouble() < 0.1) { // Pequena chance de um pop extra
                        p.spawnParticle(Particle.SCULK_CHARGE_POP, p.getLocation().add(0.5, 1, 0.5), 1);
                    }
                }

                // Efeitos globais da Ruptura
                p.spawnParticle(Particle.PORTAL,
                        p.getLocation().add((RANDOM.nextDouble() - 0.5) * 25, 12, (RANDOM.nextDouble() - 0.5) * 25),
                        10, 3, 2, 3, 1.0);
                p.spawnParticle(Particle.END_ROD,
                        p.getLocation().add((RANDOM.nextDouble() - 0.5) * 15, 8, (RANDOM.nextDouble() - 0.5) * 15),
                        3, 1, 1, 1, 0.05);
            }
        }

        // Trovões da fase Gorvax
        if (ruptureIsGorvaxPhase) {
            int thunderIntervalSec = getConfig().getInt("atmospheric_events.gorvax_fury.thunder_interval_ticks", 70)
                    / 20;
            thunderCooldown--;
            if (thunderCooldown <= 0) {
                thunderCooldown = Math.max(1, thunderIntervalSec) + RANDOM.nextInt(2);
                strikeDecorativeLightning();
            }
        }

        // Rumble periódico + som de portal
        int rumbleIntervalSec = getConfig().getInt("atmospheric_events.temporal_rupture.rumble_interval_ticks", 100)
                / 20;
        rumbleCooldown--;
        if (rumbleCooldown <= 0) {
            rumbleCooldown = Math.max(2, rumbleIntervalSec);
            for (World world_rupture : getActiveWorlds()) {
                for (Player p : world_rupture.getPlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);
                    p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 0.7f);
                }
            }
        }

        // Sinergia persistente: feixe + aura de ressonância
        drawPersistentBeam(gorvax, indrax);
        drawResonanceAura(gorvax, indrax);
    }

    /**
     * Efeito visual/sonoro no momento exato da troca de clima durante Ruptura
     * Temporal.
     */
    private void triggerRuptureEffect(WorldBoss gorvax, WorldBoss indrax) {
        int portalCount = getConfig().getInt("atmospheric_events.temporal_rupture.portal_particles", 200);
        int cloudCount = getConfig().getInt("atmospheric_events.temporal_rupture.cloud_particles", 100);
        int sculkCount = getConfig().getInt("atmospheric_events.temporal_rupture.sculk_soul_particles", 50); // Novo
        int vibrationCount = getConfig().getInt("atmospheric_events.temporal_rupture.sculk_charge_pop_particles", 30); // Novo
        int fadeIn = getConfig().getInt("atmospheric_events.temporal_rupture.title_fade_in", 10);
        int stay = getConfig().getInt("atmospheric_events.temporal_rupture.title_stay", 60);
        int fadeOut = getConfig().getInt("atmospheric_events.temporal_rupture.title_fade_out", 20);

        for (World world : getActiveWorlds()) {
            for (Player p : world.getPlayers()) {
                // Partículas de ruptura no céu
                Location skyLoc = p.getLocation().add(0, 15, 0);
                p.spawnParticle(Particle.PORTAL, skyLoc, portalCount, 10, 3, 10, 1.0);
                p.spawnParticle(Particle.SCULK_SOUL, skyLoc, sculkCount, 0.5, 0.8, 0.5, 0.05);
                p.spawnParticle(Particle.SCULK_CHARGE_POP, skyLoc, vibrationCount); // SCULK_CHARGE_POP não usa float
                                                                                    // data
                p.spawnParticle(Particle.END_ROD, skyLoc, 30, 5, 2, 5, 0.2);

                // Som de portal abrindo + rugido profundo
                p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.5f);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.3f);
                p.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.4f);

                // Título na tela
                p.showTitle(net.kyori.adventure.title.Title.title(
                        net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                                .deserialize("§5§lRUPTURA TEMPORAL"),
                        net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                                .deserialize("§7As realidades de Gorvax e Indrax entraram em colapso..."),
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(fadeIn * 50L), java.time.Duration.ofMillis(stay * 50L),
                                java.time.Duration.ofMillis(fadeOut * 50L))));
            }
        }
    }

    // ================= ONDA DE CALOR (VULGATHOR) =================

    private void processVulgathorHeat() {
        for (World world : getActiveWorlds()) {
            world.setTime(6000); // Meio-dia (calor intenso)
            world.setStorm(false);
            world.setThundering(false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }

        // Partículas de calor intensas
        if (tickCounter % 3 == 0) {
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    // Chamas no céu (calor ascendente)
                    p.spawnParticle(Particle.FLAME,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 20, 8, (RANDOM.nextDouble() - 0.5) * 20),
                            5, 1.5, 0.5, 1.5, 0.02);
                    // Fumaça pesada
                    p.spawnParticle(Particle.LARGE_SMOKE,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 25, 10, (RANDOM.nextDouble() - 0.5) * 25),
                            4, 2, 1, 2, 0.01);
                    // Lava caindo do céu
                    p.spawnParticle(Particle.LAVA,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 15, 12, (RANDOM.nextDouble() - 0.5) * 15),
                            2, 1, 0.5, 1, 0.01);
                    // Distorção de calor
                    p.spawnParticle(Particle.DRIPPING_LAVA,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 10, 6, (RANDOM.nextDouble() - 0.5) * 10),
                            3, 1, 0.3, 1, 0.01);
                }
            }
        }

        // Sons de fogo e estalos periódicos
        if (tickCounter % 4 == 0) { // A cada 4s (reduzido)
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.5f);
                    p.playSound(p.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 0.6f, 0.8f);
                }
            }
        }
    }

    // ================= CREPÚSCULO TÓXICO (ZARITH) =================

    private void processZarithJungle() {
        for (World world : getActiveWorlds()) {
            world.setTime(13000); // Crepúsculo
            world.setStorm(false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }

        // Partículas tóxicas densas
        if (tickCounter % 4 == 0) {
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    // Gosma tóxica (Corrigido Void data)
                    p.spawnParticle(Particle.ITEM_SLIME,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 15, 5, (RANDOM.nextDouble() - 0.5) * 15),
                            10, 2.0, 1.0, 2.0, 0.05);
                    // Partículas ambientais da selva
                    p.spawnParticle(Particle.SNEEZE,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 12, 3, (RANDOM.nextDouble() - 0.5) * 12),
                            4, 2, 1, 2, 0.01);
                    // Esporos venenosos (bolhas subindo)
                    p.spawnParticle(Particle.HAPPY_VILLAGER,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 12, 2, (RANDOM.nextDouble() - 0.5) * 12),
                            4, 1, 0.3, 1, 0.02);
                    // Névoa rasteira
                    p.spawnParticle(Particle.SMOKE,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 20, 0.5,
                                    (RANDOM.nextDouble() - 0.5) * 20),
                            3, 2, 0.1, 2, 0.005);
                }
            }
        }

        // Sons de floresta corrompida (Frequência ajustada)
        if (tickCounter % 5 == 0) {
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 0.8f, 0.4f);
                    p.playSound(p.getLocation(), Sound.BLOCK_HONEY_BLOCK_SLIDE, 0.6f, 0.3f);
                    p.playSound(p.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.4f, 0.2f);
                }
            }
        }
    }

    // ================= NEVASCA ETERNA (KALDUR) =================

    private void processKaldurFrost() {
        for (World world : getActiveWorlds()) {
            world.setTime(16000); // Entardecer gelado
            world.setStorm(true);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }

        // Nevasca densa
        if (tickCounter % 3 == 0) {
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    p.setPlayerWeather(WeatherType.DOWNFALL);
                    // Neve intensa
                    p.spawnParticle(Particle.SNOWFLAKE,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 20, 10, (RANDOM.nextDouble() - 0.5) * 20),
                            12, 4, 1.5, 4, 0.03);
                    // Cristais de gelo
                    p.spawnParticle(Particle.WHITE_ASH,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 15, 5, (RANDOM.nextDouble() - 0.5) * 15),
                            8, 2, 1, 2, 0.02);
                    // Névoa gelada rente ao chão
                    p.spawnParticle(Particle.CLOUD,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 18, 0.3,
                                    (RANDOM.nextDouble() - 0.5) * 18),
                            2, 1.5, 0.1, 1.5, 0.005);
                }
            }
        }

        // Sons de vento gelado
        if (tickCounter % 3 == 0) { // A cada 3s
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    p.playSound(p.getLocation(), Sound.WEATHER_RAIN, 0.8f, 0.3f);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BREATH, 0.4f, 0.5f);
                }
            }
        }
    }

    // ================= NÉVOA DA MORTE (SKULKOR) =================

    private void processSkulkorDeath() {
        for (World world : getActiveWorlds()) {
            world.setTime(18000); // Meia-noite
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setStorm(false);
        }

        // Névoa de morte densa
        if (tickCounter % 4 == 0) {
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    // Fumaça densa no chão
                    p.spawnParticle(Particle.LARGE_SMOKE,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 20, 1, (RANDOM.nextDouble() - 0.5) * 20),
                            6, 2, 0.3, 2, 0.005);
                    // Almas subindo
                    p.spawnParticle(Particle.SOUL,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 15, 2, (RANDOM.nextDouble() - 0.5) * 15),
                            5, 1, 0.8, 1, 0.02);
                    // Cinzas flutuando
                    p.spawnParticle(Particle.ASH,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 20, 4, (RANDOM.nextDouble() - 0.5) * 20),
                            8, 3, 1, 3, 0.01);
                    // Soul fire flame (chamas fantasmagóricas)
                    p.spawnParticle(Particle.SOUL_FIRE_FLAME,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 10, 0.5,
                                    (RANDOM.nextDouble() - 0.5) * 10),
                            2, 0.5, 0.2, 0.5, 0.005);
                }
            }
        }

        // Sons macabros periódicos
        if (tickCounter % 6 == 0) { // Aprox a cada 6s (reduzido)
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 0.5f, 0.3f);
                    p.playSound(p.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.4f, 0.6f);
                }
            }
        }

        // Pulso de darkness (a cada 30 segundos - muito mais leve e raro)
        if (tickCounter % 30 == 0) {
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.DARKNESS, 40, 0, true, false, false));
                }
            }
        }
    }

    // ================= DISTORÇÃO DIMENSIONAL (XYLOS) =================

    private void processXylosDistortion() {
        for (World world : getActiveWorlds()) {
            world.setTime(14000);
            world.setStorm(false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }

        // Distorção dimensional intensa (a cada 1s -> otimizado para a cada 3 ticks)
        if (tickCounter % 3 == 0) {
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    p.spawnParticle(Particle.REVERSE_PORTAL,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 15, 8, (RANDOM.nextDouble() - 0.5) * 15),
                            12, 2.5, 1.5, 2.5, 0.6);
                    p.spawnParticle(Particle.PORTAL,
                            p.getLocation().add((RANDOM.nextDouble() - 0.5) * 20, 5, (RANDOM.nextDouble() - 0.5) * 20),
                            8, 1.5, 0.8, 1.5, 0.4);
                }
            }
        }

        // Sons dimensionais a cada 12s (reduzido conforme pedido)
        if (tickCounter % 12 == 0) {
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 0.4f);
                    p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.1f, 0.6f);
                }
            }
        }

        // Pulso de Nausea leve (a cada 8 segundos)
        if (tickCounter % 8 == 0) {
            for (World world : getActiveWorlds()) {
                for (Player p : world.getPlayers()) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.NAUSEA, 100, 0, true, false, false));
                }
            }
        }
    }

    // ================= SINERGIA PERSISTENTE =================

    /**
     * Desenha um feixe contínuo de partículas entre Gorvax e Indrax.
     * O feixe pulsa com END_ROD, SOUL_FIRE_FLAME e PORTAL, criando
     * um cordão umbilical visual entre os dois bosses.
     */
    private void drawPersistentBeam(WorldBoss gorvax, WorldBoss indrax) {
        if (!getConfig().getBoolean("alliance_synergy.persistent_beam.enabled", true))
            return;
        if (gorvax == null || indrax == null
                || gorvax.getEntity() == null || indrax.getEntity() == null
                || gorvax.getEntity().isDead() || indrax.getEntity().isDead())
            return;
        if (!gorvax.getEntity().getWorld().equals(indrax.getEntity().getWorld()))
            return;

        int intervalSec = getConfig().getInt("alliance_synergy.persistent_beam.interval_seconds", 3);
        beamCooldown--;
        if (beamCooldown > 0)
            return;
        beamCooldown = Math.max(1, intervalSec);

        Location from = gorvax.getEntity().getLocation().add(0, 1.8, 0);
        Location to = indrax.getEntity().getLocation().add(0, 1.8, 0);
        double distance = from.distance(to);

        double spacing = getConfig().getDouble("alliance_synergy.persistent_beam.particle_spacing", 1.5);
        int endRodCount = getConfig().getInt("alliance_synergy.persistent_beam.end_rod_count", 3);
        int soulFireCount = getConfig().getInt("alliance_synergy.persistent_beam.soul_fire_count", 2);
        int portalCount = getConfig().getInt("alliance_synergy.persistent_beam.portal_count", 2);

        int points = Math.max(3, (int) (distance / spacing));
        double dx = (to.getX() - from.getX()) / points;
        double dy = (to.getY() - from.getY()) / points;
        double dz = (to.getZ() - from.getZ()) / points;

        World world = from.getWorld();
        for (int i = 0; i <= points; i++) {
            // Onda senoidal para dar um efeito de pulso/ondulação ao feixe
            double wave = Math.sin((double) i / points * Math.PI * 2 + tickCounter * 0.5) * 0.4;
            Location beamLoc = from.clone().add(dx * i, dy * i + wave, dz * i);

            world.spawnParticle(Particle.END_ROD, beamLoc, endRodCount, 0.08, 0.08, 0.08, 0.01);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, beamLoc, soulFireCount, 0.06, 0.06, 0.06, 0.005);
            world.spawnParticle(Particle.PORTAL, beamLoc, portalCount, 0.1, 0.1, 0.1, 0.3);
        }

        // Efeito de pulso nos endpoints (explosão sutil)
        world.spawnParticle(Particle.END_ROD, from, 8, 0.3, 0.5, 0.3, 0.03);
        world.spawnParticle(Particle.SCULK_SOUL, to, 8, 0.3, 0.5, 0.3, 0.03);
    }

    /**
     * Aura de ressonância: cada boss ganha partículas do tema do aliado,
     * mostrando visualmente que a sinergia está ativa.
     * Gorvax recebe partículas sombrias (Sculk, End Rod, Reverse Portal).
     * Indrax recebe partículas de fogo (Flame, Soul Fire, Lava).
     */
    private void drawResonanceAura(WorldBoss gorvax, WorldBoss indrax) {
        if (!getConfig().getBoolean("alliance_synergy.resonance_aura.enabled", true))
            return;
        if (gorvax == null || indrax == null
                || gorvax.getEntity() == null || indrax.getEntity() == null
                || gorvax.getEntity().isDead() || indrax.getEntity().isDead())
            return;

        int intervalSec = getConfig().getInt("alliance_synergy.resonance_aura.interval_seconds", 2);
        auraCooldown--;
        if (auraCooldown > 0)
            return;
        auraCooldown = Math.max(1, intervalSec);

        double radius = getConfig().getDouble("alliance_synergy.resonance_aura.radius", 4.0);

        // === GORVAX recebe partículas sombrias (tema Indrax) ===
        Location gorvaxLoc = gorvax.getEntity().getLocation().add(0, 1.5, 0);
        World gWorld = gorvaxLoc.getWorld();
        int gSculk = getConfig().getInt("alliance_synergy.resonance_aura.gorvax_sculk_count", 12);
        int gEndRod = getConfig().getInt("alliance_synergy.resonance_aura.gorvax_end_rod_count", 6);
        int gPortal = getConfig().getInt("alliance_synergy.resonance_aura.gorvax_reverse_portal_count", 8);

        // Anel orbital de partículas ao redor do Gorvax
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 / 8) * i + tickCounter * 0.3;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = Math.sin(angle * 2 + tickCounter * 0.5) * 0.5; // Ondulação vertical
            Location orbLoc = gorvaxLoc.clone().add(x, y, z);
            gWorld.spawnParticle(Particle.SCULK_CHARGE_POP, orbLoc, gSculk / 4, 0.15, 0.15, 0.15, 0.02);
            gWorld.spawnParticle(Particle.END_ROD, orbLoc, gEndRod / 4, 0.1, 0.1, 0.1, 0.01);
        }
        // Pilar ascendente de partículas de portal reverso
        gWorld.spawnParticle(Particle.REVERSE_PORTAL, gorvaxLoc.clone().add(0, 0.5, 0),
                gPortal, radius * 0.6, 1.5, radius * 0.6, 0.05);

        // === INDRAX recebe partículas de fogo (tema Gorvax) ===
        Location indraxLoc = indrax.getEntity().getLocation().add(0, 1.5, 0);
        World iWorld = indraxLoc.getWorld();
        int iFlame = getConfig().getInt("alliance_synergy.resonance_aura.indrax_flame_count", 12);
        int iSoulFire = getConfig().getInt("alliance_synergy.resonance_aura.indrax_soul_fire_count", 6);
        int iLava = getConfig().getInt("alliance_synergy.resonance_aura.indrax_lava_count", 4);

        // Anel orbital de chamas ao redor do Indrax
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 / 8) * i - tickCounter * 0.3; // Gira na direção oposta
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = Math.sin(angle * 2 - tickCounter * 0.5) * 0.5;
            Location orbLoc = indraxLoc.clone().add(x, y, z);
            iWorld.spawnParticle(Particle.FLAME, orbLoc, iFlame / 4, 0.15, 0.15, 0.15, 0.02);
            iWorld.spawnParticle(Particle.SOUL_FIRE_FLAME, orbLoc, iSoulFire / 4, 0.1, 0.1, 0.1, 0.01);
        }
        // Lava ascendente ao redor do Indrax
        iWorld.spawnParticle(Particle.LAVA, indraxLoc.clone().add(0, 0.5, 0),
                iLava, radius * 0.6, 1.0, radius * 0.6, 0.05);
    }

    // ================= FADE / TRANSIÇÃO =================

    private void startFade(AtmosphereState target) {
        isFading = true;
        fadeTicks = 0;
        fadeTarget = target;
        fadeMaxTicks = getConfig().getInt("atmospheric_events.temporal_rupture.fade_duration_ticks", 80) / 20;
    }

    private void processFade() {
        fadeTicks++;
        if (fadeTicks >= fadeMaxTicks) {
            // Fade completo — aplicar novo estado
            currentState = fadeTarget;
            isFading = false;
            fadeTicks = 0;

            // Reset do ciclo de ruptura ao entrar/sair
            if (fadeTarget == AtmosphereState.TEMPORAL_RUPTURE) {
                lastRuptureSwitch = System.currentTimeMillis();
                nextSwitchInterval = 0; // Forçar recálculo
                ruptureIsGorvaxPhase = true;
            }

            if (fadeTarget == AtmosphereState.NORMAL) {
                restoreNormal();
            }
        }
    }

    // ================= RESTAURAÇÃO =================

    /**
     * Restaura clima e tempo do mundo para os valores originais.
     */
    public void restoreNormal() {
        currentState = AtmosphereState.NORMAL;
        isFading = false;

        for (World world : getActiveWorlds()) {
            world.setStorm(originalStorm);
            world.setThundering(originalThundering);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, originalDaylightCycle);
            if (originalTime >= 0) {
                world.setTime(1000); // Dia
            }
        }

        // Restaurar todos os jogadores online
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.resetPlayerWeather();
            p.resetPlayerTime();

            // Remover efeitos de poção aplicados pelo AtmosphereManager
            p.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
            p.removePotionEffect(org.bukkit.potion.PotionEffectType.DARKNESS);
            p.removePotionEffect(org.bukkit.potion.PotionEffectType.NAUSEA);
        }

        // Reset variáveis
        originalTime = -1;
        originalStorm = false;
        originalThundering = false;
        originalDaylightCycle = true;
        nextSwitchInterval = 0;
    }

    // ================= UTILITÁRIOS =================

    @SuppressWarnings("unchecked")
    private <T extends WorldBoss> T findBoss(Class<T> clazz) {
        for (WorldBoss boss : plugin.getBossManager().getActiveBosses().values()) {
            if (clazz.isInstance(boss)) {
                return (T) boss;
            }
        }
        return null;
    }

    private Set<World> getActiveWorlds() {
        Set<World> worlds = new HashSet<>();
        for (WorldBoss boss : plugin.getBossManager().getActiveBosses().values()) {
            if (boss.getEntity() != null && boss.getEntity().getWorld() != null) {
                worlds.add(boss.getEntity().getWorld());
            }
        }
        if (worlds.isEmpty()) {
            worlds.addAll(Bukkit.getWorlds());
        }
        return worlds;
    }

    private boolean isNearAnyBoss(Player p, double maxDist) {
        double maxDistSq = maxDist * maxDist;
        for (WorldBoss boss : plugin.getBossManager().getActiveBosses().values()) {
            if (boss.getEntity() != null && !boss.getEntity().isDead()
                    && p.getWorld().equals(boss.getEntity().getWorld())
                    && p.getLocation().distanceSquared(boss.getEntity().getLocation()) < maxDistSq) {
                return true;
            }
        }
        return false;
    }

    public AtmosphereState getCurrentState() {
        return currentState;
    }
}
