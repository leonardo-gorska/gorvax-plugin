package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B13 — Manager central de cosméticos (partículas, trails, tags, kill effects).
 * Carrega cosmetics.yml, gerencia cosméticos ativos e renderiza partículas.
 */
public class CosmeticManager {

    // Tipos de cosmético suportados
    public enum CosmeticType {
        WALK_PARTICLE,
        KILL_PARTICLE,
        ARROW_TRAIL,
        CHAT_TAG,
        KILL_EFFECT
    }

    // Registro de um cosmético configurado
    public record CosmeticEntry(
            String id,
            CosmeticType type,
            String name,
            String description,
            String source,        // achievement, shop, crate, vip, admin
            String permission,    // permissão explícita (null = usa source)
            double price,         // preço na loja (se source=shop)
            // Dados de partícula
            Particle particle,
            int count,
            double speed,
            double offsetY,
            // Dados de tag
            String display,
            // Dados de kill effect
            String effect
    ) {}

    private final GorvaxCore plugin;
    private final Map<String, CosmeticEntry> registry = new LinkedHashMap<>();
    private File configFile;
    private FileConfiguration config;
    private BukkitTask particleTask;

    public CosmeticManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
        startParticleTask();
    }

    // ──────────────── Config ────────────────

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "cosmetics.yml");
        if (!configFile.exists()) {
            plugin.saveResource("cosmetics.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        parseCosmetics();
    }

    private void parseCosmetics() {
        registry.clear();
        ConfigurationSection section = config.getConfigurationSection("cosmetics");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection cs = section.getConfigurationSection(id);
            if (cs == null) continue;

            CosmeticType type;
            try {
                type = CosmeticType.valueOf(cs.getString("type", "WALK_PARTICLE").toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Cosméticos] Tipo inválido para '" + id + "': " + cs.getString("type"));
                continue;
            }

            // Partícula (pode ser null para tags/effects)
            Particle particle = null;
            String particleStr = cs.getString("particle", null);
            if (particleStr != null) {
                try {
                    particle = Particle.valueOf(particleStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[Cosméticos] Partícula inválida para '" + id + "': " + particleStr);
                    // Usa FLAME como fallback para tipos de partícula
                    if (type == CosmeticType.WALK_PARTICLE || type == CosmeticType.ARROW_TRAIL
                            || type == CosmeticType.KILL_PARTICLE) {
                        particle = Particle.FLAME;
                    }
                }
            }

            CosmeticEntry entry = new CosmeticEntry(
                    id,
                    type,
                    cs.getString("name", "§7" + id),
                    cs.getString("description", ""),
                    cs.getString("source", "admin"),
                    cs.getString("permission", null),
                    cs.getDouble("price", 0),
                    particle,
                    cs.getInt("count", 3),
                    cs.getDouble("speed", 0.02),
                    cs.getDouble("offset_y", 0.1),
                    cs.getString("display", null),
                    cs.getString("effect", null)
            );

            registry.put(id, entry);
        }

        plugin.getLogger().info("[GorvaxCore] " + registry.size() + " cosméticos carregados.");
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        parseCosmetics();
    }

    // ──────────────── Consultas ────────────────

    /**
     * Retorna o registro completo de cosméticos (read-only).
     */
    public Map<String, CosmeticEntry> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }

    /**
     * Retorna um cosmético pelo ID.
     */
    public CosmeticEntry getCosmetic(String id) {
        return registry.get(id);
    }

    /**
     * Retorna cosméticos disponíveis (desbloqueados) para um jogador.
     */
    public List<CosmeticEntry> getAvailableCosmetics(Player player) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        List<CosmeticEntry> available = new ArrayList<>();

        for (CosmeticEntry entry : registry.values()) {
            if (pd.hasCosmetic(entry.id())) {
                available.add(entry);
            } else if (entry.permission() != null && player.hasPermission(entry.permission())) {
                available.add(entry);
            }
        }
        return available;
    }

    /**
     * Retorna cosméticos disponíveis filtrados por tipo.
     */
    public List<CosmeticEntry> getAvailableByType(Player player, CosmeticType type) {
        return getAvailableCosmetics(player).stream()
                .filter(e -> e.type() == type)
                .toList();
    }

    /**
     * Retorna todos os cosméticos de um tipo (para GUI, mostrando bloqueados também).
     */
    public List<CosmeticEntry> getAllByType(CosmeticType type) {
        return registry.values().stream()
                .filter(e -> e.type() == type)
                .toList();
    }

    // ──────────────── Ativação / Desativação ────────────────

    /**
     * Ativa um cosmético para o jogador.
     * @return true se ativado com sucesso
     */
    public boolean activateCosmetic(Player player, String cosmeticId) {
        CosmeticEntry entry = registry.get(cosmeticId);
        if (entry == null) return false;

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

        // Verificar se desbloqueado
        boolean unlocked = pd.hasCosmetic(cosmeticId)
                || (entry.permission() != null && player.hasPermission(entry.permission()));
        if (!unlocked) return false;

        // Ativa (substitui o do mesmo tipo)
        pd.setActiveCosmetic(entry.type().name(), cosmeticId);
        plugin.getPlayerDataManager().saveData(player.getUniqueId());
        return true;
    }

    /**
     * Desativa o cosmético do tipo especificado.
     */
    public void deactivateCosmetic(Player player, String typeName) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        pd.clearActiveCosmetic(typeName);
        plugin.getPlayerDataManager().saveData(player.getUniqueId());
    }

    /**
     * Retorna o cosmético ativo de um tipo para um jogador, ou null.
     */
    public CosmeticEntry getActiveCosmetic(Player player, CosmeticType type) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        String activeId = pd.getActiveCosmetics().get(type.name());
        if (activeId == null) return null;
        return registry.get(activeId);
    }

    /**
     * Retorna o display da tag ativa do jogador, ou string vazia.
     */
    public String getActiveChatTag(Player player) {
        CosmeticEntry tag = getActiveCosmetic(player, CosmeticType.CHAT_TAG);
        return tag != null && tag.display() != null ? tag.display() : "";
    }

    // ──────────────── Desbloqueio ────────────────

    /**
     * Desbloqueia um cosmético para o jogador.
     */
    public void unlockCosmetic(Player player, String cosmeticId) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        pd.unlockCosmetic(cosmeticId);
        plugin.getPlayerDataManager().saveData(player.getUniqueId());
    }

    /**
     * Verifica se o jogador já desbloqueou o cosmético.
     */
    public boolean isUnlocked(Player player, String cosmeticId) {
        CosmeticEntry entry = registry.get(cosmeticId);
        if (entry == null) return false;

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        return pd.hasCosmetic(cosmeticId)
                || (entry.permission() != null && player.hasPermission(entry.permission()));
    }

    // ──────────────── Partículas Walk ────────────────

    /**
     * Task periódica que renderiza partículas de caminhada para todos os jogadores online.
     */
    private void startParticleTask() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    // Só renderiza se o jogador está se movendo (velocidade > 0.05)
                    if (player.getVelocity().lengthSquared() < 0.003) continue;

                    CosmeticEntry walkCos = getActiveCosmetic(player, CosmeticType.WALK_PARTICLE);
                    if (walkCos == null || walkCos.particle() == null) continue;

                    Location loc = player.getLocation().add(0, walkCos.offsetY(), 0);
                    player.getWorld().spawnParticle(
                            walkCos.particle(),
                            loc,
                            walkCos.count(),
                            0.2, 0.0, 0.2,    // offsetX, offsetY, offsetZ
                            walkCos.speed()
                    );
                }
            }
        }.runTaskTimer(plugin, 20L, 4L); // A cada 4 ticks (~200ms)
    }

    /**
     * Desliga a task de partículas (usado no onDisable).
     */
    public void shutdown() {
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }
    }
}
