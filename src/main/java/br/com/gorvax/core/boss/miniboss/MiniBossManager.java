package br.com.gorvax.core.boss.miniboss;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.boss.model.WorldBoss;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gerencia o sistema de Mini-Bosses por Bioma (B11).
 * Responsável por carregar configurações, spawnar e gerenciar mini-bosses.
 */
public class MiniBossManager {

    private final GorvaxCore plugin;
    private final Map<String, MiniBoss> activeMiniBosses = new ConcurrentHashMap<>();
    private final Map<String, MiniBossConfig> configuredBosses = new ConcurrentHashMap<>();

    // Configurações globais
    private boolean enabled;
    private int spawnInterval;
    private int maxActive;
    private int distanceFromClaims;
    private int playerSpawnRadius;
    private int playerMinRadius;
    private int despawnTime;
    private double spawnChance;

    private FileConfiguration config;
    private BukkitTask spawnTask;
    private BukkitTask cleanupTask;

    private final Random random = new Random();

    // Skill cooldowns por instância de mini-boss
    private final Map<String, Map<String, Long>> skillCooldowns = new ConcurrentHashMap<>();

    public MiniBossManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
        startTasks();
        plugin.getLogger().info(
                "[MiniBoss] Sistema de Mini-Bosses carregado com " + configuredBosses.size() + " tipos configurados.");
    }

    // ================= CONFIGURAÇÃO =================

    /**
     * Carrega ou recarrega as configurações do mini_bosses.yml.
     */
    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "mini_bosses.yml");
        if (!file.exists()) {
            plugin.saveResource("mini_bosses.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        // Configurações globais
        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings != null) {
            this.enabled = settings.getBoolean("enabled", true);
            this.spawnInterval = settings.getInt("spawn_interval", 2400);
            this.maxActive = settings.getInt("max_active", 8);
            this.distanceFromClaims = settings.getInt("distance_from_claims", 100);
            this.playerSpawnRadius = settings.getInt("player_spawn_radius", 80);
            this.playerMinRadius = settings.getInt("player_min_radius", 30);
            this.despawnTime = settings.getInt("despawn_time", 6000);
            this.spawnChance = settings.getDouble("spawn_chance", 0.35);
        }

        // Carregar definições de mini-bosses
        configuredBosses.clear();
        ConfigurationSection bossesSection = config.getConfigurationSection("mini_bosses");
        if (bossesSection == null)
            return;

        for (String bossId : bossesSection.getKeys(false)) {
            try {
                ConfigurationSection bossSection = bossesSection.getConfigurationSection(bossId);
                if (bossSection == null)
                    continue;
                MiniBossConfig bossConfig = parseMiniBossConfig(bossId, bossSection);
                configuredBosses.put(bossId, bossConfig);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "[MiniBoss] Erro ao carregar mini-boss '" + bossId + "': " + e.getMessage(), e);
            }
        }
    }

    /**
     * Parseia a configuração de um mini-boss específico.
     */
    private MiniBossConfig parseMiniBossConfig(String id, ConfigurationSection section) {
        String name = section.getString("nome", "§c§lMini-Boss").replace("&", "§");
        EntityType entityType = EntityType.valueOf(section.getString("entity_type", "ZOMBIE"));
        double hp = section.getDouble("hp", 200.0);
        double damage = section.getDouble("damage", 8.0);
        double scale = section.getDouble("scale", 1.5);
        double moveSpeed = section.getDouble("movement_speed", 0.28);
        double moneyReward = section.getDouble("money_reward", 500.0);
        int xpReward = section.getInt("xp_reward", 100);

        List<String> biomes = section.getStringList("biomes");

        // Equipamento
        Map<String, Material> equipment = new HashMap<>();
        ConfigurationSection equipSection = section.getConfigurationSection("equipment");
        if (equipSection != null) {
            for (String slot : equipSection.getKeys(false)) {
                String matName = equipSection.getString(slot);
                if (matName != null && !matName.isEmpty()) {
                    try {
                        equipment.put(slot, Material.valueOf(matName));
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger()
                                .warning("[MiniBoss] Material inválido '" + matName + "' no equipamento de " + id);
                    }
                }
            }
        }

        // Efeitos on-hit
        List<MiniBoss.EffectOnHit> effectsOnHit = new ArrayList<>();
        for (String effectStr : section.getStringList("effects_on_hit")) {
            MiniBoss.EffectOnHit effect = parseEffect(effectStr);
            if (effect != null)
                effectsOnHit.add(effect);
        }

        // Efeitos passivos
        List<String> passiveEffects = section.getStringList("passive_effects");

        // Loot
        List<MiniBoss.LootEntry> loot = new ArrayList<>();
        for (String lootStr : section.getStringList("loot")) {
            MiniBoss.LootEntry entry = parseLootEntry(lootStr);
            if (entry != null)
                loot.add(entry);
        }

        String spawnMessage = section.getString("spawn_message", "§c§l⚔ Um mini-boss apareceu!").replace("&", "§");
        String deathMessage = section.getString("death_message", "§c§l⚔ O mini-boss foi derrotado!").replace("&", "§");

        return new MiniBossConfig(id, name, entityType, hp, damage, scale, moveSpeed,
                moneyReward, xpReward, biomes, equipment, effectsOnHit, passiveEffects,
                loot, spawnMessage, deathMessage, section);
    }

    /**
     * Parseia efeito no formato "TIPO:DURAÇÃO:NÍVEL"
     */
    private MiniBoss.EffectOnHit parseEffect(String str) {
        String[] parts = str.split(":");
        if (parts.length < 3)
            return null;
        try {
            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(parts[0].trim().toLowerCase()));
            if (type == null)
                return null;
            int duration = Integer.parseInt(parts[1].trim());
            int amplifier = Integer.parseInt(parts[2].trim());
            return new MiniBoss.EffectOnHit(type, duration, amplifier);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parseia loot no formato "MATERIAL:QUANTIDADE:CHANCE"
     */
    private MiniBoss.LootEntry parseLootEntry(String str) {
        String[] parts = str.split(":");
        if (parts.length < 3)
            return null;
        try {
            Material material = Material.valueOf(parts[0].trim());
            int amount = Integer.parseInt(parts[1].trim());
            double chance = Double.parseDouble(parts[2].trim());
            return new MiniBoss.LootEntry(material, amount, chance);
        } catch (Exception e) {
            return null;
        }
    }

    // ================= SPAWN =================

    /**
     * Tenta spawnar um mini-boss aleatório próximo de jogadores.
     */
    public void tryNaturalSpawn() {
        if (!enabled)
            return;
        if (activeMiniBosses.size() >= maxActive)
            return;
        if (random.nextDouble() > spawnChance)
            return;

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.isEmpty())
            return;

        // Escolher um jogador aleatório
        Player target = onlinePlayers.get(random.nextInt(onlinePlayers.size()));
        Location playerLoc = target.getLocation();

        // Verificar bioma e encontrar mini-boss compatível
        Biome biome = playerLoc.getBlock().getBiome();
        List<MiniBossConfig> compatible = new ArrayList<>();
        for (MiniBossConfig cfg : configuredBosses.values()) {
            if (cfg.biomes().contains(biome.name())) {
                compatible.add(cfg);
            }
        }

        if (compatible.isEmpty())
            return;

        MiniBossConfig chosen = compatible.get(random.nextInt(compatible.size()));

        // Encontrar posição de spawn segura
        Location spawnLoc = findSafeSpawnLocation(playerLoc);
        if (spawnLoc == null)
            return;

        // Verificar distância de claims
        if (plugin.getClaimManager() != null) {
            if (plugin.getClaimManager().getClaimAt(spawnLoc) != null)
                return;
        }

        spawnMiniBoss(chosen.id(), spawnLoc);
    }

    /**
     * Spawna um mini-boss específico em uma localização.
     */
    public MiniBoss spawnMiniBoss(String configId, Location loc) {
        MiniBossConfig cfg = configuredBosses.get(configId);
        if (cfg == null)
            return null;
        if (activeMiniBosses.size() >= maxActive)
            return null;

        String instanceId = configId + "_" + System.currentTimeMillis();

        MiniBoss miniBoss = new MiniBoss(
                instanceId, configId, cfg.name(), cfg.hp(), cfg.damage(),
                cfg.scale(), cfg.moveSpeed(), cfg.moneyReward(), cfg.xpReward(),
                cfg.entityType(), cfg.biomes(), cfg.equipment(), cfg.effectsOnHit(),
                cfg.passiveEffects(), cfg.loot(), cfg.spawnMessage(), cfg.deathMessage());

        // Spawnar entidade
        LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, cfg.entityType());
        entity.setRemoveWhenFarAway(false);
        if (entity instanceof Mob mob)
            mob.setPersistent(true);

        // Configurar atributos (usa helper seguro para HP acima de 1024)
        WorldBoss.applySafeMaxHealth(entity, cfg.hp());

        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(cfg.damage());
        }

        if (entity.getAttribute(Attribute.GENERIC_SCALE) != null) {
            entity.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(cfg.scale());
        }

        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(cfg.moveSpeed());
        }

        // Nome customizado
        entity.customName(LegacyComponentSerializer.legacySection().deserialize(cfg.name()));
        entity.setCustomNameVisible(true);

        // Equipamento
        if (entity.getEquipment() != null) {
            Map<String, Material> equip = cfg.equipment();
            if (equip.containsKey("helmet"))
                entity.getEquipment().setHelmet(new ItemStack(equip.get("helmet")));
            if (equip.containsKey("chestplate"))
                entity.getEquipment().setChestplate(new ItemStack(equip.get("chestplate")));
            if (equip.containsKey("leggings"))
                entity.getEquipment().setLeggings(new ItemStack(equip.get("leggings")));
            if (equip.containsKey("boots"))
                entity.getEquipment().setBoots(new ItemStack(equip.get("boots")));
            if (equip.containsKey("mainhand"))
                entity.getEquipment().setItemInMainHand(new ItemStack(equip.get("mainhand")));

            // Não dropar equipamento
            entity.getEquipment().setHelmetDropChance(0f);
            entity.getEquipment().setChestplateDropChance(0f);
            entity.getEquipment().setLeggingsDropChance(0f);
            entity.getEquipment().setBootsDropChance(0f);
            entity.getEquipment().setItemInMainHandDropChance(0f);
        }

        // Efeitos passivos
        for (String passiveStr : cfg.passiveEffects()) {
            String[] parts = passiveStr.split(":");
            if (parts.length >= 3) {
                PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(parts[0].trim().toLowerCase()));
                if (type != null) {
                    int duration = Integer.parseInt(parts[1].trim());
                    int amplifier = Integer.parseInt(parts[2].trim());
                    entity.addPotionEffect(new PotionEffect(type, duration, amplifier));
                }
            }
        }

        // Efeito visual: Glowing
        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));

        // Metadata para identificação
        entity.setMetadata("miniboss_id", new FixedMetadataValue(plugin, instanceId));

        miniBoss.setEntity(entity);
        activeMiniBosses.put(instanceId, miniBoss);

        // Partículas de spawn
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0.01);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.5f);

        // Broadcast local (100 blocos)
        String msg = cfg.spawnMessage();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distanceSquared(loc) <= 10000) {
                p.sendMessage(msg);
            }
        }

        plugin.getLogger().info("[MiniBoss] Spawnou '" + configId + "' em " +
                loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                + ")");

        return miniBoss;
    }

    /**
     * Encontra uma posição segura para spawn ao redor do jogador.
     */
    private Location findSafeSpawnLocation(Location center) {
        World world = center.getWorld();
        if (world == null)
            return null;

        for (int attempts = 0; attempts < 10; attempts++) {
            double distance = playerMinRadius + random.nextDouble() * (playerSpawnRadius - playerMinRadius);
            double angle = random.nextDouble() * 2 * Math.PI;

            int x = center.getBlockX() + (int) (distance * Math.cos(angle));
            int z = center.getBlockZ() + (int) (distance * Math.sin(angle));
            int y = world.getHighestBlockYAt(x, z) + 1;

            Location loc = new Location(world, x + 0.5, y, z + 0.5);

            // Verificar se o bloco é sólido embaixo e ar em cima
            if (loc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()
                    && loc.getBlock().getType().isAir()
                    && loc.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                return loc;
            }
        }
        return null;
    }

    // ================= SKILLS =================

    /**
     * Executa habilidades do mini-boss baseado em cooldowns e proximidade.
     */
    public void executeSkills(MiniBoss miniBoss) {
        if (!miniBoss.isAlive())
            return;

        LivingEntity entity = miniBoss.getEntity();
        MiniBossConfig cfg = configuredBosses.get(miniBoss.getConfigId());
        if (cfg == null || cfg.section() == null)
            return;

        ConfigurationSection skillsSection = cfg.section().getConfigurationSection("skills");
        if (skillsSection == null)
            return;

        long now = System.currentTimeMillis();
        Map<String, Long> cooldowns = skillCooldowns.computeIfAbsent(miniBoss.getId(), k -> new ConcurrentHashMap<>());

        for (String skillName : skillsSection.getKeys(false)) {
            ConfigurationSection skill = skillsSection.getConfigurationSection(skillName);
            if (skill == null)
                continue;

            long cooldown = skill.getLong("cooldown", 10000);
            Long lastUsed = cooldowns.get(skillName);
            if (lastUsed != null && (now - lastUsed) < cooldown)
                continue;

            // Verificar se há jogadores no raio
            double radius = skill.getDouble("radius", 10.0);
            boolean hasPlayers = false;
            for (Entity nearby : entity.getNearbyEntities(radius, radius, radius)) {
                if (nearby instanceof Player) {
                    hasPlayers = true;
                    break;
                }
            }
            if (!hasPlayers)
                continue;

            // Executar a skill
            executeSkill(miniBoss, skillName, skill);
            cooldowns.put(skillName, now);
            break; // Uma skill por tick
        }
    }

    /**
     * Executa uma habilidade específica.
     */
    private void executeSkill(MiniBoss miniBoss, String skillName, ConfigurationSection skill) {
        LivingEntity entity = miniBoss.getEntity();
        String message = skill.getString("message", "");
        if (!message.isEmpty()) {
            for (Entity e : entity.getNearbyEntities(40, 40, 40)) {
                if (e instanceof Player p)
                    p.sendMessage(message.replace("&", "§"));
            }
        }

        switch (skillName) {
            case "sand_storm" -> executeSandStorm(miniBoss, skill);
            case "frost_nova" -> executeFrostNova(miniBoss, skill);
            case "ice_arrows" -> executeIceArrows(miniBoss, skill);
            case "web_trap" -> executeWebTrap(miniBoss, skill);
            case "poison_cloud" -> executePoisonCloud(miniBoss, skill);
            case "fire_rain" -> executeFireRain(miniBoss, skill);
            case "flame_shield" -> executeFlameShield(miniBoss, skill);
            case "summon_minions" -> {
                double hpThreshold = skill.getDouble("hp_threshold", 0.5);
                if (entity.getHealth() / miniBoss.getMaxHealth() <= hpThreshold) {
                    executeSummonMinions(miniBoss, skill);
                }
            }
        }
    }

    private void executeSandStorm(MiniBoss boss, ConfigurationSection skill) {
        LivingEntity entity = boss.getEntity();
        double radius = skill.getDouble("radius", 10.0);
        double damage = skill.getDouble("damage", 4.0);

        entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, 2, 0), 60,
                radius / 2, 2, radius / 2, 0.1,
                new Particle.DustOptions(Color.fromRGB(210, 180, 100), 2.0f));
        entity.getWorld().playSound(entity.getLocation(), Sound.WEATHER_RAIN, 2.0f, 0.5f);

        for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
            }
        }
    }

    private void executeFrostNova(MiniBoss boss, ConfigurationSection skill) {
        LivingEntity entity = boss.getEntity();
        double radius = skill.getDouble("radius", 12.0);
        double damage = skill.getDouble("damage", 5.0);

        entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 1, 0), 80,
                radius / 2, 2, radius / 2, 0.1);
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);

        for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 1));
            }
        }
    }

    private void executeIceArrows(MiniBoss boss, ConfigurationSection skill) {
        LivingEntity entity = boss.getEntity();
        int count = skill.getInt("count", 5);
        double damage = skill.getDouble("damage", 4.0);

        List<Player> targets = new ArrayList<>();
        for (Entity e : entity.getNearbyEntities(30, 30, 30)) {
            if (e instanceof Player p)
                targets.add(p);
        }

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ARROW_SHOOT, 2.0f, 0.5f);
        for (int i = 0; i < Math.min(count, targets.size()); i++) {
            Player p = targets.get(i);
            org.bukkit.util.Vector dir = p.getLocation().toVector()
                    .subtract(entity.getLocation().toVector()).normalize();
            Arrow arrow = entity.getWorld().spawnArrow(
                    entity.getLocation().add(0, 1.5, 0), dir, 2.0f, 0f);
            arrow.setShooter(entity);
            arrow.setDamage(damage);
        }
    }

    private void executeWebTrap(MiniBoss boss, ConfigurationSection skill) {
        LivingEntity entity = boss.getEntity();
        double radius = skill.getDouble("radius", 8.0);
        int webCount = skill.getInt("web_count", 12);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 2.0f, 0.5f);

        for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p) {
                // Colocar webs temporárias ao redor do jogador (removidas depois)
                Location pLoc = p.getLocation();
                List<Location> webLocations = new ArrayList<>();
                for (int i = 0; i < webCount / 3; i++) {
                    int dx = random.nextInt(3) - 1;
                    int dz = random.nextInt(3) - 1;
                    Location webLoc = pLoc.clone().add(dx, 0, dz);
                    if (webLoc.getBlock().getType().isAir()) {
                        webLoc.getBlock().setType(Material.COBWEB);
                        webLocations.add(webLoc);
                    }
                }
                // Remover webs após 3 segundos
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Location wl : webLocations) {
                        if (wl.getBlock().getType() == Material.COBWEB) {
                            wl.getBlock().setType(Material.AIR);
                        }
                    }
                }, 60L);
            }
        }
    }

    private void executePoisonCloud(MiniBoss boss, ConfigurationSection skill) {
        LivingEntity entity = boss.getEntity();
        double radius = skill.getDouble("radius", 6.0);
        double damage = skill.getDouble("damage", 3.0);

        entity.getWorld().spawnParticle(Particle.ITEM_SLIME, entity.getLocation().add(0, 1, 0), 40,
                radius / 2, 1, radius / 2, 0.05);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 2.0f, 0.5f);

        for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p) {
                p.damage(damage, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
            }
        }
    }

    private void executeFireRain(MiniBoss boss, ConfigurationSection skill) {
        LivingEntity entity = boss.getEntity();
        double radius = skill.getDouble("radius", 14.0);
        int fireballs = skill.getInt("fireball_count", 8);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.5f);

        for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p) {
                for (int i = 0; i < fireballs / 2; i++) {
                    Location fireLoc = p.getLocation().add(
                            (random.nextDouble() - 0.5) * 6, 10, (random.nextDouble() - 0.5) * 6);
                    SmallFireball fireball = entity.getWorld().spawn(fireLoc, SmallFireball.class);
                    fireball.setShooter(entity);
                    fireball.setDirection(new org.bukkit.util.Vector(0, -1, 0));
                    fireball.setIsIncendiary(false);
                }
            }
        }
    }

    private void executeFlameShield(MiniBoss boss, ConfigurationSection skill) {
        LivingEntity entity = boss.getEntity();
        int duration = skill.getInt("duration", 100);
        int level = skill.getInt("resistance_level", 2);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, level));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0));
        entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1, 0), 40,
                0.8, 1, 0.8, 0.05);
        entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_FIRECHARGE_USE, 2.0f, 0.5f);
    }

    private void executeSummonMinions(MiniBoss boss, ConfigurationSection skill) {
        LivingEntity entity = boss.getEntity();
        int count = skill.getInt("count", 3);
        String typeName = skill.getString("type", "ZOMBIE");
        String minionName = skill.getString("name", "§7Servo").replace("&", "§");
        EntityType minionType;
        try {
            minionType = EntityType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            minionType = EntityType.ZOMBIE;
        }

        for (int i = 0; i < count; i++) {
            Location spawnLoc = entity.getLocation().add(
                    (random.nextDouble() - 0.5) * 8, 0, (random.nextDouble() - 0.5) * 8);
            LivingEntity minion = (LivingEntity) entity.getWorld().spawnEntity(spawnLoc, minionType);
            minion.customName(LegacyComponentSerializer.legacySection().deserialize(minionName));
            minion.setCustomNameVisible(true);
            minion.setMetadata("miniboss_minion",
                    new FixedMetadataValue(plugin, boss.getId()));
        }
    }

    // ================= GERENCIAMENTO =================

    /**
     * Remove um mini-boss ativo.
     */
    public void removeMiniBoss(String instanceId) {
        MiniBoss boss = activeMiniBosses.remove(instanceId);
        if (boss != null) {
            boss.remove();
            skillCooldowns.remove(instanceId);
        }
    }

    /**
     * Remove todos os mini-bosses ativos.
     */
    public int removeAll() {
        int count = activeMiniBosses.size();
        for (MiniBoss boss : activeMiniBosses.values()) {
            boss.remove();
        }
        activeMiniBosses.clear();
        skillCooldowns.clear();
        return count;
    }

    /**
     * Obtém um mini-boss pela entidade.
     */
    public MiniBoss getByEntity(LivingEntity entity) {
        if (!entity.hasMetadata("miniboss_id"))
            return null;
        String id = entity.getMetadata("miniboss_id").get(0).asString();
        return activeMiniBosses.get(id);
    }

    /**
     * Verifica se um entity é um minion de mini-boss.
     */
    public boolean isMinion(Entity entity) {
        return entity.hasMetadata("miniboss_minion");
    }

    // ================= TASKS =================

    /**
     * Inicia as tarefas periódicas (spawn natural e cleanup).
     */
    private void startTasks() {
        // Task de spawn natural
        if (spawnTask != null)
            spawnTask.cancel();
        spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tryNaturalSpawn,
                spawnInterval, spawnInterval);

        // Task de skills e cleanup (a cada 2 segundos)
        if (cleanupTask != null)
            cleanupTask.cancel();
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            long despawnMs = (despawnTime / 20) * 1000L; // Converter ticks para ms

            Iterator<Map.Entry<String, MiniBoss>> it = activeMiniBosses.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, MiniBoss> entry = it.next();
                MiniBoss boss = entry.getValue();

                // Remover bosses mortos ou inválidos
                if (!boss.isAlive()) {
                    it.remove();
                    skillCooldowns.remove(entry.getKey());
                    continue;
                }

                // Despawn por inatividade (sem dano recebido)
                if ((now - boss.getLastDamageTimestamp()) > despawnMs) {
                    boss.remove();
                    it.remove();
                    skillCooldowns.remove(entry.getKey());
                    continue;
                }

                // Atualizar nome com HP
                updateNameTag(boss);

                // Executar habilidades
                executeSkills(boss);
            }
        }, 40L, 40L);
    }

    /**
     * Atualiza o nome visível com o HP atual.
     */
    private void updateNameTag(MiniBoss boss) {
        if (!boss.isAlive())
            return;
        LivingEntity entity = boss.getEntity();
        double hp = entity.getHealth();
        double maxHp = boss.getMaxHealth();
        double percentage = (hp / maxHp) * 100;

        String color;
        if (percentage > 50)
            color = "§a";
        else if (percentage > 25)
            color = "§e";
        else
            color = "§c";

        entity.customName(LegacyComponentSerializer.legacySection().deserialize(
                boss.getName() + " " + color + String.format("%.0f", hp) + "§7/§f"
                        + String.format("%.0f", maxHp) + " §c❤"));
    }

    // ================= RELOAD =================

    /**
     * Recarrega as configurações sem remover mini-bosses ativos.
     */
    public void reload() {
        loadConfig();
        // Reiniciar tasks com novo intervalo
        startTasks();
        plugin.getLogger()
                .info("[MiniBoss] Configurações recarregadas. " + configuredBosses.size() + " tipos configurados.");
    }

    // ================= CLEANUP =================

    /**
     * Limpa todos os recursos no onDisable.
     */
    public void cleanup() {
        if (spawnTask != null)
            spawnTask.cancel();
        if (cleanupTask != null)
            cleanupTask.cancel();
        removeAll();
    }

    // ================= GETTERS =================

    public Map<String, MiniBoss> getActiveMiniBosses() {
        return Collections.unmodifiableMap(activeMiniBosses);
    }

    public Map<String, MiniBossConfig> getConfiguredBosses() {
        return Collections.unmodifiableMap(configuredBosses);
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ================= RECORD DE CONFIGURAÇÃO =================

    /**
     * Configuração de um tipo de mini-boss (carregada do YAML).
     */
    public record MiniBossConfig(
            String id, String name, EntityType entityType,
            double hp, double damage, double scale, double moveSpeed,
            double moneyReward, int xpReward,
            List<String> biomes, Map<String, Material> equipment,
            List<MiniBoss.EffectOnHit> effectsOnHit, List<String> passiveEffects,
            List<MiniBoss.LootEntry> loot,
            String spawnMessage, String deathMessage,
            ConfigurationSection section) {
    }
}
