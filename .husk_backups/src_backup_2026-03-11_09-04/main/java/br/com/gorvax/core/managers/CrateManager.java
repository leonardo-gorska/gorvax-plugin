package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * B12 — Gerenciador do sistema de Crates / Keys.
 * Carrega tipos de crate de crates.yml, gerencia keys dos jogadores
 * e executa a lógica de abertura (sorteio por peso).
 */
public class CrateManager {

    private final GorvaxCore plugin;
    private final Map<String, CrateType> crateTypes = new ConcurrentHashMap<>();
    /** Location key "world,x,y,z" → crateId for physical crates at spawn */
    private final Map<String, String> physicalCrates = new ConcurrentHashMap<>();
    private File crateFile;

    public CrateManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadCrates();
    }

    // --- Records internos ---

    /** Definição de um tipo de crate. */
    public record CrateType(
            String id,
            String name,
            Material icon,
            String color,
            boolean broadcastOnOpen,
            List<CrateReward> rewards,
            int totalWeight) {
    }

    /** Uma recompensa possível dentro de uma crate. */
    public record CrateReward(
            String type, // money, claim_blocks, item, custom_item, title, crate_key
            double amount, // valor numérico (dinheiro, blocos, quantidade)
            int weight, // peso para sorteio
            String display, // nome exibido na GUI/chat
            Material material, // para tipo "item"
            String itemId, // para tipo "custom_item"
            String title, // para tipo "title"
            String crateKeyType // para tipo "crate_key"
    ) {
    }

    // --- Carregamento ---

    /** Carrega (ou recarrega) todos os tipos de crate do crates.yml. */
    public void loadCrates() {
        crateTypes.clear();

        crateFile = new File(plugin.getDataFolder(), "crates.yml");
        if (!crateFile.exists()) {
            plugin.saveResource("crates.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        ConfigurationSection cratesSection = config.getConfigurationSection("crates");
        if (cratesSection == null) {
            plugin.getLogger().warning("[Crates] Seção 'crates' não encontrada em crates.yml!");
            return;
        }

        for (String crateId : cratesSection.getKeys(false)) {
            ConfigurationSection sec = cratesSection.getConfigurationSection(crateId);
            if (sec == null)
                continue;

            String name = sec.getString("name", "§fCrate");
            Material icon = Material.CHEST;
            try {
                icon = Material.valueOf(sec.getString("icon", "CHEST").toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger()
                        .warning("[Crates] Material inválido para crate '" + crateId + "': " + sec.getString("icon"));
            }
            String color = sec.getString("color", "§f");
            boolean broadcast = sec.getBoolean("broadcast_on_open", false);

            List<CrateReward> rewards = new ArrayList<>();
            int totalWeight = 0;

            List<?> rewardList = sec.getList("rewards");
            if (rewardList != null) {
                for (Object obj : rewardList) {
                    if (obj instanceof Map<?, ?> map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> rewardMap = (Map<String, Object>) map;

                        String type = String.valueOf(rewardMap.getOrDefault("type", "money"));
                        double amount = rewardMap.containsKey("amount")
                                ? ((Number) rewardMap.get("amount")).doubleValue()
                                : 0;
                        int weight = rewardMap.containsKey("weight")
                                ? ((Number) rewardMap.get("weight")).intValue()
                                : 1;
                        String display = String.valueOf(rewardMap.getOrDefault("display", "§fRecompensa"));

                        Material mat = null;
                        if (rewardMap.containsKey("material")) {
                            try {
                                mat = Material.valueOf(String.valueOf(rewardMap.get("material")).toUpperCase());
                            } catch (IllegalArgumentException ignored) {
                            }
                        }

                        String itemId = rewardMap.containsKey("item_id")
                                ? String.valueOf(rewardMap.get("item_id"))
                                : null;
                        String rewardTitle = rewardMap.containsKey("title")
                                ? String.valueOf(rewardMap.get("title"))
                                : null;
                        String crateKeyType = rewardMap.containsKey("crate_type")
                                ? String.valueOf(rewardMap.get("crate_type"))
                                : null;

                        rewards.add(
                                new CrateReward(type, amount, weight, display, mat, itemId, rewardTitle, crateKeyType));
                        totalWeight += weight;
                    }
                }
            }

            crateTypes.put(crateId, new CrateType(crateId, name, icon, color, broadcast, rewards, totalWeight));
        }

        plugin.getLogger().info("[Crates] " + crateTypes.size() + " tipos de crate carregados.");

        // Carregar physical crates (coordenadas de blocos no spawn)
        physicalCrates.clear();
        List<?> physList = config.getList("physical_crates");
        if (physList != null) {
            for (Object obj : physList) {
                if (obj instanceof Map<?, ?> map) {
                    String type = String.valueOf(map.get("type"));
                    String world = String.valueOf(map.get("world"));
                    int x = ((Number) map.get("x")).intValue();
                    int y = ((Number) map.get("y")).intValue();
                    int z = ((Number) map.get("z")).intValue();
                    physicalCrates.put(world + "," + x + "," + y + "," + z, type);
                }
            }
            plugin.getLogger().info("[Crates] " + physicalCrates.size() + " crates físicas carregadas.");
        }
    }

    // --- Operações de Keys ---

    /**
     * Dá keys de um tipo de crate para um jogador.
     */
    public void giveKey(Player player, String crateId, int amount) {
        if (!crateTypes.containsKey(crateId))
            return;

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        pd.addCrateKey(crateId, amount);
        plugin.getPlayerDataManager().saveData(player.getUniqueId());
    }

    /**
     * Remove 1 key de um tipo de crate do jogador.
     * 
     * @return true se tinha key e foi removida
     */
    public boolean removeKey(Player player, String crateId) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        return pd.removeCrateKey(crateId);
    }

    /**
     * Retorna a quantidade de keys que o jogador possui de um tipo.
     */
    public int getKeyCount(Player player, String crateId) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        return pd.getCrateKeys().getOrDefault(crateId, 0);
    }

    // --- Sorteio e abertura ---

    /**
     * Sorteia uma recompensa aleatória de um tipo de crate usando weighted random.
     */
    public CrateReward getRandomReward(String crateId) {
        CrateType crate = crateTypes.get(crateId);
        if (crate == null || crate.rewards().isEmpty())
            return null;

        int roll = ThreadLocalRandom.current().nextInt(crate.totalWeight());
        int cumulative = 0;
        for (CrateReward reward : crate.rewards()) {
            cumulative += reward.weight();
            if (roll < cumulative) {
                return reward;
            }
        }
        // Fallback (nunca deve chegar aqui)
        return crate.rewards().get(crate.rewards().size() - 1);
    }

    /**
     * Aplica a recompensa sorteada ao jogador.
     */
    public void applyReward(Player player, CrateReward reward) {
        var msg = plugin.getMessageManager();

        switch (reward.type()) {
            case "money":
                if (GorvaxCore.getEconomy() != null) {
                    GorvaxCore.getEconomy().depositPlayer(player, reward.amount());
                }
                break;

            case "claim_blocks":
                PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
                pd.addClaimBlocks((int) reward.amount());
                break;

            case "item":
                if (reward.material() != null) {
                    ItemStack item = new ItemStack(reward.material(), (int) reward.amount());
                    // Se inventário cheio, dropa no chão
                    var leftover = player.getInventory().addItem(item);
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
                break;

            case "custom_item":
                if (reward.itemId() != null && plugin.getCustomItemManager() != null) {
                    ItemStack customItem = plugin.getCustomItemManager().getItem(reward.itemId());
                    if (customItem != null) {
                        var leftover = player.getInventory().addItem(customItem);
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                }
                break;

            case "title":
                if (reward.title() != null) {
                    PlayerData tpd = plugin.getPlayerDataManager().getData(player.getUniqueId());
                    tpd.addUnlockedTitle(reward.title());
                }
                break;

            case "crate_key":
                if (reward.crateKeyType() != null) {
                    giveKey(player, reward.crateKeyType(), (int) reward.amount());
                }
                break;
        }

        plugin.getPlayerDataManager().saveData(player.getUniqueId());
    }

    /**
     * Abre uma crate completa: valida key, consome, sorteia e aplica.
     * Retorna true se a abertura deve prosseguir com animação GUI.
     * Se false, já foi tratado (sem key, crate inválido, etc.).
     */
    public boolean openCrate(Player player, String crateId) {
        var msg = plugin.getMessageManager();

        CrateType crate = crateTypes.get(crateId);
        if (crate == null) {
            msg.send(player, "crate.invalid_type");
            return false;
        }

        int keys = getKeyCount(player, crateId);
        if (keys <= 0) {
            msg.send(player, "crate.no_keys", crate.name());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        // Consumir key
        removeKey(player, crateId);

        // Sortear reward
        CrateReward reward = getRandomReward(crateId);
        if (reward == null) {
            msg.send(player, "crate.error");
            return false;
        }

        // Aplica reward
        applyReward(player, reward);

        // Mensagem ao jogador
        msg.send(player, "crate.reward_received", reward.display());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        // Broadcast se lendário
        if (crate.broadcastOnOpen()) {
            String broadcast = msg.get("crate.broadcast_reward", player.getName(), crate.name(), reward.display());
            Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(broadcast));
        }

        return true;
    }

    // --- Getters ---

    public CrateType getCrateType(String id) {
        return crateTypes.get(id);
    }

    public Set<String> getCrateTypeIds() {
        return Collections.unmodifiableSet(crateTypes.keySet());
    }

    public Map<String, CrateType> getAllCrateTypes() {
        return Collections.unmodifiableMap(crateTypes);
    }

    /**
     * Retorna o tipo de crate associado a uma localização física, ou null.
     */
    public String getCrateTypeAtLocation(org.bukkit.Location loc) {
        String key = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        return physicalCrates.get(key);
    }

    /**
     * Retorna todas as localizações de crates físicas como objetos Location.
     */
    public java.util.List<org.bukkit.Location> getPhysicalCrateLocations() {
        java.util.List<org.bukkit.Location> locations = new java.util.ArrayList<>();
        for (String key : physicalCrates.keySet()) {
            String[] parts = key.split(",");
            if (parts.length != 4)
                continue;
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null)
                continue;
            try {
                locations.add(new org.bukkit.Location(world,
                        Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
            } catch (NumberFormatException ignored) {
            }
        }
        return locations;
    }

    /** Recarrega as crates (para /gorvax reload). */
    public void reload() {
        loadCrates();
    }
}
