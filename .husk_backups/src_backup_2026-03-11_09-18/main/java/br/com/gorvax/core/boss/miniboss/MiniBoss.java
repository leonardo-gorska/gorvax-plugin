package br.com.gorvax.core.boss.miniboss;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modelo de dados para um mini-boss ativo no mundo.
 * Diferente de WorldBoss, mini-bosses não têm BossBar global,
 * fases complexas ou atmosferas — são combates menores e rápidos.
 */
public class MiniBoss {

    private final String id;              // ID único desta instância
    private final String configId;        // ID no mini_bosses.yml (ex: guardiao_deserto)
    private final String name;            // Nome com cores
    private final double maxHealth;
    private final double damage;
    private final double scale;
    private final double movementSpeed;
    private final double moneyReward;
    private final int xpReward;
    private final EntityType entityType;
    private final List<String> biomes;
    private final Map<String, Material> equipment;
    private final List<EffectOnHit> effectsOnHit;
    private final List<String> passiveEffects;
    private final List<LootEntry> loot;
    private final String spawnMessage;
    private final String deathMessage;

    private LivingEntity entity;
    private final Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
    private long lastDamageTimestamp;
    private long spawnTimestamp;

    public MiniBoss(String id, String configId, String name, double maxHealth, double damage,
                    double scale, double movementSpeed, double moneyReward, int xpReward,
                    EntityType entityType, List<String> biomes,
                    Map<String, Material> equipment, List<EffectOnHit> effectsOnHit,
                    List<String> passiveEffects, List<LootEntry> loot,
                    String spawnMessage, String deathMessage) {
        this.id = id;
        this.configId = configId;
        this.name = name;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.scale = scale;
        this.movementSpeed = movementSpeed;
        this.moneyReward = moneyReward;
        this.xpReward = xpReward;
        this.entityType = entityType;
        this.biomes = biomes;
        this.equipment = equipment;
        this.effectsOnHit = effectsOnHit;
        this.passiveEffects = passiveEffects;
        this.loot = loot;
        this.spawnMessage = spawnMessage;
        this.deathMessage = deathMessage;
        this.spawnTimestamp = System.currentTimeMillis();
        this.lastDamageTimestamp = System.currentTimeMillis();
    }

    // ================= GETTERS =================

    public String getId() { return id; }
    public String getConfigId() { return configId; }
    public String getName() { return name; }
    public double getMaxHealth() { return maxHealth; }
    public double getDamage() { return damage; }
    public double getScale() { return scale; }
    public double getMovementSpeed() { return movementSpeed; }
    public double getMoneyReward() { return moneyReward; }
    public int getXpReward() { return xpReward; }
    public EntityType getEntityType() { return entityType; }
    public List<String> getBiomes() { return biomes; }
    public Map<String, Material> getEquipment() { return equipment; }
    public List<EffectOnHit> getEffectsOnHit() { return effectsOnHit; }
    public List<String> getPassiveEffects() { return passiveEffects; }
    public List<LootEntry> getLoot() { return loot; }
    public String getSpawnMessage() { return spawnMessage; }
    public String getDeathMessage() { return deathMessage; }
    public LivingEntity getEntity() { return entity; }
    public Map<UUID, Double> getDamageDealt() { return damageDealt; }
    public long getLastDamageTimestamp() { return lastDamageTimestamp; }
    public long getSpawnTimestamp() { return spawnTimestamp; }

    // ================= SETTERS =================

    public void setEntity(LivingEntity entity) { this.entity = entity; }
    public void setLastDamageTimestamp(long ts) { this.lastDamageTimestamp = ts; }

    /**
     * Registra dano de um jogador.
     */
    public void addDamage(UUID playerId, double amount) {
        damageDealt.merge(playerId, amount, Double::sum);
        this.lastDamageTimestamp = System.currentTimeMillis();
    }

    /**
     * Obtém o jogador com mais dano causado (top killer).
     */
    public UUID getTopDamager() {
        return damageDealt.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Verifica se a entidade é válida e está viva.
     */
    public boolean isAlive() {
        return entity != null && entity.isValid() && !entity.isDead();
    }

    /**
     * Remove a entidade do mundo.
     */
    public void remove() {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    // ================= MODELOS INTERNOS =================

    /**
     * Efeito aplicado ao atingir um jogador.
     */
    public record EffectOnHit(PotionEffectType type, int duration, int amplifier) {}

    /**
     * Entrada de loot com chance de drop.
     */
    public record LootEntry(Material material, int amount, double chance) {}
}
