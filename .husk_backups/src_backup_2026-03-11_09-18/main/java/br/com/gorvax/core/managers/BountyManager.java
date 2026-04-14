package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B17.3 — Sistema de Bounties (recompensas por PvP).
 * Permite colocar bounties na cabeça de jogadores.
 * Bounties acumulam valor de múltiplos apostadores.
 * Persistência em bounties.yml com save assíncrono.
 */
public class BountyManager {

    private final GorvaxCore plugin;
    private final File bountiesFile;
    private final Map<UUID, Bounty> activeBounties = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final double minValue;
    private final double maxValue;
    private final boolean sameKingdomAllowed;

    public BountyManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.bountiesFile = new File(plugin.getDataFolder(), "bounties.yml");
        this.enabled = plugin.getConfig().getBoolean("bounty.enabled", true);
        this.minValue = plugin.getConfig().getDouble("bounty.min_value", 100.0);
        this.maxValue = plugin.getConfig().getDouble("bounty.max_value", 1000000.0);
        this.sameKingdomAllowed = plugin.getConfig().getBoolean("bounty.same_kingdom_allowed", false);
        loadBounties();

        // Save assíncrono a cada 5 minutos
        if (enabled) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveBounties, 6000L, 6000L);
        }
    }

    // --- Modelo de Dados ---

    public static class Bounty {
        public final UUID targetUUID;
        public final String targetName;
        public double totalValue;
        public final Map<UUID, Double> contributors; // quem colocou quanto
        public long lastUpdated;

        public Bounty(UUID targetUUID, String targetName) {
            this.targetUUID = targetUUID;
            this.targetName = targetName;
            this.totalValue = 0;
            this.contributors = new ConcurrentHashMap<>();
            this.lastUpdated = System.currentTimeMillis();
        }

        public String getFormattedDate() {
            return new SimpleDateFormat("dd/MM HH:mm").format(new Date(lastUpdated));
        }
    }

    // --- API Pública ---

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Adiciona bounty na cabeça de um jogador.
     * Se já existe bounty, o valor é acumulado.
     * 
     * @return 0=sucesso, 1=desabilitado, 2=valor inválido, 3=mesmo reino, 4=si mesmo
     */
    public int placeBounty(UUID placerUUID, UUID targetUUID, String targetName, double value) {
        if (!enabled) return 1;
        if (value < minValue || value > maxValue) return 2;
        if (placerUUID.equals(targetUUID)) return 4;

        // Verificar mesmo reino (se configurado para bloquear)
        if (!sameKingdomAllowed) {
            var km = plugin.getKingdomManager();
            Claim placerKingdom = km.getKingdom(placerUUID);
            Claim targetKingdom = km.getKingdom(targetUUID);
            if (placerKingdom != null && targetKingdom != null
                    && placerKingdom.getId().equals(targetKingdom.getId())) return 3;
        }

        Bounty bounty = activeBounties.computeIfAbsent(targetUUID,
                k -> new Bounty(targetUUID, targetName));

        bounty.totalValue += value;
        bounty.contributors.merge(placerUUID, value, Double::sum);
        bounty.lastUpdated = System.currentTimeMillis();

        return 0;
    }

    /**
     * Resolve uma bounty (quando o alvo é morto por outro jogador).
     * 
     * @return o valor total da bounty, ou 0 se não havia bounty
     */
    public double resolveBounty(UUID targetUUID) {
        Bounty bounty = activeBounties.remove(targetUUID);
        return bounty != null ? bounty.totalValue : 0;
    }

    /**
     * Retorna a bounty de um jogador, ou null.
     */
    public Bounty getBounty(UUID targetUUID) {
        return activeBounties.get(targetUUID);
    }

    /**
     * Retorna todas as bounties ativas, ordenadas por valor (maior primeiro).
     */
    public List<Bounty> getAllBounties() {
        List<Bounty> sorted = new ArrayList<>(activeBounties.values());
        sorted.sort((a, b) -> Double.compare(b.totalValue, a.totalValue));
        return sorted;
    }

    /**
     * Remove a contribuição de um jogador em uma bounty (sem reembolso).
     * 
     * @return true se removido com sucesso
     */
    public boolean removeContribution(UUID placerUUID, UUID targetUUID) {
        Bounty bounty = activeBounties.get(targetUUID);
        if (bounty == null) return false;

        Double contribution = bounty.contributors.remove(placerUUID);
        if (contribution == null) return false;

        bounty.totalValue -= contribution;
        if (bounty.totalValue <= 0 || bounty.contributors.isEmpty()) {
            activeBounties.remove(targetUUID);
        }
        return true;
    }

    /**
     * Retorna o valor da bounty de um jogador (para PAPI).
     */
    public double getBountyValue(UUID targetUUID) {
        Bounty bounty = activeBounties.get(targetUUID);
        return bounty != null ? bounty.totalValue : 0;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    // --- Persistência ---

    private void loadBounties() {
        if (!bountiesFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(bountiesFile);
        ConfigurationSection bountiesSection = yaml.getConfigurationSection("bounties");
        if (bountiesSection == null) return;

        for (String uuidStr : bountiesSection.getKeys(false)) {
            try {
                UUID targetUUID = UUID.fromString(uuidStr);
                ConfigurationSection section = bountiesSection.getConfigurationSection(uuidStr);
                if (section == null) continue;

                String targetName = section.getString("name", "Desconhecido");
                Bounty bounty = new Bounty(targetUUID, targetName);
                bounty.totalValue = section.getDouble("total", 0);
                bounty.lastUpdated = section.getLong("last_updated", System.currentTimeMillis());

                ConfigurationSection contribs = section.getConfigurationSection("contributors");
                if (contribs != null) {
                    for (String contribUUID : contribs.getKeys(false)) {
                        bounty.contributors.put(UUID.fromString(contribUUID),
                                contribs.getDouble(contribUUID));
                    }
                }

                if (bounty.totalValue > 0) {
                    activeBounties.put(targetUUID, bounty);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[BountyManager] UUID inválido no bounties.yml: " + uuidStr);
            }
        }

        plugin.getLogger().info("[BountyManager] Carregadas " + activeBounties.size() + " bounties ativas.");
    }

    private synchronized void saveBounties() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<UUID, Bounty> entry : activeBounties.entrySet()) {
            Bounty bounty = entry.getValue();
            String path = "bounties." + entry.getKey().toString();
            yaml.set(path + ".name", bounty.targetName);
            yaml.set(path + ".total", bounty.totalValue);
            yaml.set(path + ".last_updated", bounty.lastUpdated);

            for (Map.Entry<UUID, Double> contrib : bounty.contributors.entrySet()) {
                yaml.set(path + ".contributors." + contrib.getKey().toString(), contrib.getValue());
            }
        }

        try {
            yaml.save(bountiesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[BountyManager] Erro ao salvar bounties.yml: " + e.getMessage());
        }
    }

    /**
     * Save síncrono (chamado no onDisable).
     */
    public void saveSync() {
        saveBounties();
    }

    public void reload() {
        activeBounties.clear();
        loadBounties();
    }
}
