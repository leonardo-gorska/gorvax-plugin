package br.com.gorvax.core.boss.model;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.WitherSkull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class WorldBoss {
    protected String id;
    protected String name;
    protected double maxHealth;
    protected LivingEntity entity;
    protected BossBar bossBar;
    protected Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
    protected int phase = 1;

    // Variável para controle de inatividade
    protected long lastDamageTimestamp;

    // Sistema de sincronização de teleporte
    protected UUID teleportTargetUUID = null;
    protected long teleportLockUntil = 0;

    // Sistema de Decreto Real (KingGorvax)
    protected UUID royalDecreeTarget = null;
    protected long royalDecreeUntil = 0;

    // MELHORIA-06: Throttle para IA
    private int targetingTicks = 0;

    public WorldBoss(String id, String name, double health) {
        this.id = id;
        this.name = name;
        this.maxHealth = health;

        // Inicia o timestamp com o momento da criação (spawn)
        this.lastDamageTimestamp = System.currentTimeMillis();

        // Inicializa a BossBar com a cor inicial Verde
        this.bossBar = Bukkit.createBossBar(name, BarColor.GREEN, BarStyle.SEGMENTED_10);
    }

    public abstract void spawn(Location loc);

    public abstract void update();

    public abstract void onPhaseChange(int newPhase);

    public abstract void playSpecialAttack();

    public abstract void cleanup();

    public boolean isAlive() {
        return entity != null && !entity.isDead() && entity.isValid();
    }

    /**
     * Sistema de Targeting Inteligente baseado em Threat Score.
     * Prioriza jogadores por: DPS causado, distância (anti-fuga), itens de cura,
     * e penaliza alvos com baixa vida.
     */
    protected void handleAITargeting() {
        if (!(entity instanceof Mob mob) || mob.isDead())
            return;

        // MELHORIA-06: Só processa a cada 10 ticks (0.5s) para economizar CPU
        if (++targetingTicks < 10) {
            return;
        }
        targetingTicks = 0;

        long now = System.currentTimeMillis();

        // Sincronização de teleporte: forçar foco no alvo do TP
        if (teleportTargetUUID != null && now < teleportLockUntil) {
            Player tpTarget = Bukkit.getPlayer(teleportTargetUUID);
            if (tpTarget != null && tpTarget.isOnline() && !tpTarget.isDead()
                    && tpTarget.getWorld().equals(entity.getWorld())) {
                mob.setTarget(tpTarget);
                return; // Ignora threat score enquanto em lock de TP
            }
        } else if (teleportTargetUUID != null && now >= teleportLockUntil) {
            teleportTargetUUID = null; // Limpa lock expirado
        }

        // Coletar jogadores próximos
        List<Player> nearbyPlayers = new ArrayList<>();
        for (Entity e : entity.getNearbyEntities(40, 40, 40)) {
            if (e instanceof Player p && p.getGameMode() == GameMode.SURVIVAL && !p.isDead()) {
                nearbyPlayers.add(p);
            }
        }

        if (nearbyPlayers.isEmpty())
            return;

        // Calcular threat score para cada jogador
        Player bestTarget = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        // Encontrar o top DPS para normalização
        double maxDps = 0;
        for (Player p : nearbyPlayers) {
            double dmg = damageDealt.getOrDefault(p.getUniqueId(), 0.0);
            if (dmg > maxDps)
                maxDps = dmg;
        }

        for (Player p : nearbyPlayers) {
            double score = 0;
            double distSq = p.getLocation().distanceSquared(entity.getLocation());
            double dist = Math.sqrt(distSq);

            // 1. DPS Score (jogadores que causam mais dano = maior prioridade)
            double playerDmg = damageDealt.getOrDefault(p.getUniqueId(), 0.0);
            if (maxDps > 0) {
                score += (playerDmg / maxDps) * 40; // Até +40 pontos
            }

            // 2. Fugitivo (jogador longe = maior prioridade para forçar engajamento)
            if (dist > 25) {
                score += 30;
            } else if (dist > 15) {
                score += 15;
            }

            // 3. Curador (segurou poções, golden apples ou totems)
            if (isHoldingHealingItem(p)) {
                score += 20;
            }

            // 4. Proximidade base (jogadores perto ainda são relevantes)
            if (dist <= 10) {
                score += 10;
            }

            // 5. Penalidade de HP baixo (jogador quase morrendo = menos prioridade)
            if (p.getHealth() < 6.0) {
                score -= 10;
            }

            if (score > bestScore) {
                bestScore = score;
                bestTarget = p;
            }
        }

        // Troca de alvo: sempre que houver um alvo significativamente melhor, ou sem
        // alvo
        LivingEntity currentTarget = mob.getTarget();
        if (bestTarget != null) {
            if (currentTarget == null || !(currentTarget instanceof Player)
                    || currentTarget.isDead()
                    || !currentTarget.getWorld().equals(entity.getWorld())
                    || currentTarget.getLocation().distanceSquared(entity.getLocation()) > 900 // > 30 blocos
                    || Math.random() < 0.12) { // 12% chance de reavaliar
                mob.setTarget(bestTarget);
            }
        }
    }

    /**
     * Verifica se o jogador está segurando itens de cura.
     */
    private boolean isHoldingHealingItem(Player p) {
        ItemStack mainHand = p.getInventory().getItemInMainHand();
        ItemStack offHand = p.getInventory().getItemInOffHand();
        return isHealItem(mainHand) || isHealItem(offHand);
    }

    private boolean isHealItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;
        Material m = item.getType();
        return m == Material.GOLDEN_APPLE
                || m == Material.ENCHANTED_GOLDEN_APPLE
                || m == Material.SPLASH_POTION
                || m == Material.LINGERING_POTION
                || m == Material.POTION
                || m == Material.TOTEM_OF_UNDYING;
    }

    /**
     * Anti-Kite Melhorado.
     * Se jogadores estão atirando de longe, lança projétil Wither Skull e pula
     * na direção deles com 15% de chance.
     */
    protected void handleAntiKite() {
        if (entity == null || entity.isDead())
            return;
        for (Entity e : entity.getNearbyEntities(40, 40, 40)) {
            if (e instanceof Player p && p.getLocation().distanceSquared(entity.getLocation()) > 625) { // > 25 blocos
                if (Math.random() < 0.15) {
                    say("§c§l" + name + " §8» §fNão adianta fugir!");

                    // Lança Wither Skull na direção do jogador distante
                    Vector direction = p.getLocation().toVector()
                            .subtract(entity.getLocation().toVector()).normalize();

                    WitherSkull skull = entity.getWorld().spawn(
                            entity.getLocation().add(0, 1.5, 0), WitherSkull.class);
                    skull.setDirection(direction);
                    skull.setShooter(entity);

                    // Também pula na direção do jogador
                    Vector jump = direction.clone().multiply(1.8);
                    jump.setY(0.6);
                    entity.setVelocity(jump);

                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.8f);
                    break; // Só responde a um jogador por tick
                }
            }
        }
    }

    /**
     * Seta o lock de teleporte para forçar foco no alvo após TP.
     */
    public void setTeleportLock(UUID target, long durationMs) {
        this.teleportTargetUUID = target;
        this.teleportLockUntil = System.currentTimeMillis() + durationMs;
    }

    protected double movementSpeed = -1;

    /**
     * Altera o HP máximo do boss.
     * Deve ser chamado ANTES do método spawn() para ter efeito na entidade.
     */
    public void setMaxHealth(double health) {
        this.maxHealth = health;
    }

    /**
     * Configura o HP máximo de uma entidade de forma segura, suportando valores
     * acima do limite vanilla (1024.0) do Minecraft 1.21+.
     * Usa um AttributeModifier para expandir o range antes de setar o valor.
     * Deve ser chamado DEPOIS do spawn da entidade.
     */
    public static void applySafeMaxHealth(LivingEntity entity, double maxHealth) {
        AttributeInstance attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null)
            return;

        // Remove modifier antigo se existir (re-spawn)
        NamespacedKey key = new NamespacedKey("gorvaxcore", "boss_health");
        attr.getModifiers().stream()
                .filter(m -> m.getKey().equals(key))
                .forEach(attr::removeModifier);

        // Seta o base value para o default (20.0) e usa modifier para o resto
        attr.setBaseValue(20.0);
        double bonus = maxHealth - 20.0;
        if (bonus > 0) {
            attr.addModifier(new AttributeModifier(
                    key, bonus, AttributeModifier.Operation.ADD_NUMBER));
        }

        // Agora seta a vida atual para o máximo
        entity.setHealth(attr.getValue());
    }

    public void setMovementSpeed(double speed) {
        this.movementSpeed = speed;
    }

    /**
     * Altera o nome de exibição do boss e atualiza o título da BossBar.
     */
    public void setName(String name) {
        this.name = name;
        if (this.bossBar != null) {
            this.bossBar.setTitle(name);
        }
    }

    /**
     * Atualiza o timestamp toda vez que o boss recebe dano.
     */
    public void updateLastDamage() {
        this.lastDamageTimestamp = System.currentTimeMillis();
    }

    /**
     * Faz o boss enviar uma mensagem para os jogadores próximos e loga no console.
     */
    public void say(String message) {
        if (entity == null || entity.isDead())
            return;

        entity.getWorld().getNearbyEntities(entity.getLocation(), 30, 30, 30).forEach(e -> {
            if (e instanceof Player p) {
                p.sendMessage(message);
            }
        });
        Bukkit.getConsoleSender()
                .sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_lore.log_prefix", name, message));
    }

    /**
     * Atualiza o progresso, cor da barra e a visibilidade baseada na distância (60
     * blocos).
     */
    public void updateBossBar() {
        if (entity == null || entity.isDead() || !entity.isValid()) {
            removeBossBar();
            return;
        }

        // 1. Atualização do Progresso (HP)
        double currentHealth = entity.getHealth();
        var maxHealthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxH = maxHealthAttr != null ? maxHealthAttr.getValue() : this.maxHealth;
        double progress = currentHealth / maxH;

        bossBar.setProgress(Math.max(0, Math.min(1, progress)));

        // Garante que o título esteja sempre atualizado
        // Bukkit BossBar has no non-deprecated replacement for getTitle()
        if (!bossBar.getTitle().equals(this.name)) {
            bossBar.setTitle(this.name);
        }

        // 2. Visibilidade Dinâmica (Gerencia quem vê a barra por proximidade)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(entity.getWorld())
                    && p.getLocation().distanceSquared(entity.getLocation()) < 10000) { // 100^2
                if (!bossBar.getPlayers().contains(p)) {
                    bossBar.addPlayer(p);
                }
            } else {
                bossBar.removePlayer(p);
            }
        }

        // 3. Lógica de Fases e Troca de Cores
        int oldPhase = phase;
        if (progress <= 0.25)
            phase = 4;
        else if (progress <= 0.50)
            phase = 3;
        else if (progress <= 0.75)
            phase = 2;
        else
            phase = 1;

        if (phase != oldPhase) {
            onPhaseChange(phase);
            updateBarColor();
        }
    }

    /**
     * Altera a cor da BossBar baseada na fase atual.
     */
    private void updateBarColor() {
        switch (phase) {
            case 2 -> bossBar.setColor(BarColor.YELLOW);
            case 3 -> bossBar.setColor(BarColor.RED);
            case 4 -> bossBar.setColor(BarColor.PURPLE);
            default -> bossBar.setColor(BarColor.GREEN);
        }
    }

    public void removeBossBar() {
        bossBar.removeAll();
    }

    // === SINERGIA DE ALIANÇA (Cross-Boss Support) ===

    private long lastAllyNotification = 0;

    /**
     * Notifica o boss aliado quando este boss usa uma habilidade especial.
     * O aliado responde com efeitos visuais e sonoros intensos se estiver próximo.
     * Exclusivo para a dupla Gorvax ↔ Indrax.
     */
    protected void notifyAlly(String skillName) {
        try {
            br.com.gorvax.core.GorvaxCore plugin = br.com.gorvax.core.GorvaxCore.getInstance();
            org.bukkit.configuration.file.FileConfiguration config = plugin.getBossManager().getConfigManager()
                    .getSettings();

            if (!config.getBoolean("alliance_synergy.enabled", true))
                return;

            long now = System.currentTimeMillis();
            long cooldown = config.getLong("alliance_synergy.response_cooldown_ms", 3000);
            if (now - lastAllyNotification < cooldown)
                return;

            double maxDistance = config.getDouble("alliance_synergy.max_distance", 50.0);

            // Encontrar o boss aliado ativo — apenas Gorvax ↔ Indrax
            WorldBoss ally = null;
            for (WorldBoss boss : plugin.getBossManager().getActiveBosses().values()) {
                if (boss != this && boss.getEntity() != null && !boss.getEntity().isDead()) {
                    // Sinergia exclusiva: só Gorvax ↔ Indrax
                    boolean isGorvaxIndraxPair = (this instanceof KingGorvax && boss instanceof IndraxAbissal)
                            || (this instanceof IndraxAbissal && boss instanceof KingGorvax);
                    if (isGorvaxIndraxPair) {
                        ally = boss;
                        break;
                    }
                }
            }

            if (ally == null || ally.getEntity() == null)
                return;

            // Verificar distância
            double distSq = entity.getLocation().distanceSquared(ally.getEntity().getLocation());
            if (distSq > (maxDistance * maxDistance))
                return;

            // ========== EFEITOS VISUAIS INTENSIFICADOS ==========
            Location allyLoc = ally.getEntity().getLocation();
            Location thisLoc = entity.getLocation();

            if (ally instanceof IndraxAbissal) {
                // === INDRAX RESPONDE AO GORVAX (efeitos sombrios intensos) ===
                Location effectLoc = allyLoc.clone().add(0, 1.5, 0);

                // Explosão de partículas do Sculk (30 → mais visível)
                allyLoc.getWorld().spawnParticle(org.bukkit.Particle.SCULK_CHARGE_POP, effectLoc, 50, 1.0, 1.5, 1.0,
                        0.15);
                allyLoc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, effectLoc, 30, 0.8, 1.2, 0.8, 0.08);
                allyLoc.getWorld().spawnParticle(org.bukkit.Particle.SCULK_SOUL, effectLoc, 25, 0.6, 1.0, 0.6, 0.05);
                allyLoc.getWorld().spawnParticle(org.bukkit.Particle.REVERSE_PORTAL, effectLoc, 40, 1.0, 1.5, 1.0, 0.1);

                // Sons: rugido + eco profundo
                allyLoc.getWorld().playSound(allyLoc, Sound.ENTITY_WARDEN_ROAR, 1.2f, 0.4f);
                allyLoc.getWorld().playSound(allyLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.8f, 0.6f);

                // Título na tela + mensagem para jogadores próximos (raio 40 blocos)
                for (Entity e : ally.getEntity().getNearbyEntities(40, 40, 40)) {
                    if (e instanceof Player p) {
                        p.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_lore.lore_shadow_abyss"));
                        p.showTitle(Title.title(
                                net.kyori.adventure.text.Component.empty(),
                                LegacyComponentSerializer.legacySection()
                                        .deserialize("§5§l⚡ §8A aliança das trevas pulsa §5§l⚡"),
                                Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(1500),
                                        Duration.ofMillis(500))));
                    }
                }

            } else if (ally instanceof KingGorvax) {
                // === GORVAX RESPONDE AO INDRAX (efeitos de fogo infernal intensos) ===
                Location effectLoc = allyLoc.clone().add(0, 1.5, 0);

                // Explosão de chamas e almas (muito mais partículas)
                allyLoc.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, effectLoc, 50, 1.0, 1.5, 1.0,
                        0.15);
                allyLoc.getWorld().spawnParticle(org.bukkit.Particle.FLAME, effectLoc, 35, 0.8, 1.2, 0.8, 0.08);
                allyLoc.getWorld().spawnParticle(org.bukkit.Particle.SOUL, effectLoc, 25, 0.6, 1.0, 0.6, 0.05);
                allyLoc.getWorld().spawnParticle(org.bukkit.Particle.LAVA, effectLoc, 15, 0.8, 1.0, 0.8, 0.05);

                // Sons: wither + eco fantasmagórico
                allyLoc.getWorld().playSound(allyLoc, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.3f);
                allyLoc.getWorld().playSound(allyLoc, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.5f);

                // Título na tela + mensagem para jogadores próximos (raio 40 blocos)
                for (Entity e : ally.getEntity().getNearbyEntities(40, 40, 40)) {
                    if (e instanceof Player p) {
                        p.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_lore.lore_throne_abyss"));
                        p.showTitle(Title.title(
                                net.kyori.adventure.text.Component.empty(),
                                LegacyComponentSerializer.legacySection()
                                        .deserialize("§6§l🔥 §8O pacto ancestral reverbera §6§l🔥"),
                                Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(1500),
                                        Duration.ofMillis(500))));
                    }
                }
            }

            // ========== FEIXE DE CONEXÃO ENTRE OS BOSSES ==========
            // Linha de partículas conectando os dois bosses (efeito de elo visível)
            Location from = thisLoc.clone().add(0, 1.5, 0);
            Location to = allyLoc.clone().add(0, 1.5, 0);
            double beamDist = from.distance(to);
            int beamPoints = Math.max(5, (int) (beamDist / 1.5));
            double dx = (to.getX() - from.getX()) / beamPoints;
            double dy = (to.getY() - from.getY()) / beamPoints;
            double dz = (to.getZ() - from.getZ()) / beamPoints;

            for (int i = 0; i <= beamPoints; i++) {
                Location beamLoc = from.clone().add(dx * i, dy * i, dz * i);
                entity.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, beamLoc, 2, 0.05, 0.05, 0.05, 0.01);
                entity.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, beamLoc, 1, 0.05, 0.05, 0.05,
                        0.005);
            }

            lastAllyNotification = now;

        } catch (Exception ignored) {
            // Fail silently para não quebrar o boss
            GorvaxCore.getInstance().getLogger().fine("Erro em notifyAlly: " + ignored.getMessage());
        }
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public Map<UUID, Double> getDamageDealt() {
        return damageDealt;
    }

    public int getPhase() {
        return phase;
    }

    public long getLastDamageTimestamp() {
        return lastDamageTimestamp;
    }

    public UUID getRoyalDecreeTarget() {
        return royalDecreeTarget;
    }

    public long getRoyalDecreeUntil() {
        return royalDecreeUntil;
    }

    public UUID getUniqueId() {
        return entity != null ? entity.getUniqueId() : null;
    }

    /**
     * Retorna a lista de causadores de dano ordenada do maior para o menor.
     */
    public List<Map.Entry<UUID, Double>> getTopDamagers() {
        return damageDealt.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
    }
}